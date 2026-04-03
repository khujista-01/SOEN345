package com.soen345.ticketreservation.data

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

class AuthClientIntegrationTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val raw = mockWebServer.url("/").toString()
        AuthClient.BASE_URL = if (raw.endsWith("/")) raw.dropLast(1) else raw
        AuthClient.ANON_KEY = "test-anon-key"
        AuthClient.http = OkHttpClient()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `signUp success returns 200`() : Unit= runBlocking {
        println("BASE_URL = ${AuthClient.BASE_URL}")  // check this in test output
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"id":"user-123","email":"test@test.com"}""")
        )

        val (code, _) = AuthClient.signUp("test@test.com", "password123")
        assertEquals(200, code)
    }

    @Test
    fun `signIn success returns valid session`(): Unit = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                      "access_token": "tok123",
                      "user": {
                        "id": "user-123",
                        "email": "test@test.com"
                      }
                    }
                """.trimIndent())
        )

        val session = AuthClient.signIn("test@test.com", "password123")
        assertNotNull(session)
        assertEquals("tok123", session!!.accessToken)
        assertEquals("user-123", session!!.userId)
    }

    @Test
    fun `signIn failure returns null`(): Unit = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":"invalid_credentials"}""")
        )

        val session = AuthClient.signIn("wrong@test.com", "wrongpass")
        assertNull(session)
    }

    @Test
    fun `signUp then signIn full flow`(): Unit = runBlocking {
        // signup
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"id":"user-456","email":"new@test.com"}""")
        )
        // signin
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                      "access_token": "newtoken",
                      "user": {
                        "id": "user-456",
                        "email": "new@test.com"
                      }
                    }
                """.trimIndent())
        )

        val (code, _) = AuthClient.signUp("new@test.com", "password123")
        assertEquals(200, code)

        val session = AuthClient.signIn("new@test.com", "password123")
        assertNotNull(session)
        assertEquals("newtoken", session!!.accessToken)
    }

    @Test
    fun `signOut success returns 200`(): Unit = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{}")
        )

        val (code, _) = AuthClient.signOut("tok123")
        assertEquals(200, code)
    }

    @Test
    fun `signIn network failure returns null`() : Unit = runBlocking {
        // Shut down server to simulate network failure
        mockWebServer.shutdown()

        val session = AuthClient.signIn("test@test.com", "password123")

        assertNull(session)
    }

    @Test
    fun `signIn invalid JSON returns null`() : Unit = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("INVALID_JSON")
        )

        val session = AuthClient.signIn("test@test.com", "password123")

        assertNull(session)
    }

    @Test
    fun `signIn empty response returns null`() : Unit = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{}")
        )

        val session = AuthClient.signIn("test@test.com", "password123")

        assertNull(session)
    }


    @Test
    fun `signUp failure returns non 200`() : Unit = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":"bad_request"}""")
        )

        val (code, _) = AuthClient.signUp("bad@test.com", "123")

        assertEquals(400, code)
    }


    @Test
    fun `signOut failure returns non 200`() : Unit = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":"unauthorized"}""")
        )

        val (code, _) = AuthClient.signOut("invalid-token")

        assertEquals(401, code)
    }

    @Test
    fun `signIn missing token returns null`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                {
                  "user": {
                    "id": "user-123"
                  }
                }
            """.trimIndent())
        )

        val session = AuthClient.signIn("test@test.com", "password123")

        assertNull(session)
    }

    @Test
    fun `signIn missing user returns null`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"access_token": "tok123"}""")
        )

        val session = AuthClient.signIn("test@test.com", "password123")

        assertNull(session)
    }

    @Test
    fun `signIn missing user id returns null`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                {
                  "access_token": "tok123",
                  "user": {}
                }
            """.trimIndent())
        )

        val session = AuthClient.signIn("test@test.com", "password123")

        assertNull(session)
    }
    @Test
    fun `signUp empty body still returns code`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("")
        )

        val (code, body) = AuthClient.signUp("test@test.com", "pass")

        assertEquals(200, code)
    }

    @Test
    fun `signOut empty response still returns code`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("")
        )


        val (code, _) = AuthClient.signOut("tok123")

        assertEquals(200, code)
    }
}