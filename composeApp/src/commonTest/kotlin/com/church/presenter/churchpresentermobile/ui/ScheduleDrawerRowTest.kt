package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppSettings
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
import kotlin.test.assertTrue

/**
 * What one schedule row says under its title.
 *
 * The desktop sends a `details` line for some item types and nothing for
 * others, so the row falls back to naming the type. Getting this wrong leaves a
 * row with a title and a blank second line, which reads as a half-loaded item.
 */
@OptIn(ExperimentalTestApi::class)
class ScheduleDrawerRowTest {

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

    private fun ComposeUiTest.showDrawer(vm: ScheduleViewModel) = showScreen {
        ScheduleDrawerContent(
            appSettings = AppSettings(InMemorySettingsStorage()),
            settingsSaveToken = 0,
            providedViewModel = vm,
        )
    }

    @Test
    fun theDesktopsOwnDetailLineIsShownWhenItSendsOne() = runComposeUiTest {
        val vm = drawerVm(
            """[{"id":"a","type":"song","displayText":"Amazing Grace","details":"Verse 1 of 4"}]"""
        )
        showDrawer(vm)

        awaitThat { exists(UiTags.scheduleRow(0)) }
        assertTrue(isShowing("Verse 1 of 4"))
    }

    @Test
    fun theTypeIsNamedWhenThereIsNoDetailLine() = runComposeUiTest {
        // A title with a blank line under it reads as a half-loaded item.
        val vm = drawerVm("""[{"id":"a","type":"song","displayText":"Amazing Grace"}]""")
        showDrawer(vm)

        awaitThat { exists(UiTags.scheduleRow(0)) }
        assertTrue(isShowing("Song"))
    }

    @Test
    fun aBlankDetailLineFallsBackToTheType() = runComposeUiTest {
        // The desktop sends "" rather than omitting the field for some types.
        val vm = drawerVm(
            """[{"id":"a","type":"bible","displayText":"John 3:16","details":"   "}]"""
        )
        showDrawer(vm)

        awaitThat { exists(UiTags.scheduleRow(0)) }
        assertTrue(isShowing("Bible"))
    }

    @Test
    fun theTypeIsShownWithACapitalRatherThanAsSentByTheDesktop() = runComposeUiTest {
        // The wire value is lowercase; the row is prose the operator reads.
        val vm = drawerVm("""[{"id":"a","type":"picture","displayText":"Sunset"}]""")
        showDrawer(vm)

        awaitThat { exists(UiTags.scheduleRow(0)) }
        assertTrue(isShowing("Picture"))
    }

    @Test
    fun theTitleIsShownAlongsideTheDetailLine() = runComposeUiTest {
        val vm = drawerVm(
            """[{"id":"a","type":"song","displayText":"Amazing Grace","details":"Verse 1"}]"""
        )
        showDrawer(vm)

        awaitThat { exists(UiTags.scheduleRow(0)) }
        assertTrue(isShowing("Amazing Grace"))
        assertTrue(isShowing("Verse 1"))
    }
}
