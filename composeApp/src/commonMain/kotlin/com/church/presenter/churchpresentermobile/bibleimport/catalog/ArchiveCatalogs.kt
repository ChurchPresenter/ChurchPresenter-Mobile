package com.church.presenter.churchpresentermobile.bibleimport.catalog

import com.church.presenter.churchpresentermobile.bibleimport.BibleNaming
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val catalogJson = Json { ignoreUnknownKeys = true }

/** [body] decoded, or null when it is not the JSON it should be — an error page, a truncated file. */
private fun <T> decodeOrNull(serializer: KSerializer<T>, body: String): T? = try {
    catalogJson.decodeFromString(serializer, body)
} catch (_: SerializationException) {
    null
} catch (_: IllegalArgumentException) {
    null
}

/** Percent-encodes one path segment; the Zefania archive's paths are full of spaces and brackets. */
internal fun encodePathSegment(segment: String): String = buildString {
    for (byte in segment.encodeToByteArray()) {
        val char = (byte.toInt() and 0xFF).toChar()
        if ((char.isLetterOrDigit() && char.code < ASCII_LIMIT) || char in "-._~") {
            append(char)
        } else {
            append('%')
            append(HEX[(byte.toInt() shr NIBBLE_BITS) and NIBBLE_MASK])
            append(HEX[byte.toInt() and NIBBLE_MASK])
        }
    }
}

private const val HEX = "0123456789ABCDEF"
private const val ASCII_LIMIT = 0x80
private const val NIBBLE_BITS = 4
private const val NIBBLE_MASK = 0x0F

/**
 * The Zefania XML preservation archive, listed through GitHub's git-tree API.
 *
 * A port of the desktop's `ZefaniaRepositoryIndex`. Language comes from the directory, not the
 * file name — the directory is how the archive is curated, and a handful of modules disagree with
 * their own names. The tree's blob SHA-1 and size come free with the listing, so every download is
 * verified end to end without the archive publishing checksums of its own.
 */
internal object ZefaniaCatalog {

    private const val OWNER_REPO = "ChurchPresenter/Zefania-XML-Preservation"
    private const val BRANCH = "main"
    private const val BIBLES_PREFIX = "zefania-sharp-sourceforge-backup/Bibles/"
    private const val GROUP_IDENTIFIER = 3
    private const val GROUP_DISPLAY_NAME = 4

    const val CATALOG_URL = "https://api.github.com/repos/$OWNER_REPO/git/trees/$BRANCH?recursive=1"

    fun downloadUrl(path: String): String =
        "https://raw.githubusercontent.com/$OWNER_REPO/$BRANCH/" + path.split("/").joinToString(
            "/",
            transform = ::encodePathSegment,
        )

    /** `SF_2009-01-20_ENG_ACV_(A CONSERVATIVE VERSION).zip` and its many near-misses. */
    private val NAME_REGEX = Regex(
        """^SF_(\d{4}[-_]\d{2}[-_]\d{2})_([A-Za-z]{2,8})_(.+?)(?:_\((.*)\))?\.zip$""",
        RegexOption.IGNORE_CASE,
    )

    @Serializable
    internal data class TreeEntry(
        val path: String = "",
        val type: String = "",
        val sha: String = "",
        val size: Long = 0,
    )

    @Serializable
    internal data class TreeResponse(val tree: List<TreeEntry> = emptyList(), val truncated: Boolean = false)

    /**
     * The archive's Bibles, or null when the body is not a complete listing — a truncated tree
     * rendered as if complete would silently hide translations.
     */
    fun parse(body: String, languageNames: Map<String, LanguageNaming>): List<CatalogBible>? {
        val response = decodeOrNull(TreeResponse.serializer(), body)?.takeUnless { it.truncated } ?: return null
        val taken = mutableSetOf<String>()
        return response.tree
            .filter { it.type == "blob" && it.path.startsWith(BIBLES_PREFIX) }
            .filter { it.path.endsWith(".zip", ignoreCase = true) }
            .sortedBy { it.path }
            .mapNotNull { entry -> entry(entry, languageNames) }
            .map { module ->
                val stem = BibleNaming.deduplicate(module.fileStem, taken)
                taken.add(stem)
                module.copy(fileStem = stem)
            }
    }

    private fun entry(entry: TreeEntry, languageNames: Map<String, LanguageNaming>): CatalogBible? {
        val relative = entry.path.removePrefix(BIBLES_PREFIX)
        val language = relative.substringBefore('/', "").trim().uppercase()
        if (language.isEmpty() || !relative.contains('/')) return null
        val fileName = entry.path.substringAfterLast('/')
        val match = NAME_REGEX.matchEntire(fileName)
        val identifier: String
        val displayName: String
        if (match != null) {
            identifier = match.groupValues[GROUP_IDENTIFIER]
            displayName = match.groupValues[GROUP_DISPLAY_NAME].ifBlank { identifier }.replace('_', ' ').trim()
        } else {
            // Never drop a module for not following the convention.
            identifier = fileName.removeSuffix(".zip").substringAfterLast('_')
            displayName = fileName.removeSuffix(".zip").replace('_', ' ').trim()
        }
        return CatalogBible(
            source = BibleSource.ZEFANIA,
            downloadKey = entry.path,
            checksum = entry.sha,
            sizeBytes = entry.size,
            language = language,
            languageName = languageNames[language]?.english.orEmpty(),
            languageNativeName = languageNames[language]?.native.orEmpty(),
            identifier = identifier,
            displayName = titleCase(displayName.ifBlank { fileName.removeSuffix(".zip") }),
            fileStem = BibleNaming.fileStem(language, identifier),
        )
    }

    /**
     * The archive's file names shout ("AMERICAN KING JAMES VERSION"). A row reads better in title
     * case; a name that is already mixed case is left alone, and short all-caps words stay as they
     * are because they are usually abbreviations (KJV, NT).
     */
    internal fun titleCase(name: String): String {
        if (name != name.uppercase()) return name
        return name.split(' ').joinToString(" ") { word ->
            if (word.length <= ABBREVIATION_MAX) word else word.first() + word.drop(1).lowercase()
        }
    }

    private const val ABBREVIATION_MAX = 3
}

/**
 * The Holy Bible XML (Beblia) mirror's `catalog.json`: one entry per file, with its git blob hash,
 * pinned to the commit the manifest was generated from. A port of the desktop's
 * `BebliaCatalogIndex`.
 */
internal object BebliaCatalog {

    private const val OWNER_REPO = "ChurchPresenter/Holy-Bible-XML-Format"

    const val CATALOG_URL = "https://raw.githubusercontent.com/$OWNER_REPO/master/catalog.json"

    /** `downloadKey` is `<commit>/<file>`, so a row resolves on its own. */
    fun downloadUrl(downloadKey: String): String =
        "https://raw.githubusercontent.com/$OWNER_REPO/" +
            downloadKey.split("/").joinToString("/", transform = ::encodePathSegment)

    @Serializable
    internal data class Entry(
        val file: String = "",
        val sha: String = "",
        val size: Long = 0,
        val title: String = "",
        val id: String = "",
        val lang: String = "",
        val langName: String = "",
        val rights: String = "",
        val ot: Int = 0,
        val nt: Int = 0,
    )

    @Serializable
    internal data class CatalogFile(val commit: String = "", val bibles: List<Entry> = emptyList())

    /**
     * The mirror's Bibles, or null when the manifest is unusable: one that lists nothing is a
     * truncated file rather than an empty archive, and one naming no commit leaves the downloads
     * nothing to pin to.
     */
    fun parse(body: String, languageNames: Map<String, LanguageNaming>): List<CatalogBible>? {
        val catalog = decodeOrNull(CatalogFile.serializer(), body) ?: return null
        val entries = catalog.bibles.filter { it.file.isNotBlank() }
        if (entries.isEmpty() || catalog.commit.isBlank()) return null
        val taken = mutableSetOf<String>()
        return entries.sortedBy { it.file }.map { entry ->
            val language = entry.lang.trim().uppercase().ifBlank { BibleNaming.UNKNOWN_LANGUAGE }
            val stem = BibleNaming.deduplicate(BibleNaming.fileStem(language, entry.id), taken)
            taken.add(stem)
            CatalogBible(
                source = BibleSource.BEBLIA,
                downloadKey = "${catalog.commit}/${entry.file}",
                checksum = entry.sha,
                sizeBytes = entry.size,
                language = language,
                // The shared table wins where it has the code, so all three sources label a
                // language alike; the manifest's own name fills the rest.
                languageName = languageNames[language]?.english?.ifBlank { null } ?: entry.langName.trim(),
                languageNativeName = languageNames[language]?.native.orEmpty(),
                identifier = entry.id,
                displayName = entry.title.ifBlank { entry.file.removeSuffix(".xml") },
                copyright = entry.rights,
                otBookCount = entry.ot,
                ntBookCount = entry.nt,
                fileStem = stem,
            )
        }
    }
}
