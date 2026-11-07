package com.phpizza.sola.data.auth

import android.content.Context
import android.content.SharedPreferences

class ServerPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sola_server_prefs", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value.trim()).apply()

    var apiToken: String
        get() = prefs.getString(KEY_API_TOKEN, DEFAULT_API_TOKEN) ?: DEFAULT_API_TOKEN
        set(value) = prefs.edit().putString(KEY_API_TOKEN, value.trim()).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val DEFAULT_SERVER_URL = "http://127.0.0.1:8382"
        const val DEFAULT_API_TOKEN = ""

        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_API_TOKEN = "api_token"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
}
