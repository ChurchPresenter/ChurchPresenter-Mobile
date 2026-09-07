package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.SetlistEntryType
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The standalone running order — what "add to schedule" does when there is no
 * desktop schedule. Previously the action was swallowed and reported success.
 */
class ServiceOrderTest {

    private fun order(): Pair<ServiceOrder, LibraryRepository> {
        val repository = LibraryRepository(InMemoryFileStorage()) { 0L }
        return ServiceOrder(repository) to repository
    }

    @Test
    fun addingKeepsTheOrderItWasAddedIn() {
        val (order, _) = order()

        order.add(Song(number = "42", title = "Amazing Grace", localId = "song-a"))
        order.addPassage(reference = "John:3:16-18", title = "John 3:16-18")
        order.add(LocalAnnouncement(id = "ann-a", title = "Welcome"))

        assertEquals(listOf("Amazing Grace", "John 3:16-18", "Welcome"), order.current.map { it.title })
        assertEquals(
            listOf(SetlistEntryType.SONG, SetlistEntryType.BIBLE, SetlistEntryType.ANNOUNCEMENT),
            order.current.map { it.type },
        )
    }

    @Test
    fun aSongIsReferencedByItsLibraryId() {
        // The number is not unique enough to reopen the right song after an edit.
        val (order, _) = order()

        order.add(Song(number = "42", title = "Amazing Grace", localId = "song-a"))

        assertEquals("song-a", order.current.single().reference)
    }

    @Test
    fun aSongWithNoLibraryIdFallsBackToItsNumber() {
        val (order, _) = order()

        order.add(Song(number = "42", title = "Amazing Grace"))

        assertEquals("42", order.current.single().reference)
    }

    @Test
    fun anUntitledAnnouncementIsListedByItsFirstLine() {
        val (order, _) = order()

        order.add(LocalAnnouncement(id = "ann-a", body = "Coffee after the service\nIn the hall"))

        assertEquals("Coffee after the service", order.current.single().title)
    }

    @Test
    fun theSameSongCanBeSungTwice() {
        val (order, _) = order()
        val song = Song(number = "42", title = "Amazing Grace", localId = "song-a")

        order.add(song)
        order.add(song)

        assertEquals(2, order.current.size)
    }

    @Test
    fun removingTakesOutTheEntryAtThatPosition() {
        val (order, _) = order()
        order.add(LocalSong(id = "a", number = "1", title = "First"))
        order.add(LocalSong(id = "b", number = "2", title = "Second"))
        order.add(LocalSong(id = "c", number = "3", title = "Third"))

        order.removeAt(1)

        assertEquals(listOf("First", "Third"), order.current.map { it.title })
    }

    @Test
    fun movingRehearsesTheOrder() {
        val (order, _) = order()
        order.add(LocalSong(id = "a", number = "1", title = "First"))
        order.add(LocalSong(id = "b", number = "2", title = "Second"))
        order.add(LocalSong(id = "c", number = "3", title = "Third"))

        order.move(from = 2, to = 0)

        assertEquals(listOf("Third", "First", "Second"), order.current.map { it.title })
    }

    @Test
    fun anIndexOffTheListLeavesTheOrderAlone() {
        // A drag that ends outside the list must not move the item somewhere the
        // operator didn't drop it.
        val (order, _) = order()
        order.add(LocalSong(id = "a", number = "1", title = "First"))
        order.add(LocalSong(id = "b", number = "2", title = "Second"))

        order.move(from = 0, to = 7)
        order.move(from = -1, to = 0)
        order.removeAt(9)

        assertEquals(listOf("First", "Second"), order.current.map { it.title })
    }

    @Test
    fun clearingEmptiesItForTheNextService() {
        val (order, _) = order()
        order.add(LocalSong(id = "a", number = "1", title = "First"))

        order.clear()

        assertTrue(order.current.isEmpty())
    }

    @Test
    fun itSurvivesInTheLibraryItself() = runTest {
        // Persisted with everything else, so a restart mid-service doesn't lose
        // the running order — and the .cpset export already carries setlists.
        val (order, repository) = order()
        order.add(LocalSong(id = "a", number = "1", title = "First"))

        val stored = repository.setlist(CURRENT_SERVICE_SETLIST_ID)

        assertEquals(listOf("First"), stored?.entries?.map { it.title })
        assertEquals(listOf("First"), order.entries.first().map { it.title })
    }

    @Test
    fun theDrawerFollowsEveryChange() = runTest {
        val (order, _) = order()

        order.add(LocalSong(id = "a", number = "1", title = "First"))
        assertEquals(1, order.entries.first().size)

        order.add(LocalSong(id = "b", number = "2", title = "Second"))
        assertEquals(2, order.entries.first().size)

        order.clear()
        assertTrue(order.entries.first().isEmpty())
    }

    // ── Reordering the running order ─────────────────────────────────────
    //
    // Dragged on a phone, mid-service, by someone who has just been told the
    // order changed. The guards matter: a drag that lands outside the list must
    // leave it exactly as it was rather than dropping an item.

    private fun ServiceOrder.titles(): List<String> = current.map { it.title }

    private fun threeItems(): ServiceOrder {
        val (order, _) = order()
        order.add(Song(number = "1", title = "First", localId = "a"))
        order.add(Song(number = "2", title = "Second", localId = "b"))
        order.add(Song(number = "3", title = "Third", localId = "c"))
        return order
    }

    @Test
    fun anItemCanBeMovedTowardsTheStart() {
        val order = threeItems()

        order.move(2, 0)

        assertEquals(listOf("Third", "First", "Second"), order.titles())
    }

    @Test
    fun anItemCanBeMovedTowardsTheEnd() {
        val order = threeItems()

        order.move(0, 2)

        assertEquals(listOf("Second", "Third", "First"), order.titles())
    }

    @Test
    fun movingAnItemOntoItselfChangesNothing() {
        val order = threeItems()

        order.move(1, 1)

        assertEquals(listOf("First", "Second", "Third"), order.titles())
    }

    @Test
    fun movingFromOutsideTheListChangesNothing() {
        val order = threeItems()

        order.move(9, 0)
        order.move(-1, 0)

        assertEquals(listOf("First", "Second", "Third"), order.titles())
    }

    @Test
    fun movingToOutsideTheListChangesNothing() {
        // A drag released past the end must not drop the item.
        val order = threeItems()

        order.move(0, 9)
        order.move(0, -1)

        assertEquals(listOf("First", "Second", "Third"), order.titles())
    }

    @Test
    fun movingInAnEmptyOrderChangesNothing() {
        val (order, _) = order()

        order.move(0, 0)

        assertTrue(order.titles().isEmpty())
    }

    @Test
    fun adjacentItemsCanBeSwapped() {
        val order = threeItems()

        order.move(0, 1)

        assertEquals(listOf("Second", "First", "Third"), order.titles())
    }

    @Test
    fun aMoveSurvivesBeingReadBack() {
        // The order is persisted; a reorder that only lived in memory would be
        // lost the moment the screen was left.
        val repository = LibraryRepository(InMemoryFileStorage()) { 0L }
        val order = ServiceOrder(repository)
        order.add(Song(number = "1", title = "First", localId = "a"))
        order.add(Song(number = "2", title = "Second", localId = "b"))

        order.move(1, 0)

        assertEquals(listOf("Second", "First"), ServiceOrder(repository).current.map { it.title })
    }

    // ── Removing ─────────────────────────────────────────────────────────

    @Test
    fun removingAnItemLeavesTheRestInOrder() {
        val order = threeItems()

        order.removeAt(1)

        assertEquals(listOf("First", "Third"), order.titles())
    }

    @Test
    fun removingOutsideTheListChangesNothing() {
        val order = threeItems()

        order.removeAt(9)

        assertEquals(listOf("First", "Second", "Third"), order.titles())
    }

    @Test
    fun clearingEmptiesTheOrder() {
        val order = threeItems()

        order.clear()

        assertTrue(order.titles().isEmpty())
    }
}
