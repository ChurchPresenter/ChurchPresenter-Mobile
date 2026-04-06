# Publishing to Apple App Store

This guide covers every field required to submit Church Presenter to the
App Store via App Store Connect, plus the automated GitHub Actions workflow
that builds and uploads a signed IPA.

---

## Overview

| Step                             | Who              | Once or per-release            |
|----------------------------------|------------------|--------------------------------|
| Enrol in Apple Developer Program | Developer        | Once                           |
| Create app in App Store Connect  | Developer        | Once                           |
| Fill store listing               | Developer        | Once (update as needed)        |
| App Privacy answers              | Developer        | Once (update if data changes)  |
| Age rating questionnaire         | Developer        | Once                           |
| Build signed IPA                 | GitHub Actions   | Every release                  |
| Upload to TestFlight             | GitHub Actions   | Every release                  |
| Submit for App Review            | Developer        | Every release                  |
| Release to App Store             | Developer        | Every release                  |

---

## Step 1 — Apple Developer Program

1. Go to [developer.apple.com/programs](https://developer.apple.com/programs)
2. Enrol with the Apple ID that will own the app (individual or organisation)
3. Pay the **$99 USD / year** fee
4. Wait for approval (usually instant for individuals, a few days for organisations)

---

## Step 2 — Create the app in App Store Connect

1. Go to [appstoreconnect.apple.com](https://appstoreconnect.apple.com)
2. **My Apps → (+) New App**
3. Fill in:
   - **Platform:** iOS
   - **Name:** `Church Presenter`
   - **Primary language:** English (U.S.)
   - **Bundle ID:** `com.church.presenter.churchpresentermobile`
   - **SKU:** `church-presenter-ios` (internal reference, not shown to users)
4. Click **Create**

---

## Step 3 — Store listing

Go to **App Store → [version] → App Information + App Store listing (English)**.

### App name & subtitle

| Field        | Limit     | Value                                         |
|--------------|-----------|-----------------------------------------------|
| **Name**     | 30 chars  | `Church Presenter`                            |
| **Subtitle** | 30 chars  | `Present songs, Bible & slides`               |

> The subtitle appears directly below the app name in search results and on the
> product page. It is one of the strongest ASO fields — keep it keyword-rich.

---

### Promotional text

> Max **170 characters.** Shown above the description. Can be updated at any
> time **without** submitting a new build — ideal for announcements.

```
Control your church presentation screen from anywhere in the room.
Project songs, Bible verses, and media — all over Wi-Fi.
```

*(154 characters)*

---

### Description

> Max **4 000 characters.** Shown on the App Store product page.
> Apple does **not** render markdown — use plain text with spacing for structure.

```
Church Presenter is the official mobile companion for the ChurchPresenter desktop application. Connect to your church's presentation computer over Wi-Fi and take full remote control — from anywhere in the room.

PROJECT TO SCREEN INSTANTLY
Tap any item to send it live to the screen in seconds:
• Songs — browse your full hymn and worship song library, search by number or title, filter by songbook, and project lyrics directly to the audience
• Bible — navigate any book, chapter, and verse, then put the passage on screen with one tap
• Pictures — browse your media library and project any image
• Presentations — open and display PowerPoint or Keynote-style slide decks

BUILD YOUR SERVICE SCHEDULE
Planning a service? Add songs, Bible readings, pictures, and presentations to the schedule directly from your phone. The schedule syncs with the ChurchPresenter desktop in real time so your tech team always sees what's coming next.

FULL REMOTE CONTROL
• Project any content to the screen with a single tap
• Stop projecting and clear the screen remotely
• View the current service schedule from the slide-out schedule drawer
• Refresh the schedule at any time to stay in sync with the desktop

SIMPLE SETUP
1. Run ChurchPresenter on a Mac or Windows PC connected to your projector
2. Make sure your iPhone is on the same Wi-Fi network
3. Open the app, go to Settings, and enter the server's IP address and port
4. Start presenting

No account or login required — just your local network.

DESIGNED FOR CHURCH TEAMS
Church Presenter is built for worship leaders, song leaders, and tech volunteers who need the freedom to move around the room without being tied to a laptop. Whether you're leading from the front or managing sound from the back, full control is always in your pocket.

REQUIREMENTS
• ChurchPresenter desktop software running on the same local Wi-Fi network
• iPhone running iOS 18.2 or later
```

*(~1 750 characters — well within the 4 000-character limit)*

---

### Keywords

> Max **100 characters** (including commas). Not shown to users — used only for
> search ranking. Do **not** repeat words already in the app name or subtitle.

```
church,worship,presentation,projector,remote,songs,hymns,bible,lyrics,presenter
```

*(80 characters)*

---

### Support URL

A publicly accessible page where users can get help.

Options:
- GitHub repo: `https://github.com/ChurchPresenter/ChurchPresenter-Mobile`
- A simple GitHub Pages site or README link

---

### Privacy Policy URL

Required because the app uses Firebase. Host it somewhere public (GitHub Pages,
a simple static site, or a generator) and paste the URL here.

See **PLAY_STORE.md Step 4** for suggested content and free hosting options.

---

## Step 4 — App Privacy (Data Types)

Go to **App Privacy → Get Started** and answer as follows.

### Does your app collect data?  →  **Yes**

| Data type                     | Collected | Used for                        | Linked to user | Tracking |
|-------------------------------|-----------|---------------------------------|----------------|----------|
| Crash data                    | Yes       | App functionality, Analytics    | No             | No       |
| Performance data              | Yes       | App functionality, Analytics    | No             | No       |
| Other diagnostic data         | Yes       | App functionality               | No             | No       |
| Device ID                     | Yes       | Analytics, App functionality    | No             | No       |

> All data collection is handled by Firebase/Google. No data is linked to the
> user's identity and none is used for third-party advertising or tracking.

---

## Step 5 — Age rating

Go to **General → Age Rating → Edit**.

Answer every question **No** — there is no violence, mature/suggestive themes,
profanity, gambling, medical content, or user-generated content in this app.

**Result:** `4+`

---

## Step 6 — App category

| Field                  | Value            |
|------------------------|------------------|
| **Primary category**   | Utilities        |
| **Secondary category** | Productivity     |

---

## Step 7 — Screenshots & App Preview

Apple requires at least one screenshot per supported device group.
Minimum required sets (others are optional but recommended):

| Device               | Canvas size          |
|----------------------|----------------------|
| 6.9" (iPhone 16 Pro Max) | 1320 × 2868 px  |
| 6.5" (iPhone 11 Pro Max / 12 Pro Max) | 1242 × 2688 px |
| 5.5" (iPhone 8 Plus) | 1242 × 2208 px       |
| iPad Pro 13" (M4)    | 2064 × 2752 px       |

> Tip: If you upload 6.9" screenshots, App Store Connect can auto-scale them
> for older iPhone sizes — check the "Use 6.9" display" checkbox.

Recommended screens to capture:
1. Songs list (main screen)
2. Song detail / lyric projection
3. Settings screen
4. Schedule drawer open

---

## Step 8 — What's New (release notes)

For the **first submission**, use:

```
Initial release of Church Presenter for iOS.

Control your ChurchPresenter desktop from your iPhone over Wi-Fi:
• Project songs, Bible verses, pictures, and presentations
• Build and manage your service schedule
• Full remote control — no account required
```

---

## Step 9 — Build and upload the IPA

### Option A — Automated (GitHub Actions → TestFlight directly)

The `ios-testflight.yml` workflow builds a signed IPA and uploads it straight
to **TestFlight** without any manual steps.

**One-time setup — create an App Store Connect API key:**

1. App Store Connect → **Users and Access → Integrations → App Store Connect API**
2. Click **Generate API Key**
   - Name: `github-actions-testflight`
   - Access: **App Manager**
3. Download the `.p8` key file *(can only be downloaded once — save it safely)*
4. Note the **Key ID** (10 chars) and **Issuer ID** (UUID at the top of the page)
5. Save files to the signing repo and push secrets:

```bash
# Save the .p8 key to the signing repo
cp ~/Downloads/AuthKey_XXXXXXXXXX.p8 \
  /path/to/signing-repo/mobile/ios/AuthKey.p8

# Add Key ID and Issuer ID to the properties file
cat > /path/to/signing-repo/mobile/ios/asc-secrets.properties << EOF
KEY_ID=XXXXXXXXXX
ISSUER_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
EOF

# Push all secrets (including the new ones) to GitHub
bash scripts/push_github_secrets.sh
```

Or push the three secrets manually right now:

```bash
gh secret set APP_STORE_CONNECT_KEY_ID     --repo <org>/<repo> --body "XXXXXXXXXX"
gh secret set APP_STORE_CONNECT_ISSUER_ID  --repo <org>/<repo> --body "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
gh secret set APP_STORE_CONNECT_PRIVATE_KEY --repo <org>/<repo> < ~/Downloads/AuthKey_XXXXXXXXXX.p8
```

6. Trigger the workflow:
   - Go to **Actions → iOS TestFlight Upload**
   - Click **Run workflow**

The build typically appears in TestFlight within 5–15 minutes of the workflow completing.

### Option B — Manual (Xcode)

1. Open `iosApp/iosApp.xcworkspace` in Xcode
2. Select **Any iOS Device (arm64)** as the destination
3. **Product → Archive**
4. In the Organiser window → **Distribute App → App Store Connect → Upload**

---

## Step 10 — App Encryption Documentation (Export Compliance)

Apple requires a short purpose description before you can upload encryption
documentation (EARS self-classification).

**App functionality & purpose** *(261 / 300 characters)*:

```
Church Presenter is a Wi-Fi remote control app for the ChurchPresenter desktop application. Worship leaders and tech volunteers can project songs, Bible verses, images, and presentations to a screen from their iPhone. No account required — local network only.
```

> Select **"No"** when asked if the app uses encryption beyond standard OS/TLS —
> Ktor uses the platform's built-in HTTPS and no custom cryptography is implemented.

---

## Step 11 — TestFlight & review

1. Once the build appears in TestFlight, add yourself as an **Internal Tester**
2. Test on a real device — verify song projection, Bible navigation, settings
3. When ready, go to **App Store → [version] → Submit for Review**
4. Answer the export compliance question:
   - *Does your app use encryption beyond what is provided by the OS?* → **No**
   (Ktor uses standard TLS provided by iOS — no custom encryption)
5. Click **Submit to App Review**

Review typically takes **24–48 hours** for a new app.

---

## Versioning

Every new build uploaded to App Store Connect must have a higher **Build Number**
even if the **Version** stays the same.

Edit in `composeApp/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 2        // ← Build Number in App Store Connect
    versionName = "1.1"    // ← Version string shown on the App Store
}
```

Commit the bump before triggering the release build.

---

## Troubleshooting

| Problem                                | Solution                                                                                |
|----------------------------------------|-----------------------------------------------------------------------------------------|
| "Missing compliance" in TestFlight     | Answer export compliance: No custom encryption                                          |
| "Invalid Bundle" on upload             | Ensure `PRODUCT_BUNDLE_IDENTIFIER` matches `com.church.presenter.churchpresentermobile` |
| Build not appearing in TestFlight      | Wait up to 15 min; check Processing status in App Store Connect                         |
| "No profiles for your application"     | Re-run `push_github_secrets.sh` to refresh provisioning profile secret                  |
| App rejected — guideline 4.0 (Design)  | Make sure screenshots are actual app screenshots, not mockups with misleading UI        |
| App rejected — guideline 5.1 (Privacy) | Ensure Privacy Policy URL is live and accessible                                        |


