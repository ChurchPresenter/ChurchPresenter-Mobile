package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.ApiException
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Copying Bible modules off the desktop: what is asked for, and how failures come back. */
class BibleDownloadServiceTest {

    private val paths = mutableListOf<String>()
    private val keys = mutableListOf<String?>()

    private fun service(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        settings: AppSettings = AppSettings(InMemorySettingsStorage()),
    ): BibleDownloadService {
        val client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            keys += request.headers[ApiConstants.API_KEY_HEADER]
            respond(body, status)
        })
        return BibleDownloadService(settings, client)
    }

    @Test
    fun theManifestListsWhatTheDesktopOffers() = runTest {
        val modules = service("""["en_KJV.spb","ru_RST77.spb"]""").listTranslations().getOrThrow()

        assertEquals(listOf("en_KJV.spb", "ru_RST77.spb"), modules)
        assertTrue(paths.single().endsWith("/api/bible/file/translations"))
    }

    @Test
    fun aDesktopWithNoBibleLoadedOffersAnEmptyList() {
        // Not a failure — the operator simply has not opened a translation yet, and the UI has
        // to say that rather than showing a network error.
        runTest {
            assertEquals(emptyList<String>(), service("[]").listTranslations().getOrThrow())
        }
    }

    @Test
    fun aTranslationIsFetchedByItsPositionInThatManifest() = runTest {
        val text = service("##Title: KJV\n1 Genesis 1\n-----\nB001C001V001 1 1 1 x")
            .downloadTranslation(1).getOrThrow()

        assertTrue(text.startsWith("##Title:"))
        assertTrue(paths.single().endsWith("/api/bible/file/translation/1"))
    }

    @Test
    fun theApiKeyTravelsOnBothCalls() = runTest {
        // Every /api/bible/file route is behind checkApiKey on the desktop, so a keyed server
        // rejects both of these without it.
        val settings = AppSettings(InMemorySettingsStorage()).apply { apiKey = "secret" }
        val downloads = service("[]", settings = settings)

        downloads.listTranslations()
        downloads.downloadTranslation(0)

        assertEquals(listOf<String?>("secret", "secret"), keys)
    }

    @Test
    fun aRejectedKeyComesBackAsTheServersAnswerNotAnEmptyList() = runTest {
        // The sync sheet reveals its API key field on exactly this, so it must be distinguishable
        // from "the desktop has no Bibles".
        val failure = service("no", HttpStatusCode.Unauthorized).listTranslations().exceptionOrNull()

        assertEquals(401, assertIs<ApiException>(failure).httpStatus)
    }

    @Test
    fun aMissingTranslationIsAFailureRatherThanAnEmptyModule() = runTest {
        val failure = service("Bible translation not found", HttpStatusCode.NotFound)
            .downloadTranslation(9).exceptionOrNull()

        assertEquals(404, assertIs<ApiException>(failure).httpStatus)
    }

    @Test
    fun aManifestThatIsNotJsonFailsCleanly() = runTest {
        // A captive portal answers 200 with a login page; decoding must fail rather than
        // producing a module list out of HTML.
        assertTrue(service("<html>Sign in</html>").listTranslations().isFailure)
    }
}
