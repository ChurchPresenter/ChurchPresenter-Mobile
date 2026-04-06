# Installing Release Builds for Testing

This guide explains how to trigger a signed build from GitHub Actions and get
it onto a tester's device — for both **Android** and **iOS**.

---

## Android

### Step 1 — Trigger the build

1. Go to **[Actions → Android Release Build](https://github.com/ChurchPresenter/ChurchPresenter-Mobile/actions/workflows/android-release.yml)**
2. Click **Run workflow**
3. Set **Output format** to **`apk`** (APK installs directly; AAB is for the Play Store only)
4. Click the green **Run workflow** button

### Step 2 — Download the APK

1. Once the workflow completes (≈ 5 min), click the finished run
2. Scroll to **Artifacts** at the bottom of the page
3. Download **`release-apk-<number>.zip`**
4. Unzip it — you'll find **`composeApp-release.apk`** inside

### Step 3 — Install on the device

Two ways to install — pick whichever suits the tester.

---

#### Option A — Tap to install (no computer needed)

**Send the APK to the tester** (email, Google Drive, Slack, etc.)

On the tester's Android device:

1. Open the APK file (tap the download notification or find it in Files)
2. If prompted, enable **"Install unknown apps"** for the app you used to open it
   - *Settings → Apps → [Browser/Files app] → Install unknown apps → Allow*
3. Tap **Install**
4. Tap **Open**

> **Note:** Android may show a "Play Protect" warning. Tap **Install anyway** —
> the APK is signed with your release key, not a debug key.

---

#### Option B — ADB sideload (USB, recommended for developers)

Requires **Android Debug Bridge (ADB)** on your computer and **USB Debugging**
enabled on the device.

**One-time device setup:**
1. On the Android device go to *Settings → About phone*
2. Tap **Build number** 7 times to unlock Developer Options
3. Go to *Settings → Developer Options* → enable **USB Debugging**
4. Connect the device via USB and tap **Allow** on the device when prompted

**Install ADB (if not already installed):**
```bash
# macOS (Homebrew)
brew install android-platform-tools

# Windows — download the SDK Platform Tools zip from:
# https://developer.android.com/tools/releases/platform-tools
# Unzip and add the folder to your PATH

# Linux (Debian/Ubuntu)
sudo apt install adb
```

**Verify the device is detected:**
```bash
adb devices
# Should show something like:
# List of devices attached
# R58M12345AB   device
```

**Install the APK:**
```bash
adb install path/to/composeApp-release.apk
```

**Useful ADB extras:**
```bash
# Force reinstall (keeps app data) — useful for updates
adb install -r path/to/composeApp-release.apk

# Install and grant all runtime permissions automatically
adb install -r -g path/to/composeApp-release.apk

# Launch the app immediately after install
adb shell am start -n com.church.presenter.churchpresentermobile/.MainActivity

# View live logs from the app
adb logcat -s churchpresentermobile
```

> **Tip:** If `adb devices` shows `unauthorized`, unlock the device screen and
> tap **Allow** on the USB Debugging popup, then retry.

---

## iOS

iOS has two distribution paths for testers:

| Method         | Best for                     | Requires                    |
|----------------|------------------------------|-----------------------------|
| **Ad Hoc**     | Small groups, quick testing  | Register each device's UDID |
| **TestFlight** | Larger groups, Apple-managed | App Store Connect setup     |

---

### Option A — Ad Hoc (direct IPA install)

#### 1. Register each tester's device UDID

Each iOS device must be listed in your provisioning profile before the build.

**Find the UDID:**
- Connect the device to a Mac, open **Finder**, select the device, click the
  device model name under its name until a long hex string appears — that's the UDID
- Or: iPhone → *Settings → General → VPN & Device Management → [trust]*;
  alternatively use *[Apple Configurator 2](https://apps.apple.com/app/apple-configurator-2/id1037126344)*

**Register in Apple Developer portal:**
1. [developer.apple.com → Devices → +](https://developer.apple.com/account/resources/devices/add)
2. Enter the device name and UDID → **Continue** → **Register**

#### 2. Create (or update) an Ad Hoc provisioning profile

1. [developer.apple.com → Profiles → +](https://developer.apple.com/account/resources/profiles/add)
2. Under **Distribution** select **Ad Hoc** → Continue
3. App ID: `com.church.presenter.churchpresentermobile` → Continue
4. Certificate: **Apple Distribution: Your Name (XXXXXXXXXX)** → Continue
5. Devices: select all registered test devices → Continue
6. Name it **`ChurchPresenter AdHoc`** → **Generate** → **Download**

#### 3. Push the new provisioning profile to GitHub

```bash
bash scripts/push_ios_profile.sh ~/Downloads/ChurchPresenter_AdHoc.mobileprovision
```

Also update `IOS_PROVISIONING_PROFILE_NAME` in GitHub secrets to match the new profile name:

```bash
gh secret set IOS_PROVISIONING_PROFILE_NAME \
  --repo ChurchPresenter/ChurchPresenter-Mobile \
  --body "ChurchPresenter AdHoc"
```

#### 4. Trigger the build

1. Go to **[Actions → iOS Release Build](https://github.com/ChurchPresenter/ChurchPresenter-Mobile/actions/workflows/ios-release.yml)**
2. Click **Run workflow**
3. Set **IPA export method** to **`ad-hoc`**
4. Click **Run workflow** (build takes ≈ 15–20 min)

#### 5. Download the IPA

1. Click the finished run → scroll to **Artifacts**
2. Download **`release-ipa-<number>.zip`**
3. Unzip → you'll have **`iosApp.ipa`**

#### 6. Install the IPA on the device

**Option 1 — Finder (macOS)**
1. Connect the device via USB, open **Finder**, click the device in the sidebar
2. Click **Files** tab (or drag into the device in **Apps**)
3. Drag `iosApp.ipa` onto the device in Finder → wait for install

**Option 2 — Apple Configurator 2**
1. Install [Apple Configurator 2](https://apps.apple.com/app/apple-configurator-2/id1037126344) from the Mac App Store
2. Connect device, double-click it
3. **Add → App** → drag in `iosApp.ipa`

**Option 3 — Diawi / install.ly (wireless)**
1. Upload `iosApp.ipa` to [diawi.com](https://www.diawi.com) or [install.ly](https://install.ly)
2. Share the generated link / QR code with testers
3. Tester opens the link in **Safari** on their device → **Install**

> **Trust the developer certificate** after first install:
> *Settings → General → VPN & Device Management →
> Apple Distribution: Your Name → Trust*

---

### Option B — TestFlight (recommended for regular testing)

TestFlight handles device registration automatically and sends update
notifications to testers. It requires a one-time App Store Connect setup.

#### 1. Trigger an App Store build

1. Go to **[Actions → iOS Release Build](https://github.com/ChurchPresenter/ChurchPresenter-Mobile/actions/workflows/ios-release.yml)**
2. Click **Run workflow** → set export method to **`app-store`**
3. Download the IPA artifact once the build completes

#### 2. Upload to App Store Connect

**Using Transporter (easiest):**
1. Install [Transporter](https://apps.apple.com/app/transporter/id1450874784) from the Mac App Store
2. Sign in with your Apple ID
3. Click **+** → select the IPA → click **Deliver**

**Using Xcode Organizer:**
1. Open Xcode → **Window → Organizer**
2. If the archive doesn't appear, use **Organizer → Archives → +** to import
   the IPA, or go via **Distribute App → App Store Connect**

#### 3. Add testers in TestFlight

1. Go to [App Store Connect](https://appstoreconnect.apple.com) →
   your app → **TestFlight**
2. The uploaded build will appear after Apple's processing (≈ 10–30 min)
3. **Internal testers:** add App Store Connect users directly (no review needed)
4. **External testers:** create a group → add email addresses → submit for
   Beta App Review (≈ 1 day, only first build needs review)
5. Testers receive an email invitation and install via the **TestFlight** app

---

## Quick reference

| Platform             | Artifact | Workflow input | Install method                |
|----------------------|----------|----------------|-------------------------------|
| Android              | `.apk`   | `apk`          | Side-load APK                 |
| Android (Play Store) | `.aab`   | `aab`          | Upload to Play Console        |
| iOS Ad Hoc           | `.ipa`   | `ad-hoc`       | Finder / Configurator / Diawi |
| iOS TestFlight       | `.ipa`   | `app-store`    | Transporter → TestFlight      |

---

## Troubleshooting

| Problem                        | Solution                                                                                                                   |
|--------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| Android: *"App not installed"* | Uninstall any previous version first, then re-install                                                                      |
| Android: *"Parse error"*       | The APK may be corrupted — re-download and try again                                                                       |
| iOS: *"Unable to Install"*     | Device UDID is not in the Ad Hoc provisioning profile — re-register and rebuild                                            |
| iOS: *"Untrusted Developer"*   | Go to Settings → General → VPN & Device Management → Trust the certificate                                                 |
| iOS: *app crashes on launch*   | Check that `GoogleService-Info.plist` secret is set and matches the bundle ID `com.church.presenter.churchpresentermobile` |
| Artifact expired               | GitHub retains artifacts for 30 days — trigger a new build                                                                 |

