package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.BottomTabBar
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import io.github.takahirom.roborazzi.captureRoboImage

/**
 * The screenshot suite's one way of taking a picture.
 *
 * Every capture goes through [screenshot] so that the frame around the subject —
 * theme, width, background — is identical everywhere and a diff can only be the
 * composable's own doing. A test that reached for `captureRoboImage` directly
 * would be comparing against a golden nobody else's golden is comparable with.
 *
 * ### What a screenshot test is for here
 *
 * The behavioural UI tests next door in `commonTest` already assert what the
 * operator can read and press (see TESTING.md). These assert what the
 * behavioural ones deliberately do not: colour, spacing, type, elevation, and
 * every visual state a composable can be put in — the things a hand-written
 * assertion about a `dp` value could never keep up with.
 *
 * ### Running them
 *
 * ```
 * ./gradlew :composeApp:screenshotTest -Precord   # (re)write the goldens
 * ./gradlew :composeApp:screenshotTest            # verify against them
 * ```
 *
 * The goldens live in `composeApp/screenshots/` and are committed. A failed
 * verification writes the actual and a highlighted diff into
 * `composeApp/build/outputs/roborazzi/`.
 */
internal object Screenshots {

    /** Where the goldens live, relative to the module directory. */
    const val DIR: String = "screenshots"

    /**
     * A phone's width in dp — the size nearly every composable in this app is
     * actually looked at. Fixed rather than "whatever the window was", because a
     * golden's whole value is that the next run lays out identically.
     */
    val PHONE_WIDTH: Dp = 360.dp

    /** A tablet/landscape width, for the composables whose layout changes with it. */
    val TABLET_WIDTH: Dp = 840.dp

    // ── Store and website images ─────────────────────────────────────────
    //
    // A different job from a golden: nobody diffs these, they are pictures of
    // the app for a listing page. They are written outside `screenshots/` so a
    // content tweak that makes a nicer marketing shot does not fail the test
    // gate, and so nothing here has to be reviewed as a golden.

    /** Where the store/website images go, relative to the module directory. */
    const val MARKETING_DIR: String = "marketing"

    /**
     * Pixels per dp for a store image.
     *
     * A golden renders at density 1 — 360dp of layout becomes a 360px PNG,
     * which is fine for a diff and useless on a listing page, where Apple and
     * Google both want something around 1080px wide. At 3x the same 360dp
     * phone frame comes out 1080x2340, which is a real device screenshot's
     * shape and resolution.
     */
    const val STORE_DENSITY: Float = 3f

    /** The phone frame the store images use: 1080x2340 at [STORE_DENSITY]. */
    val STORE_PHONE_HEIGHT: Dp = 780.dp

    /** The tablet frame: 2520x3360 at [STORE_DENSITY]. */
    val STORE_TABLET_HEIGHT: Dp = 1120.dp
}

/**
 * How long a capture waits for a screen's data before giving up.
 *
 * Generous, because it costs nothing when the condition is already true and the
 * alternative — a golden of a half-drawn screen — is a test that passes forever
 * while showing nothing.
 */
private const val CONTENT_TIMEOUT_MS = 5_000L

/**
 * Renders [content] once per theme and writes each frame to
 * `screenshots/<name>__light.png` / `__dark.png`.
 *
 * @param name Identifies the subject AND its state — `empty-state__with-action`,
 *   not `empty-state`. One file per state is the point; a name that collides
 *   silently overwrites another test's golden.
 * @param width How wide the subject is laid out, or **null to wrap it**. Height
 *   always wraps, so a composable that grows taller shows up as a taller image
 *   rather than a cropped one.
 *
 *   Pass a width for anything that fills the one it is given — a field, a row, a
 *   tab strip, a whole screen — because at its intrinsic width it would lay out
 *   nothing like it does in the app. Pass null for a subject with a size of its
 *   own, like a stack of buttons or a QR code: framing those at 360dp buys a
 *   picture that is mostly background, and background is not what the golden is
 *   there to protect.
 * @param until Held until this is true before the shutter opens — for a screen
 *   whose content arrives from a ViewModel rather than from its arguments.
 *   Without it such a screen is photographed while still empty.
 * @param themes Which themes to capture. Both by default — a colour that only
 *   goes wrong in the dark is exactly what this suite is for. Narrow it only
 *   for a subject that has no themed surface at all.
 */
@OptIn(ExperimentalTestApi::class)
internal fun screenshot(
    name: String,
    width: Dp? = Screenshots.PHONE_WIDTH,
    themes: List<ThemeMode> = listOf(ThemeMode.LIGHT, ThemeMode.DARK),
    dialog: Boolean = false,
    until: (ComposeUiTest.() -> Boolean)? = null,
    content: @Composable () -> Unit,
) {
    themes.forEach { theme ->
        runComposeUiTest {
            setContent {
                AppTheme(themeMode = theme) {
                    // An explicit surface, because the capture takes the root
                    // node's bounds and a transparent background would leave the
                    // PNG's alpha channel deciding what the reviewer sees.
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Box(if (width != null) Modifier.width(width) else Modifier) { content() }
                    }
                }
            }
            // A screen fed by a ViewModel answers on another dispatcher, and
            // "the composition is idle" is not the same as "the data arrived" —
            // without this the capture is a spinner, recorded as the golden for
            // a populated screen and green forever after.
            until?.let { waitUntil(timeoutMillis = CONTENT_TIMEOUT_MS) { it() } }
            drawnRoot(dialog).captureRoboImage(goldenPath(name, theme))
        }
    }
}

/**
 * The root to photograph.
 *
 * A composable that puts its content in a `Dialog` — [UploadProgressOverlay],
 * the colour picker, the confirmation sheets — composes into a window of its
 * own, so the tree has TWO roots and `onRoot()` fails outright with "expected
 * exactly 1 node but found 2". The parent root is the one left holding nothing:
 * zero height, because everything went to the dialog.
 *
 * So a dialog capture takes the root that actually drew something. Picking it by
 * height rather than by index means a capture can never quietly succeed against
 * the empty one and record a blank golden — with `dialog = true` and no dialog
 * (or two), this throws instead.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.drawnRoot(dialog: Boolean): SemanticsNodeInteraction {
    if (!dialog) return onRoot()
    val roots = onAllNodes(isRoot())
    val drawn = roots.fetchSemanticsNodes().withIndex().filter { (_, node) -> node.size.height > 0 }
    check(drawn.size == 1) {
        "dialog = true expects exactly one root with content, found ${drawn.size}. " +
            "Either the subject does not open a dialog, or it opened more than one."
    }
    return roots[drawn.single().index]
}

/**
 * Where a golden lands: `screenshots/<subject>/<state>__<theme>.png`.
 *
 * The subject is the part of the name before `__`, so the folders organise
 * themselves — `pictures__empty-folder` files itself under `pictures/` next to
 * every other state of that screen, and a reviewer opening the directory sees
 * one folder per thing rather than two hundred loose PNGs sorted by accident of
 * alphabet.
 *
 * Deriving it from the name rather than declaring it per test is deliberate:
 * there is no second place to keep in step, and a test cannot file its state
 * away from its siblings by forgetting to.
 */
private fun goldenPath(name: String, theme: ThemeMode): String {
    val parts = name.split("__", limit = 2)
    require(parts.size == 2 && parts.all { it.isNotBlank() }) {
        "screenshot name must be \"<subject>__<state>\", was \"$name\". The subject " +
            "becomes the folder and the state becomes the file, so both are needed."
    }
    val (subject, state) = parts
    return "${Screenshots.DIR}/$subject/${state}__${theme.suffix}.png"
}

/**
 * Captures one screen as a **store/website image** — `marketing/<name>__<theme>.png`.
 *
 * Not a golden and not diffed. The differences from [screenshot] are the ones
 * that matter to a listing page rather than to a test:
 *
 * - **Rendered at 3x** ([Screenshots.STORE_DENSITY]), so a phone frame comes out
 *   1080x2340 instead of 360x780. A golden's job is to be compared; this one's
 *   job is to be looked at on a Retina display and uploaded to App Store
 *   Connect, and both stores reject images this small.
 * - **A fixed height as well as a width**, so every image in the set is exactly
 *   the same shape. A store listing with screenshots of three different aspect
 *   ratios looks broken before anyone reads a word of it.
 *
 * One image per screen, in both themes, is what the set is: the app at its
 * best, not every state it can reach.
 */
@OptIn(ExperimentalTestApi::class)
internal fun marketingShot(
    name: String,
    width: Dp = Screenshots.PHONE_WIDTH,
    height: Dp = Screenshots.STORE_PHONE_HEIGHT,
    themes: List<ThemeMode> = listOf(ThemeMode.LIGHT, ThemeMode.DARK),
    tab: AppTab? = null,
    tabs: List<AppTab> = AppTab.forMode(AppMode.REMOTE),
    header: (@Composable () -> Unit)? = null,
    until: (ComposeUiTest.() -> Boolean)? = null,
    content: @Composable () -> Unit,
) {
    // The test window has to be opened at the size we want the PNG to be. The
    // default one is 1024x768, and a capture is clipped to it — asking for a
    // 1080x2340 frame inside it produced sixteen identical 1024x768 images of
    // a cropped screen.
    val density = Screenshots.STORE_DENSITY
    val widthPx = (width.value * density).toInt()
    val heightPx = (height.value * density).toInt()

    themes.forEach { theme ->
        runDesktopComposeUiTest(width = widthPx, height = heightPx) {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(density)) {
                    AppTheme(themeMode = theme) {
                        Scaffold(
                            containerColor = MaterialTheme.colorScheme.background,
                            // The app's own chrome, wired the way App.kt wires it.
                            // Without it these are pictures of a screen's contents,
                            // not of the app: no title, and no tab strip, which is
                            // the one thing every real screenshot of a phone app has
                            // along the bottom.
                            topBar = { header?.invoke() },
                            bottomBar = {
                                if (tab != null) {
                                    BottomTabBar(selectedTab = tab, onTabSelected = {}, tabs = tabs)
                                }
                            },
                        ) { padding ->
                            Box(Modifier.fillMaxSize().padding(padding)) { content() }
                        }
                    }
                }
            }
            until?.let { waitUntil(timeoutMillis = CONTENT_TIMEOUT_MS) { it() } }
            onRoot().captureRoboImage(
                "${Screenshots.MARKETING_DIR}/${name}__${theme.suffix}.png",
            )
        }
    }
}

private val ThemeMode.suffix: String
    get() = name.lowercase()
