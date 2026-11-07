package com.phpizza.sola.data

import com.google.gson.JsonObject
import com.phpizza.sola.data.network.SolakitApiService
import com.phpizza.sola.data.network.model.ApiSessionDto
import com.phpizza.sola.data.network.model.CreateSessionRequestDto
import com.phpizza.sola.data.network.model.RunRequestDto
import com.phpizza.sola.data.network.model.UpdateSessionRequestDto
import com.phpizza.sola.data.repository.NetworkChatRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkChatRepositoryTest {

    private lateinit var fakeApiService: FakeSolakitApiService
    private lateinit var repository: NetworkChatRepository

    @Before
    fun setUp() {
        fakeApiService = FakeSolakitApiService()
        repository = NetworkChatRepository(
            apiServiceSupplier = { fakeApiService }
        )
    }

    @Test
    fun getSessions_fetchesFromNetwork() = runBlocking {
        fakeApiService.sessions.add(ApiSessionDto(id = "s_100", title = "Network Session"))

        val sessions = repository.getSessions().first()
        assertEquals(1, sessions.size)
        assertEquals("s_100", sessions.first().id)
        assertEquals("Network Session", sessions.first().title)
    }

    @Test
    fun createSession_callsApiAndReturnsSession() = runBlocking {
        val created = repository.createSession("API Chat")

        assertNotNull(created)
        assertEquals("API Chat", created.title)
        assertEquals(1, fakeApiService.createdSessions.size)
        assertEquals("API Chat", fakeApiService.createdSessions.first().title)
    }

    @Test
    fun renameSession_updatesTitleViaApi() = runBlocking {
        val created = repository.createSession("Original Title")
        repository.renameSession(created.id, "Renamed API Chat")

        assertEquals("Renamed API Chat", fakeApiService.sessions.find { it.id == created.id }?.title)
    }

    @Test
    fun deleteSession_archivesSessionViaApi() = runBlocking {
        val created = repository.createSession("To Archive")
        repository.deleteSession(created.id)

        assertTrue(fakeApiService.archivedSessionIds.contains(created.id))
    }

    @Test
    fun generateAiResponse_streamsResponseFromApi() = runBlocking {
        val session = repository.createSession("Run Session")
        val emissions = repository.generateAiResponse(session.id, "Hello AI").toList()

        assertTrue(emissions.isNotEmpty())
        assertEquals("Hello from API!", emissions.last())
    }

    private class FakeSolakitApiService : SolakitApiService {
        val sessions = mutableListOf<ApiSessionDto>()
        val createdSessions = mutableListOf<CreateSessionRequestDto>()
        val archivedSessionIds = mutableListOf<String>()

        override suspend fun getSessions(
            status: String,
            query: String?,
            limit: Int,
            authorization: String?
        ): List<ApiSessionDto> = sessions

        override suspend fun createSession(
            request: CreateSessionRequestDto,
            authorization: String?
        ): ApiSessionDto {
            createdSessions.add(request)
            val newSession = ApiSessionDto(
                id = request.id ?: "s_${System.currentTimeMillis()}",
                title = request.title,
                source = request.source,
                createdAt = "2026-03-01T12:00:00Z"
            )
            sessions.add(newSession)
            return newSession
        }

        override suspend fun getSession(sessionId: String, authorization: String?): ApiSessionDto {
            return sessions.first { it.id == sessionId }
        }

        override suspend fun updateSession(
            sessionId: String,
            request: UpdateSessionRequestDto,
            authorization: String?
        ): ApiSessionDto {
            val index = sessions.indexOfFirst { it.id == sessionId }
            val updated = sessions[index].copy(title = request.title)
            sessions[index] = updated
            return updated
        }

        override suspend fun archiveSession(sessionId: String, authorization: String?): JsonObject {
            archivedSessionIds.add(sessionId)
            sessions.removeAll { it.id == sessionId }
            return JsonObject().apply { addProperty("status", "archived") }
        }

        override suspend fun sendMessage(
            sessionId: String,
            request: RunRequestDto,
            wait: Boolean,
            stream: Boolean,
            authorization: String?
        ): JsonObject {
            return JsonObject().apply {
                addProperty("output", "Hello from API!")
            }
        }

        override suspend fun getRuns(sessionId: String?, authorization: String?): List<JsonObject> = emptyList()

        override suspend fun getRunRecord(runId: String, messages: Boolean, authorization: String?): JsonObject = JsonObject()
    }
}
