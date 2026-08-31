package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Choosing which downloaded translation is read and presented. */
class BibleChoiceViewModelTest {

    private fun module(title: String, book: String) = """
        ##Title: $title
        1 $book 50
        -----
        B001C001V001 1 1 1 In the beginning.
    """.trimIndent()

    private fun repository(): LocalBibleRepository =
        LocalBibleRepository(InMemoryFileStorage(), now = { 0L })

    @Test
    fun theFirstTranslationCopiedIsTheOnePresented() = runVmTestUnconfined {
        val repository = repository()
        repository.install("en_KJV.spb", module("King James Version", "Genesis"))

        val viewModel = BibleChoiceViewModel(repository)

        assertEquals("en_KJV", viewModel.activeId.value)
        assertEquals("King James Version", viewModel.active.value?.title)
    }

    @Test
    fun theSecondTranslationCanBeTheOnePresented() = runVmTestUnconfined {
        // Reported: two downloaded, only the first one readable anywhere else.
        val repository = repository()
        repository.install("en_KJV.spb", module("King James Version", "Genesis"))
        repository.install("ru_RST77.spb", module("Synodal", "Бытие"))
        val viewModel = BibleChoiceViewModel(repository)

        viewModel.setActive("ru_RST77")

        assertEquals("ru_RST77", viewModel.activeId.value)
        assertEquals("Synodal", viewModel.active.value?.title)
        assertEquals("ru_RST77", repository.index.value.active?.id)
    }

    @Test
    fun bothTranslationsAreOfferedToChooseFrom() = runVmTestUnconfined {
        val repository = repository()
        repository.install("en_KJV.spb", module("King James Version", "Genesis"))
        repository.install("ru_RST77.spb", module("Synodal", "Бытие"))

        val viewModel = BibleChoiceViewModel(repository)

        assertEquals(listOf("en_KJV", "ru_RST77"), viewModel.installed.value.map { it.id })
    }

    @Test
    fun deletingTheChosenTranslationNamesWhateverIsLeft() = runVmTestUnconfined {
        val repository = repository()
        repository.install("en_KJV.spb", module("King James Version", "Genesis"))
        repository.install("ru_RST77.spb", module("Synodal", "Бытие"))
        val viewModel = BibleChoiceViewModel(repository)
        viewModel.setActive("ru_RST77")

        repository.remove("ru_RST77")

        assertEquals("en_KJV", viewModel.activeId.value)
    }

    @Test
    fun anEmptyLibraryHasNothingToPresent() = runVmTestUnconfined {
        val viewModel = BibleChoiceViewModel(repository())

        assertEquals("", viewModel.activeId.value)
        assertNull(viewModel.active.value)
    }
}
