package com.church.presenter.churchpresentermobile.bibleimport

/**
 * Names an installed Bible module: `ENG_ACV.spb`, `CZE_CSP.spb`, `SWA.spb`.
 *
 * A port of the desktop's `BibleCatalogNaming`, so a translation downloaded on the phone gets the
 * same file name it would on the desktop — which is what lets "Installed" be decided by name, and
 * what a translation shared between the two will one day be matched on.
 */
internal object BibleNaming {

    /** Used when a module carries no usable language code. */
    const val UNKNOWN_LANGUAGE = "UND"

    /** The `##Abbreviation:` rule: the initial of each word in the translation's name. */
    fun abbreviation(bibleName: String): String =
        bibleName.split(" ")
            .filter { it.isNotBlank() }
            .joinToString("") { it.first().toString() }

    /**
     * `("ENG", "ACV")` → `ENG_ACV`, `("THA", "KJVTHAI")` → `THA_KJVTHAI`, `("CZE", "ČSP")` → `CZE_CSP`.
     *
     * `("SWA", "SWA")` → `SWA` and `("AFR", "AFR3353")` → `AFR_3353`, so the language is not said
     * twice.
     */
    fun fileStem(language: String?, identifier: String?): String {
        val lang = slug(language).ifEmpty { UNKNOWN_LANGUAGE }
        val id = slug(identifier)
        return when {
            id.isEmpty() || id == lang -> lang
            id.startsWith(lang) && id.length > lang.length -> "${lang}_${id.removePrefix(lang)}"
            else -> "${lang}_$id"
        }
    }

    /** Appends `_2`, `_3`… until the stem is free. */
    fun deduplicate(stem: String, taken: Set<String>): String {
        if (stem !in taken) return stem
        var suffix = 2
        while ("${stem}_$suffix" in taken) suffix++
        return "${stem}_$suffix"
    }

    /**
     * Reduces to `A-Z0-9`, folding accents first so `Č` becomes `C` rather than being dropped.
     *
     * The desktop does this with an NFD decomposition, which common Kotlin has no API for; the
     * table below covers the Latin letters a translation identifier actually uses.
     */
    fun slug(value: String?): String = fold(value).filter { it in 'A'..'Z' || it in '0'..'9' }

    /**
     * A file stem the operator typed on the convert screen — `UKR_OGI` — kept as typed apart from
     * anything a file name cannot carry. Unlike [slug], underscores and hyphens survive: they are
     * how the operator separated the language from the translation.
     */
    fun typedStem(value: String): String =
        fold(value).filter { it in 'A'..'Z' || it in '0'..'9' || it == '_' || it == '-' }

    private fun fold(value: String?): String =
        value.orEmpty().map { ACCENT_FOLDS[it] ?: it }.joinToString("").uppercase()

    private val ACCENT_FOLDS: Map<Char, Char> = buildMap {
        fun fold(from: String, to: Char) = from.forEach { put(it, to) }
        fold("ÀÁÂÃÄÅĀĂĄàáâãäåāăą", 'A')
        fold("ÇĆĈĊČçćĉċč", 'C')
        fold("ĎĐďđ", 'D')
        fold("ÈÉÊËĒĔĖĘĚèéêëēĕėęě", 'E')
        fold("ĜĞĠĢĝğġģ", 'G')
        fold("ĤĦĥħ", 'H')
        fold("ÌÍÎÏĨĪĬĮİìíîïĩīĭįı", 'I')
        fold("Ĵĵ", 'J')
        fold("Ķķ", 'K')
        fold("ĹĻĽĿŁĺļľŀł", 'L')
        fold("ÑŃŅŇñńņň", 'N')
        fold("ÒÓÔÕÖØŌŎŐòóôõöøōŏő", 'O')
        fold("ŔŖŘŕŗř", 'R')
        fold("ŚŜŞŠśŝşšȘș", 'S')
        fold("ŢŤŦţťŧȚț", 'T')
        fold("ÙÚÛÜŨŪŬŮŰŲùúûüũūŭůűų", 'U')
        fold("Ŵŵ", 'W')
        fold("ÝŸŶýÿŷ", 'Y')
        fold("ŹŻŽźżž", 'Z')
    }
}

/**
 * A known broken verse in a published source file, corrected at conversion time.
 *
 * Ported from the desktop's `VersePatches` so a module converted on the phone matches one
 * converted there. Only the text corrections come across; the desktop's id-renames and inserted
 * verses repair `.spb` files already installed, which a fresh conversion never produces.
 */
internal data class VersePatch(
    val correctedText: String,
    val language: String? = null,
    val minimumPrefixLength: Int = 0,
    /** When set, the patch applies only to a verse reading exactly this. */
    val matchText: String? = null,
)

internal object VersePatches {

    private const val RUSSIAN_PATCH_MIN_LENGTH = 20

    private val PATCHES: Map<Triple<Int, Int, Int>, VersePatch> = mapOf(
        // 2 Chronicles 2:14 — truncated in Russian Synodal source XML (ends mid-word "...госпо")
        Triple(14, 2, 14) to VersePatch(
            language = "RUS",
            minimumPrefixLength = RUSSIAN_PATCH_MIN_LENGTH,
            correctedText = "Сына [одной] женщины из дочерей Дановых, — а отец его Тирянин, — умеющего " +
                "делать [изделия] из золота и из серебра, из меди, из железа, из камней и из дерев, " +
                "из [пряжи] пурпурового, яхонтового [цвета], и из виссона, и из багряницы, и " +
                "вырезывать всякую резьбу, и исполнять все, что будет поручено ему вместе с " +
                "художниками твоими и с художниками господина моего Давида, отца твоего.",
        ),
        // Psalm 146:6 (display) — grammatical error "до землю" → "до земли"
        Triple(19, 146, 6) to VersePatch(
            matchText = "Смиренных возвышает Господь, а нечестивых унижает до землю.",
            correctedText = "Смиренных возвышает Господь, а нечестивых унижает до земли.",
        ),
    )

    fun apply(text: String, language: String?, book: Int, chapter: Int, verse: Int): String {
        val patch = PATCHES[Triple(book, chapter, verse)] ?: return text
        if (patch.language != null && patch.language != language?.uppercase()) return text
        if (patch.matchText != null) return if (text == patch.matchText) patch.correctedText else text
        if (patch.minimumPrefixLength > 0 && text.length < patch.minimumPrefixLength) return text
        return patch.correctedText
    }
}
