package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.calendar.CANONICAL_BOOKS
import com.church.presenter.churchpresentermobile.calendar.CalendarRepository
import com.church.presenter.churchpresentermobile.calendar.PickerBook
import com.church.presenter.churchpresentermobile.calendar.RepeatRule
import com.church.presenter.churchpresentermobile.calendar.YearMonthRef
import com.church.presenter.churchpresentermobile.calendar.copyService
import com.church.presenter.churchpresentermobile.calendar.nowIso
import com.church.presenter.churchpresentermobile.calendar.parseStoredDate
import com.church.presenter.churchpresentermobile.calendar.repeatDates
import com.church.presenter.churchpresentermobile.calendar.ServiceHeading
import com.church.presenter.churchpresentermobile.calendar.serviceFromTemplate
import com.church.presenter.churchpresentermobile.calendar.storedDate
import com.church.presenter.churchpresentermobile.calendar.templateFrom
import com.church.presenter.churchpresentermobile.calendar.today
import com.church.presenter.churchpresentermobile.generateUUID
import com.church.presenter.churchpresentermobile.model.CalendarDocument
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.RowTiming
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.model.SongDurations
import com.church.presenter.churchpresentermobile.network.BibleCatalog
import com.church.presenter.churchpresentermobile.network.SongCatalog
import com.church.presenter.churchpresentermobile.calendar.sync.CalendarSyncEngine
import com.church.presenter.churchpresentermobile.calendar.sync.CalendarSyncState
import com.church.presenter.churchpresentermobile.calendar.sync.CalendarSyncTrigger
import com.church.presenter.churchpresentermobile.calendar.sync.EnrollDenied
import com.church.presenter.churchpresentermobile.calendar.sync.EnrollService
import com.church.presenter.churchpresentermobile.calendar.sync.EnrollSyncOff
import com.church.presenter.churchpresentermobile.calendar.sync.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** Where the two-step enrollment with the desktop stands. */
sealed class EnrollFlow {
    data object Idle : EnrollFlow()
    /** The desktop is showing its prompt; [code] is on this screen for the operator to compare. */
    data class WaitingForApproval(val code: String) : EnrollFlow()
    /**
     * Approved by a desktop too old to hand the keys over in its answer: it is showing the QR with
     * this phone's token and the instance key instead.
     */
    data object ScanQr : EnrollFlow()
    data object Done : EnrollFlow()
    data object Denied : EnrollFlow()
    /** The church computer has calendar sync switched off; somebody there has to turn it on first. */
    data object SyncOff : EnrollFlow()
    data class Failed(val message: String) : EnrollFlow()
}

private const val ENROLL_CODE_DIGITS = 6
private const val PUSH_DEBOUNCE_MS = 1_500L

/** What the Bible picker knows about a chapter once it has been read: how many verses, and their text. */
class ChapterPreview(val verseCount: Int, val firstWords: Map<Int, String>)

/**
 * The planner's state: which month and day are showing, which service is open, and the song and
 * book lists the picker offers. Every edit goes straight to the repository, which saves it.
 */
class CalendarViewModel(
    private val repository: CalendarRepository,
    private val songCatalog: SongCatalog? = null,
    private val bibleCatalog: BibleCatalog? = null,
    private val sync: CalendarSyncEngine? = null,
    private val enrollService: EnrollService? = null,
    private val deviceName: () -> String = { "" },
    private val saveEnrollment: (CalendarSyncState) -> Unit = {},
    private val newId: () -> String = { generateUUID() },
) : ViewModel() {

    val document: StateFlow<CalendarDocument> = repository.document

    val syncStatus: StateFlow<SyncStatus> = sync?.status ?: MutableStateFlow(SyncStatus.NotEnrolled)

    private val _enrollment = MutableStateFlow<EnrollFlow>(EnrollFlow.Idle)
    val enrollment: StateFlow<EnrollFlow> = _enrollment.asStateFlow()

    private val _visibleMonth = MutableStateFlow(YearMonthRef.of(today()))
    val visibleMonth: StateFlow<YearMonthRef> = _visibleMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow(today())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    /** The service whose run of show is open, or null for the month view. */
    private val _openServiceId = MutableStateFlow<String?>(null)
    val openServiceId: StateFlow<String?> = _openServiceId.asStateFlow()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _songDurations = MutableStateFlow(SongDurations.NONE)
    val songDurations: StateFlow<SongDurations> = _songDurations.asStateFlow()

    private val _songsLoading = MutableStateFlow(false)
    val songsLoading: StateFlow<Boolean> = _songsLoading.asStateFlow()

    private val _books = MutableStateFlow(CANONICAL_BOOKS)
    val books: StateFlow<List<PickerBook>> = _books.asStateFlow()

    // Created before `init`, which may start syncing through [pairing].
    val services = Services()
    val rows = Rows()
    val pairing = Pairing()

    init {
        repository.load()
        loadPickerSources()
        if (sync?.isEnrolled == true) pairing.startSyncing(sync)
    }

    // ── Relay ──────────────────────────────────────────────────────────────

    fun loadPickerSources() {
        val songsSource = songCatalog
        if (songsSource != null) {
            viewModelScope.launch {
                _songsLoading.value = true
                _songs.value = songsSource.listForPlanning()
                _songDurations.value = songsSource.durations()
                _songsLoading.value = false
            }
        }
        val booksSource = bibleCatalog
        if (booksSource != null) {
            viewModelScope.launch {
                booksSource.books().onSuccess { books ->
                    val picked = books.mapIndexedNotNull { index, book ->
                        val chapters = book.totalChapters
                        if (chapters <= 0) null else PickerBook(book.bookId ?: (index + 1), book.displayName, chapters)
                    }
                    if (picked.isNotEmpty()) _books.value = picked
                }
            }
        }
    }

    /** The verses of one chapter, with their first words, or null when no Bible can be read. */
    suspend fun chapterPreview(book: PickerBook, chapter: Int): ChapterPreview? {
        val source = bibleCatalog ?: return null
        val verses = source.chapter(book.number, chapter).getOrNull()?.takeIf { it.isNotEmpty() } ?: return null
        return ChapterPreview(
            verseCount = verses.maxOf { it.number },
            firstWords = verses.associate { it.number to it.displayText },
        )
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    fun showMonth(month: YearMonthRef) { _visibleMonth.value = month }
    fun showNextMonth() = showMonth(_visibleMonth.value.next())
    fun showPreviousMonth() = showMonth(_visibleMonth.value.previous())

    fun goToToday() {
        val now = today()
        _selectedDate.value = now
        _visibleMonth.value = YearMonthRef.of(now)
    }

    fun select(date: LocalDate) {
        _selectedDate.value = date
        if (!_visibleMonth.value.contains(date)) _visibleMonth.value = YearMonthRef.of(date)
    }

    fun openService(id: String) { _openServiceId.value = id }
    fun closeService() { _openServiceId.value = null }

    // ── Services ───────────────────────────────────────────────────────────

    fun servicesOn(date: LocalDate): List<PlannedService> = document.value.servicesOn(storedDate(date))

    // ── What the screens reach for, grouped by what it acts on ─────────────
    //
    // Three inner classes rather than thirty functions side by side: `rows.move(...)` says what it
    // moves, and each group is the whole surface for one thing the planner does. Inner rather than
    // separate collaborators because every one of them works on this ViewModel's own state.

    /** Planning a service: adding one, copying it forward, and what becomes of it. */
    inner class Services {
        /** Adds a service on the selected date and opens it. */
        fun add(name: String, startTime: String, kind: String, templateId: String?): String {
            val template = templateId?.let { id -> document.value.templates.firstOrNull { it.id == id } }
            val heading = ServiceHeading(name, startTime, kind)
            val service = serviceFromTemplate(template, _selectedDate.value, heading, newId, nowIso())
            repository.saveService(service)
            _openServiceId.value = service.id
            return service.id
        }

        fun copy(id: String, rule: RepeatRule, count: Int, includeRows: Boolean, includeCues: Boolean): Int {
            val source = repository.service(id) ?: return 0
            val from = source.date.let { LocalDate.parse(it) }
            val seriesId = source.seriesId.ifEmpty { newId() }
            val copies = repeatDates(from, rule, count).map { date ->
                copyService(source, date, newId, includeRows, includeCues, seriesId, nowIso())
            }
            val stamped = if (source.seriesId.isEmpty()) copies + source.copy(seriesId = seriesId) else copies
            repository.saveServices(stamped)
            return copies.size
        }

        /** The most recent earlier service on the same weekday as [date], the one "Copy last Sunday" copies. */
        fun lastLike(date: LocalDate): PlannedService? {
            val stored = storedDate(date)
            return document.value.services
                .filter { it.date < stored && parseStoredDate(it.date)?.dayOfWeek == date.dayOfWeek }
                .maxByOrNull { it.date + it.startTime }
        }

        /** Copies [lastLike] onto the selected date and opens the copy. */
        fun copyLastInto(date: LocalDate) {
            val source = lastLike(date) ?: return
            val copy = copyService(source, date, newId, at = nowIso())
            repository.saveService(copy)
            _openServiceId.value = copy.id
        }

        fun delete(id: String) {
            if (_openServiceId.value == id) _openServiceId.value = null
            repository.deleteService(id)
        }

        fun update(service: PlannedService) = repository.saveService(service)

        fun setArmed(id: String, armed: Boolean) {
            repository.service(id)?.let { repository.saveService(it.copy(armed = armed)) }
        }

        fun saveAsTemplate(id: String, name: String) {
            val service = repository.service(id) ?: return
            repository.saveTemplate(templateFrom(service, name, newId))
        }
    }

    /** The rows of a run of show. */
    inner class Rows {

        /** A key for a row about to be added; unique for the life of this planner. */
        fun newId(): String = this@CalendarViewModel.newId()
        fun add(serviceId: String, row: PlanRow, seconds: Int?, timing: RowTiming) {
            val service = repository.service(serviceId) ?: return
            repository.saveService(service.withRow(row, seconds, timing))
        }

        fun update(
            serviceId: String,
            row: PlanRow,
            seconds: Int?,
            timing: RowTiming,
        ) = add(serviceId, row, seconds, timing)

        fun remove(serviceId: String, rowId: String) {
            val service = repository.service(serviceId) ?: return
            repository.saveService(service.withoutRow(rowId))
        }

        fun move(serviceId: String, from: Int, to: Int) {
            val service = repository.service(serviceId) ?: return
            repository.saveService(service.withRowMoved(from, to))
        }
    }

    /** Pairing this phone with the church computer, and syncing once it is paired. */
    inner class Pairing {
        /** Whether the relay listeners are running. */
        private var syncing = false

        /**
         * Syncs now, then keeps syncing. Started on open for a phone already enrolled, and again
         * the moment an enrollment completes — without the second, a phone paired in this session
         * got one round and then nothing until the app was reopened.
         */
        internal fun startSyncing(engine: CalendarSyncEngine) {
            if (syncing) {
                viewModelScope.launch { engine.sync() }
                return
            }
            syncing = true
            viewModelScope.keepSyncing(engine, document)
        }

        fun syncNow() {
            val engine = sync ?: return
            viewModelScope.launch { engine.sync() }
        }

        fun leave() {
            sync?.leave()
            _enrollment.value = EnrollFlow.Idle
        }

        /** Ask the desktop, showing a code the operator compares with their prompt; Allow finishes it. */
        fun start() {
            val service = enrollService ?: return
            val code = List(ENROLL_CODE_DIGITS) { ('0'..'9').random() }.joinToString("")
            _enrollment.value = EnrollFlow.WaitingForApproval(code)
            viewModelScope.launch {
                service.enroll(deviceName(), code)
                    .onSuccess { reply ->
                        // Allow is the last step: the answer carries the enrollment itself.
                        val state = reply.toState()
                        if (state == null) _enrollment.value = EnrollFlow.ScanQr else enrolled(state)
                    }
                    .onFailure { e ->
                        _enrollment.value = when (e) {
                            is EnrollDenied -> EnrollFlow.Denied
                            is EnrollSyncOff -> EnrollFlow.SyncOff
                            else -> EnrollFlow.Failed(e.message.orEmpty())
                        }
                    }
            }
        }

        /** A QR carrying the token and the key: an invite, or what an older desktop shows after Allow. */
        fun complete(qrText: String) {
            val state = CalendarSyncState.fromQr(qrText)
            if (state == null) {
                _enrollment.value = EnrollFlow.Failed("")
                return
            }
            enrolled(state)
        }

        private fun enrolled(state: CalendarSyncState) {
            saveEnrollment(state)
            _enrollment.value = EnrollFlow.Done
            sync?.let(::startSyncing)
        }

        fun reset() { _enrollment.value = EnrollFlow.Idle }
    }


}

/**
 * One round now, another on every relay nudge, and one after every local edit settles — the relay
 * is what makes a plan reach the other phones and, on Sunday, the desktop.
 */
@OptIn(FlowPreview::class)
private fun CoroutineScope.keepSyncing(engine: CalendarSyncEngine, document: StateFlow<CalendarDocument>) {
    launch { engine.sync() }
    launch { CalendarSyncTrigger.requests.collect { engine.sync() } }
    launch {
        document.map { it.pendingPush.size + it.pendingDeletes.size }
            .filter { it > 0 }
            .debounce(PUSH_DEBOUNCE_MS)
            .collect { engine.sync() }
    }
}
