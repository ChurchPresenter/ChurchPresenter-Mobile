package com.church.presenter.churchpresentermobile.util

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

actual object RemoteConfig {

    private const val TAG = "RemoteConfig"
    private val rc: FirebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }

    actual fun init(defaults: Map<String, Any>, fetchIntervalSeconds: Long) {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(fetchIntervalSeconds)
            .build()
        rc.setConfigSettingsAsync(settings)
        if (defaults.isNotEmpty()) {
            rc.setDefaultsAsync(defaults.mapValues { (_, v) -> v as Any })
        }
        Logger.d("RemoteConfig", "Initialised — fetchInterval=${fetchIntervalSeconds}s defaults=${defaults.keys}")
    }

    actual fun fetchAndActivate(onComplete: (Boolean) -> Unit) {
        rc.fetchAndActivate()
            .addOnSuccessListener { activated ->
                Logger.d(TAG, "fetchAndActivate — activated=$activated")
                Logger.d(TAG, "  is_demo_mode              = ${rc.getBoolean(RemoteConfigKeys.IS_DEMO_MODE)}")
                Logger.d(TAG, "  maintenance_mode          = ${rc.getBoolean(RemoteConfigKeys.MAINTENANCE_MODE)}")
                Logger.d(TAG, "  feature_bible             = ${rc.getBoolean(RemoteConfigKeys.FEATURE_BIBLE_ENABLED)}")
                Logger.d(TAG, "  feature_songs             = ${rc.getBoolean(RemoteConfigKeys.FEATURE_SONGS_ENABLED)}")
                Logger.d(TAG, "  feature_pictures          = ${rc.getBoolean(RemoteConfigKeys.FEATURE_PICTURES_ENABLED)}")
                Logger.d(TAG, "  feature_presentation      = ${rc.getBoolean(RemoteConfigKeys.FEATURE_PRESENTATION_ENABLED)}")
                Logger.d(TAG, "  min_app_version           = ${rc.getLong(RemoteConfigKeys.MIN_APP_VERSION)}")
                Logger.d(TAG, "  announcement_banner       = ${rc.getString(RemoteConfigKeys.ANNOUNCEMENT_BANNER)}")
                onComplete(activated)
            }
            .addOnFailureListener { e ->
                Logger.e("RemoteConfig", "fetchAndActivate failed", e)
                CrashReporting.recordException(e)
                onComplete(false)
            }
    }

    actual fun getString(key: String, default: String): String =
        rc.getString(key).ifEmpty { default }

    actual fun getBoolean(key: String, default: Boolean): Boolean =
        rc.getBoolean(key)

    actual fun getLong(key: String, default: Long): Long =
        rc.getLong(key)
}

