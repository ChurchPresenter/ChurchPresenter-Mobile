#!/usr/bin/env bash
#
# Fails if the bundled presentation display page references anything off-device.
#
# The projected page must render with no internet at all — a church hall's Wi-Fi
# frequently has no uplink, and a display that fetches its stylesheet or font
# from a CDN is not actually offline-capable. (The reference app VerseCAST loads
# its theme from GitHub Pages and is broken by exactly this.) It is a one-line
# change to "just use a CDN font", so this guard exists to make that change fail
# loudly instead of silently in a service.
#
# Usage: bash scripts/check_display_assets_offline.sh

set -euo pipefail

ASSET_DIR="composeApp/src/commonMain/composeResources/files/present"

if [ ! -d "$ASSET_DIR" ]; then
  echo "error: $ASSET_DIR not found (run from the repo root)" >&2
  exit 1
fi

# Absolute URLs, protocol-relative URLs, and @import — the three ways a remote
# dependency creeps back in.
PATTERN='https?://|(^|[^:])//[a-zA-Z0-9-]+\.[a-zA-Z]{2,}|@import'

# Three things are not findings: `ws://` built from `window.location.host`, which
# is how the page reaches its own phone; comment lines, which legitimately name
# the very URLs this check exists to prevent; and lines carrying the marker
# below. Everything else is.
COMMENT_LINE='^[^:]+:[0-9]+:[[:space:]]*(\*|/\*|//|#|<!--)'

# The escape hatch, deliberately narrow: a line that names a scheme without
# fetching anything — an operator-typed address being validated, not an asset
# being loaded — is annotated `offline-ok` where it sits. Annotate the single
# line, never a whole file, and only when the page still renders with no uplink.
ALLOWED_MARKER='offline-ok'

if matches=$(grep -REn "$PATTERN" "$ASSET_DIR" \
      | grep -v 'window.location.host' \
      | grep -v "$ALLOWED_MARKER" \
      | grep -vE "$COMMENT_LINE"); then
  echo "error: bundled display assets reference a remote resource:" >&2
  echo "$matches" >&2
  echo >&2
  echo "The projected page must work with no internet. Inline or bundle it instead." >&2
  exit 1
fi

# ── The page and the phone must agree on geometry ──────────────────────────
#
# The hosted page and the app's own renderer are two implementations of one
# design, and they drift silently: the page once sized body text at 4.4vw where
# the phone used 0.066 of the width, and its top and bottom margin was nearly
# double, because CSS resolves a percentage padding against the container's
# *width* on all four sides. The operator judges a slide by the phone's preview,
# so a preview that does not predict the screen is worse than none.
#
# These are the numbers that have to match. Kotlin's side lives in
# StandaloneOutputScreen.scaledSp and model/Slide.kt's SlideMargin.
CSS="$ASSET_DIR/style.css"
JS="$ASSET_DIR/app.js"

geometry_error() {
  echo "error: the projected page has drifted from the app's own renderer:" >&2
  echo "  $1" >&2
  echo >&2
  echo "Keep style.css and app.js in step with scaledSp and SlideMargin." >&2
  exit 1
}

for ratio in 5.2vw 6.6vw 8.4vw; do
  grep -q "$ratio" "$CSS" || geometry_error "body text $ratio missing (scaledSp uses .052/.066/.084)"
done

grep -qE 'padding:[^;]*vh[^;]*vw' "$CSS" ||
  geometry_error "margins must be vh/vw — a percentage means width on every side"

for pair in "THIN:4:3" "MEDIUM:8:6" "THICK:14:10"; do
  name="${pair%%:*}"; rest="${pair#*:}"; h="${rest%%:*}"; v="${rest##*:}"
  grep -q "$name" "$JS" || geometry_error "margin $name unknown to the page"
  grep -qE "$name:[[:space:]]*\{[[:space:]]*h:[[:space:]]*$h,[[:space:]]*v:[[:space:]]*$v" "$JS" ||
    geometry_error "margin $name should be h: $h, v: $v (see SlideMargin)"
done

echo "ok: display assets are self-contained and match the app's renderer ($(find "$ASSET_DIR" -type f | wc -l | tr -d ' ') files)"
