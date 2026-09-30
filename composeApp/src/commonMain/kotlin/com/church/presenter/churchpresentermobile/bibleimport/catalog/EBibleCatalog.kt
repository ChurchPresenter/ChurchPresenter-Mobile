package com.church.presenter.churchpresentermobile.bibleimport.catalog

import com.church.presenter.churchpresentermobile.bibleimport.BibleNaming

/**
 * eBible.org's catalogue: one CSV row per translation, published at [CATALOG_URL].
 *
 * A port of the desktop's `EBibleSource.parseCatalog`. Only rows the publisher marks both
 * redistributable and downloadable are offered — the archive lists translations it is not
 * permitted to hand out, and offering those would make the app the one distributing them.
 */
internal object EBibleCatalog {

    const val CATALOG_URL = "https://ebible.org/Scriptures/translations.csv"

    fun downloadUrl(translationId: String): String = "https://ebible.org/Scriptures/${translationId}_usfx.zip"

    fun parse(body: String): List<CatalogBible> {
        val rows = Csv.parse(body)
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().map { it.trim().trim('\uFEFF') }
        val columns = Columns(header)
        if (columns.id < 0 || columns.language < 0) return emptyList()

        val taken = mutableSetOf<String>()
        return rows.drop(1)
            .mapNotNull { row -> columns.module(row) }
            // Deduplicated in catalogue order, so every device derives the same installed names.
            .map { module ->
                val stem = BibleNaming.deduplicate(module.fileStem, taken)
                taken.add(stem)
                module.copy(fileStem = stem)
            }
    }

    /** Where each column sits; a catalogue that drops an optional one loses that field, not the list. */
    private class Columns(private val header: List<String>) {
        private fun index(name: String) = header.indexOfFirst { it.equals(name, ignoreCase = true) }

        val id = index("translationId")
        val language = index("languageCode")
        private val shortTitle = index("shortTitle")
        private val title = index("title")
        private val copyright = index("Copyright")
        private val redistributable = index("Redistributable")
        private val downloadable = index("downloadable")
        private val languageNameEnglish = index("languageNameInEnglish")
        private val languageName = index("languageName")
        private val otBooks = index("OTbooks")
        private val ntBooks = index("NTbooks")

        fun module(row: List<String>): CatalogBible? {
            fun cell(i: Int) = row.getOrNull(i)?.trim().orEmpty()
            val translationId = cell(id)
            if (translationId.isEmpty()) return null
            if (!cell(redistributable).isTrue() || !cell(downloadable).isTrue()) return null
            val code = cell(language).uppercase()
            return CatalogBible(
                source = BibleSource.EBIBLE,
                downloadKey = translationId,
                language = code,
                // English first, since the autonym is no help to someone typing "english"; a row
                // with no English name falls back to the autonym rather than a bare code.
                languageName = cell(languageNameEnglish).ifBlank { cell(languageName) },
                languageNativeName = cell(languageName),
                identifier = translationId,
                displayName = cell(shortTitle).ifBlank { cell(title) }.ifBlank { translationId },
                copyright = cell(copyright),
                otBookCount = cell(otBooks).toIntOrNull() ?: 0,
                ntBookCount = cell(ntBooks).toIntOrNull() ?: 0,
                fileStem = BibleNaming.fileStem(code, translationId),
            )
        }

        private fun String.isTrue(): Boolean = lowercase() in setOf("true", "yes", "1")
    }

    /**
     * Every language the catalogue names, in both spellings — the table the other sources borrow.
     *
     * The catalogue is not consistent with itself: Ukrainian is spelt "Ukrainian" on three rows and
     * "Ukranian" on a fourth, and German is both "German" and "German, Standard". The spelling most
     * rows use wins, so one typo cannot rename a language for every row that shares its code.
     */
    fun languageNames(modules: List<CatalogBible>): Map<String, LanguageNaming> =
        modules.filter { it.languageName.isNotBlank() }
            .groupBy { it.language }
            .mapValues { (_, rows) ->
                val english = rows.mostCommon { it.languageName }
                val native = rows.mostCommon { it.languageNativeName }
                LanguageNaming(english, native.takeIf { it != english }.orEmpty())
            }

    private fun List<CatalogBible>.mostCommon(field: (CatalogBible) -> String): String =
        map(field).filter { it.isNotBlank() }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key.orEmpty()
}

/**
 * Just enough CSV for the eBible catalogue: quoted fields, embedded commas, doubled quotes —
 * copyright strings in it routinely contain both commas and quotes.
 */
internal object Csv {
    fun parse(body: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0
        fun endRow() {
            if (field.isNotEmpty() || row.isNotEmpty()) {
                row.add(field.toString())
                field.setLength(0)
                rows.add(row.toList())
                row.clear()
            }
        }
        while (index < body.length) {
            val char = body[index]
            when {
                quoted && char == '"' && body.getOrNull(index + 1) == '"' -> {
                    field.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                !quoted && char == ',' -> {
                    row.add(field.toString())
                    field.setLength(0)
                }
                !quoted && (char == '\n' || char == '\r') -> {
                    endRow()
                    if (char == '\r' && body.getOrNull(index + 1) == '\n') index++
                }
                else -> field.append(char)
            }
            index++
        }
        endRow()
        return rows
    }
}
