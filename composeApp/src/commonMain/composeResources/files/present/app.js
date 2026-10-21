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
    video: document.getElementById('video'),
    dt: document.getElementById('dt'),
    dtHeader: document.getElementById('dt-header'),
    dtNumber: document.getElementById('dt-number'),
    dtTitle: document.getElementById('dt-title'),
    dtArea: document.getElementById('dt-area'),
    dtBefore: document.getElementById('dt-before'),
    dtBody: document.getElementById('dt-body'),
    dtAfter: document.getElementById('dt-after'),
    dtFooter: document.getElementById('dt-footer')
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

  // ── Display text ───────────────────────────────────────────────────────
  //
  // Bible and song slides styled element by element, when the theme carries a
  // `display` object. A second implementation of DisplayTextLayout.kt: every
  // number below is that file's, and the defaults are DisplayText.kt's, so the
  // operator's preview predicts this screen.

  var SERIF_STACK = "Georgia, 'Times New Roman', serif";
  var SANS_STACK = "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif";

  // Lengths in DisplayTextLayout.kt, in pixels on a 1080-pixel-high output.
  var DT = {
    REFERENCE_GAP: 28,
    LANGUAGE_GAP: 18,
    HEADER_GAP: 24,
    HEADER_SPACING: 18,
    BACKING_RADIUS: 10,
    BACKING_PAD_H: 18,
    BACKING_PAD_V: 6,
    FOOTER: 22,
    FOOTER_GAP: 16,
    FOOTER_ALPHA: 0.55,
    BODY_LINE_HEIGHT: 1.32,
    ELEMENT_LINE_HEIGHT: 1.2,
    SHADOW_ALPHA: 0.7,
    MIN_SIZE: 12,
    SECOND_SCALE: 70
  };

  // CSS blurs a text-shadow with a Gaussian of sigma = blur / 2; Compose (Skia)
  // turns a shadow's blurRadius into sigma = 0.57735 * radius. Scaling by
  // 2 * 0.57735 makes the two the same blur.
  var SHADOW_BLUR_TO_CSS = 1.1547;

  // TextElementStyle()'s own defaults: what a field missing from an element
  // decodes to on the phone, whichever element it is.
  var BASE_STYLE = {
    font: 'SERIF', sizePt: 72, bold: false, italic: false, underline: false,
    letterCase: 'ORIGINAL', align: 'CENTER', verticalAlign: 'MIDDLE',
    color: '#FFFFFF', outline: false, outlineColor: '#000000', outlineWidthPx: 1.5,
    backing: false, backingColor: '#000000', backingOpacity: 45,
    shadow: false, shadowColor: '#000000', shadowBlurPx: 10, shadowOffsetPx: 3,
    letterSpacingPercent: 0, wordSpacingPx: 0, visible: true
  };

  // DisplayTextTheme()'s per-element defaults: what a whole missing element
  // decodes to. Only the fields that differ from BASE_STYLE.
  var ELEMENT_DEFAULTS = {
    bibleVerse: { bold: true, shadow: true },
    bibleReference: {
      font: 'SANS', sizePt: 34, bold: true, letterCase: 'UPPER', color: '#FDE68A',
      backing: true, backingOpacity: 60, letterSpacingPercent: 14
    },
    songLyrics: { font: 'SANS', bold: true, shadow: true },
    songLyricsSecond: { font: 'SANS', italic: true, color: '#BAE6FD', shadow: true },
    songTitle: { font: 'SANS', sizePt: 32, bold: true, align: 'LEFT' },
    songNumber: {
      font: 'SANS', sizePt: 32, bold: true, align: 'LEFT', color: '#052E16',
      backing: true, backingColor: '#86EFAC', backingOpacity: 100
    }
  };

  var STYLE_ENUMS = {
    font: ['SERIF', 'SANS'],
    letterCase: ['TITLE', 'UPPER', 'LOWER', 'ORIGINAL'],
    align: ['LEFT', 'CENTER', 'RIGHT'],
    verticalAlign: ['TOP', 'MIDDLE', 'BOTTOM']
  };
  var STYLE_COLORS = ['color', 'outlineColor', 'backingColor', 'shadowColor'];

  function isNumber(value) { return typeof value === 'number' && isFinite(value); }

  // #RRGGBB or #AARRGGBB — alpha first, the phone's order (parseHexColor). The
  // leading # is optional there too. Anything else is not a colour.
  function parseDisplayColor(value) {
    if (typeof value !== 'string') { return null; }
    var hex = value.trim();
    if (hex.charAt(0) === '#') { hex = hex.slice(1); }
    if (!/^[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$/.test(hex)) { return null; }
    var a = 1;
    if (hex.length === 8) { a = parseInt(hex.slice(0, 2), 16) / 255; hex = hex.slice(2); }
    return {
      r: parseInt(hex.slice(0, 2), 16),
      g: parseInt(hex.slice(2, 4), 16),
      b: parseInt(hex.slice(4, 6), 16),
      a: a
    };
  }

  // [alpha], when given, replaces the colour's own — as Color.copy(alpha) does.
  function rgba(hex, alpha) {
    var c = parseDisplayColor(hex) || { r: 0, g: 0, b: 0, a: 1 };
    var a = isNumber(alpha) ? alpha : c.a;
    return 'rgba(' + c.r + ', ' + c.g + ', ' + c.b + ', ' + Math.round(a * 1000) / 1000 + ')';
  }

  /**
   * One element's style, decoded as the phone decodes it: an element missing
   * altogether is that element's default, and a field missing (or one this
   * page cannot understand) is TextElementStyle's own default for that field.
   */
  function elementStyle(display, name) {
    var raw = display[name];
    var out = {};
    var key;
    if (!raw || typeof raw !== 'object') {
      for (key in BASE_STYLE) { out[key] = BASE_STYLE[key]; }
      var overrides = ELEMENT_DEFAULTS[name] || {};
      for (key in overrides) { out[key] = overrides[key]; }
      return out;
    }
    for (key in BASE_STYLE) {
      var base = BASE_STYLE[key];
      var value = raw[key];
      var ok;
      if (STYLE_ENUMS[key]) { ok = STYLE_ENUMS[key].indexOf(value) >= 0; }
      else if (STYLE_COLORS.indexOf(key) >= 0) { ok = parseDisplayColor(value) != null; }
      else if (typeof base === 'boolean') { ok = value === true || value === false; }
      else { ok = isNumber(value); }
      out[key] = ok ? value : base;
    }
    return out;
  }

  function enumOr(value, allowed, fallback) {
    return allowed.indexOf(value) >= 0 ? value : fallback;
  }

  // In the element's letter case, exactly as TextElementStyle.cased writes it:
  // TITLE splits on spaces only, so a word after a line break keeps its case.
  function cased(text, letterCase) {
    if (letterCase === 'UPPER') { return text.toUpperCase(); }
    if (letterCase === 'LOWER') { return text.toLowerCase(); }
    if (letterCase === 'TITLE') {
      return text.split(' ').map(function (word) {
        var lower = word.toLowerCase();
        if (!lower) { return lower; }
        var first = lower.charAt(0).toUpperCase();
        // uppercaseChar maps one char to one char; 'ß' does not become 'SS'.
        if (first.length !== 1) { first = lower.charAt(0); }
        return first + lower.slice(1);
      }).join(' ');
    }
    return text;
  }

  // The verse's line breaks as spaces — Slide.displayBody / joinLines.
  function joinLines(text) {
    return String(text).replace(/[ \t\n\x0B\f\r]*\n[ \t\n\x0B\f\r]*/g, ' ')
      .replace(/^[ \t\n\x0B\f\r]+|[ \t\n\x0B\f\r]+$/g, '');
  }

  function isBlank(value) {
    return value == null || String(value).trim().length === 0;
  }

  // A 1080p length as a fraction of this screen's height.
  function at1080(px) { return (px / 10.8) + 'vh'; }

  function horizontalFlex(align) {
    if (align === 'LEFT') { return 'flex-start'; }
    if (align === 'RIGHT') { return 'flex-end'; }
    return 'center';
  }

  function verticalFlex(align) {
    if (align === 'TOP') { return 'flex-start'; }
    if (align === 'BOTTOM') { return 'flex-end'; }
    return 'center';
  }

  /** Paints [node] in [style]: type, colour, outline, box and shadow. */
  function styleElement(node, style, lineHeight) {
    var s = node.style;
    s.cssText = '';
    s.fontFamily = style.font === 'SANS' ? SANS_STACK : SERIF_STACK;
    s.fontSize = at1080(style.sizePt);
    s.lineHeight = String(lineHeight);
    s.fontWeight = style.bold ? '700' : '400';
    s.fontStyle = style.italic ? 'italic' : 'normal';
    s.textDecoration = style.underline ? 'underline' : 'none';
    s.textAlign = style.align.toLowerCase();
    // Placed on its line by its own alignment, not its column's — the box round the
    // words is only as wide as they are, so a reference set to the left would otherwise
    // sit wherever the verse put it. The header's number and title share a row instead.
    var inHeader = node.parentNode && node.parentNode.classList && node.parentNode.classList.contains('dt-header');
    if (!inHeader) {
      s.alignSelf = style.align === 'LEFT' ? 'flex-start' : (style.align === 'RIGHT' ? 'flex-end' : 'center');
    }
    s.color = rgba(style.color);
    s.letterSpacing = (style.letterSpacingPercent / 100) + 'em';
    // In em of this element's size, as the phone widens its spaces, so a
    // shrink-to-fit narrows the gaps with the words.
    s.wordSpacing = (style.wordSpacingPx && style.sizePt) ? (style.wordSpacingPx / style.sizePt) + 'em' : '0';
    if (style.outline) {
      // Doubled and painted under the fill: the stroke straddles the glyph's
      // edge, so the requested width shows outside the letters — as on the phone.
      s.setProperty('-webkit-text-stroke-width', at1080(style.outlineWidthPx * 2));
      s.setProperty('-webkit-text-stroke-color', rgba(style.outlineColor));
      s.setProperty('paint-order', 'stroke fill');
    }
    if (style.backing) {
      var opacity = Math.max(0, Math.min(100, style.backingOpacity)) / 100;
      s.backgroundColor = rgba(style.backingColor, opacity);
      s.borderRadius = at1080(DT.BACKING_RADIUS);
      s.padding = at1080(DT.BACKING_PAD_V) + ' ' + at1080(DT.BACKING_PAD_H);
    }
    if (style.shadow) {
      s.textShadow = '0 ' + at1080(style.shadowOffsetPx) + ' ' +
        at1080(style.shadowBlurPx * SHADOW_BLUR_TO_CSS) + ' ' +
        rgba(style.shadowColor, DT.SHADOW_ALPHA);
    }
  }

  /** Writes one element, or hides it when there is nothing to write. */
  function putElement(node, text, style, lineHeight) {
    node.classList.remove('dt-fit');
    node.classList.remove('dt-clip');
    node.classList.remove('dt-chords');
    if (text == null || String(text).length === 0) {
      node.style.cssText = '';
      setText(node, '');
      return;
    }
    styleElement(node, style, lineHeight);
    setText(node, cased(String(text), style.letterCase));
  }

  /**
   * Shrinks the main words until they fit the height left to them, as the
   * phone's TextAutoSize does, with the same floor: 45% of the chosen size.
   * Walks in the element's own vh unit, so the result survives a resize.
   */
  function fitDisplayBody(node, sizePt) {
    node.classList.add('dt-fit');
    var fitsNow = function () {
      return node.scrollHeight <= node.clientHeight + 1 && node.scrollWidth <= node.clientWidth + 1;
    };
    var factor = 1;
    for (var i = 0; i < 24; i++) {
      if (fitsNow()) { return; }
      factor = Math.max(factor * 0.94, 0.45);
      node.style.fontSize = at1080(sizePt * factor);
      if (factor === 0.45) { break; }
    }
    // Even the floor will not fit: clip, as the phone does, rather than let
    // the words run over the reference or off the screen.
    if (!fitsNow()) { node.classList.add('dt-clip'); }
  }

  function placeGap(before, after, gapPx) {
    if (before.textContent) { before.style.marginBottom = at1080(gapPx); }
    if (after.textContent) { after.style.marginTop = at1080(gapPx); }
  }

  function renderBibleDisplay(slide, theme, display) {
    var verse = elementStyle(display, 'bibleVerse');
    var refStyle = elementStyle(display, 'bibleReference');
    var format = enumOr(display.referenceFormat, ['BOOK_CHAPTER_VERSE', 'WITH_VERSION'], 'WITH_VERSION');
    var position = enumOr(display.referencePosition, ['ABOVE', 'BELOW'], 'BELOW');

    // Slide.formattedReference.
    var reference = null;
    if (!isBlank(slide.reference) && refStyle.visible) {
      reference = String(slide.reference);
      var credit = slide.verseCredit;
      var version = (credit && typeof credit === 'object' && !isBlank(credit.bibleName)) ? String(credit.bibleName) : null;
      if (format === 'WITH_VERSION' && version) { reference = reference + ' · ' + version; }
    }

    setText(el.dtNumber, '');
    setText(el.dtTitle, '');
    el.dtHeader.classList.add('hidden');

    el.dtArea.style.justifyContent = verticalFlex(verse.verticalAlign);
    el.dtArea.style.alignItems = horizontalFlex(verse.align);

    putElement(el.dtBefore, position === 'ABOVE' ? reference : null, refStyle, DT.ELEMENT_LINE_HEIGHT);
    putElement(el.dtAfter, position === 'BELOW' ? reference : null, refStyle, DT.ELEMENT_LINE_HEIGHT);
    placeGap(el.dtBefore, el.dtAfter, DT.REFERENCE_GAP);

    var body = isBlank(slide.body) ? '' : String(slide.body);
    if (theme.ignoreLineBreaks === true) { body = joinLines(body); }
    putElement(el.dtBody, body, verse, DT.BODY_LINE_HEIGHT);
    if (theme.autoFitText === true && body) { fitDisplayBody(el.dtBody, verse.sizePt); }
  }

  function renderSongDisplay(slide, theme, display) {
    var lyrics = elementStyle(display, 'songLyrics');
    var second = elementStyle(display, 'songLyricsSecond');
    var numberStyle = elementStyle(display, 'songNumber');
    var titleStyle = elementStyle(display, 'songTitle');
    var scale = isNumber(display.secondLanguageScalePercent) ? display.secondLanguageScalePercent : DT.SECOND_SCALE;
    // DisplayTextTheme.secondLanguageSizePt — integer arithmetic, as there.
    second.sizePt = Math.max(DT.MIN_SIZE, Math.floor(lyrics.sizePt * scale / 100));
    var position = enumOr(display.secondLanguagePosition, ['ABOVE', 'BELOW'], 'BELOW');

    // The header: number badge, then title.
    var number = (!isBlank(slide.songNumber) && numberStyle.visible) ? String(slide.songNumber) : null;
    var title = (!isBlank(slide.songTitle) && titleStyle.visible) ? String(slide.songTitle) : null;
    putElement(el.dtNumber, number, numberStyle, DT.ELEMENT_LINE_HEIGHT);
    putElement(el.dtTitle, title, titleStyle, DT.ELEMENT_LINE_HEIGHT);
    var header = el.dtHeader.style;
    header.justifyContent = horizontalFlex(titleStyle.align);
    header.gap = at1080(DT.HEADER_SPACING);
    header.paddingBottom = at1080(DT.HEADER_GAP);
    el.dtHeader.classList.toggle('hidden', number == null && title == null);

    el.dtArea.style.justifyContent = verticalFlex(lyrics.verticalAlign);
    el.dtArea.style.alignItems = horizontalFlex(lyrics.align);

    var secondText = null;
    if (display.showSecondLanguage !== false && !isBlank(slide.secondaryBody)) {
      secondText = String(slide.secondaryBody);
      if (theme.ignoreLineBreaks === true) { secondText = joinLines(secondText); }
    }
    putElement(el.dtBefore, position === 'ABOVE' ? secondText : null, second, DT.BODY_LINE_HEIGHT);
    putElement(el.dtAfter, position === 'BELOW' ? secondText : null, second, DT.BODY_LINE_HEIGHT);
    placeGap(el.dtBefore, el.dtAfter, DT.LANGUAGE_GAP);

    var chordText = (theme.showChords === true) ? slide.chordBody : null;
    if (chordText) {
      // OutputChordChart: the lyrics' font, size and colour, nothing else of
      // their style; the chords in the accent colour; each line placed by the
      // lyrics' alignment.
      putElement(el.dtBody, null, lyrics, DT.BODY_LINE_HEIGHT);
      var s = el.dtBody.style;
      s.fontFamily = lyrics.font === 'SANS' ? SANS_STACK : SERIF_STACK;
      s.fontSize = at1080(lyrics.sizePt);
      s.lineHeight = String(DT.BODY_LINE_HEIGHT);
      s.color = rgba(lyrics.color);
      el.dtBody.classList.add('dt-chords');
      var accent = parseDisplayColor(theme.accentColor) ? rgba(theme.accentColor) : '#FFFFFF';
      renderChords(el.dtBody, chordText, accent);
      var lines = el.dtBody.children;
      for (var i = 0; i < lines.length; i++) { lines[i].style.justifyContent = horizontalFlex(lyrics.align); }
      return;
    }

    var body = isBlank(slide.body) ? '' : String(slide.body);
    if (theme.ignoreLineBreaks === true) { body = joinLines(body); }
    putElement(el.dtBody, body, lyrics, DT.BODY_LINE_HEIGHT);
    if (theme.autoFitText === true && body) { fitDisplayBody(el.dtBody, lyrics.sizePt); }
  }

  /** The Display text styling for this slide, or null for the legacy layout. */
  // Once Display text is in use, the theme's own font, colour and alignment are no
  // longer edited anywhere, so a notice or anything else drawn the old way takes them
  // from the Bible verse's style — as the phone's renderer does (legacyTheme there).
  function legacyTheme(theme) {
    var display = theme.display;
    if (!display || typeof display !== 'object' || !display.bibleVerse) { return theme; }
    var verse = display.bibleVerse;
    var copy = {};
    for (var key in theme) { if (Object.prototype.hasOwnProperty.call(theme, key)) { copy[key] = theme[key]; } }
    if (verse.font === 'SERIF' || verse.font === 'SANS') { copy.font = verse.font; }
    if (parseDisplayColor(verse.color)) { copy.textColor = rgba(verse.color); }
    if (typeof verse.align === 'string') { copy.textAlign = verse.align; }
    if (typeof verse.verticalAlign === 'string') { copy.verticalAlign = verse.verticalAlign; }
    return copy;
  }

  function displayTextFor(slide, theme) {
    var display = theme.display;
    if (!display || typeof display !== 'object') { return null; }
    return (slide.kind === 'SONG' || slide.kind === 'BIBLE') ? display : null;
  }

  function applyDisplayText(slide, theme, display) {
    el.screen.className = (theme.font === 'SANS' ? 'sans' : 'serif') + ' v-middle';
    el.content.className = 'content dt-mode';
    var margin = MARGINS[String(theme.margin)] || MARGINS.MEDIUM;
    el.content.style.padding = margin.v + 'vh ' + margin.h + 'vw';

    // The legacy elements stand down; the footer moves to #dt-footer.
    setText(el.body, '');
    el.body.style.fontSize = '';
    setText(el.reference, '');
    setText(el.footer, '');
    el.dt.classList.remove('hidden');

    // The copyright line is not styled — it is a licence condition, not part
    // of the look — but a song shown without it is a song shown unlicensed.
    var footer = isBlank(slide.footer) ? '' : String(slide.footer);
    setText(el.dtFooter, footer);
    el.dtFooter.style.fontSize = at1080(DT.FOOTER);
    el.dtFooter.style.paddingTop = at1080(DT.FOOTER_GAP);
    el.dtFooter.style.color = 'rgba(255, 255, 255, ' + DT.FOOTER_ALPHA + ')';

    if (slide.kind === 'SONG') {
      renderSongDisplay(slide, theme, display);
    } else {
      renderBibleDisplay(slide, theme, display);
    }
  }

  function applySlide(slide) {
    var theme = slide.theme || {};

    var display = displayTextFor(slide, theme);
    if (display) {
      applyDisplayText(slide, theme, display);
    } else {
      el.dt.classList.add('hidden');
      applyLegacyText(slide, legacyTheme(theme));
    }

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

  // Every slide without Display text: one font, one colour, the size ladder.
  function applyLegacyText(slide, theme) {
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
