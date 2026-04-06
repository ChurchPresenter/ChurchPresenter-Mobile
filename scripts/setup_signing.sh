#!/bin/bash
# =============================================================================
# setup_signing.sh
#
# One-time setup script for developer machines.
# Wires up release signing for both Android and iOS from the private
# signing repository.
#
# Usage:
#   bash scripts/setup_signing.sh [/path/to/signing-repo]
#
# If no path is supplied the script looks for the signing repo next to this
# repo (the recommended side-by-side layout).
# =============================================================================

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# ---------------------------------------------------------------------------
# Locate the signing repo
# ---------------------------------------------------------------------------
if [ $# -ge 1 ]; then
  SIGNING_REPO="$1"
else
  SIGNING_REPO="$(cd "$REPO_ROOT/../signing-repo" 2>/dev/null && pwd)" || true
fi

if [ -z "$SIGNING_REPO" ] || [ ! -d "$SIGNING_REPO" ]; then
  echo ""
  echo "❌  Signing repo not found."
  echo "    Clone it first:"
  echo "    git clone <signing-repo-url> ../signing-repo"
  echo ""
  echo "    Then re-run:"
  echo "    bash scripts/setup_signing.sh /path/to/signing-repo"
  echo ""
  exit 1
fi

echo "✅  Found signing repo at: $SIGNING_REPO"

# ---------------------------------------------------------------------------
# Android — write signing.repo.path into local.properties
# ---------------------------------------------------------------------------
LOCAL_PROPS="$REPO_ROOT/local.properties"
ANDROID_SIGNING_PATH="$SIGNING_REPO/mobile"

if [ ! -f "$LOCAL_PROPS" ]; then
  cp "$REPO_ROOT/local.properties.example" "$LOCAL_PROPS"
  echo "📄  Created local.properties from template"
fi

if grep -q "^signing.repo.path=" "$LOCAL_PROPS"; then
  # Update existing value
  sed -i '' "s|^signing.repo.path=.*|signing.repo.path=$ANDROID_SIGNING_PATH|" "$LOCAL_PROPS"
  echo "✏️   Updated signing.repo.path in local.properties"
else
  echo "" >> "$LOCAL_PROPS"
  echo "# Release signing — managed by scripts/setup_signing.sh" >> "$LOCAL_PROPS"
  echo "signing.repo.path=$ANDROID_SIGNING_PATH" >> "$LOCAL_PROPS"
  echo "✏️   Added signing.repo.path to local.properties"
fi

# ---------------------------------------------------------------------------
# iOS — symlink signing.xcconfig into iosApp/Configuration/
# ---------------------------------------------------------------------------
IOS_SOURCE="$SIGNING_REPO/mobile/ios/signing.xcconfig"
IOS_LINK="$REPO_ROOT/iosApp/Configuration/Signing.xcconfig"

if [ ! -f "$IOS_SOURCE" ]; then
  echo "⚠️   $IOS_SOURCE not found — skipping iOS signing setup."
else
  # Remove stale link/file if it exists
  if [ -L "$IOS_LINK" ] || [ -f "$IOS_LINK" ]; then
    rm "$IOS_LINK"
  fi
  ln -s "$IOS_SOURCE" "$IOS_LINK"
  echo "🔗  Symlinked iOS signing.xcconfig → $IOS_LINK"
fi

# ---------------------------------------------------------------------------
# Firebase — symlink google-services.json and GoogleService-Info.plist
# ---------------------------------------------------------------------------
GOOGLE_SERVICES_SOURCE="$SIGNING_REPO/mobile/google-services.json"
GOOGLE_SERVICES_LINK="$REPO_ROOT/composeApp/google-services.json"

if [ ! -f "$GOOGLE_SERVICES_SOURCE" ]; then
  echo "⚠️   $GOOGLE_SERVICES_SOURCE not found — skipping Android Firebase setup."
else
  if [ -L "$GOOGLE_SERVICES_LINK" ] || [ -f "$GOOGLE_SERVICES_LINK" ]; then
    rm "$GOOGLE_SERVICES_LINK"
  fi
  ln -s "$GOOGLE_SERVICES_SOURCE" "$GOOGLE_SERVICES_LINK"
  echo "🔗  Symlinked google-services.json → $GOOGLE_SERVICES_LINK"
fi

GOOGLE_SERVICE_INFO_SOURCE="$SIGNING_REPO/mobile/ios/GoogleService-Info.plist"
GOOGLE_SERVICE_INFO_LINK="$REPO_ROOT/iosApp/iosApp/GoogleService-Info.plist"

if [ ! -f "$GOOGLE_SERVICE_INFO_SOURCE" ]; then
  echo "⚠️   $GOOGLE_SERVICE_INFO_SOURCE not found — skipping iOS Firebase setup."
else
  if [ -L "$GOOGLE_SERVICE_INFO_LINK" ] || [ -f "$GOOGLE_SERVICE_INFO_LINK" ]; then
    rm "$GOOGLE_SERVICE_INFO_LINK"
  fi
  ln -s "$GOOGLE_SERVICE_INFO_SOURCE" "$GOOGLE_SERVICE_INFO_LINK"
  echo "🔗  Symlinked GoogleService-Info.plist → $GOOGLE_SERVICE_INFO_LINK"
fi

# ---------------------------------------------------------------------------
# Done
# ---------------------------------------------------------------------------
echo ""
echo "🎉  Signing setup complete!"
echo ""
echo "    Android: release builds will use $ANDROID_SIGNING_PATH"
echo "    Android: google-services.json symlinked from signing repo"
echo "    iOS:     GoogleService-Info.plist symlinked from signing repo"
echo "    iOS:     open iosApp.xcodeproj and set your Team in Signing & Capabilities,"
echo "             or update TEAM_ID in $IOS_SOURCE"
echo ""

