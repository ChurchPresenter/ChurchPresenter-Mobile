package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

/**
 * Renders [content] as a scannable QR code.
 *
 * Always drawn dark-on-white regardless of app theme — a dark-mode QR on a dark
 * card is a QR most scanners refuse, and the quiet zone around it is part of the
 * spec rather than padding taste.
 *
 * Note the asymmetry with [QrScanButton], which only *reads* codes; generating
 * one is what lets someone point a phone at the operator's screen and join as a
 * display without typing an IP address.
 */
@Composable
fun QrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
) {
    val painter = rememberQrCodePainter(content)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(10.dp),
    ) {
        Image(
            painter = painter,
            contentDescription = content,
            modifier = Modifier.size(size),
        )
    }
}
