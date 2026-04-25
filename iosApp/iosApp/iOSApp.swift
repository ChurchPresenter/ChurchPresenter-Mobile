import ComposeApp
import FirebaseCore
import FirebaseCrashlytics
import FirebaseMessaging
import FirebaseRemoteConfig
import StoreKit
import SwiftUI
import UserNotifications

// MARK: - Constants

private enum AppConstants {
    static let bundleId           = "com.church.presenter.churchpresentermobile"
    static let appStoreLookupUrl  = "https://itunes.apple.com/lookup?bundleId=\(bundleId)"
    static let appOpenCountKey    = "app_open_count"
    static let fcmTokenKey        = "fcm_token"
}

private enum AppUpdateStrings {
    static let alertTitle   = "Update Available"
    static let alertMessage = "Version %@ is available. Update now for the latest features and fixes."
    static let updateButton = "Update"
    static let laterButton  = "Later"
}

// MARK: - App entry point

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                // Forward churchpresenter://connect?… URLs to the Kotlin deep-link handler.
                // Works for both cold starts and when the app is already running.
                .onOpenURL { url in
                    MainViewControllerKt.handleDeepLinkUrl(url: url.absoluteString)
                }
        }
    }
}

// MARK: - AppDelegate

class AppDelegate: NSObject, UIApplicationDelegate,
                   UNUserNotificationCenterDelegate,
                   MessagingDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // 1. Initialise Firebase (Crashlytics auto-starts here)
        FirebaseApp.configure()

        // 1a. Bridge iOS Crashlytics to Kotlin — lets CrashReporting.ios.kt
        //     route non-fatal exceptions and logs to the real Crashlytics SDK.
        IosCrashlyticsReporterBridge.shared.reporter = SwiftCrashlyticsReporter()

        // 2. Track launches and show the App Store review prompt at milestones
        let defaults = UserDefaults.standard
        let openCount = defaults.integer(forKey: AppConstants.appOpenCountKey) + 1
        defaults.set(openCount, forKey: AppConstants.appOpenCountKey)
        if openCount == 3 || openCount == 10 || (openCount > 10 && openCount % 20 == 0) {
            if let scene = UIApplication.shared.connectedScenes
                .first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene {
                AppStore.requestReview(in: scene)
            }
        }

        // 3. Check App Store for a newer version (delayed to not block launch)
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            self?.checkForAppStoreUpdate()
        }

        // 4. Remote Config — set defaults and fetch
        let rc = RemoteConfig.remoteConfig()
        let rcSettings = RemoteConfigSettings()
        rcSettings.minimumFetchInterval = 3600
        rc.configSettings = rcSettings
        try? rc.setDefaults(from: [
            "maintenance_mode"            : "false",
            "min_app_version"             : "1",
            "announcement_banner"         : "",
            "feature_bible_enabled"       : "true",
            "feature_songs_enabled"       : "true",
            "feature_pictures_enabled"    : "true",
            "feature_presentation_enabled": "true",
            "is_demo_mode"                : "false",
        ])
        rc.fetchAndActivate { status, error in
            if let error = error {
                print("RemoteConfig fetchAndActivate error: \(error)")
            } else {
                print("RemoteConfig activated: \(status != .error)")
            }
            // Push all fetched values to the Kotlin RemoteConfig bridge regardless
            // of whether the fetch succeeded — Firebase returns the last cached
            // (or default) values even on error, so Kotlin always gets something.
            // Dispatch to main thread so Kotlin Compose state updates safely.
            let keys = [
                "maintenance_mode", "min_app_version", "announcement_banner",
                "feature_bible_enabled", "feature_songs_enabled",
                "feature_pictures_enabled", "feature_presentation_enabled",
                "is_demo_mode"
            ]
            var values: [String: String] = [:]
            for key in keys {
                values[key] = rc[key].stringValue ?? ""
            }
            DispatchQueue.main.async {
                KotlinRemoteConfig.shared.applyValues(values: values)
            }
        }

        // 5. FCM delegate
        Messaging.messaging().delegate = self

        // 6. Request notification permission
        UNUserNotificationCenter.current().delegate = self
        UNUserNotificationCenter.current().requestAuthorization(
            options: [.alert, .badge, .sound]
        ) { granted, _ in
            print("Notification permission granted: \(granted)")
        }
        application.registerForRemoteNotifications()

        return true
    }

    // MARK: - Home-screen quick actions (long-press app icon)

    func application(
        _ application: UIApplication,
        performActionFor shortcutItem: UIApplicationShortcutItem,
        completionHandler: @escaping (Bool) -> Void
    ) {
        switch shortcutItem.type {
        case "com.church.presenter.churchpresentermobile.songs":
            MainViewControllerKt.navigateToTab(tab: "songs")
        case "com.church.presenter.churchpresentermobile.bible":
            MainViewControllerKt.navigateToTab(tab: "bible")
        default:
            break
        }
        completionHandler(true)
    }

    // Forward APNs device token to Firebase
    func application(        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }

    // Handle APNs registration failure
    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("APNs registration failed: \(error.localizedDescription)")
    }

    // Handle silent / data-only push in background (content-available: 1)
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        // Forward to Firebase Messaging so it can process the message internally
        // (e.g. update FCM token if needed).
        Messaging.messaging().appDidReceiveMessage(userInfo)
        completionHandler(.newData)
    }

    // MARK: - App Store update check

    private func checkForAppStoreUpdate() {
        guard let url = URL(string: AppConstants.appStoreLookupUrl) else { return }
        URLSession.shared.dataTask(with: url) { [weak self] data, _, _ in
            guard
                let data = data,
                let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                let result = (json["results"] as? [[String: Any]])?.first,
                let storeVersion  = result["version"] as? String,
                let storeUrlStr   = result["trackViewUrl"] as? String,
                let currentVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String,
                storeVersion.compare(currentVersion, options: .numeric) == .orderedDescending
            else { return }

            DispatchQueue.main.async {
                self?.showUpdateAlert(storeVersion: storeVersion, storeUrl: storeUrlStr)
            }
        }.resume()
    }

    private func showUpdateAlert(storeVersion: String, storeUrl: String) {
        guard
            let scene = UIApplication.shared.connectedScenes
                .first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene,
            let rootVC = scene.windows.first?.rootViewController
        else { return }

        let message = String(format: AppUpdateStrings.alertMessage, storeVersion)
        let alert = UIAlertController(
            title:          AppUpdateStrings.alertTitle,
            message:        message,
            preferredStyle: .alert
        )
        alert.addAction(UIAlertAction(title: AppUpdateStrings.updateButton, style: .default) { _ in
            guard let url = URL(string: storeUrl) else { return }
            UIApplication.shared.open(url)
        })
        alert.addAction(UIAlertAction(title: AppUpdateStrings.laterButton, style: .cancel))
        rootVC.present(alert, animated: true)
    }

    // MARK: MessagingDelegate

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else { return }
        print("FCM token: \(token)")
        UserDefaults.standard.set(token, forKey: AppConstants.fcmTokenKey)
    }

    // MARK: UNUserNotificationCenterDelegate

    // Called when a notification arrives while the app is in the FOREGROUND.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }

    // Called when the user TAPS on a notification (foreground or background).
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        print("Notification tapped: \(userInfo)")
        // Forward to Firebase so analytics / messaging can process it.
        Messaging.messaging().appDidReceiveMessage(userInfo)
        completionHandler()
    }
}

// MARK: - Crashlytics bridge

/**
 * Swift implementation of the Kotlin [IosCrashlyticsReporter] interface.
 * Registered from [AppDelegate.application(_:didFinishLaunchingWithOptions:)]
 * so that Kotlin's CrashReporting can forward non-fatal errors and logs to
 * the real Firebase Crashlytics SDK on iOS.
 */
class SwiftCrashlyticsReporter: NSObject, IosCrashlyticsReporter {

    func log(message: String) {
        Crashlytics.crashlytics().log(message)
    }

    func recordError(message: String, type: String, stackTrace: String) {
        // Firebase Crashlytics on iOS records non-fatals as NSError.
        // We encode the Kotlin exception class and stack trace as user-info keys
        // so they appear in the Crashlytics dashboard alongside the error.
        let error = NSError(
            domain: "com.church.presenter.network",
            code: -1,
            userInfo: [
                NSLocalizedDescriptionKey       : message,
                "kotlin_exception_type"         : type,
                "kotlin_stack_trace"            : String(stackTrace.prefix(3_900)),
            ]
        )
        Crashlytics.crashlytics().record(error: error)
    }

    func setUserId(userId: String) {
        Crashlytics.crashlytics().setUserID(userId)
    }

    func setCustomKey(key: String, value: String) {
        Crashlytics.crashlytics().setCustomValue(value, forKey: key)
    }
}

