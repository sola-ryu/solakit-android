package com.phpizza.sola.ui.chat

import com.phpizza.sola.data.model.ChatMessage
import com.phpizza.sola.data.model.ChatSession

data class ChatUiState(
    val sessions: List<ChatSession> = emptyList(),
    val currentSessionId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isLoadingMessages: Boolean = false,
    val messageLoadError: String? = null,
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val isDrawerOpen: Boolean = false,
    val renamingSession: ChatSession? = null
) {
    val activeSession: ChatSession?
        get() = sessions.find { it.id == currentSessionId }
}
