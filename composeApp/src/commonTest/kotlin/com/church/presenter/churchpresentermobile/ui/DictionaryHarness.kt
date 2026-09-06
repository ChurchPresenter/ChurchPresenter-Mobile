package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.BibleBooksResponse
import com.church.presenter.churchpresentermobile.model.BibleChapterInfo
import com.church.presenter.churchpresentermobile.model.BibleChapterResponse
import com.church.presenter.churchpresentermobile.model.BibleVerse
import com.church.presenter.churchpresentermobile.model.DictionaryVerse
import com.church.presenter.churchpresentermobile.model.DictionaryVersesResponse
import com.church.presenter.churchpresentermobile.model.StrongsEntry
import com.church.presenter.churchpresentermobile.network.BibleService
import com.church.presenter.churchpresentermobile.network.DictionaryService
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.viewmodel.DictionaryViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Setup for the Strong's dictionary screen.
 *
 * Every path on this screen is a request — the dictionary lives on the desktop
 * and there is no demo or offline catalogue — so the screen is driven through a
 * real [DictionaryViewModel] whose two services answer from [FakeDesktop]
 * instead of a computer. Recording what each request asked for is the point:
 * the reference filter is only correct if the book, chapter and verse the
 * operator picked actually reach the server.
 */
private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

internal fun strongs(
    number: String,
    word: String = "word",
    transliteration: String = "",
    pronunciation: String = "",
    definition: String = "",
    kjvUsage: String = "",
    occurrences: Int = 0,
) = StrongsEntry(
    number = number,
    word = word,
    transliteration = transliteration,
    pronunciation = pronunciation,
    definition = definition,
    kjvUsage = kjvUsage,
    occurrences = occurrences,
)

/** A Hebrew entry with everything the sheet can show filled in. */
internal val bara = strongs(
    number = "H1254",
    word = "bara",
    transliteration = "baw-raw",
    pronunciation = "baw-raw'",
    definition = "to shape, create; compare H1262 and G26",
    kjvUsage = "create, creator, choose, make",
    occurrences = 54,
)

/** A Greek entry — the other half of the language split. */
internal val agape = strongs(
    number = "G26",
    word = "agape",
    transliteration = "ag-ah-pay",
    pronunciation = "ag-ah'-pay",
    definition = "love, affection, benevolence",
    kjvUsage = "love, charity, feast of charity",
    occurrences = 116,
)

internal fun refBook(
    name: String,
    chapters: Int,
    id: Int? = null,
    verseTotals: Map<Int, Int> = emptyMap(),
) =
    BibleBook(
        name = name,
        bookId = id,
        chapterTotal = chapters,
        chapters = verseTotals.map { (c, v) -> BibleChapterInfo(chapter = c, verseTotal = v) }
            .takeIf { it.isNotEmpty() },
    )

internal fun appearsIn(vararg refs: Pair<String, String>, total: Int? = null) =
    DictionaryVersesResponse(
        number = "H1254",
        total = total ?: refs.size,
        verses = refs.map { (reference, text) ->
            DictionaryVerse(
                bookName = reference.substringBeforeLast(' '),
                chapter = 1,
                verse = 1,
                reference = reference,
                text = text,
            )
        },
    )

/**
 * The desktop the dictionary screen talks to.
 *
 * Holds what the two endpoints answer with and records what was asked, so a
 * test can assert on the request the screen produced as well as on the screen
 * the response produced.
 */
internal class FakeDesktop(
    var entries: List<StrongsEntry> = listOf(bara, agape),
    var searchStatus: HttpStatusCode = HttpStatusCode.OK,
    var lookupEntry: StrongsEntry? = null,
    var lookupStatus: HttpStatusCode = HttpStatusCode.OK,
    var verses: DictionaryVersesResponse? = null,
    var books: List<BibleBook> = emptyList(),
    var chapterVerseCount: Int = 0,
) {
    val sender = FakeWsSender()

    /** The query parameters of every dictionary search the screen has made. */
    val searches = mutableListOf<Map<String, String>>()

    /** The query parameters of every "appears in" request. */
    val verseRequests = mutableListOf<Map<String, String>>()

    /** Every Strong's number looked up by a tapped definition link. */
    val lookups = mutableListOf<String>()

    val lastSearch: Map<String, String> get() = searches.last()

    private fun paramsOf(url: Url): Map<String, String> =
        url.parameters.names().associateWith { url.parameters[it].orEmpty() }

    private fun dictionaryClient() = HttpClient(MockEngine { request ->
        val path = request.url.encodedPath
        val params = paramsOf(request.url)
        when {
            path.endsWith("/verses") -> {
                verseRequests += params
                val body = verses
                if (body == null) respond("not found", HttpStatusCode.NotFound)
                else respond(json.encodeToString(DictionaryVersesResponse.serializer(), body))
            }
            path.endsWith("/dictionary") -> {
                searches += params
                if (searchStatus != HttpStatusCode.OK) respond("boom", searchStatus)
                else respond(json.encodeToString(entriesSerializer, entries))
            }
            else -> {
                lookups += path.substringAfterLast('/')
                val found = lookupEntry
                if (lookupStatus != HttpStatusCode.OK || found == null) {
                    respond("no such entry", HttpStatusCode.NotFound)
                } else {
                    respond(json.encodeToString(StrongsEntry.serializer(), found))
                }
            }
        }
    })

    private fun bibleClient() = HttpClient(MockEngine { request ->
        // getBooks and getChapter share /api/bible; only the chapter request
        // carries a chapter parameter.
        if (request.url.parameters["chapter"] == null) {
            respond(json.encodeToString(BibleBooksResponse.serializer(), BibleBooksResponse(books = books)))
        } else {
            val chapter = BibleChapterResponse(
                verses = (1..chapterVerseCount).map { BibleVerse(verse = it, text = "verse $it") },
            )
            respond(json.encodeToString(BibleChapterResponse.serializer(), chapter))
        }
    })

    fun viewModel(): DictionaryViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return DictionaryViewModel(
            appSettings = settings,
            eventService = sender,
            serviceFactory = { DictionaryService(it, sender, dictionaryClient()) },
            bibleServiceFactory = { BibleService(it, sender, bibleClient()) },
        )
    }

    private companion object {
        val entriesSerializer =
            ListSerializer(StrongsEntry.serializer())
    }
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showDictionary(
    viewModel: DictionaryViewModel,
    settingsSaveToken: Int = 0,
) = showScreen {
    DictionaryScreen(viewModel = viewModel, settingsSaveToken = settingsSaveToken)
}

/** Renders the entry sheet's body on its own — no sheet, no ViewModel. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showEntryDetail(
    entry: StrongsEntry = bara,
    scheduleAdded: Boolean = false,
    appearsIn: DictionaryVersesResponse? = null,
    appearsInLoading: Boolean = false,
    onProject: () -> Unit = {},
    onAddToSchedule: () -> Unit = {},
    onOpenNumber: (String) -> Unit = {},
) = showScreen {
    EntryDetail(
        entry = entry,
        scheduleAdded = scheduleAdded,
        appearsIn = appearsIn,
        appearsInLoading = appearsInLoading,
        onProject = onProject,
        onAddToSchedule = onAddToSchedule,
        onOpenNumber = onOpenNumber,
    )
}
