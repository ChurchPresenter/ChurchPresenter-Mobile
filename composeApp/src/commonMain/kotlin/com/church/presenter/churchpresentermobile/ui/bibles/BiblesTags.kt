package com.church.presenter.churchpresentermobile.ui.bibles

/**
 * Test tags for the Bible downloads screens, named once so the screens and their tests cannot
 * drift apart. See `LibraryTags` for why tags rather than labels: compose-resources renders empty
 * on the wasmJs test runtime.
 */
internal object BiblesTags {
    const val SELECTOR = "bibles_selector"
    const val SHEET = "bibles_sheet"
    const val MANAGE = "bibles_manage"
    const val GET_MORE = "bibles_get_more"
    const val EMPTY = "bibles_empty"
    const val EMPTY_GET = "bibles_empty_get"

    const val CATALOG = "bibles_catalog"
    const val CATALOG_BACK = "bibles_catalog_back"
    const val CATALOG_SEARCH = "bibles_catalog_search"
    const val LANGUAGE_BUTTON = "bibles_language_button"
    const val LANGUAGE_SHEET = "bibles_language_sheet"
    const val LANGUAGE_DONE = "bibles_language_done"
    const val CATALOG_LOADING = "bibles_catalog_loading"
    const val CATALOG_FAILED = "bibles_catalog_failed"
    const val CATALOG_RETRY = "bibles_catalog_retry"
    const val CATALOG_NO_MATCH = "bibles_catalog_no_match"
    const val CATALOG_STALE = "bibles_catalog_stale"

    const val LICENCE = "bibles_licence"
    const val LICENCE_ACCEPT = "bibles_licence_accept"
    const val LICENCE_CANCEL = "bibles_licence_cancel"

    const val INSTALL_SHEET = "bibles_install_sheet"
    const val INSTALL_BACKGROUND = "bibles_install_background"
    const val INSTALL_CANCEL = "bibles_install_cancel"
    const val INSTALL_RETRY = "bibles_install_retry"
    const val INSTALL_DONE = "bibles_install_done"
    const val INSTALL_OPEN = "bibles_install_open"

    const val CONVERT = "bibles_convert"
    const val CONVERT_PICK = "bibles_convert_pick"
    const val CONVERT_TITLE = "bibles_convert_title"
    const val CONVERT_ABBREVIATION = "bibles_convert_abbreviation"
    const val CONVERT_INSTALL = "bibles_convert_install"
    const val CONVERT_ERROR = "bibles_convert_error"
    const val CONVERT_REPLACES = "bibles_convert_replaces"

    const val INSTALLED = "bibles_installed"
    const val REMOVE_CONFIRM = "bibles_remove_confirm"

    const val NAV_INSTALLED = "bibles_nav_installed"
    const val NAV_GET = "bibles_nav_get"
    const val NAV_CONVERT = "bibles_nav_convert"
    const val DETAIL_EMPTY = "bibles_detail_empty"

    fun translation(id: String) = "bibles_translation_$id"
    fun catalogRow(key: String) = "bibles_row_$key"
    fun rowAction(key: String) = "bibles_row_action_$key"
    fun source(name: String) = "bibles_source_$name"
    fun language(code: String?) = "bibles_language_${code ?: "all"}"
    fun installedRow(id: String) = "bibles_installed_$id"
    fun remove(id: String) = "bibles_remove_$id"
}
