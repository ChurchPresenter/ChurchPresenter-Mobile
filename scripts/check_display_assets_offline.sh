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

echo "ok: display assets are fully self-contained ($(find "$ASSET_DIR" -type f | wc -l | tr -d ' ') files)"
