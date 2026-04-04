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

        // Point AuthClient to the mock server
        AuthClient.BASE_URL = mockServer.url("/auth").toString().removeSuffix("/")

        // Mock Android Log to avoid RuntimeException
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun `signUp returns mocked code and body`() = runBlocking {
        val mockJson = """{"id":"1"}"""

        mockServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(mockJson)
        )

        val (code, body) = AuthClient.signUp("test@test.com", "password")

        assertEquals(201, code)
        assertEquals(mockJson, body)
    }

    @Test
    fun `signUp returns error on exception`() = runBlocking {
        // Shutting down the server to trigger an IOException
        mockServer.shutdown()

        val (code, body) = AuthClient.signUp("test@test.com", "password")

        assertEquals(0, code)
        assertNotEquals("error", body) // Should contain exception message
    }

    @Test
    fun `signIn returns AuthSession when successful`() = runBlocking {
        val mockJson = """
            {
                "access_token":"token",
                "user":{"id":"123","email":"test@test.com"}
            }
        """.trimIndent()

        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockJson)
        )

        val session = AuthClient.signIn("test@test.com", "password")

        assertNotNull(session)
        assertEquals("123", session?.userId)
        assertEquals("test@test.com", session?.email)
        assertEquals("token", session?.accessToken)
    }

    @Test
    fun `signIn returns null when response not successful`() = runBlocking {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":"unauthorized"}""")
        )

        val session = AuthClient.signIn("test@test.com", "wrong")

        assertNull(session)
    }

    @Test
    fun `signIn returns null when json is invalid`() = runBlocking {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("invalid json")
        )

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

        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockJson)
        )

        val session = AuthClient.signIn("test@test.com", "password")

        assertNull(session)
    }

    @Test
    fun `signOut returns mocked code and body`() = runBlocking {
        val mockJson = "{}"

        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockJson)
        )

        val (code, body) = AuthClient.signOut("token")

        assertEquals(200, code)
        assertEquals(mockJson, body)
    }

    @Test
    fun `signOut returns error on exception`() = runBlocking {
        mockServer.shutdown()

        val (code, body) = AuthClient.signOut("token")

        assertEquals(0, code)
    }
}
