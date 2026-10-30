package com.church.presenter.churchpresentermobile.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.app_title
import churchpresentermobile.composeapp.generated.resources.tv_address_hint
import churchpresentermobile.composeapp.generated.resources.tv_address_invalid
import churchpresentermobile.composeapp.generated.resources.tv_connect
import churchpresentermobile.composeapp.generated.resources.tv_enter_address
import churchpresentermobile.composeapp.generated.resources.tv_live_body
import churchpresentermobile.composeapp.generated.resources.tv_live_title
import churchpresentermobile.composeapp.generated.resources.tv_pick_hint
import churchpresentermobile.composeapp.generated.resources.tv_pick_searching
import churchpresentermobile.composeapp.generated.resources.tv_pick_title
import churchpresentermobile.composeapp.generated.resources.tv_signage_body
import churchpresentermobile.composeapp.generated.resources.tv_signage_title
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.tv.PhoneFinder
import com.church.presenter.churchpresentermobile.tv.PhoneSource
import com.church.presenter.churchpresentermobile.tv.parseAddress
import com.church.presenter.churchpresentermobile.ui.AppBackHandler
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.model.ThemeMode
import org.jetbrains.compose.resources.stringResource

/** Where the TV is. */
private sealed interface TvPage {
    data object Home : TvPage
    data object Pick : TvPage
    data class Live(val source: PhoneSource) : TvPage
}

/**
 * ChurchPresenter on a TV: not the phone app on a big screen, but a screen.
 *
 * Driven entirely with a remote — every control is a large focusable card, and
 * Back always goes one step out. A TV that was last showing a phone goes
 * straight back to it when it starts, so a lobby TV recovers from a power cut
 * with nobody touching it.
 */
@Composable
fun TvApp(settings: AppSettings = remember { AppSettings() }) {
    var page by remember {
        mutableStateOf<TvPage>(settings.tvLastSource?.let { TvPage.Live(it) } ?: TvPage.Home)
    }
    // Dark always: a TV is in a room with the lights down, and white would glare.
    AppTheme(themeMode = ThemeMode.DARK) {
        when (val current = page) {
            TvPage.Home -> TvHome(onLive = { page = TvPage.Pick })
            TvPage.Pick -> {
                AppBackHandler { page = TvPage.Home }
                PhonePicker(
                    last = settings.tvLastSource,
                    onPick = { source ->
                        settings.tvLastSource = source
                        page = TvPage.Live(source)
                    },
                )
            }
            is TvPage.Live -> {
                // Back means "show something else": forget the source, so a
                // restart does not jump straight back into it.
                AppBackHandler {
                    settings.tvLastSource = null
                    page = TvPage.Pick
                }
                TvLiveScreen(current.source)
            }
        }
    }
}

@Composable
private fun TvHome(onLive: () -> Unit) {
    val colors = LocalAppColors.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Column(
        modifier = Modifier.fillMaxSize().background(colors.background).padding(64.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Text(stringResource(Res.string.app_title), color = colors.text, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            TvCard(
                title = stringResource(Res.string.tv_live_title),
                body = stringResource(Res.string.tv_live_body),
                onClick = onLive,
                modifier = Modifier.width(420.dp).focusRequester(first).testTag(TvTags.HOME_LIVE),
            )
            // Phase 3. Shown, not hidden, so a church that came for signage sees
            // it is on its way rather than missing.
            TvCard(
                title = stringResource(Res.string.tv_signage_title),
                body = stringResource(Res.string.tv_signage_body),
                onClick = null,
                modifier = Modifier.width(420.dp).testTag(TvTags.HOME_SIGNAGE),
            )
        }
    }
}

@Composable
private fun PhonePicker(last: PhoneSource?, onPick: (PhoneSource) -> Unit) {
    val colors = LocalAppColors.current
    val finder = remember { PhoneFinder() }
    DisposableEffect(finder) {
        finder.start()
        onDispose { finder.stop() }
    }
    val found by finder.found.collectAsState()
    val first = remember { FocusRequester() }
    LaunchedEffect(found.isEmpty()) { runCatching { first.requestFocus() } }

    Row(
        modifier = Modifier.fillMaxSize().background(colors.background).padding(64.dp),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                stringResource(Res.string.tv_pick_title),
                color = colors.text,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            if (found.isEmpty()) {
                Text(stringResource(Res.string.tv_pick_searching), color = colors.text, fontSize = 18.sp)
                Text(stringResource(Res.string.tv_pick_hint), color = colors.muted, fontSize = 16.sp)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(found, key = { it.name }) { source ->
                    TvCard(
                        title = source.name,
                        body = "${source.host}:${source.port}",
                        onClick = { onPick(source) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (source == found.first()) Modifier.focusRequester(first) else Modifier)
                            .testTag(TvTags.source(source.name)),
                    )
                }
            }
        }
        AddressEntry(
            initial = last?.let { "${it.host}:${it.port}" }.orEmpty(),
            onConnect = onPick,
            modifier = Modifier.width(380.dp),
            focus = if (found.isEmpty()) first else null,
        )
    }
}

/** The fallback: a phone on another network, or one that is not advertising. */
@Composable
private fun AddressEntry(
    initial: String,
    onConnect: (PhoneSource) -> Unit,
    modifier: Modifier = Modifier,
    focus: FocusRequester? = null,
) {
    val colors = LocalAppColors.current
    var text by remember { mutableStateOf(initial) }
    var invalid by remember { mutableStateOf(false) }
    val connect = {
        val source = parseAddress(text)
        invalid = source == null
        source?.let(onConnect)
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(Res.string.tv_enter_address),
            color = colors.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; invalid = false },
            singleLine = true,
            placeholder = { Text(stringResource(Res.string.tv_address_hint)) },
            isError = invalid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { connect() }),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focus != null) Modifier.focusRequester(focus) else Modifier)
                .testTag(TvTags.ADDRESS),
        )
        if (invalid) {
            Text(
                stringResource(Res.string.tv_address_invalid),
                color = colors.danger,
                fontSize = 14.sp,
                modifier = Modifier.testTag(TvTags.ADDRESS_INVALID),
            )
        }
        TvCard(
            title = stringResource(Res.string.tv_connect),
            body = null,
            onClick = { connect() },
            modifier = Modifier.fillMaxWidth().testTag(TvTags.CONNECT),
        )
    }
}

/**
 * A remote's whole vocabulary is "move" and "OK", so the focused card has to be
 * unmistakable from across a room: a thick accent border, not a faint tint.
 *
 * @param onClick Null for a card that is shown but cannot be chosen yet; it
 *   takes no focus, so the remote skips over it.
 */
@Composable
internal fun TvCard(
    title: String,
    body: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (focused) colors.accentTint else colors.surface)
            .border(if (focused) 4.dp else 1.dp, if (focused) colors.accent else colors.border, shape)
            .then(
                if (onClick == null) Modifier
                else Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            color = if (onClick == null) colors.muted else colors.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (body != null) Text(body, color = colors.muted, fontSize = 16.sp)
    }
}

/** Names the TV screens' controls for a UI test; see the "tag it" section of AGENT.md. */
internal object TvTags {
    const val HOME_LIVE = "tv:home:live"
    const val HOME_SIGNAGE = "tv:home:signage"
    const val ADDRESS = "tv:pick:address"
    const val ADDRESS_INVALID = "tv:pick:addressInvalid"
    const val CONNECT = "tv:pick:connect"
    const val LIVE = "tv:live"
    const val STATUS = "tv:live:status"

    fun source(name: String) = "tv:pick:source:$name"
}

/** The page behind a TV that is not showing a slide: plain, dark, centred words. */
@Composable
internal fun TvMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White.copy(alpha = 0.7f), fontSize = 24.sp)
    }
}
