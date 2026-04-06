#!/usr/bin/env bash
# =============================================================================
# push_github_secrets.sh
#
# Reads signing artefacts from the signing repo and pushes every secret
# required by the GitHub Actions release workflows.
#
# Prerequisites:
#   brew install gh          # GitHub CLI
#   gh auth login            # authenticate once
#
# Usage:
#   bash scripts/push_github_secrets.sh [/path/to/signing-repo]
#
# If no path is supplied the script looks for the repo next to this one
# (side-by-side layout: ../signing-repo).
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
  echo "    Clone it and re-run:"
  echo "    bash scripts/push_github_secrets.sh /path/to/signing-repo"
  echo ""
  exit 1
fi

MOBILE="$SIGNING_REPO/mobile"
echo "✅  Using signing repo: $SIGNING_REPO"

# ---------------------------------------------------------------------------
# Require the GitHub CLI
# ---------------------------------------------------------------------------
if ! command -v gh &>/dev/null; then
  echo ""
  echo "❌  GitHub CLI (gh) not found."
  echo "    Install with:  brew install gh"
  echo "    Then auth:     gh auth login"
  echo ""
  exit 1
fi

# ---------------------------------------------------------------------------
# Detect the repo slug from git remote
# ---------------------------------------------------------------------------
REPO_SLUG="$(git -C "$REPO_ROOT" remote get-url origin \
  | sed 's|.*github\.com[:/]\(.*\)\.git|\1|; s|.*github\.com[:/]\(.*\)|\1|')"

echo "📦  Target repository: $REPO_SLUG"
echo ""

# ---------------------------------------------------------------------------
# Helpers — push secrets without any shell quoting or printf corruption.
#
# push_secret_value: plain strings (passwords, aliases)
# push_secret_file:  binary-derived base64 — write to temp file, feed via <
#   This avoids the "base64: invalid input" error that occurs when a large
#   base64 string is piped through printf in certain shell environments.
# ---------------------------------------------------------------------------
push_secret_value() {
  local name="$1"
  local value="$2"
  gh secret set "$name" --repo "$REPO_SLUG" --body "$value"
  echo "   ✅  $name"
}

push_secret_file() {
  local name="$1"
  local file="$2"           # path to the raw (binary) file to encode
  local tmp
  tmp=$(mktemp /tmp/ghs_XXXXXX.b64)
  base64 -b 0 -i "$file" > "$tmp"
  gh secret set "$name" --repo "$REPO_SLUG" < "$tmp"
  rm -f "$tmp"
  echo "   ✅  $name"
}

# ---------------------------------------------------------------------------
# Android secrets
# ---------------------------------------------------------------------------
echo "🔐  Setting Android secrets…"

KEYSTORE="$MOBILE/church-presenter.jks"
if [ ! -f "$KEYSTORE" ]; then
  echo "❌  Keystore not found: $KEYSTORE"; exit 1
fi
push_secret_file "ANDROID_KEYSTORE_BASE64" "$KEYSTORE"

# Read values from signing.properties
PROPS="$MOBILE/signing.properties"
if [ ! -f "$PROPS" ]; then
  echo "❌  signing.properties not found: $PROPS"; exit 1
fi

get_prop() { grep "^$1=" "$PROPS" | cut -d= -f2-; }

push_secret_value "ANDROID_STORE_PASSWORD" "$(get_prop storePassword)"
push_secret_value "ANDROID_KEY_ALIAS"      "$(get_prop keyAlias)"
push_secret_value "ANDROID_KEY_PASSWORD"   "$(get_prop keyPassword)"

# google-services.json
GS_JSON="$MOBILE/google-services.json"
if [ ! -f "$GS_JSON" ]; then
  echo "❌  google-services.json not found: $GS_JSON"; exit 1
fi
push_secret_file "GOOGLE_SERVICES_JSON_BASE64" "$GS_JSON"

# Google Play service account JSON (required for android-play-store.yml)
PLAY_SA_JSON="$MOBILE/play-store-service-account.json"
if [ -f "$PLAY_SA_JSON" ]; then
  gh secret set "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON" --repo "$REPO_SLUG" < "$PLAY_SA_JSON"
  echo "   ✅  GOOGLE_PLAY_SERVICE_ACCOUNT_JSON"
else
  echo "   ⚠️   $PLAY_SA_JSON not found — skipping GOOGLE_PLAY_SERVICE_ACCOUNT_JSON"
  echo "       Create a service account in Google Cloud Console, download the JSON key,"
  echo "       and place it at $PLAY_SA_JSON"
  echo "       Or set it manually:"
  echo "       gh secret set GOOGLE_PLAY_SERVICE_ACCOUNT_JSON --repo $REPO_SLUG < ~/Downloads/github-actions-play-key.json"
fi

echo ""

# ---------------------------------------------------------------------------
# iOS secrets
# ---------------------------------------------------------------------------
echo "🔐  Setting iOS secrets…"

IOS_DIR="$MOBILE/ios"

# GoogleService-Info.plist
GSINFO="$IOS_DIR/GoogleService-Info.plist"
if [ -f "$GSINFO" ]; then
  push_secret_file "GOOGLE_SERVICE_INFO_PLIST_BASE64" "$GSINFO"
else
  echo "   ⚠️   $GSINFO not found — skipping GOOGLE_SERVICE_INFO_PLIST_BASE64"
fi

# iOS signing xcconfig
IOS_XCCONFIG="$IOS_DIR/ios-secrets.properties"
if [ -f "$IOS_XCCONFIG" ]; then
  get_ios_prop() { grep "^$1=" "$IOS_XCCONFIG" | cut -d= -f2-; }
  TEAM_ID="$(get_ios_prop TEAM_ID)"
  CS_IDENTITY="$(get_ios_prop CODE_SIGN_IDENTITY)"
  PP_NAME="$(get_ios_prop PROVISIONING_PROFILE_NAME)"
  [ -n "$TEAM_ID"      ] && push_secret_value "IOS_TEAM_ID"                   "$TEAM_ID"
  [ -n "$CS_IDENTITY"  ] && push_secret_value "IOS_CODE_SIGN_IDENTITY"        "$CS_IDENTITY"
  [ -n "$PP_NAME"      ] && push_secret_value "IOS_PROVISIONING_PROFILE_NAME" "$PP_NAME"
else
  echo "   ⚠️   $IOS_XCCONFIG not found — skipping IOS_TEAM_ID / IOS_CODE_SIGN_IDENTITY / IOS_PROVISIONING_PROFILE_NAME"
  echo "       Create that file with:"
  echo "       TEAM_ID=XXXXXXXXXX"
  echo "       CODE_SIGN_IDENTITY=Apple Distribution: Your Org (XXXXXXXXXX)"
  echo "       PROVISIONING_PROFILE_NAME=ChurchPresenter Distribution"
fi

# p12 certificate
P12="$IOS_DIR/distribution.p12"
if [ -f "$P12" ]; then
  push_secret_file "IOS_CERTIFICATE_BASE64" "$P12"
else
  echo "   ⚠️   $P12 not found — skipping IOS_CERTIFICATE_BASE64"
  echo "       Export your Apple Distribution cert from Keychain Access as distribution.p12"
  echo "       and place it at $P12"
fi

# Certificate + keychain passwords (prompt if not already in env)
if [ -z "${IOS_CERT_PASS:-}" ]; then
  read -r -s -p "   Enter IOS_CERTIFICATE_PASSWORD (p12 export password): " IOS_CERT_PASS
  echo ""
fi
if [ -n "$IOS_CERT_PASS" ]; then
  push_secret_value "IOS_CERTIFICATE_PASSWORD" "$IOS_CERT_PASS"
fi

if [ -z "${IOS_KEYCHAIN_PASS:-}" ]; then
  read -r -s -p "   Enter IOS_KEYCHAIN_PASSWORD (any strong random value): " IOS_KEYCHAIN_PASS
  echo ""
fi
if [ -n "$IOS_KEYCHAIN_PASS" ]; then
  push_secret_value "IOS_KEYCHAIN_PASSWORD" "$IOS_KEYCHAIN_PASS"
fi

# Provisioning profile
PP="$IOS_DIR/distribution.mobileprovision"
if [ -f "$PP" ]; then
  push_secret_file "IOS_PROVISIONING_PROFILE_BASE64" "$PP"
else
  echo "   ⚠️   $PP not found — skipping IOS_PROVISIONING_PROFILE_BASE64"
  echo "       Download your distribution profile from developer.apple.com"
  echo "       and place it at $PP"
fi

echo ""

# ---------------------------------------------------------------------------
# App Store Connect API (required for ios-testflight.yml)
# ---------------------------------------------------------------------------
echo "🔐  Setting App Store Connect API secrets…"

ASC_PROPS="$IOS_DIR/asc-secrets.properties"
if [ -f "$ASC_PROPS" ]; then
  get_asc_prop() { grep "^$1=" "$ASC_PROPS" | cut -d= -f2-; }
  ASC_KEY_ID="$(get_asc_prop KEY_ID)"
  ASC_ISSUER_ID="$(get_asc_prop ISSUER_ID)"
  [ -n "$ASC_KEY_ID"     ] && push_secret_value "APP_STORE_CONNECT_KEY_ID"     "$ASC_KEY_ID"
  [ -n "$ASC_ISSUER_ID"  ] && push_secret_value "APP_STORE_CONNECT_ISSUER_ID"  "$ASC_ISSUER_ID"
else
  echo "   ⚠️   $ASC_PROPS not found — skipping APP_STORE_CONNECT_KEY_ID / APP_STORE_CONNECT_ISSUER_ID"
  echo "       Create that file with:"
  echo "       KEY_ID=XXXXXXXXXX"
  echo "       ISSUER_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
fi

# .p8 private key — look for AuthKey_<KEY_ID>.p8 first, then bare AuthKey.p8
ASC_P8=""
if [ -n "${ASC_KEY_ID:-}" ] && [ -f "$IOS_DIR/AuthKey_${ASC_KEY_ID}.p8" ]; then
  ASC_P8="$IOS_DIR/AuthKey_${ASC_KEY_ID}.p8"
elif [ -f "$IOS_DIR/AuthKey.p8" ]; then
  ASC_P8="$IOS_DIR/AuthKey.p8"
else
  # Last-ditch: pick any .p8 file in the directory
  ASC_P8=$(find "$IOS_DIR" -maxdepth 1 -name "AuthKey_*.p8" | head -1)
fi

if [ -n "$ASC_P8" ] && [ -f "$ASC_P8" ]; then
  gh secret set "APP_STORE_CONNECT_PRIVATE_KEY" --repo "$REPO_SLUG" < "$ASC_P8"
  echo "   ✅  APP_STORE_CONNECT_PRIVATE_KEY  (from $(basename "$ASC_P8"))"
else
  echo "   ⚠️   No AuthKey*.p8 found in $IOS_DIR — skipping APP_STORE_CONNECT_PRIVATE_KEY"
  echo "       Download the .p8 key from App Store Connect → Users and Access → Integrations"
  echo "       and place it at $IOS_DIR/AuthKey_<KEY_ID>.p8"
  echo "       Or set it manually:"
  echo "       gh secret set APP_STORE_CONNECT_PRIVATE_KEY --repo $REPO_SLUG < ~/Downloads/AuthKey_KEYID.p8"
fi

echo ""
echo "🎉  Done!  Trigger a build at:"
echo "    https://github.com/$REPO_SLUG/actions"

