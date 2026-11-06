package com.church.presenter.churchpresentermobile.ui.tv

import android.app.Activity
import android.os.Build
import android.widget.VideoView
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.tv_signage_pair_body
import churchpresentermobile.composeapp.generated.resources.tv_signage_pair_title
import churchpresentermobile.composeapp.generated.resources.tv_signage_searching
import coil3.compose.AsyncImage
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.createWebSocketClient
import com.church.presenter.churchpresentermobile.signage.HttpContentSource
import com.church.presenter.churchpresentermobile.signage.ManifestSync
import com.church.presenter.churchpresentermobile.signage.PlayerInfo
import com.church.presenter.churchpresentermobile.signage.ScreenPreviews
import com.church.presenter.churchpresentermobile.signage.SignageConnection
import com.church.presenter.churchpresentermobile.signage.SignagePlayer
import com.church.presenter.churchpresentermobile.signage.SignageScreen
import com.church.presenter.churchpresentermobile.signage.SignageStore
import com.church.presenter.churchpresentermobile.tv.DESKTOP_SERVICE_TYPE
import com.church.presenter.churchpresentermobile.tv.KeepScreenOn
import com.church.presenter.churchpresentermobile.tv.ServiceFinder
import com.church.presenter.churchpresentermobile.util.appVersion
import kotlinx.coroutines.launch
import org.churchpresenter.signage.core.MediaKind
import org.churchpresenter.signage.core.PlaylistItem
import org.jetbrains.compose.resources.stringResource
import java.io.File

/** `java.time`, which the schedule runs on, arrived in Android 8; every Android TV has it. */
actual val signageSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

/**
 * The TV as a signage player, alongside the Raspberry Pi one: paired from the
 * desktop's Signage tab, it plays that screen's playlists and schedule on its
 * own, with the computer off.
 *
 * It lists only what it can do, so the desktop sends nothing else: pictures,
 * videos and streams, a wipe and a preview. No web pages or touch kiosks (no
 * Chromium here), no self-update (the app store does that) and no HDMI-CEC, so a
 * `tv_off` block shows black and the TV's own timer turns it off.
 */
@Composable
actual fun SignageMode(settings: AppSettings) {
    KeepScreenOn()
    val context = LocalContext.current
    val finder = remember { ServiceFinder(DESKTOP_SERVICE_TYPE) }
    DisposableEffect(finder) {
        finder.start()
        onDispose { finder.stop() }
    }
    // One client for the socket and the downloads: it has no request timeout, which
    // a large video needs.
    val client = remember { createWebSocketClient() }
    DisposableEffect(client) { onDispose { client.close() } }
    val store = remember { SignageStore(File(context.filesDir, "signage").apply { mkdirs() }) }
    val player = remember {
        val previews = (context as? Activity)?.window?.let(::ScreenPreviews)
        SignagePlayer(
            store = store,
            sync = { address ->
                val token = store.identity().token.orEmpty()
                ManifestSync(store, HttpContentSource(client, "http://$address", token)).sync()
            },
            previews = previews?.let { it::take },
        )
    }
    LaunchedEffect(player) {
        val metrics = context.resources.displayMetrics
        launch { player.runScreen() }
        SignageConnection(
            client = client,
            store = store,
            player = player,
            info = PlayerInfo(appVersion, metrics.widthPixels, metrics.heightPixels),
            addresses = { finder.found.value.map { "${it.host}:${it.port}" } },
        ).run()
    }
    val screen by player.screen.collectAsState()
    val identify by player.identify.collectAsState()
    Box(modifier = Modifier.fillMaxSize().background(Color.Black).testTag(TvTags.SIGNAGE)) {
        SignageContent(screen, onVideoEnded = player::itemEnded)
        identify?.let { name ->
            Text(
                text = name,
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun SignageContent(screen: SignageScreen, onVideoEnded: () -> Unit) {
    when (screen) {
        SignageScreen.Searching -> TvMessage(stringResource(Res.string.tv_signage_searching))
        is SignageScreen.Pairing -> PairingCard(screen.code)
        is SignageScreen.Idle -> IdleCard(screen.title, screen.logo)
        SignageScreen.Blank -> Box(Modifier.fillMaxSize().background(Color.Black))
        is SignageScreen.Playing -> PlayingItem(screen.item, screen.file, onVideoEnded)
    }
}

/** The code, large enough to read from where the church's computer is. */
@Composable
private fun PairingCard(code: String) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(Res.string.tv_signage_pair_title), color = Color.White, fontSize = 28.sp)
        Text(
            code,
            color = Color.White,
            fontSize = 96.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag(TvTags.PAIRING_CODE),
        )
        Text(
            stringResource(Res.string.tv_signage_pair_body),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 20.sp,
        )
    }
}

@Composable
private fun IdleCard(title: String, logo: File?) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        logo?.let { AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(200.dp)) }
        if (title.isNotBlank()) Text(title, color = Color.White, fontSize = 40.sp)
    }
}

@Composable
private fun PlayingItem(item: PlaylistItem, file: File?, onVideoEnded: () -> Unit) {
    val fade = tween<Float>(item.transitionMs.toInt())
    Crossfade(targetState = item to file, animationSpec = fade, label = "signage") { (shown, source) ->
        when (shown.kind) {
            MediaKind.IMAGE -> AsyncImage(
                model = source,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            MediaKind.VIDEO, MediaKind.STREAM ->
                VideoItem(source?.toUri()?.toString() ?: shown.url.orEmpty(), onVideoEnded)
            MediaKind.WEB -> Unit
        }
    }
}

@Composable
private fun VideoItem(uri: String, onEnded: () -> Unit) {
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(uri.toUri())
                setOnPreparedListener { it.start() }
                setOnCompletionListener { onEnded() }
                // A stream that will not play is passed over, never left as an error dialog.
                setOnErrorListener { _, _, _ ->
                    onEnded()
                    true
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
