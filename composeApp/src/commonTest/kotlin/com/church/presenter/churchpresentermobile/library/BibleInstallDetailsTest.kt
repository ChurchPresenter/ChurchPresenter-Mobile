package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.InstalledBible
import com.church.presenter.churchpresentermobile.model.InstalledBibleBook
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlin.test.Test
import kotlin.test.assertEquals

/** What the translation list says about an installed Bible beyond its title. */
class BibleInstallDetailsTest {

    private val module = """
        ##Title:	Berean Standard Bible
        ##Abbreviation:	BSB
        40	Matthew	28
        -----
        B040C001V001	40	1	1	This is the record of the genealogy of Jesus Christ.
    """.trimIndent()

    @Test
    fun `a download's details are stored with it`() {
        val repository = LocalBibleRepository(InMemoryFileStorage())

        val installed = repository.install(
            "ENG_BSB.spb",
            module,
            details = InstallDetails(
                languageName = "English",
                origin = "eBible.org",
                license = "Public domain",
                catalogKey = "EBIBLE:engbsb",
            ),
        )!!

        assertEquals("BSB", installed.abbreviation, "read from the module's own header")
        assertEquals("English", installed.languageName)
        assertEquals("eBible.org", installed.origin)
        assertEquals("Public domain", installed.license)
        assertEquals("EBIBLE:engbsb", installed.catalogKey)
    }

    @Test
    fun `a translation synced from a desktop has no download details`() {
        val installed = LocalBibleRepository(InMemoryFileStorage()).install("en_BSB.spb", module)!!

        assertEquals("", installed.languageName)
        assertEquals("", installed.origin)
        assertEquals("BSB", installed.abbreviation)
    }

    @Test
    fun `the abbreviation header is read and is blank when absent`() {
        assertEquals("BSB", SpbParser.parse(module).abbreviation)
        assertEquals("", SpbParser.parse(module.replace("##Abbreviation:\tBSB\n", "")).abbreviation)
    }

    private fun withBooks(vararg ids: Int) = InstalledBible(
        id = "x", fileName = "x.spb", title = "x", verseCount = 1, sizeBytes = 1,
        books = ids.map { InstalledBibleBook(bookId = it, name = "Book $it", chapterCount = 1) },
    )

    @Test
    fun `coverage is read off the books a module carries`() {
        assertEquals("OT-NT", withBooks(1, 40).coverage)
        assertEquals("NT", withBooks(40, 66).coverage)
        assertEquals("OT", withBooks(1, 39).coverage)
        assertEquals("", withBooks().coverage)
    }
}
