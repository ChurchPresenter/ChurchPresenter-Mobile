# GitHub Secrets Setup for Signed Builds

This document explains every GitHub Actions secret required to produce signed
Android (AAB / APK) and iOS (IPA) release builds and upload them directly to
the stores from the following workflows:

| Workflow                      | Purpose                                  |
|-------------------------------|------------------------------------------|
| **Android Release Build**     | Builds a signed AAB / APK artifact       |
| **Android Play Store Upload** | Builds + uploads directly to Google Play |
| **iOS Release Build**         | Builds a signed IPA artifact             |
| **iOS TestFlight Upload**     | Builds + uploads directly to TestFlight  |

---

## ⚡ Quick start — automated script (recommended)

The script `scripts/push_github_secrets.sh` reads every artefact from the
signing repo and pushes all secrets to GitHub in one command.

```bash
# 1. Install & authenticate the GitHub CLI (once)
brew install gh
gh auth login

# 2. Run the script (signing repo is auto-detected when placed next to this repo)
bash scripts/push_github_secrets.sh

# Or specify the path explicitly:
bash scripts/push_github_secrets.sh /path/to/signing-repo
```

The script handles base64 encoding with `-b 0` automatically and validates that
every file exists before uploading. After it completes, trigger a build from the
**Actions** tab — no manual secret entry required.

---

## Manual setup (if you prefer)

Add secrets individually at:
`https://github.com/<org>/<repo>/settings/secrets/actions`

> **⚠️ Important — always encode with `-b 0` (no line wrapping)**
>
> macOS `base64` wraps output at 76 characters by default.  Those embedded
> newlines cause `Malformed root json` and `empty after decode` errors when
> the runner decodes the secret.  Every `base64` encode command below includes
> `-b 0` which disables wrapping and produces a clean single-line value.
> If you ever re-encode a file, always use `-b 0`.
>
> **Common error:** `ERROR: /tmp/release.keystore is empty after decode`  
> **Cause:** The `ANDROID_KEYSTORE_BASE64` secret is not set or is empty.  
> **Fix:** Run `bash scripts/push_github_secrets.sh` or manually encode the
> keystore with `base64 -b 0 -i church-presenter.jks | pbcopy` and paste the
> result into the GitHub secret.

---

## Android secrets

### `ANDROID_KEYSTORE_BASE64`
The release keystore file, base64-encoded.

**Where to find it:** `signing-repo/mobile/` — the `.jks` or
`.keystore` file referenced in `signing.properties`.

```bash
# Encode the keystore and copy to clipboard
# -b 0 disables macOS base64 line-wrapping (produces a clean single-line value)
base64 -b 0 -i /path/to/signing-repo/mobile/release.keystore | pbcopy
```

---

### `ANDROID_STORE_PASSWORD`
The keystore password. Found in `signing-repo/mobile/signing.properties`
as the `storePassword` value.

---

### `ANDROID_KEY_ALIAS`
The signing key alias. Found in `signing.properties` as the `keyAlias` value.

---

### `ANDROID_KEY_PASSWORD`
The key password. Found in `signing.properties` as the `keyPassword` value.

---

### `GOOGLE_SERVICES_JSON_BASE64`
`google-services.json` (Android Firebase config), base64-encoded.

**Where to find it:** `signing-repo/mobile/google-services.json`

```bash
# -b 0 disables line-wrapping — always use this when encoding secrets
base64 -b 0 -i /path/to/signing-repo/mobile/google-services.json | pbcopy
```

---

### `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`
The raw JSON key for the Google Play service account that GitHub Actions uses to
upload AABs directly to Play Store.  Used by the **Android Play Store Upload** workflow.

**How to create:**
1. Play Console → **Setup → API access** → link to a Google Cloud project
2. Google Cloud Console → **IAM & Admin → Service Accounts → Create service account**
   - Name: `github-actions-play`
3. Create a **JSON key** for that service account → download the `.json` file
4. Play Console → **Users and permissions → Invite new users**
   - Email: `github-actions-play@<project>.iam.gserviceaccount.com`
   - Permission: **Release apps to testing tracks** + **Release apps to production**
5. Save the JSON key to `signing-repo/mobile/play-store-service-account.json`
   (the `push_github_secrets.sh` script will pick it up automatically on next run)

**Or set it manually right now:**
```bash
gh secret set GOOGLE_PLAY_SERVICE_ACCOUNT_JSON \
  --repo <org>/<repo> \
  < ~/Downloads/github-actions-play-key.json
```

> ⚠️ This secret is the **raw JSON file**, not base64-encoded.  Feed it with
> `< file` (stdin redirect), not `--body`.

---

## iOS secrets

### `IOS_CERTIFICATE_BASE64`
Apple **Distribution** certificate exported as a `.p12` file, base64-encoded.

**How to export:**
1. Open **Keychain Access** → login keychain → **My Certificates**
2. Find *"Apple Distribution: Your Org (XXXXXXXXXX)"*
3. Right-click → **Export** → save as `distribution.p12`, set a password
4. Encode:

```bash
# -b 0 disables line-wrapping — always use this when encoding secrets
base64 -b 0 -i ~/Desktop/distribution.p12 | pbcopy
```

> You can also export the certificate stored in
> `signing-repo/mobile/ios/` if one has been saved there.

---

### `IOS_CERTIFICATE_PASSWORD`
The password you set when exporting the `.p12` file above.

---

### `IOS_PROVISIONING_PROFILE_BASE64`
Distribution provisioning profile (`.mobileprovision`), base64-encoded.

**How to obtain:**
1. Log in to [developer.apple.com](https://developer.apple.com) → **Profiles**
2. Download the distribution profile for `com.church.presenter.churchpresentermobile`
3. Encode:

```bash
# -b 0 disables line-wrapping — always use this when encoding secrets
base64 -b 0 -i ~/Downloads/ChurchPresenter_Distribution.mobileprovision | pbcopy
```

---

### `IOS_TEAM_ID`
Your 10-character Apple Developer Team ID.

**Where to find it:**
[developer.apple.com → Membership → Team ID](https://developer.apple.com/account)

Example: `A1B2C3D4E5`

---

### `IOS_CODE_SIGN_IDENTITY`
The exact name of your distribution certificate as it appears in Keychain Access.

Example: `Apple Distribution: Church Presenter (A1B2C3D4E5)`

```bash
# List available identities to copy the exact string
security find-identity -v -p codesigning
```

---

### `IOS_PROVISIONING_PROFILE_NAME`
The **name** of the provisioning profile as it appears in the Apple Developer portal
(not the filename).

Example: `ChurchPresenter Distribution`

---

### `IOS_KEYCHAIN_PASSWORD`
An **arbitrary** password used only for the temporary CI keychain.
Use any strong random string — it is discarded after each workflow run.

```bash
# Generate a random password
openssl rand -base64 32 | pbcopy
```

---

### `GOOGLE_SERVICE_INFO_PLIST_BASE64`
`GoogleService-Info.plist` (iOS Firebase config), base64-encoded.

**Where to find it:** `signing-repo/mobile/ios/GoogleService-Info.plist`

```bash
# -b 0 disables line-wrapping — always use this when encoding secrets
base64 -b 0 -i /path/to/signing-repo/mobile/ios/GoogleService-Info.plist | pbcopy
```

---

## Summary table

| Secret                             | Platform | Source                                                         |
|------------------------------------|----------|----------------------------------------------------------------|
| `ANDROID_KEYSTORE_BASE64`          | Android  | `signing-repo/mobile/*.jks`                                    |
| `ANDROID_STORE_PASSWORD`           | Android  | `signing.properties → storePassword`                           |
| `ANDROID_KEY_ALIAS`                | Android  | `signing.properties → keyAlias`                                |
| `ANDROID_KEY_PASSWORD`             | Android  | `signing.properties → keyPassword`                             |
| `GOOGLE_SERVICES_JSON_BASE64`      | Android  | `signing-repo/mobile/google-services.json`                     |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Android  | `signing-repo/mobile/play-store-service-account.json`          |
| `IOS_CERTIFICATE_BASE64`           | iOS      | Exported from Keychain / signing repo                          |
| `IOS_CERTIFICATE_PASSWORD`         | iOS      | P12 export password                                            |
| `IOS_PROVISIONING_PROFILE_BASE64`  | iOS      | Apple Developer portal                                         |
| `IOS_TEAM_ID`                      | iOS      | Apple Developer → Membership                                   |
| `IOS_CODE_SIGN_IDENTITY`           | iOS      | `security find-identity -v -p codesigning`                     |
| `IOS_PROVISIONING_PROFILE_NAME`    | iOS      | Apple Developer portal profile name                            |
| `IOS_KEYCHAIN_PASSWORD`            | iOS      | Any strong random string                                       |
| `GOOGLE_SERVICE_INFO_PLIST_BASE64` | iOS      | `signing-repo/mobile/ios/GoogleService-Info.plist`             |
| `APP_STORE_CONNECT_KEY_ID`         | iOS      | App Store Connect → Users and Access → Integrations → API Keys |
| `APP_STORE_CONNECT_ISSUER_ID`      | iOS      | Same page as Key ID (UUID shown at the top)                    |
| `APP_STORE_CONNECT_PRIVATE_KEY`    | iOS      | `signing-repo/mobile/ios/AuthKey.p8` (raw, not base64)         |

---

## App Store Connect API secrets (iOS TestFlight upload)

Required only for the **iOS TestFlight Upload** workflow.

### `APP_STORE_CONNECT_KEY_ID`
The 10-character ID of an App Store Connect API key with **App Manager** role.

**Where to find it:**
App Store Connect → **Users and Access → Integrations → App Store Connect API → Generate API Key**

### `APP_STORE_CONNECT_ISSUER_ID`
The UUID shown at the top of the same API Keys page (shared across all keys in your team).

### `APP_STORE_CONNECT_PRIVATE_KEY`
The raw content of the `.p8` private key file downloaded when creating the key.

> ⚠️ This secret is stored as **plain text** (not base64). Feed it with `< file` (stdin redirect).
> The key can only be downloaded once — store it in `signing-repo/mobile/ios/AuthKey.p8`.

```bash
# Set manually:
gh secret set APP_STORE_CONNECT_PRIVATE_KEY \
  --repo <org>/<repo> \
  < ~/Downloads/AuthKey_XXXXXXXXXX.p8
```

---

## Triggering a build

| Goal | Workflow to run |
|---|---|
| Signed IPA to download | **iOS Release Build** (ad-hoc or app-store) |
| Upload directly to TestFlight | **iOS TestFlight Upload** |
| Signed AAB / APK to download | **Android Release Build** |
| Upload directly to Google Play | **Android Play Store Upload** |

1. Go to the **Actions** tab in GitHub
2. Select the desired workflow
3. Click **Run workflow**
4. Download the artifact (if applicable) from the **Artifacts** section of the completed run

