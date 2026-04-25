# ============================================================
# ChurchPresenter Mobile — ProGuard / R8 rules
# ============================================================

# ── Kotlin ───────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# ── Kotlin Serialization ─────────────────────────────────────
-keepattributes *Annotation*, Signature
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep class kotlinx.serialization.** { *; }

# ── Ktor ─────────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn io.ktor.**

# ── Firebase ─────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firebase Crashlytics — preserve stack-trace class/method names
-keepattributes SourceFile, LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.crashlytics.** { *; }

# ── Jetpack Compose ──────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Coil ─────────────────────────────────────────────────────
-keep class coil3.** { *; }
-dontwarn coil3.**

# ── Play Core (Review / App Update) ──────────────────────────
-keep class com.google.android.play.** { *; }
-dontwarn com.google.android.play.**

# ── Google Code Scanner ──────────────────────────────────────
-keep class com.google.android.gms.codescanner.** { *; }

# ── App model classes (keep data classes used in JSON/API) ───
-keep class com.church.presenter.churchpresentermobile.model.** { *; }
-keep class com.church.presenter.churchpresentermobile.network.** { *; }

# ── Enum names (used in analytics, settings, etc.) ───────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Suppress common harmless warnings ────────────────────────
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
-dontwarn okio.**

