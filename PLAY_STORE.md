# Publishing to Google Play Store

This guide walks through every step — from creating a Play Console account to
promoting a build to production. It also covers the automated GitHub Actions
workflow that uploads a signed AAB directly to Play Store without manual
downloading.

---

## Overview

| Step                                | Who                                  | Once or per-release                      |
|-------------------------------------|--------------------------------------|------------------------------------------|
| Create Play Console account         | Developer                            | Once                                     |
| Create app & fill store listing     | Developer                            | Once (update text/screenshots as needed) |
| Set up Data Safety & Content Rating | Developer                            | Once (update if data practices change)   |
| Create Privacy Policy               | Developer                            | Once                                     |
| Build signed AAB                    | GitHub Actions                       | Every release                            |
| Upload to Play Store                | GitHub Actions (automated) or manual | Every release                            |
| Promote Internal → Production       | Developer                            | Every release                            |

---

## Step 1 — Create a Google Play Developer account

1. Go to [play.google.com/console](https://play.google.com/console)
2. Sign in with a Google account you want to own the developer profile
3. Pay the **one-time $25 USD** registration fee
4. Complete the account details form (developer name, email, phone)
5. Accept the Developer Distribution Agreement

> **Developer name** shown to users on Play Store. Use `ChurchPresenter` or
> your organisation name.

---

## Step 2 — Create the app in Play Console

1. In Play Console click **Create app**
2. Fill in:
   - **App name:** `Church Presenter`
   - **Default language:** English (United States)
   - **App or game:** App
   - **Free or paid:** Free
3. Accept the declarations → **Create app**

---

## Step 3 — Store listing

Go to **Store presence → Main store listing**.

### App details

| Field                                 | Value                                                                              |
|---------------------------------------|------------------------------------------------------------------------------------|
| **App name**                          | Church Presenter                                                                   |
| **Short description** (80 chars max)  | Control ChurchPresenter — project songs, Bible & slides, build schedules.          |
| **Full description** (4000 chars max) | See template below                                                                 |

**Full description template:**
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
2. Make sure your phone is on the same Wi-Fi network
3. Open the app, go to Settings, and enter the server's IP address and port
4. Start presenting

No account or login required — just your local network.

DESIGNED FOR CHURCH TEAMS
Church Presenter is built for worship leaders, song leaders, and tech volunteers who need the freedom to move around the room without being tied to a laptop. Whether you're leading from the front or managing sound from the back, full control is always in your pocket.

REQUIREMENTS
• ChurchPresenter desktop software running on the same local Wi-Fi network
• Android 7.0 (API 24) or later
```

### Graphics

You must provide all of the following before publishing:

| Asset                 | Size                        | Notes                                                                          |
|-----------------------|-----------------------------|--------------------------------------------------------------------------------|
| **App icon**          | 512 × 512 px PNG            | High-res version of your launcher icon; no rounded corners (Play applies them) |
| **Feature graphic**   | 1024 × 500 px PNG/JPG       | Banner shown at the top of your store listing                                  |
| **Phone screenshots** | Min 2, max 8 — 16:9 or 9:16 | See screenshot guide below                                                     |

#### Taking screenshots

The easiest way is to run the app on a real device or emulator and capture
screens for each main tab:

```bash
# Take a screenshot via ADB (device must be connected)
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png ~/Desktop/screen.png
```

Recommended screens to capture:
1. Songs list (main screen)
2. Songs search in action
3. Settings screen
4. Any "coming soon" tab showing the navigation

---

## Step 4 — Privacy Policy (required)

Google requires a Privacy Policy for any app that uses Firebase or collects
any data. You must host it at a public URL.

**Minimum content to include:**
- What data is collected (see Data Safety section below)
- How it is used (crash reporting, analytics, push notifications)
- That data is processed by Google Firebase
- Contact email for privacy questions

**Free hosting options:**
- GitHub Pages — create a `privacy-policy.md` in a public repo
- [privacypolicygenerator.info](https://www.privacypolicygenerator.info)
- [app-privacy-policy-generator.nisrulz.com](https://app-privacy-policy-generator.nisrulz.com)

Once hosted, paste the URL into:
**Store presence → Store settings → Privacy policy**

---

## Step 5 — Data Safety section

Go to **Policy → Data safety**.

Answer the questionnaire with these values:

| Question                                                              | Answer                             |
|-----------------------------------------------------------------------|------------------------------------|
| Does your app collect or share any of the required user data types?   | **Yes**                            |
| Is all of the user data collected by your app encrypted in transit?   | **Yes**                            |
| Do you provide a way for users to request that their data is deleted? | **No** (or Yes, via contact email) |

**Data collected — tick these:**

| Data type              | Collected | Shared                | Purpose              | Required |
|------------------------|-----------|-----------------------|----------------------|----------|
| Crash logs             | Yes       | Yes (Google Firebase) | Analytics            | No       |
| App interactions       | Yes       | Yes (Google Firebase) | Analytics            | No       |
| App info & performance | Yes       | Yes (Google Firebase) | App functionality    | No       |
| Device or other IDs    | Yes       | Yes (Google Firebase) | Analytics, Messaging | No       |

> All Firebase data collection is handled by Google. Refer to
> [firebase.google.com/support/privacy](https://firebase.google.com/support/privacy)
> for Google's data processing details.

---

## Step 6 — Content rating

Go to **Policy → App content → Content rating**.

1. Click **Start questionnaire**
2. Category: **Utility**
3. Answer all questions — for this app all answers are **No** (no violence,
   no sexual content, no user-generated content, no location sharing, etc.)
4. Submit → you will receive an **Everyone** rating (PEGI 3 / Everyone)

---

## Step 7 — App access

Go to **Policy → App content → App access**.

Select: **All or most functionality is accessible without special access**

(The app only requires a Wi-Fi server address, no login.)

---

## Step 8 — Build and upload the AAB

### Option A — Automated (GitHub Actions → Play Store directly)

The `android-play-store.yml` workflow builds a signed AAB and uploads it
straight to the **internal testing** track.

**One-time setup — create a Play Store service account:**

1. In Play Console → **Setup → API access**
2. Click **Link to a Google Cloud project** (or create a new one)
3. In Google Cloud Console → **IAM & Admin → Service Accounts → Create service account**
   - Name: `github-actions-play`
   - Role: none at project level
4. Create a JSON key for the service account → download it
5. Back in Play Console → **Users and permissions → Invite new users**
   - Email: the service account email (`github-actions-play@...iam.gserviceaccount.com`)
   - Permissions: **Release apps to testing tracks** + **Release apps to production**
6. Add the JSON key as a GitHub secret:

```bash
gh secret set GOOGLE_PLAY_SERVICE_ACCOUNT_JSON \
  --repo ChurchPresenter/ChurchPresenter-Mobile \
  < ~/Downloads/github-actions-play-key.json
```

7. Trigger the workflow:
   - Go to **Actions → Android Play Store Upload**
   - Click **Run workflow**
   - Choose track: `internal` (recommended first) or `beta` / `production`

### Option B — Manual upload

1. Run the **Android Release Build** workflow with output `aab`
2. Download the `release-aab-<number>.zip` artifact and unzip it
3. In Play Console → **Release → Internal testing → Create new release**
4. Drag the `.aab` file into the upload area
5. Add release notes → **Review release** → **Start rollout**

---

## Step 9 — Release tracks

Google Play uses a staged rollout system. Start at **Internal** and promote
upward after testing.

```
Internal testing  →  Closed testing (Alpha)  →  Open testing (Beta)  →  Production
   (100 users)         (invite list)             (anyone opts in)        (everyone)
```

**Recommended flow for first release:**

1. Upload AAB to **Internal testing** — add yourself and a few team members
2. Test thoroughly on real devices
3. Promote to **Beta** — share the opt-in link with church members for broader testing
4. Promote to **Production** — choose rollout percentage (start at 20%, increase over days)

**To promote a release:**
Play Console → Release → [current track] → find the release → **Promote release**

---

## Step 10 — Pre-launch checklist

Before submitting to production, verify everything in Play Console is complete:

- [ ] Store listing: title, description, icon, feature graphic, screenshots
- [ ] Privacy policy URL set
- [ ] Data safety answers submitted
- [ ] Content rating completed (shows "Everyone")
- [ ] App access declared
- [ ] At least one release on internal testing track that has been tested
- [ ] Release notes written (what's new in this version)
- [ ] Target API level ≥ current Play Store requirement (currently API 36 ✅)

---

## Versioning

Every new release **must** increment `versionCode`. `versionName` is the
human-readable version shown to users.

Edit in `composeApp/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 2        // ← must be higher than the previous upload
    versionName = "1.1"    // ← shown on the Play Store
}
```

Commit the version bump before triggering the release build.

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| "You need to publish to at least one testing track" | Complete all store listing required fields first, then upload to internal testing |
| "Version code already used" | Increment `versionCode` in `build.gradle.kts` and rebuild |
| "APK/AAB not signed correctly" | Re-run `push_github_secrets.sh` to refresh the Android keystore secret |
| "Target API level too low" | Ensure `targetSdk` in `libs.versions.toml` meets the current year's Play Store requirement |
| Service account gets 403 | Make sure the service account was granted **Release to testing tracks** permission in Play Console (not just Google Cloud IAM) |

