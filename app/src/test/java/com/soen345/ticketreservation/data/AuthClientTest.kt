package com.soen345.ticketreservation.data

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AuthClientTest {

    private lateinit var mockServer: MockWebServer

    @Before
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()

        AuthClient.BASE_URL = mockServer.url("/auth").toString().removeSuffix("/")

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        try { mockServer.shutdown() } catch (_: Exception) {}
    }

    // ===================== ORIGINAL TESTS =====================

    @Test
    fun `signUp returns mocked code and body`() = runBlocking {
        val mockJson = """{"id":"1"}"""

        mockServer.enqueue(MockResponse().setResponseCode(201).setBody(mockJson))

        val (code, body) = AuthClient.signUp("test@test.com", "password")

        assertEquals(201, code)
        assertEquals(mockJson, body)
    }

    @Test
    fun `signUp returns error on exception`() = runBlocking {
        mockServer.shutdown()

        val (code, body) = AuthClient.signUp("test@test.com", "password")

        assertEquals(0, code)
        assertNotEquals("error", body)
    }

    @Test
    fun `signIn returns AuthSession when successful`() = runBlocking {
        val mockJson = """
            {
                "access_token":"token",
                "user":{"id":"123","email":"test@test.com"}
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val session = AuthClient.signIn("test@test.com", "password")

        assertNotNull(session)
        assertEquals("123", session?.userId)
        assertEquals("test@test.com", session?.email)
        assertEquals("token", session?.accessToken)
    }

    @Test
    fun `signIn returns null when response not successful`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"unauthorized"}"""))

        val session = AuthClient.signIn("test@test.com", "wrong")

        assertNull(session)
    }

    @Test
    fun `signIn returns null when json is invalid`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("invalid json"))

        val session = AuthClient.signIn("test@test.com", "password")

        assertNull(session)
    }

    @Test
    fun `signIn returns null when essential fields are missing`() = runBlocking {
        val mockJson = """
            {
                "access_token":"",
                "user":{"id":""}
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val session = AuthClient.signIn("test@test.com", "password")

        assertNull(session)
    }

    @Test
    fun `signOut returns mocked code and body`() = runBlocking {
        val mockJson = "{}"

        mockServer.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val (code, body) = AuthClient.signOut("token")

        assertEquals(200, code)
        assertEquals(mockJson, body)
    }

    @Test
    fun `signOut returns error on exception`() = runBlocking {
        mockServer.shutdown()

        val (code, _) = AuthClient.signOut("token")

        assertEquals(0, code)
    }

    // ===================== BRANCH COVERAGE TESTS =====================

    // --- signIn branches: blank access_token / blank userId ---

    @Test
    fun `signIn returns null when access_token is blank but userId is present`() = runBlocking {
        val json = """{"access_token":"","user":{"id":"123","email":"a@b.com"}}"""
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val session = AuthClient.signIn("a@b.com", "pass")
        assertNull(session)
    }

    @Test
    fun `signIn returns null when userId is blank but access_token is present`() = runBlocking {
        val json = """{"access_token":"tok","user":{"id":"","email":"a@b.com"}}"""
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val session = AuthClient.signIn("a@b.com", "pass")
        assertNull(session)
    }

    // --- signIn branch: user object missing entirely ---

    @Test
    fun `signIn returns null when user object is missing from response`() = runBlocking {
        val json = """{"access_token":"tok"}"""
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val session = AuthClient.signIn("a@b.com", "pass")
        assertNull(session)
    }

    // --- signIn branch: email field missing from user object ---
    // optString("email", "") returns "" (not null), so ?: fallback does NOT trigger.
    // The session is still created with an empty email since userId and accessToken are valid.

    @Test
    fun `signIn returns session with empty email when user object has no email field`() = runBlocking {
        val json = """{"access_token":"tok","user":{"id":"abc"}}"""
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val session = AuthClient.signIn("fallback@test.com", "pass")
        assertNotNull(session)
        assertEquals("", session!!.email)   // optString returns "" not null, so fallback doesn't fire
        assertEquals("abc", session.userId)
        assertEquals("tok", session.accessToken)
    }

    // --- signIn branch: network exception ---

    @Test
    fun `signIn returns null on network exception`() = runBlocking {
        mockServer.shutdown()
        val session = AuthClient.signIn("a@b.com", "pass")
        assertNull(session)
    }

    // --- signUp branches: various response codes ---

    @Test
    fun `signUp returns 400 on bad request`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"bad"}"""))
        val (code, body) = AuthClient.signUp("bad", "pass")
        assertEquals(400, code)
        assertTrue(body.contains("bad"))
    }

    @Test
    fun `signUp handles empty response body`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val (code, body) = AuthClient.signUp("a@b.com", "pass")
        assertEquals(200, code)
        assertEquals("", body)
    }

    // --- signOut branches ---

    @Test
    fun `signOut returns 401 on unauthorized`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"unauthorized"}"""))
        val (code, _) = AuthClient.signOut("bad-token")
        assertEquals(401, code)
    }

    @Test
    fun `signOut handles empty body gracefully`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(204).setBody(""))
        val (code, body) = AuthClient.signOut("token")
        assertEquals(204, code)
        assertEquals("", body)
    }
}
