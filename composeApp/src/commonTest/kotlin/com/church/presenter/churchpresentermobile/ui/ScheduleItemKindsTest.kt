package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ScheduleItem
import com.church.presenter.churchpresentermobile.network.ScheduleService
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import com.church.presenter.churchpresentermobile.viewmodel.ScheduleViewModel
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Every kind of item the desktop can put in a running order.
 *
 * The drawer draws each type differently and builds its label from whichever
 * fields the desktop happened to send — `displayText`, a title, a Bible
 * reference assembled from three fields, or, failing everything, an id. A type
 * the drawer does not recognise still has to appear: an item the operator can
 * see on the desktop and not on the phone reads as a lost item.
 */
@OptIn(ExperimentalTestApi::class)
class ScheduleItemKindsTest {

    private fun drawerVm(response: String): ScheduleViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return ScheduleViewModel(settings, ServerEventService(settings), isDemoMode = false) {
            ScheduleService(
                it,
                mockClient {
                    respond(
                        response,
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                },
            )
        }
    }

    private fun ComposeUiTest.showDrawer(
        vm: ScheduleViewModel,
        onItemClick: (ScheduleItem) -> Unit = {},
        onClose: () -> Unit = {},
    ) = showScreen {
        ScheduleDrawerContent(
            appSettings = AppSettings(InMemorySettingsStorage()),
            settingsSaveToken = 0,
            onItemClick = onItemClick,
            onClose = onClose,
            providedViewModel = vm,
        )
    }

    private fun oneItem(vararg fields: Pair<String, String>) =
        "[{" + fields.joinToString(",") { (k, v) -> "\"$k\":$v" } + "}]"

    private fun str(value: String) = "\"$value\""

    // ── Each type gets a row ─────────────────────────────────────────────

    @Test
    fun aSongIsListed() = runComposeUiTest {
        val vm = drawerVm(oneItem("id" to str("a"), "type" to str("song"), "displayText" to str("1 - Amazing Grace")))
        showDrawer(vm)

        awaitThat { isShowing("1 - Amazing Grace") }
    }

    @Test
    fun aBibleReadingIsListed() = runComposeUiTest {
        val vm = drawerVm(oneItem("id" to str("a"), "type" to str("bible"), "displayText" to str("John 3:16")))
        showDrawer(vm)

        awaitThat { isShowing("John 3:16") }
    }

    @Test
    fun aPictureIsListed() = runComposeUiTest {
        val vm = drawerVm(oneItem("id" to str("a"), "type" to str("picture"), "displayText" to str("Sunset")))
        showDrawer(vm)

        awaitThat { isShowing("Sunset") }
    }

    @Test
    fun anImageIsListed() = runComposeUiTest {
        val vm = drawerVm(oneItem("id" to str("a"), "type" to str("image"), "displayText" to str("Sunrise")))
        showDrawer(vm)

        awaitThat { isShowing("Sunrise") }
    }

    @Test
    fun aDictionaryEntryIsListed() = runComposeUiTest {
        val vm = drawerVm(oneItem("id" to str("a"), "type" to str("dictionary"), "displayText" to str("H1254 bara")))
        showDrawer(vm)

        awaitThat { isShowing("H1254 bara") }
    }

    @Test
    fun anAnnouncementIsListed() = runComposeUiTest {
        val vm = drawerVm(
            oneItem("id" to str("a"), "type" to str("announcement"), "displayText" to str("Coffee after"))
        )
        showDrawer(vm)

        awaitThat { isShowing("Coffee after") }
    }

    @Test
    fun aWebsiteIsListed() = runComposeUiTest {
        val vm = drawerVm(oneItem("id" to str("a"), "type" to str("website"), "displayText" to str("example.org")))
        showDrawer(vm)

        awaitThat { isShowing("example.org") }
    }

    @Test
    fun aMediaItemIsListed() = runComposeUiTest {
        val vm = drawerVm(oneItem("id" to str("a"), "type" to str("media"), "displayText" to str("Welcome video")))
        showDrawer(vm)

        awaitThat { isShowing("Welcome video") }
    }

    @Test
    fun aTypeTheDrawerDoesNotRecogniseIsStillListed() = runComposeUiTest {
        // An item on the desktop and not on the phone reads as a lost item.
        val vm = drawerVm(oneItem("id" to str("a"), "type" to str("something-new"), "displayText" to str("Mystery")))
        showDrawer(vm)

        awaitThat { isShowing("Mystery") }
    }

    @Test
    fun anItemWithNoTypeAtAllIsNotListed() = runComposeUiTest {
        // The drawer hides what it cannot drive, and an item with no type
        // cannot be routed to a tab. Pinned as it stands: it is a silent drop,
        // so if the desktop ever starts omitting the field this test is where
        // the missing rows will be explained.
        val vm = drawerVm(oneItem("id" to str("a"), "displayText" to str("Typeless")))
        showDrawer(vm)

        awaitThat { exists(UiTags.DRAWER_EMPTY) }
        assertFalse(isShowing("Typeless"))
    }

    @Test
    fun aLowerThirdIsNotListed() = runComposeUiTest {
        // The phone cannot drive one; a row that does nothing when tapped reads
        // as a broken drawer.
        val vm = drawerVm(oneItem("id" to str("a"), "type" to str("lower-third"), "displayText" to str("Speaker name")))
        showDrawer(vm)

        awaitThat { exists(UiTags.DRAWER_EMPTY) }
        assertFalse(isShowing("Speaker name"))
    }

    @Test
    fun aSceneIsNotListed() = runComposeUiTest {
        val vm = drawerVm(oneItem("id" to str("a"), "type" to str("scene"), "displayText" to str("Opening scene")))
        showDrawer(vm)

        awaitThat { exists(UiTags.DRAWER_EMPTY) }
    }

    @Test
    fun aCanvasIsNotListed() = runComposeUiTest {
        val vm = drawerVm(oneItem("id" to str("a"), "type" to str("canvas"), "displayText" to str("Canvas 1")))
        showDrawer(vm)

        awaitThat { exists(UiTags.DRAWER_EMPTY) }
    }

    @Test
    fun theItemsThePhoneCanDriveSurviveTheFilter() = runComposeUiTest {
        val vm = drawerVm(
            """
            [{"id":"a","type":"lower-third","displayText":"Hidden"},
             {"id":"b","type":"song","displayText":"Shown"}]
            """.trimIndent()
        )
        showDrawer(vm)

        awaitThat { isShowing("Shown") }
        assertFalse(isShowing("Hidden"))
    }

    @Test
    fun aTypeInCapitalsIsRecognisedJustTheSame() = runComposeUiTest {
        val vm = drawerVm(oneItem("id" to str("a"), "type" to str("SONG"), "displayText" to str("Shouted")))
        showDrawer(vm)

        awaitThat { isShowing("Shouted") }
    }

    @Test
    fun everyTypeInOneOrderIsListed() = runComposeUiTest {
        val vm = drawerVm(
            """
            [{"id":"1","type":"song","displayText":"A song"},
             {"id":"2","type":"bible","displayText":"A reading"},
             {"id":"3","type":"picture","displayText":"A picture"},
             {"id":"4","type":"announcement","displayText":"A notice"},
             {"id":"5","type":"media","displayText":"A video"}]
            """.trimIndent()
        )
        showDrawer(vm)

        awaitThat { isShowing("A song") }
        assertTrue(isShowing("A reading"))
        assertTrue(isShowing("A picture"))
        assertTrue(isShowing("A notice"))
        assertTrue(isShowing("A video"))
    }

    // ── How a row gets its words ─────────────────────────────────────────

    @Test
    fun anItemIsNamedByItsDisplayText() = runComposeUiTest {
        val vm = drawerVm(
            oneItem("id" to str("a"), "type" to str("song"), "title" to str("Title"), "displayText" to str("Display"))
        )
        showDrawer(vm)

        awaitThat { isShowing("Display") }
    }

    @Test
    fun anItemWithNoDisplayTextFallsBackToItsTitle() = runComposeUiTest {
        val vm = drawerVm(oneItem("id" to str("a"), "type" to str("song"), "title" to str("Amazing Grace")))
        showDrawer(vm)

        awaitThat { isShowing("Amazing Grace") }
    }

    @Test
    fun aBlankDisplayTextFallsBackToTheTitle() = runComposeUiTest {
        val vm = drawerVm(
            oneItem("id" to str("a"), "type" to str("song"), "title" to str("Amazing Grace"), "displayText" to str(""))
        )
        showDrawer(vm)

        awaitThat { isShowing("Amazing Grace") }
    }

    @Test
    fun aVerseIsNamedByItsReferenceWhenNothingElseIsSent() = runComposeUiTest {
        val vm = drawerVm(
            oneItem(
                "id" to str("a"), "type" to str("bible"),
                "bookName" to str("John"), "chapter" to "3", "verseNumber" to "16",
            )
        )
        showDrawer(vm)

        awaitThat { isShowing("John 3:16") }
    }

    @Test
    fun aVerseRangeIsNamedAsARange() = runComposeUiTest {
        val vm = drawerVm(
            oneItem(
                "id" to str("a"), "type" to str("bible"),
                "bookName" to str("John"), "chapter" to "3", "verseNumber" to "16",
                "verseRange" to str("16-18"),
            )
        )
        showDrawer(vm)

        awaitThat { isShowing("John 3:16-18") }
    }

    @Test
    fun aPictureIsNamedByItsFolderAndFile() = runComposeUiTest {
        val vm = drawerVm(
            oneItem(
                "id" to str("a"), "type" to str("image"),
                "displayText" to str("sunset.jpg"), "folderName" to str("Advent"),
            )
        )
        showDrawer(vm)

        awaitThat { isShowing("Advent / sunset.jpg") }
    }

    @Test
    fun aPictureWithNoFolderIsNamedByItsFile() = runComposeUiTest {
        val vm = drawerVm(
            oneItem("id" to str("a"), "type" to str("image"), "displayText" to str("sunset.jpg"))
        )
        showDrawer(vm)

        awaitThat { isShowing("sunset.jpg") }
    }

    @Test
    fun aPictureWithOnlyAFolderIsNamedByIt() = runComposeUiTest {
        val vm = drawerVm(
            oneItem("id" to str("a"), "type" to str("image"), "folderName" to str("Advent"))
        )
        showDrawer(vm)

        awaitThat { isShowing("Advent") }
    }

    @Test
    fun aPictureWithNeitherFallsBackToItsTitle() = runComposeUiTest {
        val vm = drawerVm(
            oneItem("id" to str("a"), "type" to str("image"), "title" to str("Untitled photo"))
        )
        showDrawer(vm)

        awaitThat { isShowing("Untitled photo") }
    }

    @Test
    fun anItemWithNothingButAnIdIsNamedByIt() = runComposeUiTest {
        // Better a UUID than an empty row the operator cannot tap with confidence.
        val vm = drawerVm(oneItem("id" to str("item-42"), "type" to str("song")))
        showDrawer(vm)

        awaitThat { isShowing("item-42") }
    }

    // ── Which row is live ────────────────────────────────────────────────

    @Test
    fun theRowTheDesktopIsShowingIsMarkedLive() = runComposeUiTest {
        val vm = drawerVm(
            """
            [{"id":"a","type":"song","displayText":"First","isActive":false},
             {"id":"b","type":"song","displayText":"Second","isActive":true}]
            """.trimIndent()
        )
        showDrawer(vm)

        awaitThat { exists(UiTags.scheduleRowLive(1)) }
    }

    @Test
    fun theOtherRowsAreNotMarkedLive() = runComposeUiTest {
        val vm = drawerVm(
            """
            [{"id":"a","type":"song","displayText":"First","isActive":false},
             {"id":"b","type":"song","displayText":"Second","isActive":true}]
            """.trimIndent()
        )
        showDrawer(vm)

        awaitThat { exists(UiTags.scheduleRowLive(1)) }
        assertFalse(exists(UiTags.scheduleRowLive(0)))
    }

    @Test
    fun anOrderWithNothingLiveMarksNothing() = runComposeUiTest {
        val vm = drawerVm(oneItem("id" to str("a"), "type" to str("song"), "displayText" to str("First")))
        showDrawer(vm)

        awaitThat { exists(UiTags.scheduleRow(0)) }
        assertFalse(exists(UiTags.scheduleRowLive(0)))
    }

    @Test
    fun theLiveFlagIsReadFromEitherSpellingTheDesktopSends() = runComposeUiTest {
        // Two server versions, two field names, one meaning.
        val vm = drawerVm(
            oneItem(
                "id" to str("a"),
                "type" to str("song"),
                "displayText" to str("First"),
                "isActive" to "true",
            )
        )
        showDrawer(vm)

        awaitThat { exists(UiTags.scheduleRowLive(0)) }
    }

    // ── Tapping a row ────────────────────────────────────────────────────

    @Test
    fun tappingARowReportsThatItem() = runComposeUiTest {
        var tapped: ScheduleItem? = null
        val vm = drawerVm(
            """
            [{"id":"a","type":"song","displayText":"First"},
             {"id":"b","type":"bible","displayText":"Second"}]
            """.trimIndent()
        )
        showDrawer(vm, onItemClick = { tapped = it })
        awaitThat { exists(UiTags.scheduleRow(1)) }

        click(UiTags.scheduleRow(1))

        awaitThat { tapped != null }
        assertEquals("b", tapped?.id)
    }

    @Test
    fun tappingTheFirstRowReportsTheFirstItem() = runComposeUiTest {
        var tapped: ScheduleItem? = null
        val vm = drawerVm(
            """
            [{"id":"a","type":"song","displayText":"First"},
             {"id":"b","type":"bible","displayText":"Second"}]
            """.trimIndent()
        )
        showDrawer(vm, onItemClick = { tapped = it })
        awaitThat { exists(UiTags.scheduleRow(0)) }

        click(UiTags.scheduleRow(0))

        awaitThat { tapped != null }
        assertEquals("a", tapped?.id)
    }

    @Test
    fun tappingARowDoesNotCloseTheDrawer() = runComposeUiTest {
        // The parent decides that; the drawer only reports the tap.
        var closed = 0
        val vm = drawerVm(oneItem("id" to str("a"), "type" to str("song"), "displayText" to str("First")))
        showDrawer(vm, onClose = { closed++ })
        awaitThat { exists(UiTags.scheduleRow(0)) }

        click(UiTags.scheduleRow(0))

        assertEquals(0, closed)
    }
}
