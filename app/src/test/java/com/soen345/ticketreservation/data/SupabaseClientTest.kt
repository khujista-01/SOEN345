package com.soen345.ticketreservation.data

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class SupabaseClientTest {

    private lateinit var mockServer: MockWebServer

    @Before
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()
        SupabaseClient.BASE_URL = mockServer.url("/rest/v1").toString().removeSuffix("/")
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
    fun `test fetchEvents returns a list`() = runBlocking {
        // Mock JSON response for events
        val mockJson = """
            [
                {
                    "id": "1",
                    "title": "Concert",
                    "description": "Fun concert",
                    "category": "Music",
                    "location": "Hall A",
                    "date": "2026-03-10",
                    "available_tickets": 50,
                    "price": 30.0
                }
            ]
        """.trimIndent()

        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockJson)
        )

        val result = SupabaseClient.fetchEvents(
            accessToken = "fake-token",
            userId = "user1"
        )

        Assert.assertNull(result.errorMessage)
        Assert.assertNotNull(result.events)
        Assert.assertEquals(1, result.events!!.size)
        Assert.assertEquals("Concert", result.events!![0].title)
    }

    @Test
    fun `test upsertUserProfile returns code and body`() {
        val mockResponseBody = """{"id":"123","email":"test@test.com"}"""
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(mockResponseBody)
        )

        val (code, body) = SupabaseClient.upsertUserProfile(
            accessToken = "fake-token",
            userId = "123",
            email = "test@test.com",
            fullName = "Test User",
            phone = null
        )

        Assert.assertEquals(201, code)
        Assert.assertEquals(mockResponseBody, body)
    }
    @Test
    fun `test insertReservation returns true on success`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(201))

        val result = SupabaseClient.insertReservation("event1", "user1", "fake-token")
        Assert.assertTrue(result)
    }

    @Test
    fun `insertReservation returns false on failure`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(400))
        val result = SupabaseClient.insertReservation("event1", "user1", "fake-token")
        Assert.assertFalse(result)
    }

    @Test
    fun `test deleteReservation returns true on success`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200))
        val result = SupabaseClient.deleteReservation("event1", "user1", "fake-token")
        Assert.assertTrue(result)
    }
}