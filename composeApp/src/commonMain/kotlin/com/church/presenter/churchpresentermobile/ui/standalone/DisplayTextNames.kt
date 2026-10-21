package com.church.presenter.churchpresentermobile.ui.standalone

import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.display_element_lyrics
import churchpresentermobile.composeapp.generated.resources.display_element_number
import churchpresentermobile.composeapp.generated.resources.display_element_reference
import churchpresentermobile.composeapp.generated.resources.display_element_title
import churchpresentermobile.composeapp.generated.resources.display_element_verse
import churchpresentermobile.composeapp.generated.resources.display_heading_bible_reference
import churchpresentermobile.composeapp.generated.resources.display_heading_bible_verse
import churchpresentermobile.composeapp.generated.resources.display_heading_song_lyrics
import churchpresentermobile.composeapp.generated.resources.display_heading_song_lyrics_second
import churchpresentermobile.composeapp.generated.resources.display_heading_song_number
import churchpresentermobile.composeapp.generated.resources.display_heading_song_title
import churchpresentermobile.composeapp.generated.resources.display_picker_backing
import churchpresentermobile.composeapp.generated.resources.display_picker_outline
import churchpresentermobile.composeapp.generated.resources.display_picker_part_backing
import churchpresentermobile.composeapp.generated.resources.display_picker_part_fill
import churchpresentermobile.composeapp.generated.resources.display_picker_part_outline
import churchpresentermobile.composeapp.generated.resources.display_picker_part_shadow
import churchpresentermobile.composeapp.generated.resources.display_picker_shadow
import churchpresentermobile.composeapp.generated.resources.display_picker_text
import com.church.presenter.churchpresentermobile.model.TextElement
import com.church.presenter.churchpresentermobile.model.TextElementStyle
import org.jetbrains.compose.resources.StringResource

internal fun TextElement.tabLabel(): StringResource = when (this) {
    TextElement.BIBLE_VERSE -> Res.string.display_element_verse
    TextElement.BIBLE_REFERENCE -> Res.string.display_element_reference
    TextElement.SONG_LYRICS, TextElement.SONG_LYRICS_SECOND -> Res.string.display_element_lyrics
    TextElement.SONG_TITLE -> Res.string.display_element_title
    TextElement.SONG_NUMBER -> Res.string.display_element_number
}

internal fun TextElement.heading(): StringResource = when (this) {
    TextElement.BIBLE_VERSE -> Res.string.display_heading_bible_verse
    TextElement.BIBLE_REFERENCE -> Res.string.display_heading_bible_reference
    TextElement.SONG_LYRICS -> Res.string.display_heading_song_lyrics
    TextElement.SONG_LYRICS_SECOND -> Res.string.display_heading_song_lyrics_second
    TextElement.SONG_TITLE -> Res.string.display_heading_song_title
    TextElement.SONG_NUMBER -> Res.string.display_heading_song_number
}

internal fun ColorTarget.title(): StringResource = when (this) {
    ColorTarget.FILL -> Res.string.display_picker_text
    ColorTarget.OUTLINE -> Res.string.display_picker_outline
    ColorTarget.BACKING -> Res.string.display_picker_backing
    ColorTarget.SHADOW -> Res.string.display_picker_shadow
}

internal fun ColorTarget.part(): StringResource = when (this) {
    ColorTarget.FILL -> Res.string.display_picker_part_fill
    ColorTarget.OUTLINE -> Res.string.display_picker_part_outline
    ColorTarget.BACKING -> Res.string.display_picker_part_backing
    ColorTarget.SHADOW -> Res.string.display_picker_part_shadow
}

internal fun TextElementStyle.colorOf(target: ColorTarget): String = when (target) {
    ColorTarget.FILL -> color
    ColorTarget.OUTLINE -> outlineColor
    ColorTarget.BACKING -> backingColor
    ColorTarget.SHADOW -> shadowColor
}

internal fun TextElementStyle.withColor(target: ColorTarget, hex: String): TextElementStyle = when (target) {
    ColorTarget.FILL -> copy(color = hex)
    ColorTarget.OUTLINE -> copy(outlineColor = hex)
    ColorTarget.BACKING -> copy(backingColor = hex)
    ColorTarget.SHADOW -> copy(shadowColor = hex)
}
