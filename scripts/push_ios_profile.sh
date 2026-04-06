#!/usr/bin/env bash
# =============================================================================
# push_ios_profile.sh
#
# Copies a downloaded .mobileprovision into the signing repo and pushes
# IOS_PROVISIONING_PROFILE_BASE64 to GitHub Actions secrets.
#
# Usage:
#   bash scripts/push_ios_profile.sh ~/Downloads/ChurchPresenter_Distribution.mobileprovision
# =============================================================================

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SIGNING_REPO="$(cd "$REPO_ROOT/../signing-repo" 2>/dev/null && pwd)" || true

if [ -z "$SIGNING_REPO" ] || [ ! -d "$SIGNING_REPO" ]; then
  echo "❌  Signing repo not found alongside this repo."
  exit 1
fi

PROFILE_SRC="${1:-}"
if [ -z "$PROFILE_SRC" ] || [ ! -f "$PROFILE_SRC" ]; then
  echo "Usage: bash scripts/push_ios_profile.sh /path/to/profile.mobileprovision"
  exit 1
fi

DEST="$SIGNING_REPO/mobile/ios/distribution.mobileprovision"
cp "$PROFILE_SRC" "$DEST"
echo "✅  Saved to $DEST"

REPO_SLUG="$(git -C "$REPO_ROOT" remote get-url origin \
  | sed 's|.*github\.com[:/]\(.*\)\.git|\1|; s|.*github\.com[:/]\(.*\)|\1|')"

TMP=$(mktemp /tmp/pp_XXXXXX.b64)
base64 -b 0 -i "$DEST" > "$TMP"
gh secret set IOS_PROVISIONING_PROFILE_BASE64 --repo "$REPO_SLUG" < "$TMP"
rm -f "$TMP"
echo "✅  IOS_PROVISIONING_PROFILE_BASE64 pushed to $REPO_SLUG"
echo ""
echo "🎉  Re-trigger the iOS build at:"
echo "    https://github.com/$REPO_SLUG/actions"

