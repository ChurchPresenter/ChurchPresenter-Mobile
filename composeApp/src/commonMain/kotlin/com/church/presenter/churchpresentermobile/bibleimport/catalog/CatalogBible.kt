package com.church.presenter.churchpresentermobile.bibleimport.catalog

import com.church.presenter.churchpresentermobile.bibleimport.BibleXmlFormat

/** The archives Bibles can be downloaded from, in the order they are offered. */
enum class BibleSource(val label: String, val format: BibleXmlFormat) {
    EBIBLE("eBible.org", BibleXmlFormat.USFX),
    ZEFANIA("Zefania", BibleXmlFormat.ZEFANIA),
    BEBLIA("Holy Bible XML", BibleXmlFormat.BEBLIA),
}

/** Which portion of scripture a translation covers. */
enum class Testament(val label: String) {
    OLD("OT"),
    NEW("NT"),
    FULL("OT-NT"),
}

private val NT_TOKEN = Regex("\\bNT\\b")
private val OT_TOKEN = Regex("\\bOT\\b")

/**
 * One downloadable translation, as a catalogue describes it — before anything is downloaded.
 *
 * A port of the desktop's `BibleModule`, so the two derive the same [fileStem] for the same row.
 *
 * @param downloadKey How the source locates the download: a translation id (eBible), a repository
 *   path (Zefania), or `<commit>/<file>` (Holy Bible XML, pinned so the hash stays meaningful).
 * @param checksum The git blob SHA-1 the archive lists; blank where the source publishes none.
 * @param sizeBytes Zero when the source does not say — eBible's catalogue carries no sizes.
 */
data class CatalogBible(
    val source: BibleSource,
    val downloadKey: String,
    val checksum: String = "",
    val sizeBytes: Long = 0,
    val language: String,
    val languageName: String = "",
    val languageNativeName: String = "",
    val identifier: String,
    val displayName: String,
    val copyright: String = "",
    val otBookCount: Int = 0,
    val ntBookCount: Int = 0,
    val fileStem: String,
) {
    /** Stable across sources, so an install from one cannot be confused with one from another. */
    val key: String get() = "${source.name}:$downloadKey"

    val fileName: String get() = "$fileStem.spb"

    /** What to call the language: its English name, else its code. */
    val languageLabel: String get() = languageName.ifBlank { language }

    /**
     * Taken from the published book counts where the source has them; otherwise a standalone
     * "NT" or "OT" in the name decides, and a name with neither is taken to be a whole Bible.
     */
    val testament: Testament
        get() {
            if (otBookCount > 0 || ntBookCount > 0) {
                return when {
                    otBookCount > 0 && ntBookCount > 0 -> Testament.FULL
                    ntBookCount > 0 -> Testament.NEW
                    else -> Testament.OLD
                }
            }
            val hasNt = NT_TOKEN.containsMatchIn(displayName)
            val hasOt = OT_TOKEN.containsMatchIn(displayName)
            return when {
                hasNt && !hasOt -> Testament.NEW
                hasOt && !hasNt -> Testament.OLD
                else -> Testament.FULL
            }
        }

    /** Whether [query] names this translation, its language in either spelling, or its code. */
    fun matches(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return listOf(displayName, identifier, language, languageName, languageNativeName)
            .any { it.contains(q, ignoreCase = true) }
    }
}

/** What a language is called, in English and in itself. */
internal data class LanguageNaming(val english: String, val native: String = "")

/**
 * Language code to name, for the catalogue's language filter and for the convert screen.
 *
 * eBible's catalogue names every one of its ~1,240 codes in both spellings, so it doubles as the
 * table for the Zefania archive, whose folders are codes alone. [UNLISTED] fills only the gap —
 * the archive's codes eBible has no row for — and is a port of the desktop's table.
 */
internal object BibleLanguageNames {

    private val UNLISTED = mapOf(
        "AFR" to LanguageNaming("Afrikaans"),
        "ALB" to LanguageNaming("Albanian", "Shqip"),
        "ARA" to LanguageNaming("Arabic", "العربية"),
        "BAQ" to LanguageNaming("Basque", "Euskara"),
        "BUL" to LanguageNaming("Bulgarian", "български"),
        "CHI" to LanguageNaming("Chinese", "中文"),
        "CHU" to LanguageNaming("Church Slavonic"),
        "CZE" to LanguageNaming("Czech", "Čeština"),
        "ESP" to LanguageNaming("Esperanto"),
        "FRE" to LanguageNaming("French", "Français"),
        "GAE" to LanguageNaming("Gaelic"),
        "GER" to LanguageNaming("German", "Deutsch"),
        "GLA" to LanguageNaming("Scottish Gaelic", "Gàidhlig"),
        "GOT" to LanguageNaming("Gothic"),
        "GRE" to LanguageNaming("Greek", "Ελληνικά"),
        "JAM" to LanguageNaming("Jamaican Creole"),
        "KAB" to LanguageNaming("Kabyle", "Taqbaylit"),
        "LAV" to LanguageNaming("Latvian", "Latviešu"),
        "MAO" to LanguageNaming("Maori", "Māori"),
        "NDS" to LanguageNaming("Low German", "Plattdüütsch"),
        "NL" to LanguageNaming("Dutch", "Nederlands"),
        "NOR" to LanguageNaming("Norwegian", "Norsk"),
        "RUM" to LanguageNaming("Romanian", "Română"),
        "SCR" to LanguageNaming("Croatian", "Hrvatski"),
        "SHU" to LanguageNaming("Chadian Arabic"),
        "SWA" to LanguageNaming("Swahili", "Kiswahili"),
        "SYR" to LanguageNaming("Syriac"),
        "UND" to LanguageNaming("Unknown"),
        "XKL" to LanguageNaming("Kenyang"),
    )

    /**
     * The languages the app has book-name tables for, named — so a file converted with no
     * catalogue behind it still says "Ukrainian" rather than "UKR" on the convert screen.
     */
    private val TABLED = mapOf(
        "ENG" to LanguageNaming("English"),
        "UKR" to LanguageNaming("Ukrainian", "Українська"),
        "RUS" to LanguageNaming("Russian", "Русский"),
        "DEU" to LanguageNaming("German", "Deutsch"),
        "FRA" to LanguageNaming("French", "Français"),
        "SPA" to LanguageNaming("Spanish", "Español"),
        "POR" to LanguageNaming("Portuguese", "Português"),
        "ITA" to LanguageNaming("Italian", "Italiano"),
        "NLD" to LanguageNaming("Dutch", "Nederlands"),
        "DUT" to LanguageNaming("Dutch", "Nederlands"),
        "POL" to LanguageNaming("Polish", "Polski"),
        "ZHO" to LanguageNaming("Chinese", "中文"),
        "KOR" to LanguageNaming("Korean", "한국어"),
        "HEB" to LanguageNaming("Hebrew", "עברית"),
        "TAM" to LanguageNaming("Tamil", "தமிழ்"),
        "CES" to LanguageNaming("Czech", "Čeština"),
        "SLK" to LanguageNaming("Slovak", "Slovenčina"),
        "SLO" to LanguageNaming("Slovak", "Slovenčina"),
        "HRV" to LanguageNaming("Croatian", "Hrvatski"),
        "RON" to LanguageNaming("Romanian", "Română"),
        "MOL" to LanguageNaming("Romanian", "Română"),
        "SWE" to LanguageNaming("Swedish", "Svenska"),
        "NOB" to LanguageNaming("Norwegian", "Norsk"),
        "FIN" to LanguageNaming("Finnish", "Suomi"),
        "EST" to LanguageNaming("Estonian", "Eesti"),
        "BEL" to LanguageNaming("Belarusian", "Беларуская"),
        "KAZ" to LanguageNaming("Kazakh", "Қазақ"),
        "UZB" to LanguageNaming("Uzbek", "Oʻzbek"),
        "TUR" to LanguageNaming("Turkish", "Türkçe"),
        "FAS" to LanguageNaming("Persian", "فارسی"),
        "PER" to LanguageNaming("Persian", "فارسی"),
        "HIN" to LanguageNaming("Hindi", "हिन्दी"),
        "NEP" to LanguageNaming("Nepali", "नेपाली"),
        "THA" to LanguageNaming("Thai", "ไทย"),
        "LAO" to LanguageNaming("Lao", "ລາວ"),
        "JPN" to LanguageNaming("Japanese", "日本語"),
        "IND" to LanguageNaming("Indonesian", "Bahasa Indonesia"),
        "MSA" to LanguageNaming("Malay", "Bahasa Melayu"),
        "MAY" to LanguageNaming("Malay", "Bahasa Melayu"),
        "TGL" to LanguageNaming("Tagalog"),
        "FIL" to LanguageNaming("Filipino"),
    )

    /**
     * The table as it stands. A catalogue's names fill in the thousand-odd languages nothing here
     * covers, but the names above win where they exist: they were checked by hand, and a catalogue
     * says "German, Standard" where the operator expects "German".
     */
    fun resolve(catalogue: Map<String, LanguageNaming>): Map<String, LanguageNaming> =
        UNLISTED + catalogue + TABLED

    /** A code's English name from the built-in names alone, or the code itself. */
    fun nameOf(code: String?): String {
        val key = code?.trim()?.uppercase().orEmpty()
        return (UNLISTED[key] ?: TABLED[key])?.english ?: key
    }
}
