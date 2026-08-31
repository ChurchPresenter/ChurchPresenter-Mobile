package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.network.BibleDownloadService
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Picking translations, and telling a wrong address apart from a missing key. */
class BibleSyncViewModelTest {

    private val module = """
        ##Title: King James Version
        1 Genesis 50
        -----
        B001C001V001 1 1 1 In the beginning.
    """.trimIndent()

    private fun vm(
        manifest: String = """["en_KJV.spb","ru_RST77.spb"]""",
        manifestStatus: HttpStatusCode = HttpStatusCode.OK,
        storage: InMemoryFileStorage = InMemoryFileStorage(),
    ): Pair<BibleSyncViewModel, LocalBibleRepository> {
        val settings = AppSettings(InMemorySettingsStorage())
        val repository = LocalBibleRepository(storage, now = { 0L })
        val client = HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith(ApiConstants.BIBLE_TRANSLATIONS_ENDPOINT)) {
                respond(manifest, manifestStatus)
            } else {
                respond(module, HttpStatusCode.OK)
            }
        })
        return BibleSyncViewModel(repository, settings, BibleDownloadService(settings, client)) to repository
    }

    @Test
    fun theDesktopsModulesBecomeThePickList() = runVmTestUnconfined {
        val (viewModel, _) = vm()

        viewModel.loadChoices()

        val choices = viewModel.choices.first { it.isNotEmpty() }
        assertEquals(listOf("en KJV", "ru RST77"), choices.map { it.displayName })
        assertTrue(choices.none { it.isInstalled })
    }

    @Test
    fun thereIsNoCapOnHowManyMayBePicked() = runVmTestUnconfined {
        // A bilingual congregation wanting three is not the app's business to prevent.
        val (viewModel, _) = vm(manifest = """["a.spb","b.spb","c.spb"]""")
        viewModel.loadChoices()
        viewModel.choices.first { it.isNotEmpty() }

        viewModel.toggle("a.spb")
        viewModel.toggle("b.spb")
        viewModel.toggle("c.spb")

        assertEquals(3, viewModel.selection.value.size)
    }

    @Test
    fun tappingAPickedTranslationAgainUnpicksIt() = runVmTestUnconfined {
        val (viewModel, _) = vm()

        viewModel.toggle("en_KJV.spb")
        viewModel.toggle("en_KJV.spb")

        assertTrue(viewModel.selection.value.isEmpty())
    }

    @Test
    fun aRejectedKeyIsToldApartFromAnUnreachableComputer() = runVmTestUnconfined {
        // Only one of those two is fixed by typing a key, which is why the field appears here
        // and not on every failure.
        val (viewModel, _) = vm(manifest = "no", manifestStatus = HttpStatusCode.Unauthorized)

        viewModel.loadChoices()
        viewModel.loadError.first { it != null }

        assertTrue(viewModel.needsApiKey.value)
    }

    @Test
    fun anOrdinaryFailureDoesNotAskForAKey() = runVmTestUnconfined {
        val (viewModel, _) = vm(manifest = "nope", manifestStatus = HttpStatusCode.ServiceUnavailable)

        viewModel.loadChoices()
        viewModel.loadError.first { it != null }

        assertFalse(viewModel.needsApiKey.value)
    }

    @Test
    fun copyingATranslationLeavesItTickedAsInstalled() = runVmTestUnconfined {
        val (viewModel, repository) = vm()
        viewModel.loadChoices()
        viewModel.choices.first { it.isNotEmpty() }
        viewModel.toggle("en_KJV.spb")

        viewModel.sync()
        viewModel.outcome.first { it != null }

        assertEquals(1, repository.index.value.bibles.size)
        assertTrue(viewModel.choices.value.first { it.fileName == "en_KJV.spb" }.isInstalled)
        // The picks are spent — leaving them ticked invites copying the same thing twice.
        assertTrue(viewModel.selection.value.isEmpty())
    }

    @Test
    fun removingATranslationClearsItsTick() = runVmTestUnconfined {
        val (viewModel, repository) = vm()
        viewModel.loadChoices()
        viewModel.choices.first { it.isNotEmpty() }
        viewModel.toggle("en_KJV.spb")
        viewModel.sync()
        viewModel.outcome.first { it != null }

        viewModel.remove("en_KJV")

        assertTrue(repository.index.value.isEmpty)
        assertFalse(viewModel.choices.value.first { it.fileName == "en_KJV.spb" }.isInstalled)
    }

    @Test
    fun theSecondTranslationCanBeTheOnePresented() = runVmTestUnconfined {
        // Reported: two downloaded, only the first readable anywhere else.
        val (viewModel, repository) = vm()
        viewModel.loadChoices()
        viewModel.choices.first { it.isNotEmpty() }
        viewModel.toggle("en_KJV.spb")
        viewModel.toggle("ru_RST77.spb")
        viewModel.sync()
        viewModel.outcome.first { it != null }
        assertEquals("en_KJV", viewModel.activeId.value)

        viewModel.setActive("ru_RST77")

        assertEquals("ru_RST77", viewModel.activeId.value)
        assertEquals("ru_RST77", repository.index.value.active?.id)
    }
}
