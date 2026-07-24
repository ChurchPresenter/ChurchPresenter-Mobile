package com.church.presenter.churchpresentermobile.network

object ApiConstants {
    /** Default host for real devices on the local network.
     *  Change this to the IP of the machine running ChurchPresenter Server
     *  on your local network (visible in the server's Settings screen). */
    const val DEFAULT_HOST = "192.168.1.100"
    /** Host used when running inside the Android emulator (maps to host machine). */
    const val EMULATOR_HOST = "10.0.2.2"
    const val DEFAULT_PORT = 8765

    // ── Network timeouts (milliseconds) ───────────────────────────────────
    // Kept short so an unreachable server fails fast instead of leaving
    // coroutines/threads blocked on socket connect — a long connect timeout
    // combined with launch-time fan-out is what triggered a background ANR.
    /** Socket-connect timeout for data and action requests. */
    const val CONNECT_TIMEOUT_MS = 4_000L
    /** Overall request timeout for data requests. */
    const val REQUEST_TIMEOUT_MS = 12_000L
    /** Socket read timeout for data requests. */
    const val SOCKET_TIMEOUT_MS = 15_000L
    /** Socket-connect timeout for the persistent WebSocket connection. */
    const val WS_CONNECT_TIMEOUT_MS = 4_000L

    const val SONGS_ENDPOINT = "songs"
    const val SONG_SELECT_ENDPOINT = "select"
    const val PROJECT_ENDPOINT = "project"
    const val SCHEDULE_ADD_ENDPOINT = "schedule/add"
    const val SCHEDULE_ADD_BATCH_ENDPOINT = "schedule/add-batch"
    const val BIBLE_ENDPOINT = "bible"
    const val BIBLE_SELECT_ENDPOINT = "bible/select"
    const val PRESENTATIONS_ENDPOINT = "presentations"
    const val PRESENTATION_SELECT_ENDPOINT = "select"
    const val PRESENTATIONS_UPLOAD_ENDPOINT = "presentations/upload"
    const val PICTURES_ENDPOINT = "pictures"
    const val PICTURES_SELECT_ENDPOINT = "pictures/select"
    const val PICTURES_UPLOAD_ENDPOINT = "pictures/upload"
    const val MEDIA_UPLOAD_ENDPOINT = "media/upload"
    const val SCHEDULE_ENDPOINT = "schedule"
    const val CLEAR_ENDPOINT = "clear"
    const val STATUS_ENDPOINT = "status"
    const val DICTIONARY_ENDPOINT       = "dictionary"
    const val QA_STATUS_ENDPOINT        = "qa/status"
    const val QA_QUESTIONS_ENDPOINT     = "qa/questions"
    const val QA_ADD_ENDPOINT           = "qa/add"
    const val QA_CLEAR_DISPLAY_ENDPOINT = "qa/clear-display"
    const val API_KEY_HEADER        = "X-Api-Key"
    /**
     * Header the server's Q&A admin endpoints authenticate with (see the desktop
     * `CompanionServer.checkQaAdmin`). When the server has the API key enabled it
     * sets the Q&A admin password to the API key, so we send the same value here.
     */
    const val QA_ADMIN_PASSWORD_HEADER = "X-QA-Password"
    const val DEVICE_ID_HEADER      = "X-Device-Id"
    const val APP_VERSION_HEADER    = "X-App-Version"
    const val SERVER_VERSION_HEADER = "X-Server-Version"
}
