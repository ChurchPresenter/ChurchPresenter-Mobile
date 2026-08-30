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

  // Which reference toggle governs this slide. An older app that sends the single
  // showReference flag is still honoured, so a mixed pair keeps working.
  function referenceWanted(theme, kind) {
    if (kind === 'SONG') { return firstDefined(theme.showSongReference, theme.showReference, true); }
    if (kind === 'BIBLE') { return firstDefined(theme.showBibleReference, theme.showReference, true); }
    return firstDefined(theme.showOtherReference, theme.showReference, true);
  }

  function firstDefined(a, b, fallback) {
    if (a === true || a === false) { return a; }
    if (b === true || b === false) { return b; }
    return fallback;
  }

  /**
   * Draws the words with a chord above each run they belong to.
   *
   * Built from DOM nodes with textContent for the same reason setText uses it:
   * slide text is operator-authored and arrives over an unauthenticated LAN
   * socket, so it is content and never markup.
   */
  function renderChords(node, text, accent) {
    node.textContent = '';
    node.classList.remove('hidden');
    String(text).split('\n').forEach(function (line) {
      var row = document.createElement('div');
      row.className = 'chord-line';
      parseChordLine(line).forEach(function (segment) {
        var run = document.createElement('span');
        run.className = 'chord-run';
        var chord = document.createElement('span');
        chord.className = 'chord';
        chord.textContent = segment.chord;
        if (accent) { chord.style.color = accent; }
        var words = document.createElement('span');
        words.className = 'chord-words';
        words.textContent = segment.text;
        run.appendChild(chord);
        run.appendChild(words);
        row.appendChild(run);
      });
      node.appendChild(row);
    });
  }

  // The same rule the app and the desktop use: a bracketed token is a chord only
  // if it parses as one, so [Repeat] stays a word.
  var CHORD_RE = /^[A-G][#b]?(maj|min|dim|aug|sus|add|m|M)?[0-9]*(sus[24]|add[0-9]+|dim|aug)?(\/[A-G][#b]?)?$/;
  var BRACKETED_RE = /\[[^\]\n]*\]/g;

  function parseChordLine(line) {
    var segments = [];
    var cursor = 0;
    var match;
    BRACKETED_RE.lastIndex = 0;
    while ((match = BRACKETED_RE.exec(line)) !== null) {
      var inner = match[0].slice(1, -1);
      if (!CHORD_RE.test(inner.trim())) { continue; }
      if (match.index > cursor) {
        segments.push({ chord: '', text: line.slice(cursor, match.index) });
      }
      var afterChord = match.index + match[0].length;
      // The chord owns the words up to the next chord, or the line's end.
      var rest = line.slice(afterChord);
      var nextAt = -1;
      var scan = /\[[^\]\n]*\]/g;
      var candidate;
      while ((candidate = scan.exec(rest)) !== null) {
        if (CHORD_RE.test(candidate[0].slice(1, -1).trim())) { nextAt = candidate.index; break; }
      }
      var end = nextAt >= 0 ? afterChord + nextAt : line.length;
      segments.push({ chord: inner, text: line.slice(afterChord, end) });
      cursor = end;
      BRACKETED_RE.lastIndex = end;
    }
    if (cursor < line.length) { segments.push({ chord: '', text: line.slice(cursor) }); }
    if (segments.length === 0) { segments.push({ chord: '', text: line }); }
    return segments;
  }

  // Must match SlideMargin in the app: one set of numbers for every renderer.
  var MARGINS = {
    THIN: { h: 4, v: 3 },
    MEDIUM: { h: 8, v: 6 },
    THICK: { h: 14, v: 10 },
  };

  /**
   * Shrinks the words until the slide fits, when the operator asks for it.
   *
   * The stylesheet sizes text as a fraction of the screen, which is right for a
   * verse of ordinary length and wrong for a long one: it runs off the bottom,
   * where nobody sees it. This walks the size down until the content fits the
   * space it has.
   *
   * A floor, matching the app's own renderer: text small enough to fit anything
   * is text nobody at the back can read, so a verse that still will not fit is
   * left clipped rather than shrunk into illegibility.
   */
  function fitTextToScreen(enabled) {
    el.body.style.fontSize = '';
    if (!enabled || !el.body.textContent) { return; }

    var start = parseFloat(window.getComputedStyle(el.body).fontSize);
    if (!start || !isFinite(start)) { return; }
    var floor = start * 0.45;
    var size = start;
    // A bounded walk: each step is 6% smaller, and 24 of them cannot outlast the
    // floor. Bounded on purpose — a measuring loop with no limit is a frozen
    // screen if an assumption about layout ever stops holding.
    for (var i = 0; i < 24; i++) {
      if (fits()) { return; }
      size = size * 0.94;
      if (size < floor) { size = floor; el.body.style.fontSize = size + 'px'; return; }
      el.body.style.fontSize = size + 'px';
    }
  }

  /**
   * Whether the slide is inside the screen.
   *
   * Measured against #screen, which is the element that actually has the
   * viewport's height. .content is a flex child that sizes to its own words, so
   * its scrollHeight and clientHeight are always equal and comparing those two —
   * as this first did — reports "it fits" for a verse hanging off the bottom.
   */
  function fits() {
    return el.content.getBoundingClientRect().height <= el.screen.clientHeight &&
      el.body.scrollWidth <= el.body.clientWidth;
  }

  function applySlide(slide) {
    var theme = slide.theme || {};

    el.screen.className = (theme.font === 'SANS' ? 'sans' : 'serif')
      + ' v-' + alignClass(theme.verticalAlign, ['TOP', 'MIDDLE', 'BOTTOM'], 'middle');
    el.content.className = 'content'
      + ' size-' + String(slide.textSize || 'MEDIUM').toLowerCase()
      + ' h-' + alignClass(theme.textAlign, ['LEFT', 'CENTER', 'RIGHT'], 'center');

    // vh/vw, so these are fractions of the output's height and width — the same
    // thing the phone's renderer means by them. An app that sends no margin gets
    // the medium one, which is what every output used before it was a setting.
    var margin = MARGINS[String(theme.margin)] || MARGINS.MEDIUM;
    el.content.style.padding = margin.v + 'vh ' + margin.h + 'vw';

    // Chords ride alongside the clean words rather than replacing them, so an
    // output that is not asked for them shows exactly what it always did.
    var chordText = (theme.showChords === true) ? slide.chordBody : null;
    // The poet's line breaks are kept unless the operator turns them off, in
    // which case the words wrap to the screen's own shape instead — a hymnbook
    // line is set for a narrow page and is often far too long for a projector.
    el.body.style.whiteSpace = (theme.ignoreLineBreaks === true) ? 'normal' : 'pre-line';
    if (chordText) {
      renderChords(el.body, chordText, theme.accentColor);
    } else {
      setText(el.body, slide.body);
    }
    // The reference line is asked for by kind: a church that wants no heading over
    // a hymn usually still wants the chapter and verse over scripture.
    var wantsReference = referenceWanted(theme, slide.kind);
    setText(el.reference, (wantsReference && slide.reference) ? String(slide.reference).toUpperCase() : '');
    setText(el.footer, slide.footer);

    if (theme.textColor) { el.screen.style.color = theme.textColor; }
    if (theme.accentColor) { el.reference.style.color = theme.accentColor; }

    // Shrink to fit after the text and every size that affects it are set, and
    // only then: it measures the laid-out page, so anything decided afterwards
    // would be measured against the wrong thing.
    fitTextToScreen(theme.autoFitText === true);

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
