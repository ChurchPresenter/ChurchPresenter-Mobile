import java.io.File
import java.util.Properties
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.3.0"
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

// ---------------------------------------------------------------------------
// Generates the actual isDebugBuild for the JS/WasmJs targets (webMain), since
// neither has a runtime debug flag analogous to Android's BuildConfig.DEBUG.
// Mirrors the desktop app's generateBuildConfig task: dev-vs-prod is inferred
// from which Gradle task was requested. Every relevant JS/Wasm compile task
// literally contains "Development" or "Production" (e.g.
// compileDevelopmentExecutableKotlinJs vs compileProductionExecutableKotlinJs),
// except the CI distribution tasks (jsBrowserDistribution/wasmJsBrowserDistribution,
// used by .github/workflows/web-deploy.yml) which build the production executable
// under the hood without saying "production" in the task name itself — hence the
// extra "distribution" check.
// ---------------------------------------------------------------------------
val generateWebBuildConfig by tasks.registering {
    val isRelease = gradle.startParameter.taskNames.any { task ->
        val t = task.lowercase()
        t.contains("production") || t.contains("distribution")
    }
    val outputDir = layout.buildDirectory.dir("generated/webbuildconfig")
    // isRelease depends on the requested task names, not on any tracked file input,
    // so Gradle's normal up-to-date check can't detect when it needs to rerun —
    // always re-run (same as desktop's generateBuildConfig task).
    outputs.upToDateWhen { false }
    outputs.dir(outputDir)

    doLast {
        val dir = outputDir.get().asFile
            .resolve("com/church/presenter/churchpresentermobile/util")
        dir.mkdirs()
        dir.resolve("WebBuildUtils.kt").writeText(
            """
            |package com.church.presenter.churchpresentermobile.util
            |
            |actual val isDebugBuild: Boolean = ${!isRelease}
            """.trimMargin()
        )
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // Extend the default hierarchy with a "web" intermediate source set shared
    // by JS and WasmJS targets.  Using the hierarchy-template API (instead of
    // manual dependsOn calls on jsMain / wasmJsMain) is required in Kotlin 2.x:
    // explicit dependsOn edges on those source sets suppress the default
    // hierarchy template entirely, which disconnects iosMain from all iOS
    // compilations and causes "no actual declaration for Native" errors.
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("web") {
                withJs()
                withWasmJs()
            }
        }
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            freeCompilerArgs += listOf("-Xbinary=bundleId=com.church.presenter.churchpresentermobile")
        }
    }

    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        // NOTE: webMain is created automatically by the applyDefaultHierarchyTemplate
        // call above — do NOT add dependsOn(webMain) to jsMain or wasmJsMain here.

        // Provides the actual isDebugBuild for both JS and WasmJs from one generated
        // file (see generateWebBuildConfig above) — an actual declared on this shared
        // intermediate source set satisfies the expect for both leaf targets. "web" is
        // a custom hierarchy-template group (not a built-in name like jsMain/androidMain),
        // so it has no typesafe accessor — look it up by name instead.
        getByName("webMain") {
            kotlin.srcDir(generateWebBuildConfig.map { layout.buildDirectory.dir("generated/webbuildconfig") })
        }

        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }

        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.core.splashscreen)
            // Firebase — BOM version is enforced via the top-level dependencies block
            implementation(libs.firebase.crashlytics)
            implementation(libs.firebase.messaging)
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.config)
            implementation(libs.firebase.inappmessaging)
            implementation(libs.sentry.android)
            implementation(libs.play.review)
            implementation(libs.play.app.update)
            // QR code scanner — no camera permission required, Google provides the UI
            implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.ios)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.materialIconsExtended)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.websockets)
            implementation(libs.kotlinx.serialization)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// ---------------------------------------------------------------------------
// Signing configuration — loaded from the private signing repo.
// Resolution order (first found wins):
//   1. Individual Gradle project properties  (-Pandroid.signing.*)
//   2. signing.properties file in the repo pointed to by signing.repo.path
//      (set in local.properties or via SIGNING_REPO_PATH env variable)
// ---------------------------------------------------------------------------
val signingRepoPath: String? =
    findProperty("signing.repo.path")?.toString()
        ?: System.getenv("SIGNING_REPO_PATH")
        ?: run {
            val localProps = Properties()
            val localPropsFile = rootProject.file("local.properties")
            if (localPropsFile.exists()) localProps.load(localPropsFile.inputStream())
            localProps.getProperty("signing.repo.path")
        }

val signingProps = Properties()
val signingPropsFile = signingRepoPath?.let { file("$it/signing.properties") }
if (signingPropsFile?.exists() == true) {
    signingProps.load(signingPropsFile.inputStream())
}

fun signingProp(key: String, gradleKey: String = "android.signing.$key"): String? =
    findProperty(gradleKey)?.toString() ?: signingProps.getProperty(key)

android {
    namespace = "com.church.presenter.churchpresentermobile"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.church.presenter.churchpresentermobile"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 15
        versionName = "1.0.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Register release signing config only when credentials are available.
    val storeFileProp  = signingProp("storeFile")
    val storePassProp  = signingProp("storePassword")
    val keyAliasProp   = signingProp("keyAlias")
    val keyPassProp    = signingProp("keyPassword")

    if (storeFileProp != null && storePassProp != null && keyAliasProp != null && keyPassProp != null) {
        signingConfigs {
            create("release") {
                // storeFile may be relative (resolved against signing repo) or absolute.
                // Use java.io.File to check absoluteness of the raw string before
                // letting Gradle's file() resolve it (Gradle always resolves relative
                // paths against the project dir, which would be wrong here).
                val ksFile = if (File(storeFileProp).isAbsolute) {
                    file(storeFileProp)
                } else {
                    file("$signingRepoPath/$storeFileProp")
                }
                storeFile     = ksFile
                storePassword = storePassProp
                keyAlias      = keyAliasProp
                keyPassword   = keyPassProp
            }
        }
    } else {
        logger.warn(
            "[ChurchPresenter] Release signing config not found. " +
            "Set signing.repo.path in local.properties or SIGNING_REPO_PATH env variable."
        )
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
    // Firebase BOM — constrains all firebase-* library versions for the Android target
    add("androidMainImplementation", platform(libs.firebase.bom))
}

// ---------------------------------------------------------------------------
// NOTE on Sentry source-context upload: the io.sentry.android.gradle plugin
// (which provides that feature) isn't Kotlin-Multiplatform-aware — applying it
// to this module leaks Android-only companion artifacts (sentry-compose-android,
// sentry-kotlin-extensions) into the JS/WasmJs dependency graph and breaks
// resolution there, regardless of its autoInstallation/tracingInstrumentation
// toggles. So it's deliberately not applied here; Sentry itself works fully via
// the plain io.sentry:sentry-android dependency in androidMain below — this
// only costs the "source context" (readable source lines in the Sentry
// dashboard), not any actual crash/error reporting functionality.
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// Disable ART startup-profile compilation tasks to prevent
// INSTALL_BASELINE_PROFILE_FAILED when installing the APK on devices /
// emulators that cannot compile the embedded binary profile.
// The gradle.properties flag `android.experimental.art.profile.default.warp`
// handles this on newer AGP versions; the configureEach block below is a
// belt-and-suspenders guard for all AGP versions.
// ---------------------------------------------------------------------------
tasks.configureEach {
    if (name.contains("ArtProfile", ignoreCase = true)) {
        enabled = false
    }
}

