package com.church.presenter.churchpresentermobile.bibleimport.catalog

import com.church.presenter.churchpresentermobile.bibleimport.BibleXmlReader
import com.church.presenter.churchpresentermobile.bibleimport.ConvertProgress
import com.church.presenter.churchpresentermobile.bibleimport.CorruptArchiveException
import com.church.presenter.churchpresentermobile.bibleimport.ImportHints
import com.church.presenter.churchpresentermobile.bibleimport.NotABibleException
import com.church.presenter.churchpresentermobile.bibleimport.SourceBible
import com.church.presenter.churchpresentermobile.bibleimport.SpbWriter
import com.church.presenter.churchpresentermobile.bibleimport.UnsupportedEncodingException
import com.church.presenter.churchpresentermobile.bibleimport.UsfxReader
import com.church.presenter.churchpresentermobile.bibleimport.XmlFormatException
import com.church.presenter.churchpresentermobile.bibleimport.XmlText
import com.church.presenter.churchpresentermobile.bibleimport.ZipEntry
import com.church.presenter.churchpresentermobile.bibleimport.ZipReader
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.SHA1
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.CancellationException

private const val TAG = "BibleInstaller"

/** The three steps design 3a lists, in order. */
enum class InstallStep { DOWNLOAD, CONVERT, INSTALL }

/** Why an install stopped — each is worded differently on screen, and each wants a different fix. */
enum class InstallFailure {
    /** No connection, or it dropped: check the network and retry. */
    NETWORK,

    /** The archive answered with an error status. */
    HTTP,

    /** The bytes did not match the size or hash the catalogue listed — a truncated or altered file. */
    CHECKSUM,

    /** Not an archive, or nothing usable inside it. */
    CORRUPT,

    /** Readable, but not one of the three Bible dialects. */
    NOT_A_BIBLE,

    /** Declared in a text encoding the app has no table for. */
    ENCODING,

    /** It converted, but produced no scripture. */
    NO_VERSES,

    /** The device would not store it. */
    STORAGE,
}

class InstallException(
    val failure: InstallFailure,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** Ends an install with [failure] — the one way any step of it gives up. */
internal fun installFailed(failure: InstallFailure, message: String, cause: Throwable? = null): Nothing =
    throw InstallException(failure, message, cause)

/** Everything the install screen draws while a translation is on its way. */
data class InstallProgress(
    val step: InstallStep = InstallStep.DOWNLOAD,
    val downloadedBytes: Long = 0,
    val totalBytes: Long? = null,
    /** The file as the archive names it — `engbsb_usfx.zip`. */
    val downloadName: String = "",
    /** How long the download took, once it has finished. */
    val downloadMs: Long? = null,
    val convert: ConvertProgress? = null,
)

/** A converted module, ready to be stored. */
internal class ConvertedModule(val bible: SourceBible, val spbText: String, val license: String)

/**
 * Downloads one catalogue row and converts it to `.spb` — everything except storing it.
 *
 * Kept free of storage and of coroutine scopes so the whole pipeline is tested through a fake
 * [WebFetcher]: a zip that does not unzip, a hash that does not match and a file that is not a
 * Bible each come back as the [InstallFailure] the screen explains.
 */
internal class BibleInstaller(
    private val fetcher: WebFetcher,
    private val clock: () -> Long,
) {
    suspend fun download(bible: CatalogBible, onProgress: (InstallProgress) -> Unit): Pair<ByteArray, InstallProgress> {
        val url = downloadUrl(bible)
        val name = url.substringAfterLast('/').replace("%20", " ")
        val started = clock()
        var progress = InstallProgress(downloadName = name, totalBytes = bible.sizeBytes.takeIf { it > 0 })
        onProgress(progress)
        val response = try {
            fetcher.get(url, emptyMap()) { received, total ->
                progress = progress.copy(downloadedBytes = received, totalBytes = total ?: progress.totalBytes)
                onProgress(progress)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // Each engine reports a dropped connection as its own exception type.
            installFailed(InstallFailure.NETWORK, e.message ?: "download failed", e)
        }
        if (!response.isSuccess) installFailed(InstallFailure.HTTP, "HTTP ${response.status}")
        val body = response.body
        if (bible.sizeBytes > 0 && body.size.toLong() != bible.sizeBytes) {
            installFailed(InstallFailure.CHECKSUM, "expected ${bible.sizeBytes} bytes, got ${body.size}")
        }
        if (bible.checksum.isNotBlank() && gitBlobSha1(body) != bible.checksum.lowercase()) {
            installFailed(InstallFailure.CHECKSUM, "hash does not match the catalogue")
        }
        progress = progress.copy(
            step = InstallStep.CONVERT,
            downloadedBytes = body.size.toLong(),
            totalBytes = body.size.toLong(),
            downloadMs = clock() - started,
        )
        onProgress(progress)
        return body to progress
    }

    /** Unzips when the source zips, reads the Bible, and writes the module. CPU-bound. */
    fun convert(bible: CatalogBible, body: ByteArray, onProgress: (ConvertProgress) -> Unit): ConvertedModule {
        val (xml, bookNames) = extract(bible.source, body)
        val hints = when (bible.source) {
            BibleSource.EBIBLE -> ImportHints(
                language = bible.language,
                name = bible.displayName,
                rights = bible.copyright,
                source = "https://ebible.org/details.php?id=${bible.identifier}",
                identifier = bible.identifier,
                bookNames = bookNames,
            )
            // The file's own `biblename` beats the archive's shouting file name, so no name here.
            BibleSource.ZEFANIA -> ImportHints(language = bible.language, path = bible.downloadKey)
            BibleSource.BEBLIA -> ImportHints(
                language = bible.language,
                name = bible.displayName,
                rights = bible.copyright,
                identifier = bible.identifier,
            )
        }
        val parsed = readBible(xml, hints, onProgress)
        return ConvertedModule(parsed, SpbWriter.write(parsed), license = bible.copyright.ifBlank { parsed.rights })
    }

    private fun extract(source: BibleSource, body: ByteArray): Pair<ByteArray, Map<String, String>> {
        if (source == BibleSource.BEBLIA) return body to emptyMap()
        val zip = try {
            ZipReader(body)
        } catch (e: CorruptArchiveException) {
            installFailed(InstallFailure.CORRUPT, e.message ?: "not a zip", e)
        }
        val xmlEntries = zip.entries.filter { it.baseName.endsWith(".xml", ignoreCase = true) }
        val bibleEntry: ZipEntry? = when (source) {
            BibleSource.EBIBLE -> xmlEntries.firstOrNull { it.baseName.endsWith("_usfx.xml", ignoreCase = true) }
            else -> xmlEntries.maxByOrNull { it.size }
        }
        bibleEntry ?: installFailed(InstallFailure.CORRUPT, "no Bible inside the archive")
        val xml = try {
            zip.read(bibleEntry)
        } catch (e: CorruptArchiveException) {
            installFailed(InstallFailure.CORRUPT, e.message ?: "archive is damaged", e)
        }
        val names = if (source == BibleSource.EBIBLE) bookNames(zip, xmlEntries) else emptyMap()
        return xml to names
    }

    /**
     * eBible's `BookNames.xml`, or nothing. A damaged one costs the translation its own book names
     * — the app's tables and then English stand in — which is no reason to refuse the Bible.
     */
    private fun bookNames(zip: ZipReader, entries: List<ZipEntry>): Map<String, String> {
        val entry = entries.firstOrNull { it.baseName.equals("BookNames.xml", ignoreCase = true) } ?: return emptyMap()
        return try {
            UsfxReader.parseBookNames(XmlText.decode(zip.read(entry)))
        } catch (e: CorruptArchiveException) {
            Logger.e(TAG, "BookNames.xml is damaged: ${e.message}")
            emptyMap()
        } catch (e: XmlFormatException) {
            Logger.e(TAG, "BookNames.xml is not well-formed: ${e.message}")
            emptyMap()
        }
    }

    private companion object {
        fun downloadUrl(bible: CatalogBible): String = when (bible.source) {
            BibleSource.EBIBLE -> EBibleCatalog.downloadUrl(bible.downloadKey)
            BibleSource.ZEFANIA -> ZefaniaCatalog.downloadUrl(bible.downloadKey)
            BibleSource.BEBLIA -> BebliaCatalog.downloadUrl(bible.downloadKey)
        }
    }
}

/**
 * Reads XML bytes as a Bible, turning each way that can fail into the [InstallFailure] that
 * explains it. Shared by the downloader and the convert-a-file screen.
 */
internal fun readBible(xml: ByteArray, hints: ImportHints, onProgress: (ConvertProgress) -> Unit = {}): SourceBible {
    val parsed = try {
        BibleXmlReader.read(XmlText.decode(xml), hints, onProgress)
    } catch (e: UnsupportedEncodingException) {
        installFailed(InstallFailure.ENCODING, "the file is encoded as ${e.encoding}", e)
    } catch (e: NotABibleException) {
        installFailed(InstallFailure.NOT_A_BIBLE, e.message ?: "not a Bible", e)
    } catch (e: XmlFormatException) {
        installFailed(InstallFailure.NOT_A_BIBLE, e.message ?: "not well-formed XML", e)
    }
    if (!parsed.hasVerses) installFailed(InstallFailure.NO_VERSES, "no verses were found")
    return parsed
}

/**
 * The SHA-1 git gives a blob: of `"blob <size>\0"` followed by the content.
 *
 * Both GitHub-hosted archives list it for every file, which is what lets a download be checked end
 * to end without either publishing a checksum of its own.
 *
 * SHA-1 is "delicate" as a security primitive; here it only has to match what git computed, and
 * it guards against a truncated or substituted download, not an adversary.
 */
@OptIn(DelicateCryptographyApi::class)
internal suspend fun gitBlobSha1(content: ByteArray): String {
    val header = "blob ${content.size}\u0000".encodeToByteArray()
    val digest = CryptographyProvider.Default.get(SHA1).hasher().hash(header + content)
    return digest.joinToString("") { byte -> (byte.toInt() and BYTE_MASK).toString(HEX_RADIX).padStart(2, '0') }
}

private const val BYTE_MASK = 0xFF
private const val HEX_RADIX = 16
