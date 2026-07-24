package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests [QuestionDto.toQuestion], especially the status enum fallback. */
class QAModelsTest {

    private fun dto(status: String) = QuestionDto(
        id = "q1",
        text = "Why?",
        submitterName = "Ada",
        timestamp = 123L,
        status = status,
        upvotes = 4,
        downvotes = 1,
    )

    @Test
    fun mapsAllScalarFields() {
        val q = dto("PENDING").toQuestion()
        assertEquals("q1", q.id)
        assertEquals("Why?", q.text)
        assertEquals("Ada", q.submitterName)
        assertEquals(123L, q.timestamp)
        assertEquals(4, q.upvotes)
        assertEquals(1, q.downvotes)
    }

    @Test
    fun mapsEachValidStatus() {
        assertEquals(QuestionStatus.PENDING, dto("PENDING").toQuestion().status)
        assertEquals(QuestionStatus.APPROVED, dto("APPROVED").toQuestion().status)
        assertEquals(QuestionStatus.DENIED, dto("DENIED").toQuestion().status)
        assertEquals(QuestionStatus.DONE, dto("DONE").toQuestion().status)
    }

    @Test
    fun unknownOrWrongCaseStatusFallsBackToPending() {
        assertEquals(QuestionStatus.PENDING, dto("something-else").toQuestion().status)
        assertEquals(QuestionStatus.PENDING, dto("approved").toQuestion().status) // valueOf is case-sensitive
        assertEquals(QuestionStatus.PENDING, dto("").toQuestion().status)
    }
}
