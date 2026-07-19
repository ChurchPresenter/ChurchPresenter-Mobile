package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.action_clear
import churchpresentermobile.composeapp.generated.resources.action_go_live
import churchpresentermobile.composeapp.generated.resources.cd_delete
import churchpresentermobile.composeapp.generated.resources.label_add_to_schedule
import churchpresentermobile.composeapp.generated.resources.label_live
import churchpresentermobile.composeapp.generated.resources.overline_on_screen_preview
import churchpresentermobile.composeapp.generated.resources.web_add_bookmark
import churchpresentermobile.composeapp.generated.resources.web_bookmarks_label
import churchpresentermobile.composeapp.generated.resources.web_no_bookmarks
import churchpresentermobile.composeapp.generated.resources.web_url_placeholder
import org.jetbrains.compose.resources.stringResource
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.Bookmark
import com.church.presenter.churchpresentermobile.viewmodel.WebViewModel
import com.church.presenter.churchpresentermobile.viewmodel.domainOf
import com.church.presenter.churchpresentermobile.viewmodel.normalizeUrl

/**
 * Drives the desktop's Web tab — enter a URL, project it live, and keep a list
 * of reusable bookmarks. Matches design screen 15 (Remote: Web).
 */
@Composable
fun WebScreen(
    viewModel: WebViewModel,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val url by viewModel.url.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val liveUrl by viewModel.liveUrl.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message!!, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            // ── URL bar ───────────────────────────────────────────────────
            UrlBar(
                value = url,
                onValueChange = viewModel::setUrl,
                onGo = { viewModel.projectPage() },
            )

            Spacer(Modifier.height(18.dp))
            // ── On-screen preview ─────────────────────────────────────────
            Overline(stringResource(Res.string.overline_on_screen_preview))
            PreviewCard(url)

            Spacer(Modifier.height(14.dp))
            // ── Actions (Clear + Project page — matches design) ───────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .clickable { viewModel.clearScreen() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(Res.string.action_clear), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Row(
                    modifier = Modifier
                        .weight(2f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.accent)
                        .clickable { viewModel.projectPage() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.DesktopWindows, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(Res.string.action_go_live), color = colors.onAccent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(12.dp))
            // Secondary: add to schedule (queue without going live)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.amber.copy(alpha = 0.16f))
                    .clickable { viewModel.addToSchedule() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = colors.amber, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.label_add_to_schedule), color = colors.amber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))
            // ── Bookmarks (matches design) ────────────────────────────────
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.web_bookmarks_label), color = colors.muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.05.em, modifier = Modifier.weight(1f))
                Text(stringResource(Res.string.web_add_bookmark), color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { viewModel.addBookmark() })
            }
            Spacer(Modifier.height(10.dp))
            if (bookmarks.isEmpty()) {
                Text(stringResource(Res.string.web_no_bookmarks), color = colors.muted, fontSize = 13.sp)
            } else {
                bookmarks.forEach { bm ->
                    BookmarkRow(
                        bookmark = bm,
                        isLive = liveUrl != null && liveUrl.equals(bm.url, ignoreCase = true),
                        onClick = { viewModel.loadBookmark(bm.id) },
                        onDelete = { viewModel.deleteBookmark(bm.id) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Overline(label: String) {
    val colors = LocalAppColors.current
    Text(label.uppercase(), color = colors.muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.05.em)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun UrlBar(value: String, onValueChange: (String) -> Unit, onGo: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.inputBg)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.accent, modifier = Modifier.size(15.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(stringResource(Res.string.web_url_placeholder), color = colors.muted, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.text, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PreviewCard(url: String) {
    val colors = LocalAppColors.current
    val urlPlaceholder = stringResource(Res.string.web_url_placeholder)
    val domain = domainOf(url).ifBlank { urlPlaceholder }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
    ) {
        // Browser chrome: three window dots
        Row(
            modifier = Modifier.fillMaxWidth().height(24.dp).background(colors.inputBg).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Dot(Color(0xFFFF5F57)); Dot(Color(0xFFFEBC2E)); Dot(Color(0xFF28C840))
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(domain, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            FauxLine(0.60f); Spacer(Modifier.height(7.dp)); FauxLine(0.85f); Spacer(Modifier.height(7.dp)); FauxLine(0.72f)
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
}

@Composable
private fun FauxLine(fraction: Float) {
    val colors = LocalAppColors.current
    Box(
        Modifier.fillMaxWidth(fraction).height(4.dp).clip(RoundedCornerShape(2.dp))
            .background(colors.border),
    )
}

@Composable
private fun BookmarkRow(bookmark: Bookmark, isLive: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isLive) colors.accentTint else colors.surface)
            .border(1.dp, if (isLive) colors.accent else colors.borderSubtle, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(colors.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Public, contentDescription = null, tint = colors.accent, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(bookmark.title, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(domainOf(bookmark.url).ifBlank { bookmark.url }, color = colors.muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (isLive) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.accent))
                Text(stringResource(Res.string.label_live), color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Box(
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.cd_delete), tint = colors.muted, modifier = Modifier.size(16.dp))
            }
        }
    }
}
