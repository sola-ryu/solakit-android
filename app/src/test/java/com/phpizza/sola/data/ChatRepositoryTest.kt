package com.phpizza.sola.data

import com.phpizza.sola.data.repository.InMemoryChatRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChatRepositoryTest {

    private lateinit var repository: InMemoryChatRepository

    @Before
    fun setUp() {
        repository = InMemoryChatRepository()
    }

    @Test
    fun getSessions_initialState_returnsDefaultSession() = runBlocking {
        val sessions = repository.getSessions().first()
        assertTrue(sessions.isNotEmpty())
        assertEquals("Welcome & Jetpack Compose", sessions.first().title)
    }

    @Test
    fun createSession_addsNewSession() = runBlocking {
        val newSession = repository.createSession("Test Session")
        val sessions = repository.getSessions().first()

        assertNotNull(newSession)
        assertEquals("Test Session", newSession.title)
        assertTrue(sessions.any { it.id == newSession.id })
    }

    @Test
    fun renameSession_updatesTitle() = runBlocking {
        val session = repository.createSession("Original Title")
        repository.renameSession(session.id, "Renamed Title")

        val updatedSessions = repository.getSessions().first()
        val updated = updatedSessions.find { it.id == session.id }

        assertNotNull(updated)
        assertEquals("Renamed Title", updated?.title)
    }

    @Test
    fun deleteSession_removesSessionAndMessages() = runBlocking {
        val session = repository.createSession("To Delete")
        repository.deleteSession(session.id)

        val sessions = repository.getSessions().first()
        assertTrue(sessions.none { it.id == session.id })
    }

    @Test
    fun addUserMessage_addsMessageToSession() = runBlocking {
        val session = repository.createSession("Message Test")
        repository.addUserMessage(session.id, "Hello AI!")

        val messages = repository.getMessages(session.id).first()
        assertEquals(1, messages.size)
        assertEquals("Hello AI!", messages.first().content)
    }
}
