package com.phpizza.sola.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phpizza.sola.data.auth.ServerPreferences
import com.phpizza.sola.data.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = ServerPreferences(application)

    private val _uiState = MutableStateFlow(
        LoginUiState(
            serverUrl = prefs.serverUrl,
            apiToken = prefs.apiToken,
            rememberConnection = prefs.isLoggedIn
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onServerUrlChanged(newUrl: String) {
        _uiState.update { it.copy(serverUrl = newUrl, errorMessage = null) }
    }

    fun onApiTokenChanged(newToken: String) {
        _uiState.update { it.copy(apiToken = newToken, errorMessage = null) }
    }

    fun toggleTokenVisibility() {
        _uiState.update { it.copy(isTokenVisible = !it.isTokenVisible) }
    }

    fun onRememberConnectionChanged(remember: Boolean) {
        _uiState.update { it.copy(rememberConnection = remember) }
    }

    fun connect() {
        if (_uiState.value.isLoading) return
        val url = _uiState.value.serverUrl.trim()
        val token = _uiState.value.apiToken.trim()

        val parsedUrl = okhttp3.HttpUrl.Companion.run { url.toHttpUrlOrNull() }
        if (parsedUrl == null) {
            _uiState.update { it.copy(errorMessage = "Enter a valid HTTP or HTTPS endpoint URL") }
            return
        }

        if (token.isBlank()) {
            _uiState.update { it.copy(errorMessage = "API token cannot be empty") }
            return
        }
        val remember = _uiState.value.rememberConnection
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                // Configure ApiClient with specified URL and Token
                ApiClient.configure(url, token)

                // Test connection by retrieving sessions list or verifying API
                val apiService = ApiClient.getService(url, token)
                apiService.getSessions(limit = 1)

                // Save credentials if remember option is enabled
                if (remember) {
                    prefs.serverUrl = url
                    prefs.apiToken = token
                    prefs.isLoggedIn = true
                } else {
                    prefs.clear()
                }

                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Network check failed - surface user-friendly message
                val errorMsg = when {
                    e.message?.contains("ConnectException", ignoreCase = true) == true ->
                        "Could not connect to $url. Check server status and network."
                    e.message?.contains("401") == true || e.message?.contains("403") == true ->
                        "Authentication failed. Please check your API token."
                    else -> "Connection test finished: ${e.localizedMessage ?: "Unknown error"}"
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMsg,
                        isSuccess = false
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
