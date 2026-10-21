package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.cd_back
import churchpresentermobile.composeapp.generated.resources.display_group_bible
import churchpresentermobile.composeapp.generated.resources.display_group_songs
import churchpresentermobile.composeapp.generated.resources.display_picker_subtitle
import churchpresentermobile.composeapp.generated.resources.display_reset
import churchpresentermobile.composeapp.generated.resources.display_save
import churchpresentermobile.composeapp.generated.resources.display_screen_options
import churchpresentermobile.composeapp.generated.resources.display_screen_options_hint
import churchpresentermobile.composeapp.generated.resources.display_section_screen
import churchpresentermobile.composeapp.generated.resources.display_text_title
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.TextElement
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.DisplayTextViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Display text: how each piece of words looks on the standalone output.
 *
 * Opened from the Look link on the Present tab, full screen over it. The phone pins a live
 * preview above the controls for one element at a time; the tablet lists every element on the
 * left, its controls in the middle and a large preview on the right. Both edit a draft that only
 * reaches the output on Save style.
 *
 * @param onOpenScreenOptions Opens the Look sheet for what is not text: backdrop colours, saved
 *   looks, the brand line, the clock.
 */
@Composable
fun DisplayTextScreen(
    engine: StandaloneEngine,
    settings: AppSettings,
    twoPane: Boolean,
    onOpenScreenOptions: () -> Unit,
    onDismiss: () -> Unit,
    /** Supplied by tests only; the screen owns its own otherwise. */
    providedViewModel: DisplayTextViewModel? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        DisplayTextEditor(engine, settings, twoPane, onOpenScreenOptions, onDismiss, providedViewModel)
    }
}

/** The screen without the dialog around it, so it can be drawn directly in a test. */
@Composable
internal fun DisplayTextEditor(
    engine: StandaloneEngine,
    settings: AppSettings?,
    twoPane: Boolean,
    onOpenScreenOptions: () -> Unit,
    onDismiss: () -> Unit,
    providedViewModel: DisplayTextViewModel? = null,
) {
    val viewModel: DisplayTextViewModel = providedViewModel
        ?: viewModel(key = "displayText") { DisplayTextViewModel(engine, settings) }
    val draft by viewModel.draft.collectAsState()
    val element by viewModel.element.collectAsState()
    val recent by viewModel.recentColors.collectAsState()
    val baseTheme by viewModel.baseTheme.collectAsState()
    val live by viewModel.liveSlide.collectAsState()

    val samples = rememberSampleSlides()
    var options by remember { mutableStateOf(PreviewOptions()) }
    var picking by remember { mutableStateOf<ColorTarget?>(null) }
    var fullScreen by remember { mutableStateOf(false) }
    // The preview's Bible / Song switch, which on the tablet can differ from the element edited.
    var previewKind by remember { mutableStateOf(element.kind) }

    val previewTheme = baseTheme.copy(display = draft)
    val slideFor: (SlideKind) -> Slide = { kind -> previewSlide(kind, previewTheme, live, samples, options) }
    val editor = ElementEditor(
        element = element,
        display = draft,
        referenceSamples = referenceSamples(samples),
        actions = EditorActions(
            onStyle = viewModel::updateStyle,
            onDisplay = viewModel::update,
            onSelect = { viewModel.select(it) },
            onPickColor = { picking = it },
        ),
    )
    val selectFromPreview: (TextElement) -> Unit = { tapped ->
        viewModel.select(tapped)
        previewKind = tapped.kind
    }

    val screen = DisplayTextFrame(
        editor = editor,
        preview = PreviewState(
            slideFor = slideFor,
            kind = if (twoPane) previewKind else element.kind,
            onKind = { previewKind = it },
            onElementTap = selectFromPreview,
            options = options,
            onOptions = { options = it },
        ),
        actions = FrameActions(
            onGroup = { kind ->
                viewModel.selectGroup(kind)
                previewKind = kind
            },
            onExpand = { fullScreen = true },
            onReset = viewModel::resetElement,
            onSave = viewModel::save,
            onBack = onDismiss,
            onOpenScreenOptions = onOpenScreenOptions,
        ),
    )
    if (twoPane) DisplayTextTablet(screen) else DisplayTextPhone(screen)

    if (fullScreen) {
        FullScreenPreview(
            slideFor = slideFor,
            kind = previewKind,
            onKindChange = { previewKind = it },
            element = element,
            onElementTap = { tapped ->
                selectFromPreview(tapped)
                fullScreen = false
            },
            aspect = options.aspect,
            onClose = { fullScreen = false },
        )
    }

    picking?.let { target ->
        val request = PickerRequest(
            title = stringResource(target.title()),
            subtitle = stringResource(
                Res.string.display_picker_subtitle,
                stringResource(element.heading()),
                stringResource(target.part()),
            ),
            initial = editor.style.colorOf(target),
        )
        val onChange: (String) -> Unit = { hex -> viewModel.updateStyle { it.withColor(target, hex) } }
        val onDone: (String) -> Unit = { hex ->
            viewModel.rememberColor(hex)
            picking = null
        }
        if (twoPane) ColorPickerPopover(request, recent, onChange, onDone)
        else ColorPickerSheet(request, recent, onChange, onDone)
    }
}

/** The preview: what it shows, and how the operator has asked to see it. */
internal class PreviewState(
    val slideFor: (SlideKind) -> Slide,
    val kind: SlideKind,
    val onKind: (SlideKind) -> Unit,
    val onElementTap: (TextElement) -> Unit,
    val options: PreviewOptions,
    val onOptions: (PreviewOptions) -> Unit,
)

/** The screen's own buttons. */
internal class FrameActions(
    val onGroup: (SlideKind) -> Unit,
    val onExpand: () -> Unit,
    val onReset: () -> Unit,
    val onSave: () -> Unit,
    val onBack: () -> Unit,
    val onOpenScreenOptions: () -> Unit,
)

/** Everything both layouts draw from, so neither re-derives it. */
internal class DisplayTextFrame(val editor: ElementEditor, val preview: PreviewState, val actions: FrameActions)

/** Phone: title, the pinned preview, the element tabs and its controls, then Reset / Save. */
@Composable
private fun DisplayTextPhone(frame: DisplayTextFrame) {
    val colors = LocalAppColors.current
    val element = frame.editor.element
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag(DisplayTextTags.SCREEN),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackArrow(frame.actions.onBack)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(Res.string.display_text_title),
                color = colors.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.03).em,
                modifier = Modifier.weight(1f),
            )
            StandaloneBadge()
        }
        PreviewCard(
            slide = frame.preview.slideFor(element.kind),
            element = element,
            onElementTap = frame.preview.onElementTap,
            aspect = frame.preview.options.aspect,
            onExpand = frame.actions.onExpand,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GroupTabs(element.kind, frame.actions.onGroup)
            ElementTabs(frame.editor)
            if (element == TextElement.SONG_LYRICS || element == TextElement.SONG_LYRICS_SECOND) {
                LanguageTabs(frame.editor)
            }
            ElementControls(frame.editor, inlineLanguage = false)
            ScreenOptionsCard(frame.actions.onOpenScreenOptions)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
        DisplayActions(
            onReset = frame.actions.onReset,
            onSave = frame.actions.onSave,
            resetLabel = stringResource(Res.string.display_reset),
            saveLabel = stringResource(Res.string.display_save),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
internal fun BackArrow(onBack: () -> Unit) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = stringResource(Res.string.cd_back),
        tint = LocalAppColors.current.accent,
        modifier = Modifier.size(24.dp).testTag(DisplayTextTags.BACK).clickable(onClick = onBack),
    )
}

/** Bible / Songs. */
@Composable
internal fun GroupTabs(kind: SlideKind, onGroup: (SlideKind) -> Unit) {
    DisplaySegments(
        options = listOf(
            stringResource(Res.string.display_group_bible),
            stringResource(Res.string.display_group_songs),
        ),
        selectedIndex = if (kind == SlideKind.SONG) 1 else 0,
        onSelect = { onGroup(if (it == 1) SlideKind.SONG else SlideKind.BIBLE) },
        tag = { DisplayTextTags.group(it) },
    )
}

/** The elements of the selected group: Verse / Reference, or Lyrics / Title / Number. */
@Composable
private fun ElementTabs(editor: ElementEditor) {
    val elements = if (editor.element.kind == SlideKind.SONG) SONG_TABS else BIBLE_TABS
    // Language 2 is a page of Lyrics, so Lyrics stays lit while it is open.
    val shown = if (editor.element == TextElement.SONG_LYRICS_SECOND) TextElement.SONG_LYRICS else editor.element
    DisplaySegments(
        options = elements.map { stringResource(it.tabLabel()) },
        selectedIndex = elements.indexOf(shown),
        onSelect = { editor.onSelect(elements[it]) },
        tag = { DisplayTextTags.elementTab(elements[it]) },
        compact = true,
    )
}

/** The way to everything about the screen that is not words. */
@Composable
internal fun ScreenOptionsCard(onOpen: () -> Unit) {
    val colors = LocalAppColors.current
    DisplayOverline(stringResource(Res.string.display_section_screen))
    DisplayCard(Modifier.testTag(DisplayTextTags.SCREEN_OPTIONS).clickable(onClick = onOpen)) {
        LabeledRow(
            stringResource(Res.string.display_screen_options),
            stringResource(Res.string.display_screen_options_hint),
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.muted)
        }
    }
}

internal val BIBLE_TABS = listOf(TextElement.BIBLE_VERSE, TextElement.BIBLE_REFERENCE)
internal val SONG_TABS = listOf(TextElement.SONG_LYRICS, TextElement.SONG_TITLE, TextElement.SONG_NUMBER)

/** What the picker is colouring, for its header: "Text color", "Bible verse · fill". */
internal class PickerRequest(val title: String, val subtitle: String, val initial: String)
