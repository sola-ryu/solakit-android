package com.phpizza.sola.data.repository

import com.google.gson.JsonObject
import com.phpizza.sola.data.model.ChatMessage
import com.phpizza.sola.data.model.ChatSession
import com.phpizza.sola.data.model.MessageSender
import com.phpizza.sola.data.network.ApiClient
import com.phpizza.sola.data.network.SolakitApiService
import com.phpizza.sola.data.network.model.CreateSessionRequestDto
import com.phpizza.sola.data.network.model.RunRequestDto
import com.phpizza.sola.data.network.model.UpdateSessionRequestDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class NetworkChatRepository(
    private val apiServiceSupplier: () -> SolakitApiService = { ApiClient.getService() },
    private val fallbackLocalRepo: ChatRepository = InMemoryChatRepository()
) : ChatRepository {

    private val sessionsState = MutableStateFlow<List<ChatSession>>(emptyList())
    private val messagesState = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())

    override fun getSessions(): Flow<List<ChatSession>> {
        return flow {
            val apiSessions = try {
                apiServiceSupplier().getSessions()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                null
            }

            if (apiSessions != null) {
                val mapped = apiSessions.map { dto ->
                    ChatSession(
                        id = dto.id,
                        title = dto.title ?: "Chat Session",
                        createdAt = parseTimestamp(dto.createdAt),
                        lastUpdatedAt = parseTimestamp(dto.createdAt),
                        source = dto.source
                    )
                }
                sessionsState.value = mapped
                emitAll(sessionsState)
            } else {
                fallbackLocalRepo.getSessions().collect { emit(it) }
            }
        }
    }

    override fun getMessages(sessionId: String): Flow<List<ChatMessage>> {
        return flow {
            val api = apiServiceSupplier()
            val runs = api.getRuns(sessionId).sortedBy { it.get("created_at")?.asString.orEmpty() }
            val history = runs.flatMap { run ->
                val runId = run.get("id").asString
                val record = api.getRunRecord(runId)
                record.getAsJsonArray("messages")?.mapIndexedNotNull { index, element ->
                    val message = element.asJsonObject
                    val sender = when (message.get("role")?.asString) {
                        "user" -> MessageSender.USER
                        "assistant" -> MessageSender.ASSISTANT
                        else -> return@mapIndexedNotNull null
                    }
                    val content = message.get("content")
                        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                        ?.asString?.takeIf { it.isNotBlank() }
                        ?: return@mapIndexedNotNull null
                    ChatMessage(
                        id = "$runId:$index",
                        sessionId = sessionId,
                        sender = sender,
                        content = content,
                        timestamp = parseTimestamp(message.get("ts")?.takeUnless { it.isJsonNull }?.asString)
                    )
                }.orEmpty()
            }
            messagesState.value = messagesState.value + (sessionId to history)
            emitAll(messagesState.asStateFlow().map { it[sessionId].orEmpty() })
        }
    }

    override suspend fun createSession(title: String): ChatSession {
        return try {
            val dto = apiServiceSupplier().createSession(
                CreateSessionRequestDto(title = title, source = "api")
            )
            val session = ChatSession(
                id = dto.id,
                title = dto.title ?: title,
                createdAt = parseTimestamp(dto.createdAt),
                lastUpdatedAt = parseTimestamp(dto.createdAt),
                        source = dto.source
            )
            sessionsState.value = listOf(session) + sessionsState.value.filterNot { it.id == session.id }
            messagesState.value = messagesState.value + (session.id to emptyList())
            session
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            fallbackLocalRepo.createSession(title)
        }
    }

    override suspend fun renameSession(sessionId: String, newTitle: String) {
        try {
            apiServiceSupplier().updateSession(sessionId, UpdateSessionRequestDto(title = newTitle))
            sessionsState.value = sessionsState.value.map {
                if (it.id == sessionId) it.copy(title = newTitle, lastUpdatedAt = System.currentTimeMillis()) else it
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            fallbackLocalRepo.renameSession(sessionId, newTitle)
        }
    }

    override suspend fun deleteSession(sessionId: String) {
        try {
            apiServiceSupplier().archiveSession(sessionId)
            sessionsState.value = sessionsState.value.filterNot { it.id == sessionId }
            messagesState.value = messagesState.value - sessionId
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            fallbackLocalRepo.deleteSession(sessionId)
        }
    }

    override suspend fun clearSessionMessages(sessionId: String) {
        messagesState.value = messagesState.value + (sessionId to emptyList())
    }

    override suspend fun addUserMessage(sessionId: String, content: String): ChatMessage {
        val userMsg = ChatMessage(
            sessionId = sessionId,
            sender = MessageSender.USER,
            content = content
        )
        val currentList = messagesState.value[sessionId] ?: emptyList()
        messagesState.value = messagesState.value + (sessionId to (currentList + userMsg))
        return userMsg
    }

    override fun generateAiResponse(sessionId: String, prompt: String): Flow<String> = flow {
        val assistantMsgId = UUID.randomUUID().toString()

        val responseObj: JsonObject? = try {
            apiServiceSupplier().sendMessage(
                sessionId = sessionId,
                request = RunRequestDto(message = prompt)
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }

        if (responseObj != null) {
            val outputContent = extractTextFromResponse(responseObj)
            val words = outputContent.split(" ")
            val currentText = StringBuilder()

            for (i in words.indices) {
                if (i > 0) currentText.append(" ")
                currentText.append(words[i])

                val assistantMsg = ChatMessage(
                    id = assistantMsgId,
                    sessionId = sessionId,
                    sender = MessageSender.ASSISTANT,
                    content = currentText.toString(),
                    isStreaming = i < words.size - 1
                )
                val updatedList = (messagesState.value[sessionId] ?: emptyList()).filterNot { it.id == assistantMsgId }
                messagesState.value = messagesState.value + (sessionId to (updatedList + assistantMsg))

                emit(currentText.toString())
                delay(30L)
            }
        } else {
            fallbackLocalRepo.generateAiResponse(sessionId, prompt).collect { text ->
                emit(text)
            }
        }
    }

    private fun parseTimestamp(dateStr: String?): Long {
        if (dateStr == null) return System.currentTimeMillis()
        return try {
            java.time.Instant.parse(dateStr).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun extractTextFromResponse(jsonObj: JsonObject): String {
        return when {
            jsonObj.has("output") -> jsonObj.get("output").asString
            jsonObj.has("result") -> jsonObj.get("result").asString
            jsonObj.has("message") -> jsonObj.get("message").asString
            jsonObj.has("content") -> jsonObj.get("content").asString
            else -> jsonObj.toString()
        }
    }
}
