package com.church.presenter.churchpresentermobile.ui.bibles

import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleSource
import com.church.presenter.churchpresentermobile.bibleimport.catalog.CatalogSnapshot
import com.church.presenter.churchpresentermobile.viewmodel.CatalogFilter
import com.church.presenter.churchpresentermobile.viewmodel.CatalogRow
import com.church.presenter.churchpresentermobile.viewmodel.CatalogView
import com.church.presenter.churchpresentermobile.viewmodel.ConvertPhase
import com.church.presenter.churchpresentermobile.viewmodel.PickedBible

/*
 * The plain state and callback bundles the Bible downloads screens are drawn from — gathered
 * here so each screen file is about its layout, and the phone and tablet arrangements share one
 * shape of input.
 */

/** The three places the Bibles screen can open on — the tablet's left-hand navigation. */
enum class BiblesPage { INSTALLED, GET, CONVERT }

/** Everything the catalogue list draws, gathered so the phone screen and the tablet pane share it. */
internal class CatalogUi(
    val snapshot: CatalogSnapshot,
    val view: CatalogView,
    val isLoading: Boolean,
    val filter: CatalogFilter,
    val nowMs: Long,
)

/** What a catalogue screen can ask for. */
internal class CatalogActions(
    val onSource: (BibleSource?) -> Unit,
    val onQuery: (String) -> Unit,
    val onLanguage: (String?) -> Unit,
    val onRow: (CatalogRow) -> Unit,
    val onRetry: () -> Unit,
)

/** What the install view can ask for. */
internal class InstallActions(
    val onBackground: () -> Unit,
    val onCancel: () -> Unit,
    val onRetry: () -> Unit,
    val onDone: () -> Unit,
    val onOpen: () -> Unit,
)

/** Everything the convert form draws. */
internal class ConvertUi(
    val file: PickedBible?,
    val phase: ConvertPhase,
    val title: String,
    val abbreviation: String,
    val savedFileName: String,
    val replaces: Boolean,
)

internal class ConvertActions(
    val onPick: () -> Unit,
    val onTitle: (String) -> Unit,
    val onAbbreviation: (String) -> Unit,
    val onConvert: () -> Unit,
    val onDone: () -> Unit,
    val onOpen: () -> Unit,
)

