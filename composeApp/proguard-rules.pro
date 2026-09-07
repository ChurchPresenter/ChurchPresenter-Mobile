# ============================================================
# ChurchPresenter Mobile — ProGuard / R8 rules
# ============================================================
#
# Google Play measures how much of the shipped code R8 actually renamed and
# flags an app below 25%. This file was the reason ours sat at 22%: it kept
# every large dependency under its original name — Compose, Ktor, coroutines,
# Firebase, GMS, Play Core, Coil — and between them those are most of the
# classes in the bundle. Compose alone dwarfs the app's own code.
#
# None of those keeps were doing anything. Each of those libraries ships its
# own consumer ProGuard rules inside its AAR/JAR, and AGP merges them into this
# build automatically; a blanket `-keep class x.** { *; }` on top of that only
# stops R8 renaming and shrinking code the library itself has declared safe to
# touch. What is left here is what no library can know about: this app's own
# wire types, and the attributes Crashlytics reads.
#
# Rule of thumb for anything added later: a `-keep` earns its place by naming
# something reached WITHOUT a compile-time reference — reflection, a manifest
# entry, a ServiceLoader. Nothing in this app reaches for a class by name (the
# one class-name-shaped string in the code, ScheduleItem's `type` discriminator
# in model/Song.kt, names a class in the DESKTOP app and is only ever sent as
# text), and the Ktor engine is passed explicitly as `HttpClient(OkHttp)` rather
# than discovered, so there is no ServiceLoader lookup to protect.

# ── This app's wire types — deliberately NOT obfuscated ──────
# The models and the network layer are the JSON that goes to and from the
# desktop companion server. Renaming them is not worth the risk of a silent
# field-name mismatch in the field, so they stay readable by choice; they are a
# small share of the app, and keeping them costs only a point or two of the
# obfuscation figure.
-keep class com.church.presenter.churchpresentermobile.model.** { *; }
-keep class com.church.presenter.churchpresentermobile.network.** { *; }

# ── Kotlin ───────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# ── Kotlin Serialization ─────────────────────────────────────
# The @Serializable classes this app defines are kept above. These two rules
# cover the library's own lookup path — a serializer is reached through its
# class's Companion — which is all R8's built-in serialization support needs.
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Enum names (used in analytics, settings, etc.) ───────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Crashlytics — readable stack traces ──────────────────────
# Line numbers and the source-file attribute survive R8 so a trace can be mapped
# back; the file NAME itself is renamed, which is what -renamesourcefileattribute
# is for. The mapping file uploaded with each release is what turns an obfuscated
# trace back into source — keeping class names unrenamed was never what made
# Crashlytics work.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# ── Warning suppression — these keep NOTHING ─────────────────
# -dontwarn only silences references R8 cannot resolve (optional dependencies a
# library declares but this app never pulls in). Unlike -keep it has no effect
# on what is renamed or removed.
-dontwarn io.ktor.**
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.android.play.**
-dontwarn coil3.**
-dontwarn androidx.compose.**
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
-dontwarn okio.**
