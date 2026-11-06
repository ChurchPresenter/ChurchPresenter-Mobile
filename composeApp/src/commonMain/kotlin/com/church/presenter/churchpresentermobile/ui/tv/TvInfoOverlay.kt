package com.church.presenter.churchpresentermobile.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.tv_connecting
import churchpresentermobile.composeapp.generated.resources.tv_ended
import churchpresentermobile.composeapp.generated.resources.tv_info_showing
import churchpresentermobile.composeapp.generated.resources.tv_kind_computer
import churchpresentermobile.composeapp.generated.resources.tv_kind_phone
import churchpresentermobile.composeapp.generated.resources.tv_output_number
import churchpresentermobile.composeapp.generated.resources.tv_reconnecting
import churchpresentermobile.composeapp.generated.resources.tv_too_new
import com.church.presenter.churchpresentermobile.tv.LiveState
import com.church.presenter.churchpresentermobile.tv.SourceKind
import com.church.presenter.churchpresentermobile.tv.TvSource
import org.jetbrains.compose.resources.stringResource

/** The keys a remote's OK button arrives as. */
internal val OK_KEYS = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

/**
 * Tells a held OK from a press. A held key repeats its key-down, so the second
 * key-down with no key-up between them is the hold.
 */
internal class HeldOkKey {
    private var down = false

    /** Takes one key event; true when it shows OK is being held. */
    fun update(isOk: Boolean, isDown: Boolean): Boolean {
        if (!isOk) return false
        val held = isDown && down
        down = isDown
        return held
    }
}

/**
 * What this TV is connected to, for whoever is standing in front of it with the
 * remote wondering why it is not showing what they expect.
 */
@Composable
internal fun TvInfoOverlay(source: TvSource, state: LiveState, modifier: Modifier = Modifier) {
    val kind = when (source.kind) {
        SourceKind.PHONE -> stringResource(Res.string.tv_kind_phone)
        SourceKind.DESKTOP ->
            "${stringResource(Res.string.tv_kind_computer)} · " +
                stringResource(Res.string.tv_output_number, source.output)
    }
    Column(
        modifier = modifier
            .padding(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag(TvTags.INFO),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(source.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(kind, color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
        Text("${source.host}:${source.port}", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
        Text(stateLabel(state, source.name), color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
    }
}

@Composable
private fun stateLabel(state: LiveState, sourceName: String): String = when (state) {
    LiveState.Connecting -> stringResource(Res.string.tv_connecting, sourceName)
    is LiveState.Showing, is LiveState.Picture -> stringResource(Res.string.tv_info_showing)
    is LiveState.Lost -> stringResource(Res.string.tv_reconnecting, sourceName)
    LiveState.Ended -> stringResource(Res.string.tv_ended)
    LiveState.TooNew -> stringResource(Res.string.tv_too_new)
}
