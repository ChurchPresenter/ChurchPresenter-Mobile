package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.QAStatusResponse
import com.church.presenter.churchpresentermobile.model.QATextRequest
import com.church.presenter.churchpresentermobile.model.Question
import com.church.presenter.churchpresentermobile.model.QuestionDto
import com.church.presenter.churchpresentermobile.model.toQuestion
import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "QAService"
private val json = Json { ignoreUnknownKeys = true; isLenient = true }

class QAService(
    private val settings: AppSettings,
    private val client: HttpClient = createHttpClient(),
) {
    private fun HttpRequestBuilder.applyApiKey() {
        val key = settings.apiKey
        if (key.isNotBlank()) {
            header(ApiConstants.API_KEY_HEADER, key)
            // The Q&A admin endpoints authenticate via X-QA-Password (checkQaAdmin),
            // which the server sets equal to the API key when the key is enabled.
            // Without this the admin routes return 401 "Invalid admin password".
            header(ApiConstants.QA_ADMIN_PASSWORD_HEADER, key)
        }
        header(ApiConstants.DEVICE_ID_HEADER, settings.deviceId)
    }

    private fun baseUrl() = settings.apiBaseUrl

    suspend fun fetchStatus(): Result<QAStatusResponse> = apiRunCatching {
        val url = "${baseUrl()}/${ApiConstants.QA_STATUS_ENDPOINT}"
        Logger.d(TAG, "fetchStatus — GET $url")
        val response = client.get(url) { applyApiKey() }
        val raw = response.bodyAsText()
        if (!response.status.isSuccess()) throw Exception("HTTP ${response.status.value}")
        json.decodeFromString<QAStatusResponse>(raw)
    }

    suspend fun fetchQuestions(): Result<List<Question>> = apiRunCatching {
        val url = "${baseUrl()}/${ApiConstants.QA_QUESTIONS_ENDPOINT}"
        Logger.d(TAG, "fetchQuestions — GET $url")
        val response = client.get(url) { applyApiKey() }
        val raw = response.bodyAsText()
        if (!response.status.isSuccess()) throw Exception("HTTP ${response.status.value}")
        json.decodeFromString<List<QuestionDto>>(raw).map { it.toQuestion() }
    }

    suspend fun approveQuestion(id: String): Result<Unit> = apiRunCatching {
        val response = client.post("${baseUrl()}/${ApiConstants.QA_QUESTIONS_ENDPOINT}/$id/approve") { applyApiKey() }
        if (!response.status.isSuccess()) throw Exception("HTTP ${response.status.value}")
    }

    suspend fun denyQuestion(id: String): Result<Unit> = apiRunCatching {
        val response = client.post("${baseUrl()}/${ApiConstants.QA_QUESTIONS_ENDPOINT}/$id/deny") { applyApiKey() }
        if (!response.status.isSuccess()) throw Exception("HTTP ${response.status.value}")
    }

    suspend fun editQuestion(id: String, newText: String): Result<Unit> = apiRunCatching {
        val response = client.post("${baseUrl()}/${ApiConstants.QA_QUESTIONS_ENDPOINT}/$id/edit") {
            applyApiKey()
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(QATextRequest(newText)))
        }
        if (!response.status.isSuccess()) throw Exception("HTTP ${response.status.value}")
    }

    suspend fun markDone(id: String): Result<Unit> = apiRunCatching {
        val response = client.post("${baseUrl()}/${ApiConstants.QA_QUESTIONS_ENDPOINT}/$id/done") { applyApiKey() }
        if (!response.status.isSuccess()) throw Exception("HTTP ${response.status.value}")
    }

    suspend fun displayQuestion(id: String): Result<Unit> = apiRunCatching {
        val response = client.post("${baseUrl()}/${ApiConstants.QA_QUESTIONS_ENDPOINT}/$id/display") { applyApiKey() }
        if (!response.status.isSuccess()) throw Exception("HTTP ${response.status.value}")
    }

    suspend fun deleteQuestion(id: String): Result<Unit> = apiRunCatching {
        val response = client.delete("${baseUrl()}/${ApiConstants.QA_QUESTIONS_ENDPOINT}/$id") { applyApiKey() }
        if (!response.status.isSuccess()) throw Exception("HTTP ${response.status.value}")
    }

    suspend fun addQuestion(text: String, name: String = ""): Result<Question> = apiRunCatching {
        val response = client.post("${baseUrl()}/${ApiConstants.QA_ADD_ENDPOINT}") {
            applyApiKey()
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(QATextRequest(text, name)))
        }
        val raw = response.bodyAsText()
        if (!response.status.isSuccess()) throw Exception("HTTP ${response.status.value}")
        json.decodeFromString<QuestionDto>(raw).toQuestion()
    }

    suspend fun clearDisplay(): Result<Unit> = apiRunCatching {
        val response = client.post("${baseUrl()}/${ApiConstants.QA_CLEAR_DISPLAY_ENDPOINT}") { applyApiKey() }
        if (!response.status.isSuccess()) throw Exception("HTTP ${response.status.value}")
    }

    fun closeClient() = client.close()
}
