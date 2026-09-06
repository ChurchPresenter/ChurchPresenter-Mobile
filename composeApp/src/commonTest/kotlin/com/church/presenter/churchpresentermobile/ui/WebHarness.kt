package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.viewmodel.WebViewModel

/** Setup for the web-page screen, driven through its real ViewModel. */
internal fun viewModel(sender: FakeWsSender = FakeWsSender()) =
    WebViewModel(AppSettings(InMemorySettingsStorage()), sender)

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showWeb(vm: WebViewModel) = showScreen { WebScreen(viewModel = vm) }

/** Types an address and saves it, returning the id it was given. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.bookmark(vm: WebViewModel, url: String): String {
    type(UiTags.WEB_URL, url)
    click(UiTags.WEB_ADD_BOOKMARK)
    return vm.bookmarks.value.last().id
}
