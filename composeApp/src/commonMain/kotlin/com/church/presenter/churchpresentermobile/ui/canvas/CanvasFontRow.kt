package com.church.presenter.churchpresentermobile.ui.canvas

import com.church.presenter.churchpresentermobile.ui.verticalScrollbar
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.canvas_choose_font
import churchpresentermobile.composeapp.generated.resources.canvas_font_default
import churchpresentermobile.composeapp.generated.resources.canvas_font_handwriting
import churchpresentermobile.composeapp.generated.resources.canvas_font_mono
import churchpresentermobile.composeapp.generated.resources.canvas_font_sans
import churchpresentermobile.composeapp.generated.resources.canvas_text_size
import com.church.presenter.churchpresentermobile.model.CanvasFont
import com.church.presenter.churchpresentermobile.model.TextOptions
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The text tool's font well and the size beside it, each opening a list to pick
 * from. Every font is listed in itself, so the list doubles as a preview.
 */
@Composable
internal fun FontRow(text: TextOptions, onChange: (TextOptions) -> Unit) {
    val colors = canvasColors
    val shape = RoundedCornerShape(10.dp)
    var fonts by remember { mutableStateOf(false) }
    var sizes by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(shape)
                    .background(colors.control)
                    .border(1.dp, colors.border, shape)
                    .clickable(role = Role.DropdownList) { fonts = true }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(text.font.label()),
                    color = colors.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = fontFamily(text.font),
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = stringResource(Res.string.canvas_choose_font),
                    tint = colors.text,
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(expanded = fonts, onDismissRequest = { fonts = false }, containerColor = colors.panel) {
                CanvasFont.entries.forEach { font ->
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(font.label()), color = colors.text, fontFamily = fontFamily(font))
                        },
                        onClick = {
                            onChange(text.copy(font = font))
                            fonts = false
                        },
                    )
                }
            }
        }
        Box {
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(36.dp)
                    .clip(shape)
                    .background(colors.control)
                    .border(1.dp, colors.border, shape)
                    .clickable(role = Role.DropdownList, onClickLabel = stringResource(Res.string.canvas_text_size)) {
                        sizes = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${text.size}",
                    color = colors.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }
            val sizeScroll = rememberScrollState()
            DropdownMenu(
                expanded = sizes,
                onDismissRequest = { sizes = false },
                containerColor = colors.panel,
                modifier = Modifier.verticalScrollbar(sizeScroll),
                scrollState = sizeScroll,
            ) {
                TextSizes.forEach { size ->
                    DropdownMenuItem(
                        text = { Text("$size", color = colors.text, fontFamily = FontFamily.Monospace) },
                        onClick = {
                            onChange(text.copy(size = size))
                            sizes = false
                        },
                    )
                }
            }
        }
    }
}

/** The sizes the text tool offers, from a caption to a slide title. */
private val TextSizes = listOf(14, 18, 24, 32, 40, 48, 64, 80)

private fun CanvasFont.label(): StringResource = when (this) {
    CanvasFont.SERIF -> Res.string.canvas_font_default
    CanvasFont.SANS -> Res.string.canvas_font_sans
    CanvasFont.MONO -> Res.string.canvas_font_mono
    CanvasFont.HANDWRITING -> Res.string.canvas_font_handwriting
}
