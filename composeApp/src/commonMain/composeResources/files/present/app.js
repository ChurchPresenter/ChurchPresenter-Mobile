/*
 * Display client for the phone-hosted presentation page.
 *
 * One-way: the phone pushes a SlideEnvelope, this page renders it. There is no
 * reverse channel and no state kept beyond the last envelope applied, so the
 * display is fully reconstructable from a single message — which is why the
 * server replays the current slide to every new connection.
 *
 * Deliberately dependency-free and framework-free. It runs on whatever browser
 * a ten-year-old smart TV happens to ship.
 */
(function () {
  'use strict';

  var PROTOCOL_VERSION = 1;
  var RECONNECT_MIN_MS = 500;
  var RECONNECT_MAX_MS = 5000;

  var el = {
    backdrop: document.getElementById('backdrop'),
    scrim: document.getElementById('scrim'),
    screen: document.getElementById('screen'),
    content: document.getElementById('content'),
    body: document.getElementById('body'),
    reference: document.getElementById('reference'),
    footer: document.getElementById('footer'),
    brand: document.getElementById('brand'),
    clock: document.getElementById('clock'),
    standby: document.getElementById('standby'),
    standbyText: document.getElementById('standby-text'),
    web: document.getElementById('web'),
    video: document.getElementById('video')
  };

  // Envelopes may arrive out of order across a reconnect; anything not newer
  // than what is already on screen is dropped.
  var lastRevision = -1;
  var socket = null;
  var reconnectDelay = RECONNECT_MIN_MS;
  var showClock = false;

  // ── Rendering ──────────────────────────────────────────────────────────

  function setText(node, value) {
    var text = value == null ? '' : String(value);
    // textContent, never innerHTML: slide text is operator-authored and
    // arrives over an unauthenticated LAN socket. It is content, not markup.
    node.textContent = text;
    node.classList.toggle('hidden', text.length === 0);
  }

  // Only #RGB / #RRGGBB reaches a style property. The colours arrive over the
  // wire, and 'url(...)' in a background is a fetch the audience screen would make.
  function safeColor(value) {
    return (typeof value === 'string' && /^#[0-9a-fA-F]{3}([0-9a-fA-F]{3})?$/.test(value.trim()))
      ? value.trim() : null;
  }

  // Photos are served by the very server that served this page, but the URL is
  // minted from the address the phone answers on its Wi-Fi. A viewer who reached
  // this page by any other route — a second interface, a VPN, localhost on the
  // phone itself — cannot fetch that host, and the slide silently renders as
  // nothing. Re-point our own photo URLs at whatever origin this page came from,
  // which is by definition reachable: it is how you are reading this.
  function ownPhoto(url) {
    if (typeof url !== 'string') { return null; }
    try {
      var parsed = new URL(url, window.location.href); // offline-ok: parsing, not fetching
      if (parsed.pathname.indexOf('/photo/') === 0) { return parsed.pathname + parsed.search; }
    } catch (e) { /* not a URL we can reason about — send it on unchanged */ }
    return url;
  }

  function applyBackdrop(slide) {
    var kind = slide.backdrop || 'GRADIENT';
    var url = ownPhoto(slide.backdropUrl);
    if (kind === 'IMAGE' && url) {
      var imageTheme = slide.theme || {};
      var imageTop = safeColor(imageTheme.gradientTop);
      var imageBottom = safeColor(imageTheme.gradientBottom);
      // Keep the wash *underneath* the photo as a second background layer. If the
      // photo cannot be fetched the audience sees the gradient rather than a bare
      // screen, which is indistinguishable from the app having failed.
      var wash = (imageTop && imageBottom)
        ? 'linear-gradient(160deg, ' + imageTop + ' 0%, ' + imageBottom + ' 100%)'
        : 'linear-gradient(160deg, #2a1d5e 0%, #05060d 100%)';
      el.backdrop.className = '';
      el.backdrop.style.background = '';
      el.backdrop.style.backgroundImage = 'url("' + encodeURI(url) + '"), ' + wash;
      el.scrim.classList.remove('hidden');
    } else {
      el.backdrop.style.backgroundImage = '';
      el.backdrop.className = kind === 'BLACK' ? 'bd-black' : 'bd-gradient';
      var theme = slide.theme || {};
      var top = safeColor(theme.gradientTop);
      var bottom = safeColor(theme.gradientBottom);
      // The operator's own wash when they chose one; the stylesheet's otherwise.
      el.backdrop.style.background = (kind !== 'BLACK' && top && bottom)
        ? 'linear-gradient(160deg, ' + top + ' 0%, ' + bottom + ' 100%)'
        : '';
      el.scrim.classList.add('hidden');
    }
  }

  // Only http(s) reaches the iframe or the player. The address was typed by the
  // operator, but 'javascript:' or 'file:' on an audience screen is code
  // execution and disk access, not a slide.
  function projectableLink(url) {
    if (typeof url !== 'string') { return null; }
    var trimmed = url.trim();
    var lower = trimmed.toLowerCase();
    if (lower.indexOf('http://') === 0 || lower.indexOf('https://') === 0) { return trimmed; } // offline-ok: matching a scheme, not loading an asset
    return null;
  }

  // A page or a video replaces the slide. Anything not currently showing is
  // torn down rather than hidden: a video left with its src set keeps
  // downloading, and an iframe keeps running scripts, behind whatever is on
  // screen next.
  function applyMedia(slide) {
    var kind = slide.kind;
    var url = projectableLink(slide.mediaUrl);
    var hidden = slide.isBlank === true || slide.isLive === false;

    if (kind === 'WEB' && url && !hidden) {
      if (el.web.getAttribute('src') !== url) { el.web.setAttribute('src', url); }
      el.web.classList.remove('hidden');
    } else {
      if (el.web.getAttribute('src')) { el.web.removeAttribute('src'); }
      el.web.classList.add('hidden');
    }

    if (kind === 'VIDEO' && url && !hidden) {
      if (el.video.getAttribute('src') !== url) {
        el.video.setAttribute('src', url);
        // Muted, because a display that autoplays with sound is a display the
        // browser blocks — and the hall's sound comes from the desk, not the TV.
        el.video.muted = true;
        el.video.autoplay = true;
        var playing = el.video.play();
        if (playing && typeof playing.catch === 'function') { playing.catch(function () {}); }
      }
      el.video.classList.remove('hidden');
    } else {
      if (el.video.getAttribute('src')) {
        el.video.pause();
        el.video.removeAttribute('src');
        el.video.load();
      }
      el.video.classList.add('hidden');
    }

    return (kind === 'WEB' || kind === 'VIDEO') && url != null;
  }

  // An alignment this page does not know — an older or newer app, or a value
  // that never arrived — falls back to centred rather than dropping the class
  // and leaving the words wherever the stylesheet last put them.
  function alignClass(value, allowed, fallback) {
    return allowed.indexOf(String(value)) >= 0 ? String(value).toLowerCase() : fallback;
  }

  function applySlide(slide) {
    var theme = slide.theme || {};

    el.screen.className = (theme.font === 'SANS' ? 'sans' : 'serif')
      + ' v-' + alignClass(theme.verticalAlign, ['TOP', 'MIDDLE', 'BOTTOM'], 'middle');
    el.content.className = 'content'
      + ' size-' + String(slide.textSize || 'MEDIUM').toLowerCase()
      + ' h-' + alignClass(theme.textAlign, ['LEFT', 'CENTER', 'RIGHT'], 'center');

    setText(el.body, slide.body);
    // The operator can turn the reference line off entirely — some churches want the words
    // and nothing else on screen.
    var wantsReference = theme.showReference !== false;
    setText(el.reference, (wantsReference && slide.reference) ? String(slide.reference).toUpperCase() : '');
    setText(el.footer, slide.footer);

    if (theme.textColor) { el.screen.style.color = theme.textColor; }
    if (theme.accentColor) { el.reference.style.color = theme.accentColor; }

    applyBackdrop(slide);

    // Corner furniture survives a blank on purpose: a black screen still
    // carrying the church name reads as intentional rather than broken.
    setText(el.brand, theme.brandLine ? String(theme.brandLine).toUpperCase() : '');
    showClock = theme.showClock !== false;
    el.clock.classList.toggle('hidden', !showClock);

    // isBlank or not isLive — either way the audience should see backdrop only.
    var hidden = slide.isBlank === true || slide.isLive === false;
    // A page or a video is the slide, so the text layer stands down for it.
    var showingMedia = applyMedia(slide);
    el.content.classList.toggle('hidden', hidden || showingMedia);

    el.standby.classList.add('hidden');
  }

  function showStandby(message) {
    // Tear the page or video down first: standby over a still-playing video is
    // a screen that looks connected when it isn't.
    applyMedia({ kind: 'BLANK' });
    el.standbyText.textContent = message;
    el.standby.classList.remove('hidden');
  }

  function handleEnvelope(envelope) {
    if (!envelope || typeof envelope !== 'object') { return; }

    if (envelope.v > PROTOCOL_VERSION) {
      // A newer phone is talking to an older bundled page. Say so plainly
      // rather than rendering a slide we may only half understand.
      showStandby('Update ChurchPresenter on this screen');
      return;
    }
    if (typeof envelope.rev === 'number' && envelope.rev < lastRevision) { return; }
    if (typeof envelope.rev === 'number') { lastRevision = envelope.rev; }

    if (envelope.type === 'PING') { return; }
    if (envelope.type === 'BYE') { showStandby('Presentation ended'); return; }

    if (envelope.slide) {
      applySlide(envelope.slide);
    } else if (envelope.type === 'CLEAR') {
      applyMedia({ kind: 'BLANK' });
      el.content.classList.add('hidden');
    }
  }

  // ── Transport ──────────────────────────────────────────────────────────

  function connect() {
    var scheme = window.location.protocol === 'https:' ? 'wss' : 'ws';
    var url = scheme + '://' + window.location.host + '/live';

    try {
      socket = new WebSocket(url);
    } catch (e) {
      scheduleReconnect();
      return;
    }

    socket.onopen = function () {
      reconnectDelay = RECONNECT_MIN_MS;
    };

    socket.onmessage = function (event) {
      try {
        handleEnvelope(JSON.parse(event.data));
      } catch (e) {
        // A malformed frame must never take the display down mid-service.
      }
    };

    socket.onclose = function () {
      // The phone may have locked, backgrounded, or moved network. Keep the
      // last slide on screen and keep trying — do not black out the room.
      lastRevision = -1;
      scheduleReconnect();
    };

    socket.onerror = function () {
      if (socket) { socket.close(); }
    };
  }

  function scheduleReconnect() {
    socket = null;
    window.setTimeout(connect, reconnectDelay);
    reconnectDelay = Math.min(reconnectDelay * 2, RECONNECT_MAX_MS);
  }

  // ── Clock ──────────────────────────────────────────────────────────────

  function tick() {
    if (!showClock) { return; }
    var now = new Date();
    var h = String(now.getHours()).padStart(2, '0');
    var m = String(now.getMinutes()).padStart(2, '0');
    el.clock.textContent = h + ':' + m;
  }

  tick();
  window.setInterval(tick, 10000);
  connect();
})();
