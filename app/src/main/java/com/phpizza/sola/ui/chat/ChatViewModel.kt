package com.phpizza.sola.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phpizza.sola.data.model.ChatSession
import com.phpizza.sola.data.repository.ChatRepository
import com.phpizza.sola.data.repository.NetworkChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import com.phpizza.sola.data.model.ChatMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val repository: ChatRepository = NetworkChatRepository()
) : ViewModel() {

    private val currentSessionIdState = MutableStateFlow<String?>(null)
    private val inputTextState = MutableStateFlow("")
    private val isGeneratingState = MutableStateFlow(false)
    private val isDrawerOpenState = MutableStateFlow(false)
    private val renamingSessionState = MutableStateFlow<ChatSession?>(null)

    private var generationJob: Job? = null

    private val sessionsFlow = repository.getSessions()

    private val messageRefreshState = MutableStateFlow(0)
    private data class MessageLoad(
        val sessionId: String? = null,
        val messages: List<ChatMessage> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null
    )

    private val messagesFlow = combine(currentSessionIdState, messageRefreshState) { id, _ -> id }
        .flatMapLatest { sessionId ->
            if (sessionId == null) {
                flowOf(MessageLoad())
            } else {
                repository.getMessages(sessionId)
                    .map { MessageLoad(sessionId, it) }
                    .onStart { emit(MessageLoad(sessionId, loading = true)) }
                    .catch { error ->
                        if (error is CancellationException) throw error
                        emit(MessageLoad(sessionId, error = "Could not load messages. Check your connection and try again."))
                    }
            }
        }

    fun retryMessages() {
        messageRefreshState.value += 1
    }

    private val sessionAndMessagesFlow = combine(
        sessionsFlow,
        currentSessionIdState,
        messagesFlow
    ) { sessions, currentId, messages ->
        Triple(sessions, currentId, messages)
    }

    private val uiControlStateFlow = combine(
        inputTextState,
        isGeneratingState,
        isDrawerOpenState,
        renamingSessionState
    ) { inputText, isGenerating, isDrawerOpen, renaming ->
        UiControlState(inputText, isGenerating, isDrawerOpen, renaming)
    }

    val uiState: StateFlow<ChatUiState> = combine(
        sessionAndMessagesFlow,
        uiControlStateFlow
    ) { (sessions, currentId, messages), uiControl ->

        val effectiveSessionId = currentId ?: sessions.firstOrNull { !it.source.equals("cron", ignoreCase = true) }?.id

        ChatUiState(
            sessions = sessions,
            currentSessionId = effectiveSessionId,
            messages = if (messages.sessionId == currentId) messages.messages else emptyList(),
            isLoadingMessages = messages.sessionId != currentId || messages.loading,
            messageLoadError = if (messages.sessionId == currentId) messages.error else null,
            inputText = uiControl.inputText,
            isGenerating = uiControl.isGenerating,
            isDrawerOpen = uiControl.isDrawerOpen,
            renamingSession = uiControl.renaming
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
    )

    init {
        // Auto-select first session if available
        viewModelScope.launch {
            sessionsFlow.collect { sessions ->
                if (currentSessionIdState.value == null) {
                    currentSessionIdState.value = sessions.firstOrNull {
                        !it.source.equals("cron", ignoreCase = true)
                    }?.id
                }
            }
        }
    }

    fun onInputTextChanged(newText: String) {
        inputTextState.value = newText
    }

    fun sendMessage(textToSend: String? = null) {
        val content = textToSend ?: inputTextState.value.trim()
        if (content.isBlank() || isGeneratingState.value || uiState.value.isLoadingMessages ||
            uiState.value.messageLoadError != null) return

        val sessionId = uiState.value.currentSessionId
        viewModelScope.launch {
            val targetSessionId = if (sessionId == null) {
                val newSession = repository.createSession()
                currentSessionIdState.value = newSession.id
                newSession.id
            } else {
                sessionId
            }

            // Clear text input
            inputTextState.value = ""

            // Add user message
            repository.addUserMessage(targetSessionId, content)

            // Start AI generation
            isGeneratingState.value = true
            generationJob = launch {
                try {
                    repository.generateAiResponse(targetSessionId, content).collect {
                        // AI streaming response progress updated in repository state
                    }
                } finally {
                    isGeneratingState.value = false
                }
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        generationJob = null
        isGeneratingState.value = false
    }

    fun selectSession(sessionId: String) {
        if (currentSessionIdState.value != sessionId) {
            stopGeneration()
            currentSessionIdState.value = sessionId
            setDrawerOpen(false)
        }
    }

    fun createNewSession() {
        stopGeneration()
        viewModelScope.launch {
            val newSession = repository.createSession("New Chat")
            currentSessionIdState.value = newSession.id
            setDrawerOpen(false)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (currentSessionIdState.value == sessionId) {
                val remaining = uiState.value.sessions.filterNot {
                    it.id == sessionId || it.source.equals("cron", ignoreCase = true)
                }
                if (remaining.isNotEmpty()) {
                    currentSessionIdState.value = remaining.first().id
                } else {
                    val newSession = repository.createSession("New Chat")
                    currentSessionIdState.value = newSession.id
                }
            }
        }
    }

    fun startRenameSession(session: ChatSession) {
        renamingSessionState.value = session
    }

    fun confirmRenameSession(newTitle: String) {
        val target = renamingSessionState.value ?: return
        if (newTitle.isNotBlank()) {
            viewModelScope.launch {
                repository.renameSession(target.id, newTitle.trim())
                renamingSessionState.value = null
            }
        }
    }

    fun cancelRenameSession() {
        renamingSessionState.value = null
    }

    fun clearCurrentSessionHistory() {
        val sessionId = uiState.value.currentSessionId ?: return
        viewModelScope.launch {
            repository.clearSessionMessages(sessionId)
        }
    }

    fun setDrawerOpen(isOpen: Boolean) {
        isDrawerOpenState.value = isOpen
    }

    private data class UiControlState(
        val inputText: String,
        val isGenerating: Boolean,
        val isDrawerOpen: Boolean,
        val renaming: ChatSession?
    )
}
