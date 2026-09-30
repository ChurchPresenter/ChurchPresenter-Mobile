package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.bibleimport.BibleXmlFormat
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallFailure
import com.church.presenter.churchpresentermobile.library.InstallDetails
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Converting a Bible file from the device — design 4 — and the copyright step in front of it. */
@OptIn(ExperimentalCoroutinesApi::class)
class ConvertBibleViewModelTest {

    private val zefania = """
        <?xml version="1.0" encoding="utf-8"?>
        <XMLBIBLE biblename="Біблія (Огієнко)">
          <INFORMATION><language>UKR</language><identifier>OGI</identifier><rights>Public domain</rights></INFORMATION>
          <BIBLEBOOK bnumber="43"><CHAPTER cnumber="3">
            <VERS vnumber="16">Бо так полюбив Бог світ</VERS>
          </CHAPTER></BIBLEBOOK>
        </XMLBIBLE>
    """.trimIndent().encodeToByteArray()

    private fun TestScope.vm(repository: LocalBibleRepository = LocalBibleRepository(InMemoryFileStorage())) =
        ConvertBibleViewModel(repository, UnconfinedTestDispatcher(testScheduler))

    @Test
    fun `a Bible file is read on pick and described before anything is saved`() = runVmTestUnconfined {
        val repository = LocalBibleRepository(InMemoryFileStorage())
        val vm = vm(repository)
        try {
            vm.pick("SF_UKR_OGI.xml", zefania)

            val ready = assertIs<ConvertPhase.Ready>(vm.phase.first { it is ConvertPhase.Ready })
            assertEquals(BibleXmlFormat.ZEFANIA, ready.detected.format)
            assertEquals("Ukrainian", ready.detected.languageName)
            assertEquals(1, ready.detected.books)
            assertEquals(1, ready.detected.verses)
            assertEquals("Бо так полюбив Бог світ", ready.detected.preview?.second)
            assertEquals("Біблія (Огієнко)", vm.title.value)
            assertEquals("UKR_OGI", vm.abbreviation.value)
            assertEquals(PickedBible("SF_UKR_OGI.xml", zefania.size.toLong()), vm.file.value)
            assertTrue(repository.index.value.isEmpty, "reading is not installing")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a file that is not a Bible is unreadable and says why`() = runVmTestUnconfined {
        val vm = vm()
        try {
            vm.pick("export.xml", "<songs><song/></songs>".encodeToByteArray())

            val phase = assertIs<ConvertPhase.Unreadable>(vm.phase.first { it is ConvertPhase.Unreadable })
            assertEquals(InstallFailure.NOT_A_BIBLE, phase.failure)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `convert and install asks for the copyright confirmation first`() = runVmTestUnconfined {
        val repository = LocalBibleRepository(InMemoryFileStorage())
        val vm = vm(repository)
        try {
            vm.pick("SF_UKR_OGI.xml", zefania)
            vm.phase.first { it is ConvertPhase.Ready }

            vm.requestConvert()

            assertTrue(vm.licencePending.value)
            assertTrue(repository.index.value.isEmpty, "nothing is installed before the confirmation")
            assertEquals("Public domain", vm.fileRights)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `declining the confirmation installs nothing`() = runVmTestUnconfined {
        val repository = LocalBibleRepository(InMemoryFileStorage())
        val vm = vm(repository)
        try {
            vm.pick("SF_UKR_OGI.xml", zefania)
            vm.phase.first { it is ConvertPhase.Ready }
            vm.requestConvert()

            vm.dismissLicence()

            assertFalse(vm.licencePending.value)
            assertTrue(repository.index.value.isEmpty)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `confirming installs under the chosen title and abbreviation`() = runVmTestUnconfined {
        val repository = LocalBibleRepository(InMemoryFileStorage())
        val vm = vm(repository)
        try {
            vm.pick("SF_UKR_OGI.xml", zefania)
            vm.phase.first { it is ConvertPhase.Ready }
            vm.setTitle("Огієнко")
            vm.setAbbreviation("ukr_ogi2")
            vm.requestConvert()

            vm.confirmLicence()

            val done = assertIs<ConvertPhase.Installed>(vm.phase.first { it is ConvertPhase.Installed })
            val bible = done.bible
            assertEquals("UKR_OGI2.spb", bible.fileName)
            assertEquals("Огієнко", bible.title)
            assertEquals("ukr_ogi2", bible.abbreviation)
            assertEquals("Ukrainian", bible.languageName)
            assertEquals(InstallDetails.ORIGIN_FILE, bible.origin)
            assertEquals("Public domain", bible.license)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `open in Bible makes the converted translation the one read`() = runVmTestUnconfined {
        val repository = LocalBibleRepository(InMemoryFileStorage())
        repository.install("en_KJV.spb", "##Title:\tKJV\n1\tGenesis\t1\n-----\nB001C001V001\t1\t1\t1\tIn the beginning")
        val vm = vm(repository)
        try {
            vm.pick("SF_UKR_OGI.xml", zefania)
            vm.phase.first { it is ConvertPhase.Ready }
            vm.requestConvert()
            vm.confirmLicence()
            vm.phase.first { it is ConvertPhase.Installed }

            vm.openInBible()

            assertEquals("Біблія (Огієнко)", repository.index.value.active?.title)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `an abbreviation already installed is flagged as a replacement`() = runVmTestUnconfined {
        val repository = LocalBibleRepository(InMemoryFileStorage())
        repository.install("UKR_OGI.spb", "##Title:\tOld\n43\tJohn\t21\n-----\nB043C003V016\t43\t3\t16\tOld text")
        val vm = vm(repository)
        try {
            assertTrue(vm.replacesInstalled("ukr_ogi"))
            assertFalse(vm.replacesInstalled("UKR_NEW"))
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `choosing another file starts again`() = runVmTestUnconfined {
        val vm = vm()
        try {
            vm.pick("export.xml", "not xml".encodeToByteArray())
            vm.phase.first { it is ConvertPhase.Unreadable }

            vm.reset()

            assertEquals(ConvertPhase.Empty, vm.phase.value)
            assertNull(vm.file.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `the saved file name comes from the abbreviation as typed`() {
        assertEquals("UKR_OGI.spb", convertedFileName("ukr_ogi"))
        assertEquals("UKR-OGI.spb", convertedFileName("ukr-ogi"))
        assertEquals("UND.spb", convertedFileName("  /  "), "an abbreviation with nothing usable still names a file")
    }
}
