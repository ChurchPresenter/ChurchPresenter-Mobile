package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The sizes the startup status screen is drawn at.
 *
 * The tablet design is not the phone screen stretched: at 1366dp it draws the
 * same things — hero, title, cards, buttons — at roughly one-and-a-half times
 * the size, because a 13sp row read from a tablet on a music stand is a row
 * nobody can read. So the screen has two sets of sizes and the same composables,
 * which pick theirs up from [LocalStatusMetrics] rather than each taking a
 * dozen parameters.
 *
 * Widths are not here: those live in `AdaptiveLayout.kt` with every other pane
 * width, and are the same whichever set of type sizes is in force.
 */
internal class StatusMetrics(
    val hero: Hero,
    val card: Card,
    val row: Row,
    val notice: Notice,
    /** Between two notices, and between the two columns of them on a tablet. */
    val noticeGap: Dp,
    val button: Button,
) {
    /** The icon-in-a-circle, title and small print a screen leads with. */
    class Hero(
        val circle: Dp,
        val glyph: Dp,
        val title: TextUnit,
        val subtitle: TextUnit,
        /** Between the circle and the title. */
        val gapAfterIcon: Dp,
        /** Between the small print and whatever comes next. */
        val gapAfter: Dp,
    ) {
        /**
         * The bare warning triangle an error screen leads with instead of the
         * circle. The design draws it at the same fraction of the circle on
         * both canvases — 56 of 72, 86 of 110.
         */
        val errorGlyph: Dp get() = circle * ERROR_GLYPH_OF_CIRCLE
    }

    /** A bordered summary card and the headings inside it. */
    class Card(
        val radius: Dp,
        val paddingHorizontal: Dp,
        val paddingVertical: Dp,
        val title: TextUnit,
        /** Under a heading, before its rows. */
        val titleGap: Dp,
        /** Between two headed groups in one card. */
        val groupGap: Dp,
    )

    /** One permission row, or one line of a list, inside a card. */
    class Row(
        val icon: Dp,
        val iconGap: Dp,
        val text: TextUnit,
        /** The "true"/"false" at a permission row's end. */
        val value: TextUnit,
        val gap: Dp,
        val lineHeight: TextUnit,
    )

    /** A tinted, bordered notice — a warning or an informational card. */
    class Notice(
        val radius: Dp,
        val padding: Dp,
        val icon: Dp,
        val title: TextUnit,
        val titleGap: Dp,
        val body: TextUnit,
    )

    class Button(
        val height: Dp,
        val radius: Dp,
        val text: TextUnit,
        /**
         * Whether the quiet secondary action is outlined. The phone design
         * leaves it as bare text under the primary; the tablet's draws a
         * bordered surface, so the pair reads as two buttons at that size.
         */
        val outlineQuiet: Boolean,
    )

    companion object {
        /** The sizes the phone screen has always used. */
        val Phone = StatusMetrics(
            hero = Hero(
                circle = 72.dp, glyph = 36.dp,
                title = 22.sp, subtitle = 13.sp,
                gapAfterIcon = 20.dp, gapAfter = 28.dp,
            ),
            card = Card(
                radius = 16.dp, paddingHorizontal = 18.dp, paddingVertical = 16.dp,
                title = 13.sp, titleGap = 6.dp, groupGap = 12.dp,
            ),
            row = Row(icon = 18.dp, iconGap = 8.dp, text = 13.sp, value = 12.sp, gap = 10.dp, lineHeight = 18.sp),
            notice = Notice(
                radius = 14.dp, padding = 14.dp, icon = 20.dp,
                title = 13.sp, titleGap = 2.dp, body = 12.sp,
            ),
            noticeGap = 8.dp,
            button = Button(height = 48.dp, radius = 13.dp, text = 14.sp, outlineQuiet = false),
        )

        /** The design's sizes at 1366dp — sections 7 to 9 of the tablet canvas. */
        val Tablet = StatusMetrics(
            hero = Hero(
                circle = 110.dp, glyph = 54.dp,
                title = 44.sp, subtitle = 19.sp,
                gapAfterIcon = 28.dp, gapAfter = 38.dp,
            ),
            card = Card(
                radius = 20.dp, paddingHorizontal = 26.dp, paddingVertical = 24.dp,
                title = 19.sp, titleGap = 10.dp, groupGap = 22.dp,
            ),
            row = Row(icon = 22.dp, iconGap = 14.dp, text = 18.sp, value = 17.sp, gap = 16.dp, lineHeight = 32.sp),
            notice = Notice(
                radius = 18.dp, padding = 22.dp, icon = 24.dp,
                title = 18.sp, titleGap = 11.dp, body = 15.sp,
            ),
            noticeGap = 14.dp,
            button = Button(height = 58.dp, radius = 16.dp, text = 18.sp, outlineQuiet = true),
        )
    }
}

private const val ERROR_GLYPH_OF_CIRCLE = 0.78f

/** The sizes in force for the status screen being drawn — phone unless the screen says otherwise. */
internal val LocalStatusMetrics = compositionLocalOf { StatusMetrics.Phone }
