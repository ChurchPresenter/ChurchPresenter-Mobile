package com.church.presenter.churchpresentermobile.ui.library

import com.church.presenter.churchpresentermobile.model.CpsetError
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What the operator is told when a shared `.cpset` will not open.
 *
 * Each reason points at a different fix — re-export it, pick a different file,
 * update the app — so two of them sharing a message would send someone down the
 * wrong one, with a service about to start.
 */
class ImportErrorMessageTest {

    @Test
    fun `every reason has a message`() {
        for (error in CpsetError.entries) {
            assertEquals(error.messageResource(), error.messageResource(), error.name)
        }
    }

    @Test
    fun `no two reasons share a message`() {
        val messages = CpsetError.entries.map { it.messageResource() }

        assertEquals(messages.size, messages.toSet().size, "duplicated import error message: $messages")
    }

    @Test
    fun `a file that cannot be read is not confused with one in the wrong format`() {
        // The first means try again; the second means this is not a song set.
        assertEquals(
            false,
            CpsetError.UNREADABLE.messageResource() == CpsetError.WRONG_FORMAT.messageResource(),
        )
    }

    @Test
    fun `a set from a newer app is not confused with an empty one`() {
        // "Update the app" and "there is nothing in this file" are different
        // problems with different fixes.
        assertEquals(
            false,
            CpsetError.TOO_NEW.messageResource() == CpsetError.EMPTY.messageResource(),
        )
    }
}
