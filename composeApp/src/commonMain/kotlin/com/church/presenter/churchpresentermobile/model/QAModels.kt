package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.Serializable

enum class QuestionStatus { PENDING, APPROVED, DENIED, DONE }

data class Question(
    val id: String,
    val text: String,
    val submitterName: String = "",
    val timestamp: Long,
    val status: QuestionStatus = QuestionStatus.PENDING,
    val upvotes: Int = 0,
    val downvotes: Int = 0
)

@Serializable
data class QuestionDto(
    val id: String,
    val text: String,
    val submitterName: String = "",
    val timestamp: Long,
    val status: String,
    val upvotes: Int = 0,
    val downvotes: Int = 0
)

@Serializable
data class QAStatusResponse(
    val sessionActive: Boolean,
    val cooldownSeconds: Int = 30,
    val displayedQuestionId: String = "",
    val votingEnabled: Boolean = false
)

@Serializable
data class QATextRequest(val text: String, val name: String = "")

fun QuestionDto.toQuestion() = Question(
    id = id,
    text = text,
    submitterName = submitterName,
    timestamp = timestamp,
    status = try { QuestionStatus.valueOf(status) } catch (_: Exception) { QuestionStatus.PENDING },
    upvotes = upvotes,
    downvotes = downvotes
)
