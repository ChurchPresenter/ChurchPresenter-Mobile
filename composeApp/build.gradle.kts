import java.io.File
import java.util.Properties
import org.gradle.api.tasks.testing.Test
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
    jacoco
    alias(libs.plugins.detekt)
}

// ---------------------------------------------------------------------------
// Firebase placeholder for developers without the signing repo.
//
// The Google Services plugin fails the Android build outright when
// google-services.json is absent, so a fresh clone could not build at all
// without credentials most contributors have no reason to hold. This writes a
// stand-in when the file is missing, and leaves a real one (setup_signing.sh
// symlinks it from the signing repo) untouched.
//
// The shape matters as much as the presence: FirebaseInitProvider runs on every
// launch regardless of build type, and rejects a malformed key with "Please set
// a valid API key" *before any of our code runs* — an app that compiles and then
// dies on the splash screen. So the key here is the real 39-character format.
// Firebase then initialises against a project that does not exist and its calls
// fail quietly, which is what a developer build wants.
// ---------------------------------------------------------------------------
run {
    val googleServices = layout.projectDirectory.file("google-services.json").asFile
    if (!googleServices.exists()) {
        googleServices.writeText(
            """
            {
              "project_info": {
                "project_number": "000000000000",
                "project_id": "churchpresenter-placeholder",
                "storage_bucket": "churchpresenter-placeholder.appspot.com"
              },
              "client": [
                {
                  "client_info": {
                    "mobilesdk_app_id": "1:000000000000:android:0000000000000000000000",
                    "android_client_info": { "package_name": "com.church.presenter.churchpresentermobile" }
                  },
                  "oauth_client": [],
                  "api_key": [ { "current_key": "AIzaSy${"A".repeat(33)}" } ],
                  "services": { "appinvite_service": { "other_platform_oauth_client": [] } }
                }
              ],
              "configuration_version": "1"
            }
            """.trimIndent()
        )
        logger.warn(
            "[ChurchPresenter] No google-services.json — wrote a placeholder so the build can " +
            "run. Analytics, Crashlytics and push are inert. Run scripts/setup_signing.sh for the real one."
        )
    }
}

// ---------------------------------------------------------------------------
// Build provenance — mirrors the desktop app's helpers in ChurchPresenter's
// composeApp/build.gradle.kts.
//
// The app is open-source and hardcodes the live-map ping URL, so a fork built
// from source pings churchpresenter.org with a payload identical to an official
// install. Stamping the build's git origin and state lets the server tell them
// apart (see the build_channel column and api/ping.ts in the website repo).
// Attribution for honest forks, not authentication — anyone can patch these
// constants out. Every helper falls back rather than failing the build.
// ---------------------------------------------------------------------------

/**
 * Runs a git command, returning null when git is missing or exits non-zero.
 *
 * Uses providers.exec rather than ProcessBuilder: this project has the
 * configuration cache enabled, which forbids starting external processes at
 * configuration time. providers.exec is the supported route — Gradle tracks the
 * result as a configuration-cache input instead of rejecting it.
 *
 * stderr is captured separately and discarded, so git's "fatal: not a git
 * repository" never comes back looking like a real value.
 */
fun gitOutput(vararg args: String): String? {
    return try {
        val result = providers.exec {
            commandLine("git", *args)
            workingDir = rootProject.projectDir
            isIgnoreExitValue = true
        }
        if (result.result.get().exitValue != 0) null
        else result.standardOutput.asText.get().trim()
    } catch (_: Exception) { null }
}

// Matches the server's REPO_RE — anything else is stored as 'unknown' there, so
// there's no point sending it.
val repoSlugRegex = Regex("^[a-z0-9][a-z0-9._-]{0,38}/[a-z0-9][a-z0-9._-]{0,99}$")

/**
 * Parses a git remote URL down to a lowercase "owner/name" slug.
 *
 * Only the slug is ever sent — never the raw URL, which can embed credentials
 * (https://user:token@github.com/...). Handles https://, ssh:// and scp-style
 * (git@host:owner/name) remotes; anything else yields "unknown".
 */
fun parseRepoSlug(remoteUrl: String): String {
    val trimmed = remoteUrl.trim().removeSuffix(".git").trimEnd('/')
    if (trimmed.isEmpty()) return "unknown"
    val path = when {
        trimmed.contains("://") -> trimmed.substringAfter("://").substringAfter('/', "")
        trimmed.contains(':') -> trimmed.substringAfter(':')
        // A bare filesystem path is a local clone, not an identifiable remote.
        else -> return "unknown"
    }
    val parts = path.split('/').filter { it.isNotEmpty() }
    if (parts.size < 2) return "unknown"
    val slug = "${parts[parts.size - 2]}/${parts[parts.size - 1]}".lowercase()
    return if (repoSlugRegex.matches(slug)) slug else "unknown"
}

// Provenance is a property of the build, not of the target, so unlike
// isDebugBuild this needs no expect/actual — one generated file in commonMain
// serves Android, iOS and web alike.
//
// Only the two raw git facts are baked in here. Whether the build is a "release"
// is decided at runtime in BuildUtils.kt from the per-target isDebugBuild, which
// is far more reliable than guessing from Gradle task names across three
// different toolchains.
val generateProvenanceConfig by tasks.registering {
    val repoSlug = parseRepoSlug(gitOutput("remote", "get-url", "origin") ?: "")
    val commitHash = gitOutput("rev-parse", "--short=12", "HEAD") ?: "unknown"
    val hasGit = gitOutput("rev-parse", "--git-dir") != null
    val isDirty = !gitOutput("status", "--porcelain").isNullOrEmpty()
    val outputDir = layout.buildDirectory.dir("generated/provenance")

    // Git state can change with no file edit Gradle can see, so never cache.
    outputs.upToDateWhen { false }
    outputs.dir(outputDir)

    doLast {
        val dir = outputDir.get().asFile
            .resolve("com/church/presenter/churchpresentermobile/util")
        dir.mkdirs()
        dir.resolve("BuildProvenance.kt").writeText(
            """
            |package com.church.presenter.churchpresentermobile.util
            |
            |// Generated by the generateProvenanceConfig task — do not edit.
            |internal object BuildProvenance {
            |    const val REPO_SLUG = "$repoSlug"
            |    const val COMMIT_HASH = "$commitHash"
            |    const val HAS_GIT = $hasGit
            |    const val IS_DIRTY = $isDirty
            |}
            """.trimMargin()
        )
    }
}

// ---------------------------------------------------------------------------
// Static analysis (detekt) — same ruleset as the ChurchPresenter desktop app,
// whose config/detekt/detekt.yml this file mirrors verbatim. Keeping the two in
// step matters because the same people move between the repos: a rule that fails
// here and passes there (or the reverse) turns the gate into noise.
//
// Two of those rules are this project's own house rules from CODING_STANDARDS.md,
// now enforced rather than remembered: WildcardImport, and ForbiddenImport on
// `androidx.compose.material.*` (Material 2), which the icons package is exempt
// from because materialIconsExtended lives under it.
//
// Unlike the desktop's single jvmMain source set, `src` here covers every KMP
// source set at once — common, android, ios, js, wasmJs, mobile, web and the
// tests — so adding a target does not silently fall out of the analysis.
//
// Pre-existing findings are baselined in config/detekt/baseline.xml, so the gate
// fails only on NEW findings. Regenerate with `./gradlew :composeApp:detektBaseline`
// — but prefer fixing over re-baselining; the file exists to stop day one from
// being a 900-issue refactor, not to absorb new debt.
// ---------------------------------------------------------------------------
detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    baseline = rootProject.file("config/detekt/baseline.xml")
    source.setFrom("src")
    parallel = true
}

// JVM_11 here, against the desktop's 21 — this module compiles to Android's
// Java 11 target (see `compileOptions` below) and detekt has to agree with it.
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "11"
    reports {
        html.required.set(true)
        xml.required.set(false)
        sarif.required.set(false)
        txt.required.set(false)
        md.required.set(false)
    }
}
tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    jvmTarget = "11"
}

// ---------------------------------------------------------------------------
// Code coverage (JaCoCo) — measured on the Android unit-test JVM, where the
// commonTest suite runs.
//
// Run:  ./gradlew :composeApp:jacocoTestReport
// HTML: composeApp/build/reports/jacoco/jacocoTestReport/html/index.html
//
// This is a Kotlin Multiplatform build, so there is no conventional test/main
// pair for the plugin's default report to attach to — hence an explicit task
// pointing at the Android debug compilation's own output.
// ---------------------------------------------------------------------------
jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Coverage for the app's own code, from the Android unit-test run."
    dependsOn("testDebugUnitTest")

    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "jacoco/testDebugUnitTest.exec",
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
            )
        }
    )

    // Generated code only — there is no source behind it to cover. Nothing else is
    // excluded: not the ui/viewmodel/util packages, not the platform expect/actual
    // leaves, not @Composable functions. The figure this produces is coverage of the
    // whole module, so it is low; that is the point. Raise it by adding tests, never
    // by adding an exclusion.
    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
            exclude("**/ComposableSingletons*")
            exclude("**/churchpresentermobile/composeapp/generated/resources/**")
        }
    )
    sourceDirectories.setFrom(
        files("src/commonMain/kotlin", "src/androidMain/kotlin", "src/mobileMain/kotlin"),
    )

    // A filtered run measures only what it ran, so the number would be nonsense.
    // Resolved at configuration time: reading `gradle` from inside onlyIf captures
    // the Project, which the configuration cache refuses to serialize.
    val isFilteredRun = gradle.startParameter.taskRequests.any { request ->
        request.args.any { it == "--tests" }
    }
    onlyIf { !isFilteredRun }

    reports {
        html.required.set(true)
        xml.required.set(true)   // for CI / coverage services
        csv.required.set(false)
    }

    // JaCoCo writes the files it reports on and never removes the ones it doesn't, so
    // a page for a class since renamed or excluded stays on disk at 0% for anyone who
    // opens it directly. The XML is one file and is overwritten in place; only the
    // HTML tree needs clearing.
    // deleteRecursively() on the resolved File rather than Project.delete(): the
    // latter is a Gradle script object reference, which the configuration cache
    // cannot serialize.
    val htmlReportDir = layout.buildDirectory.dir("reports/jacoco/jacocoTestReport/html")
    doFirst { htmlReportDir.get().asFile.deleteRecursively() }

    finalizedBy("printCoverageLink")
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    group = "verification"
    description = "Fails the build when line coverage drops below the floor."
    dependsOn("jacocoTestReport")
    executionData.setFrom(tasks.named<JacocoReport>("jacocoTestReport").map { it.executionData })
    classDirectories.setFrom(tasks.named<JacocoReport>("jacocoTestReport").map { it.classDirectories })
    sourceDirectories.setFrom(tasks.named<JacocoReport>("jacocoTestReport").map { it.sourceDirectories })

    violationRules {
        rule {
            // The project's coverage floor.
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

// Prints the headline numbers and a clickable file:// link so the report doesn't have to be hunted
// for under build/. Ported from the desktop app's `printCoverageLink`, and reads the same
// JaCoCo XML schema.
//
// Deliberately a SEPARATE task rather than a doLast on jacocoTestReport: a doLast is skipped when the
// report is UP-TO-DATE, so re-running `check` would silently print nothing. This task declares no
// outputs, so it never goes up-to-date and always prints.
tasks.register("printCoverageLink") {
    val reportDir = layout.buildDirectory.dir("reports/jacoco/jacocoTestReport")
    // The Android unit-test run is the one JaCoCo measures, so it is the run whose test counts
    // belong next to the coverage figure. The jsBrowserTest and wasmJsBrowserTest suites cover
    // the same code but produce no coverage data — quoting their totals here would credit the
    // percentage to tests that did not produce it.
    val testResultsDir = layout.buildDirectory.dir("test-results/testDebugUnitTest")
    // Where to append the run's summary block, resolved at CONFIGURATION time from a property the
    // CI client passes in — not from this process's environment. `System.getenv` here reads the
    // Gradle DAEMON's environment, which is frozen when the daemon starts, while GITHUB_STEP_SUMMARY
    // is a fresh temp file PER STEP that the runner reads and discards when that step ends. A daemon
    // started by an earlier step would therefore append to a file belonging to a step that had
    // already finished — written successfully, read by nobody. (This bit the desktop app on a real
    // run; the fix is ported here before it can happen rather than after.)
    //
    // A `-P` property travels with each individual invocation, so it is the live path every time.
    // The environment stays as the fallback for anyone running the task by hand.
    val stepSummaryPath = providers.gradleProperty("stepSummary")
        .orElse(providers.environmentVariable("GITHUB_STEP_SUMMARY"))
        .orNull
    doLast {
        val dir = reportDir.get().asFile
        val htmlIndex = dir.resolve("html/index.html")
        if (!htmlIndex.exists()) return@doLast

        // Each per-suite TEST-*.xml carries its own totals on the root <testsuite> tag; sum them
        // for the whole-run figure, since Gradle writes no combined summary of its own.
        val testSummary = runCatching {
            val suiteAttrs = Regex("""tests="(\d+)" skipped="(\d+)" failures="(\d+)" errors="(\d+)"""")
            val files = testResultsDir.get().asFile.listFiles { f -> f.name.endsWith(".xml") }
            if (files.isNullOrEmpty()) return@runCatching null
            var tests = 0; var skipped = 0; var failures = 0; var errors = 0
            files.forEach { file ->
                val match = suiteAttrs.find(file.readText().lineSequence().take(2).joinToString("\n"))
                    ?: return@forEach
                tests += match.groupValues[1].toInt()
                skipped += match.groupValues[2].toInt()
                failures += match.groupValues[3].toInt()
                errors += match.groupValues[4].toInt()
            }
            "$tests run, ${failures + errors} failed, $skipped skipped"
        }.getOrNull()

        // Regex rather than a DOM parse: the JaCoCo XML declares an external DTD, which a
        // DocumentBuilder tries to resolve over the network. The report-wide totals are the LAST
        // <counter> of each type in the document (they appear per-package/-class first, then once
        // more on the closing </report> element), so the final match per type is the overall figure.
        val lines = runCatching {
            val xml = dir.resolve("jacocoTestReport.xml")
            if (!xml.exists()) return@runCatching null
            val text = xml.readText()
            // Order matches the JaCoCo HTML overview table's own column order.
            val labels = listOf(
                "INSTRUCTION" to "instructions",
                "BRANCH" to "branches",
                "LINE" to "lines",
                "COMPLEXITY" to "complexity",
                "METHOD" to "methods",
                "CLASS" to "classes",
            )
            labels.mapNotNull { (type, label) ->
                val last = Regex("""<counter type="$type" missed="(\d+)" covered="(\d+)"/>""")
                    .findAll(text).lastOrNull() ?: return@mapNotNull null
                val missed = last.groupValues[1].toInt()
                val covered = last.groupValues[2].toInt()
                val total = covered + missed
                if (total == 0) null
                else "%.1f%% of %s (%d/%d)".format(100.0 * covered / total, label, covered, total)
            }
        }.getOrNull()

        val summaryLines = buildList {
            if (testSummary != null) add("Tests:    $testSummary")
            if (lines != null) {
                add("Coverage:")
                lines.forEach { add("  $it") }
            }
        }

        logger.lifecycle("")
        summaryLines.forEach { logger.lifecycle(it) }
        // The HTML tree only exists on the machine that produced it. On a CI runner it is deleted
        // with the workspace when the job ends, so the line is noise there — a `file://` path
        // pointing at a directory the reader has no way to open. Printed locally, skipped in CI.
        //
        // Three slashes: File.toURI() yields "file:/path", which many terminals refuse to linkify.
        if (System.getenv("GITHUB_ACTIONS") != "true") {
            logger.lifecycle("Report:   file://${htmlIndex.absolutePath}")
        }

        // Also put it on the run's summary page. The numbers otherwise land in the middle of the
        // job log; the step summary is the page a reviewer actually opens from a pull request.
        // Best-effort — a coverage print must never fail a build.
        stepSummaryPath?.takeIf { it.isNotBlank() }?.let { path ->
            runCatching {
                File(path).appendText(
                    buildString {
                        appendLine("### Unit test coverage")
                        appendLine()
                        appendLine("```")
                        summaryLines.forEach { appendLine(it) }
                        appendLine("```")
                        appendLine()
                    }
                )
            }
        }
    }
}

// Every test runs on the Android unit-test JVM: kotlinx's Dispatchers.setMain()
// supplies the main dispatcher Android itself does not. No test is filtered out.
//
// Three classes used to be, because they created a ViewModel inside runVmTest and
// never cancelled its viewModelScope — a coroutine could then resume after
// resetMain() and fail an unrelated later test with "Dispatchers.Main was accessed
// when the platform dispatcher was absent". They now tearDown(vm) in a finally,
// which is what any new ViewModel test should do (see testutil/CoroutineTest.kt).
tasks.withType<Test>().configureEach {
    filter {
        isFailOnNoMatchingTests = false
    }
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
            // Android + iOS share everything that needs real device APIs the
            // browser has no analogue for — chiefly the standalone presenter's
            // embedded HTTP/WebSocket server. One Ktor CIO implementation
            // serves both (JVM artifact on Android, Kotlin/Native on iOS).
            group("mobile") {
                withAndroidTarget()
                withIos()
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
        browser {
            // Headless Chrome so `jsBrowserTest` runs the commonTest suite on CI
            // (and locally) without a visible browser window.
            testTask {
                useKarma { useChromeHeadless() }
            }
        }
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            // Same headless Chrome as the js target above. Without it this target
            // opens a real browser window on the developer's desktop whenever a
            // task reaches its tests — `check` and `allTests` both do.
            testTask {
                useKarma { useChromeHeadless() }
            }
        }
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

        // Build provenance for the live-map ping (see generateProvenanceConfig
        // above). Unlike isDebugBuild this is target-independent, so it lives in
        // commonMain and every target compiles the same generated file.
        commonMain {
            kotlin.srcDir(generateProvenanceConfig.map { layout.buildDirectory.dir("generated/provenance") })
        }

        getByName("mobileMain").dependencies {
            // Embedded presentation server — standalone mode only.
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.cio)
            implementation(libs.ktor.server.websockets)
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
            // QR generation for the standalone display URL (QrScanButton only scans).
            implementation(libs.qrose)
            implementation(libs.kotlinx.serialization)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
        // Compose UI tests, on the wasmJs browser target only.
        //
        // They need a Skia surface. The js (legacy) Karma runtime does not load
        // skiko, so running them there fails with
        // "org_jetbrains_skia_Surface__1nMakeRasterN32Premul is not defined";
        // the wasmJs target ships skiko with the test bundle and runs them as-is.
        // Kept out of commonTest for exactly that reason — jsBrowserTest, the main
        // gate, must not pick them up. Run with :composeApp:wasmJsBrowserTest.
        wasmJsTest.dependencies {
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
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
        versionCode = 19
        versionName = "1.0.18"
    }
    testOptions {
        // Let commonTest run on the Android unit-test JVM (used by the best-effort
        // JaCoCo coverage job): unmocked android.jar methods return defaults (e.g.
        // Log.* no-ops) instead of throwing.
        unitTests.isReturnDefaultValues = true
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
        getByName("debug") {
            // Makes AGP run the unit tests under the JaCoCo agent, which is what
            // writes the .exec file jacocoTestReport reads.
            enableUnitTestCoverage = true
        }
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

