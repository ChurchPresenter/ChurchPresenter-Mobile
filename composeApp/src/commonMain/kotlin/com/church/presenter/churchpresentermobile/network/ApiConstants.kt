package com.church.presenter.churchpresentermobile.network

object ApiConstants {
    /** Default host for real devices on the local network.
     *  Change this to the IP of the machine running ChurchPresenter Server
     *  on your local network (visible in the server's Settings screen). */
    const val DEFAULT_HOST = "192.168.1.100"
    /** Host used when running inside the Android emulator (maps to host machine). */
    const val EMULATOR_HOST = "10.0.2.2"
    const val DEFAULT_PORT = 8765
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
    const val SCHEDULE_ENDPOINT = "schedule"
    const val CLEAR_ENDPOINT = "clear"
    const val API_KEY_HEADER = "X-Api-Key"
    const val DEVICE_ID_HEADER = "X-Device-Id"
}
