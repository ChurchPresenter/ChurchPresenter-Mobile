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
    standbyText: document.getElementById('standby-text')
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

  function applyBackdrop(slide) {
    var kind = slide.backdrop || 'GRADIENT';
    var url = slide.backdropUrl;
    if (kind === 'IMAGE' && url) {
      el.backdrop.className = '';
      el.backdrop.style.backgroundImage = 'url("' + encodeURI(url) + '")';
      el.scrim.classList.remove('hidden');
    } else {
      el.backdrop.style.backgroundImage = '';
      el.backdrop.className = kind === 'BLACK' ? 'bd-black' : 'bd-gradient';
      el.scrim.classList.add('hidden');
    }
  }

  function applySlide(slide) {
    var theme = slide.theme || {};

    el.screen.className = (theme.font === 'SANS' ? 'sans' : 'serif');
    el.content.className = 'content size-' + String(slide.textSize || 'MEDIUM').toLowerCase();

    setText(el.body, slide.body);
    setText(el.reference, slide.reference ? String(slide.reference).toUpperCase() : '');
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
    el.content.classList.toggle('hidden', hidden);

    el.standby.classList.add('hidden');
  }

  function showStandby(message) {
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
