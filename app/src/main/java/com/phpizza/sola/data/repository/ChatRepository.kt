package com.phpizza.sola.data.repository

import com.phpizza.sola.data.model.ChatMessage
import com.phpizza.sola.data.model.ChatSession
import com.phpizza.sola.data.model.MessageSender
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface ChatRepository {
    fun getSessions(): Flow<List<ChatSession>>
    fun getMessages(sessionId: String): Flow<List<ChatMessage>>
    suspend fun createSession(title: String = "New Chat"): ChatSession
    suspend fun renameSession(sessionId: String, newTitle: String)
    suspend fun deleteSession(sessionId: String)
    suspend fun clearSessionMessages(sessionId: String)
    suspend fun addUserMessage(sessionId: String, content: String): ChatMessage
    fun generateAiResponse(sessionId: String, prompt: String): Flow<String>
}

class InMemoryChatRepository : ChatRepository {

    private val sessionsState = MutableStateFlow<List<ChatSession>>(emptyList())
    private val messagesState = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())

    init {
        // Seed default sample data for initial demonstration
        val defaultSession = ChatSession(
            id = "session_welcome",
            title = "Welcome & Jetpack Compose",
            createdAt = System.currentTimeMillis() - 3600000,
            lastUpdatedAt = System.currentTimeMillis()
        )
        val sampleMessages = listOf(
            ChatMessage(
                id = "msg_1",
                sessionId = defaultSession.id,
                sender = MessageSender.USER,
                content = "Hi! How can I build modern Android UIs with Jetpack Compose?",
                timestamp = System.currentTimeMillis() - 3500000
            ),
            ChatMessage(
                id = "msg_2",
                sessionId = defaultSession.id,
                sender = MessageSender.ASSISTANT,
                content = "Hello! Jetpack Compose is Android's modern toolkit for building native UI. It simplifies UI development with a declarative approach, powerful state management, and seamless Material 3 integration.\n\nYou can compose layouts using functions like Column, Row, and LazyColumn, backed by State and ViewModels!",
                timestamp = System.currentTimeMillis() - 3400000
            )
        )

        sessionsState.value = listOf(defaultSession)
        messagesState.value = mapOf(defaultSession.id to sampleMessages)
    }

    override fun getSessions(): Flow<List<ChatSession>> {
        return sessionsState.asStateFlow().map { list ->
            list.sortedByDescending { it.lastUpdatedAt }
        }
    }

    override fun getMessages(sessionId: String): Flow<List<ChatMessage>> {
        return messagesState.asStateFlow().map { map ->
            map[sessionId] ?: emptyList()
        }
    }

    override suspend fun createSession(title: String): ChatSession {
        val newSession = ChatSession(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis()
        )
        sessionsState.value = listOf(newSession) + sessionsState.value
        messagesState.value = messagesState.value + (newSession.id to emptyList())
        return newSession
    }

    override suspend fun renameSession(sessionId: String, newTitle: String) {
        sessionsState.value = sessionsState.value.map { session ->
            if (session.id == sessionId) {
                session.copy(title = newTitle, lastUpdatedAt = System.currentTimeMillis())
            } else {
                session
            }
        }
    }

    override suspend fun deleteSession(sessionId: String) {
        sessionsState.value = sessionsState.value.filterNot { it.id == sessionId }
        messagesState.value = messagesState.value - sessionId
    }

    override suspend fun clearSessionMessages(sessionId: String) {
        messagesState.value = messagesState.value + (sessionId to emptyList())
    }

    override suspend fun addUserMessage(sessionId: String, content: String): ChatMessage {
        val message = ChatMessage(
            sessionId = sessionId,
            sender = MessageSender.USER,
            content = content
        )

        val currentList = messagesState.value[sessionId] ?: emptyList()
        messagesState.value = messagesState.value + (sessionId to (currentList + message))

        // Update session title if it's the first user message or default
        val session = sessionsState.value.find { it.id == sessionId }
        if (session != null && (session.title == "New Chat" || currentList.isEmpty())) {
            val autoTitle = if (content.length > 28) content.take(28) + "..." else content
            renameSession(sessionId, autoTitle)
        } else if (session != null) {
            sessionsState.value = sessionsState.value.map {
                if (it.id == sessionId) it.copy(lastUpdatedAt = System.currentTimeMillis()) else it
            }
        }

        return message
    }

    override fun generateAiResponse(sessionId: String, prompt: String): Flow<String> = flow {
        val responseTemplate = getAiResponseForPrompt(prompt)
        val words = responseTemplate.split(" ")
        val currentText = StringBuilder()

        val assistantMsgId = UUID.randomUUID().toString()

        for (i in words.indices) {
            if (i > 0) currentText.append(" ")
            currentText.append(words[i])

            // Update state with partial message during streaming
            val fullMessage = ChatMessage(
                id = assistantMsgId,
                sessionId = sessionId,
                sender = MessageSender.ASSISTANT,
                content = currentText.toString(),
                isStreaming = i < words.size - 1
            )
            val currentList = (messagesState.value[sessionId] ?: emptyList()).filterNot { it.id == assistantMsgId }
            messagesState.value = messagesState.value + (sessionId to (currentList + fullMessage))

            emit(currentText.toString())
            delay(40L + (words[i].length * 5L).coerceAtMost(100L))
        }

        // Final update session timestamp
        sessionsState.value = sessionsState.value.map {
            if (it.id == sessionId) it.copy(lastUpdatedAt = System.currentTimeMillis()) else it
        }
    }

    private fun getAiResponseForPrompt(prompt: String): String {
        val p = prompt.lowercase()
        return when {
            "kotlin" in p -> "Kotlin is a modern, concise, and safe programming language. Key features include null safety, coroutines for asynchronous programming, extension functions, and first-class functional programming support."
            "compose" in p || "ui" in p -> "Jetpack Compose relies on declarative composable functions. You describe what the UI should look like for a given state, and Compose handles recomposition when that state changes."
            "hello" in p || "hi" in p -> "Hello there! How can I assist you with your Android project or AI features today?"
            "architecture" in p || "viewmodel" in p -> "In Android development, MVVM or MVI with StateFlow and Coroutines provides a robust clean architecture. ViewModels hold state safely across configuration changes, while Repository patterns isolate data sources."
            "idea" in p || "brainstorm" in p -> "Here are 3 app ideas:\n1. Personal Knowledge Assistant with local AI.\n2. Smart Workout & Habit Tracker.\n3. AI Voice Note Summarizer with Tagging."
            else -> "That's an interesting topic regarding \"$prompt\". As an AI assistant, I can help you write code, design user interfaces, outline architectural patterns, or answer general questions. What specific area would you like to explore further?"
        }
    }
}
