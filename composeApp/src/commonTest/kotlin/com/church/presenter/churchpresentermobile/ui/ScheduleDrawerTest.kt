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
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The drawer that shows the desktop's running order.
 *
 * Driven through the real ViewModel over a mocked desktop, so the assertions
 * are about what an operator sees for a given server response. Two things here
 * have actually gone wrong and are worth pinning: the item the desktop says is
 * live must be the row marked live, and the item types the phone cannot drive
 * — lower thirds, scenes, canvases — must not be listed at all, because tapping
 * one does nothing and looks like a broken drawer.
 */
@OptIn(ExperimentalTestApi::class)
class ScheduleDrawerTest {

    private fun json(body: String) = body.trimIndent()

    private fun drawerVm(response: String, status: HttpStatusCode = HttpStatusCode.OK): ScheduleViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return ScheduleViewModel(settings, ServerEventService(settings), isDemoMode = false) {
            ScheduleService(
                it,
                mockClient {
                    respond(
                        response,
                        status,
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

    private val twoItems = json(
        """
        [{"id":"a","type":"song","displayText":"1 - Amazing Grace"},
         {"id":"b","type":"bible","displayText":"John 3:16"}]
        """
    )

    // ── What is listed ───────────────────────────────────────────────────

    @Test
    fun theDesktopsScheduleIsListed() = runComposeUiTest {
        val vm = drawerVm(twoItems)
        showDrawer(vm)

        awaitThat { vm.items.value.isNotEmpty() }
        assertTrue(isShowing("1 - Amazing Grace"))
        assertTrue(isShowing("John 3:16"))
    }

    @Test
    fun everyItemGetsARowOfItsOwn() = runComposeUiTest {
        val vm = drawerVm(twoItems)
        showDrawer(vm)

        awaitThat { exists(UiTags.scheduleRow(1)) }
        assertTrue(exists(UiTags.scheduleRow(0)))
        assertTrue(exists(UiTags.scheduleRow(1)))
    }

    @Test
    fun thereIsNoRowBeyondTheLastItem() = runComposeUiTest {
        val vm = drawerVm(twoItems)
        showDrawer(vm)

        awaitThat { exists(UiTags.scheduleRow(1)) }
        assertFalse(exists(UiTags.scheduleRow(2)))
    }

    @Test
    fun theCountIsShown() = runComposeUiTest {
        val vm = drawerVm(twoItems)
        showDrawer(vm)

        assertTrue(exists(UiTags.DRAWER_COUNT))
    }

    @Test
    fun anEmptyScheduleSaysSoRatherThanLookingBroken() = runComposeUiTest {
        // A service with nothing scheduled yet is the ordinary state on a
        // Sunday morning, not a failure.
        val vm = drawerVm("[]")
        showDrawer(vm)

        awaitThat { exists(UiTags.DRAWER_EMPTY) }
        assertTrue(exists(UiTags.DRAWER_EMPTY))
    }

    @Test
    fun theEmptyMessageIsGoneOnceItemsArrive() = runComposeUiTest {
        val vm = drawerVm(twoItems)
        showDrawer(vm)

        awaitThat { vm.items.value.isNotEmpty() }
        assertFalse(exists(UiTags.DRAWER_EMPTY))
    }

    // ── The types the phone cannot drive ─────────────────────────────────

    @Test
    fun aLowerThirdIsNotListed() = runComposeUiTest {
        // Tapping one would do nothing, which reads as a broken drawer.
        val vm = drawerVm(
            json("""[{"id":"a","type":"lowerThird","displayText":"Speaker name"}]""")
        )
        showDrawer(vm)

        awaitThat { vm.items.value.isNotEmpty() }
        assertFalse(exists(UiTags.scheduleRow(0)))
    }

    @Test
    fun aSceneIsNotListed() = runComposeUiTest {
        val vm = drawerVm(json("""[{"id":"a","type":"scene","displayText":"Opening scene"}]"""))
        showDrawer(vm)

        awaitThat { vm.items.value.isNotEmpty() }
        assertFalse(exists(UiTags.scheduleRow(0)))
    }

    @Test
    fun aCanvasIsNotListed() = runComposeUiTest {
        val vm = drawerVm(json("""[{"id":"a","type":"canvas","displayText":"Countdown"}]"""))
        showDrawer(vm)

        awaitThat { vm.items.value.isNotEmpty() }
        assertFalse(exists(UiTags.scheduleRow(0)))
    }

    @Test
    fun anItemWithNoTypeIsNotListed() = runComposeUiTest {
        // Nothing can be done with an item whose type the phone does not know.
        val vm = drawerVm(json("""[{"id":"a","displayText":"Mystery item"}]"""))
        showDrawer(vm)

        awaitThat { vm.items.value.isNotEmpty() }
        assertFalse(exists(UiTags.scheduleRow(0)))
    }

    @Test
    fun theDrivableItemsSurviveAlongsideOnesThatAreNot() = runComposeUiTest {
        // The filter must remove only the undrivable rows, and must renumber
        // the rest — a stale index would open the wrong item.
        val vm = drawerVm(
            json(
                """
                [{"id":"a","type":"lowerThird","displayText":"Speaker name"},
                 {"id":"b","type":"song","displayText":"1 - Amazing Grace"}]
                """
            )
        )
        showDrawer(vm)

        awaitThat { exists(UiTags.scheduleRow(0)) }
        assertTrue(isShowing("1 - Amazing Grace"))
        assertFalse(exists(UiTags.scheduleRow(1)))
    }

    @Test
    fun aFilteredScheduleWithNothingDrivableReadsAsEmpty() = runComposeUiTest {
        val vm = drawerVm(json("""[{"id":"a","type":"scene","displayText":"Opening"}]"""))
        showDrawer(vm)

        awaitThat { exists(UiTags.DRAWER_EMPTY) }
        assertTrue(exists(UiTags.DRAWER_EMPTY))
    }

    // ── What the congregation is looking at ──────────────────────────────

    @Test
    fun theActiveItemIsMarkedLive() = runComposeUiTest {
        val vm = drawerVm(
            json(
                """
                [{"id":"a","type":"song","displayText":"1 - Amazing Grace","isActive":true},
                 {"id":"b","type":"bible","displayText":"John 3:16"}]
                """
            )
        )
        showDrawer(vm)

        awaitThat { exists(UiTags.scheduleRow(0)) }
        assertTrue(exists(UiTags.scheduleRowLive(0)))
    }

    @Test
    fun onlyTheActiveItemIsMarkedLive() = runComposeUiTest {
        // The single cue that says which item the congregation can see.
        val vm = drawerVm(
            json(
                """
                [{"id":"a","type":"song","displayText":"1 - Amazing Grace","isActive":true},
                 {"id":"b","type":"bible","displayText":"John 3:16"}]
                """
            )
        )
        showDrawer(vm)

        awaitThat { exists(UiTags.scheduleRow(1)) }
        assertFalse(exists(UiTags.scheduleRowLive(1)))
    }

    @Test
    fun theLiveMarkerFollowsTheItemTheDesktopNames() = runComposeUiTest {
        val vm = drawerVm(
            json(
                """
                [{"id":"a","type":"song","displayText":"1 - Amazing Grace"},
                 {"id":"b","type":"bible","displayText":"John 3:16","isActive":true}]
                """
            )
        )
        showDrawer(vm)

        awaitThat { exists(UiTags.scheduleRow(1)) }
        assertTrue(exists(UiTags.scheduleRowLive(1)))
        assertFalse(exists(UiTags.scheduleRowLive(0)))
    }

    @Test
    fun nothingIsMarkedLiveWhenTheDesktopSaysNothingIs() = runComposeUiTest {
        val vm = drawerVm(twoItems)
        showDrawer(vm)

        awaitThat { exists(UiTags.scheduleRow(1)) }
        assertFalse(exists(UiTags.scheduleRowLive(0)))
        assertFalse(exists(UiTags.scheduleRowLive(1)))
    }

    // ── Opening an item ──────────────────────────────────────────────────

    @Test
    fun tappingAnItemOpensIt() = runComposeUiTest {
        var opened: ScheduleItem? = null
        val vm = drawerVm(twoItems)
        showDrawer(vm, onItemClick = { opened = it })

        awaitThat { exists(UiTags.scheduleRow(0)) }
        click(UiTags.scheduleRow(0))

        assertEquals("1 - Amazing Grace", opened?.displayTitle)
    }

    @Test
    fun tappingReportsTheItemThatWasTapped() = runComposeUiTest {
        var opened: ScheduleItem? = null
        val vm = drawerVm(twoItems)
        showDrawer(vm, onItemClick = { opened = it })

        awaitThat { exists(UiTags.scheduleRow(1)) }
        click(UiTags.scheduleRow(1))

        assertEquals("John 3:16", opened?.displayTitle)
    }

    @Test
    fun tappingReportsTheDrivableItemEvenWhenOneWasFilteredOut() = runComposeUiTest {
        // The row index is into the filtered list; using the unfiltered one
        // would open the item next to the one under the finger.
        var opened: ScheduleItem? = null
        val vm = drawerVm(
            json(
                """
                [{"id":"a","type":"scene","displayText":"Opening"},
                 {"id":"b","type":"song","displayText":"1 - Amazing Grace"}]
                """
            )
        )
        showDrawer(vm, onItemClick = { opened = it })

        awaitThat { exists(UiTags.scheduleRow(0)) }
        click(UiTags.scheduleRow(0))

        assertEquals("1 - Amazing Grace", opened?.displayTitle)
    }

    // ── Closing, and failures ────────────────────────────────────────────

    @Test
    fun theDrawerCanBeClosed() = runComposeUiTest {
        var closed = false
        val vm = drawerVm(twoItems)
        showDrawer(vm, onClose = { closed = true })

        click(UiTags.DRAWER_CLOSE)

        assertTrue(closed)
    }

    @Test
    fun closingDoesNotOpenAnItem() = runComposeUiTest {
        var opened: ScheduleItem? = null
        val vm = drawerVm(twoItems)
        showDrawer(vm, onItemClick = { opened = it })

        click(UiTags.DRAWER_CLOSE)

        assertNull(opened)
    }

    @Test
    fun aDesktopThatFailsIsReportedRatherThanShowingAnEmptySchedule() = runComposeUiTest {
        // "Nothing scheduled" and "could not reach the desktop" send the
        // operator to different places.
        val vm = drawerVm("nope", HttpStatusCode.InternalServerError)
        showDrawer(vm)

        awaitThat { exists(UiTags.DRAWER_ERROR) }
        assertTrue(exists(UiTags.DRAWER_ERROR))
    }

    @Test
    fun noErrorIsShownWhenTheScheduleLoads() = runComposeUiTest {
        val vm = drawerVm(twoItems)
        showDrawer(vm)

        awaitThat { vm.items.value.isNotEmpty() }
        assertFalse(exists(UiTags.DRAWER_ERROR))
    }

    @Test
    fun theDrawerCanStillBeClosedAfterAFailure() = runComposeUiTest {
        var closed = false
        val vm = drawerVm("nope", HttpStatusCode.InternalServerError)
        showDrawer(vm, onClose = { closed = true })

        awaitThat { exists(UiTags.DRAWER_ERROR) }
        click(UiTags.DRAWER_CLOSE)

        assertTrue(closed)
    }
}
