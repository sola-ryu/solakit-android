package com.phpizza.sola.ui.login

data class LoginUiState(
    val serverUrl: String = "http://127.0.0.1:8382",
    val apiToken: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val isTokenVisible: Boolean = false,
    val rememberConnection: Boolean = true
)
