package com.soen345.ticketreservation.data

import android.util.Log
import com.soen345.ticketreservation.admin.AdminEvent
import com.soen345.ticketreservation.ui.events_page.Event
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
        val mockUrl = mockServer.url("/").toString().removeSuffix("/")
        SupabaseClient.BASE_URL = "$mockUrl/rest/v1"
        SupabaseClient.FUNCTIONS_URL = "$mockUrl/functions/v1"
        
        // Mock Android Log to avoid RuntimeException
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun `test fetchEvents returns a list`() = runBlocking {
        // First mock response for fetchReservedEventIdsForUser
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("[]")
        )

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
    fun `fetchEvents returns error when fields are missing`() = runBlocking {
        // Mock reserved events
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        // Missing "price" field
        val mockJson = """
            [
                {
                    "id": "1",
                    "title": "Concert",
                    "description": "Fun concert",
                    "category": "Music",
                    "location": "Hall A",
                    "date": "2026-03-10",
                    "available_tickets": 50
                }
            ]
        """.trimIndent()

        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockJson)
        )

        val result = SupabaseClient.fetchEvents("fake-token", "user1")

        Assert.assertNotNull(result.errorMessage)
        Assert.assertTrue(result.errorMessage!!.contains("parse", ignoreCase = true))
    }

    @Test
    fun `fetchEvents returns error on network exception`() = runBlocking {
        // Mock reserved events success
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        
        // Shutdown server before second call
        mockServer.shutdown()

        val result = SupabaseClient.fetchEvents("fake-token", "user1")

        Assert.assertNotNull(result.errorMessage)
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
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody("""[{"event_id":"event1","user_id":"user1"}]""")
        )
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"available_tickets":10}]""")
        )
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"available_tickets":9}]""")
        )

        val result = SupabaseClient.insertReservation("event1", "user1", "fake-token")
        Assert.assertTrue(result)
    }

    @Test
    fun `insertReservation returns false when ticket update fails and rolls back`() = runBlocking {
        // 1. insertReservationRowOnly success
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"id":1}]"""))
        // 2. updateEventTicketCount -> fetchCurrentAvailableTickets success
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":10}]"""))
        // 3. updateEventTicketCount -> patch call fails
        mockServer.enqueue(MockResponse().setResponseCode(400))
        // 4. deleteReservationRowOnly (rollback) success
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":1}]"""))

        val result = SupabaseClient.insertReservation("event1", "user1", "fake-token")
        Assert.assertFalse(result)
    }

    @Test
    fun `test deleteReservation returns true on success`() = runBlocking {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"event_id":"event1","user_id":"user1"}]""")
        )
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"available_tickets":9}]""")
        )
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"available_tickets":10}]""")
        )

        val result = SupabaseClient.deleteReservation("event1", "user1", "fake-token")
        Assert.assertTrue(result)
    }

    @Test
    fun `deleteReservation returns false when rollback occurs`() = runBlocking {
        // 1. deleteReservationRowOnly success
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":1}]"""))
        // 2. updateEventTicketCount -> fetchCurrentAvailableTickets fails
        mockServer.enqueue(MockResponse().setResponseCode(500))
        // 3. insertReservationRowOnly (rollback) success
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"id":1}]"""))

        val result = SupabaseClient.deleteReservation("event1", "user1", "fake-token")
        Assert.assertFalse(result)
    }

    @Test
    fun `test sendConfirmationEmail success`() = runBlocking {
        val event = Event("1", "Concert", "Fun", "Music", "Hall A", "2026-03-10", 50, 30.0)
        
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"message":"Email sent"}""")
        )

        val (code, msg) = SupabaseClient.sendConfirmationEmail(
            userEmail = "test@test.com",
            userName = "Test User",
            event = event,
            accessToken = "fake-token"
        )

        Assert.assertEquals(200, code)
        Assert.assertTrue("Expected success message but got: ${msg}", msg.contains("Ticket email sent", ignoreCase = true))
    }

    @Test
    fun `sendConfirmationEmail returns error on exception`() = runBlocking {
        val event = Event("1", "Concert", "Fun", "Music", "Hall A", "2026-03-10", 50, 30.0)
        mockServer.shutdown()

        val (code, _) = SupabaseClient.sendConfirmationEmail("test@test.com", null, event, "token")
        Assert.assertEquals(500, code)
    }

    @Test
    fun `fetchAdminEvents success`() = runBlocking {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"id":"1","title":"Admin Event"}]""")
        )
        val (events, error) = SupabaseClient.fetchAdminEvents("token")
        Assert.assertNull(error)
        Assert.assertEquals(1, events?.size)
        Assert.assertEquals("Admin Event", events?.get(0)?.title)
    }

    @Test
    fun `fetchAdminEvents failure`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(404))
        val (events, error) = SupabaseClient.fetchAdminEvents("token")
        Assert.assertNull(events)
        Assert.assertNotNull(error)
    }

    @Test
    fun `insertAdminEvent success`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(201))
        val (success, _) = SupabaseClient.insertAdminEvent(
            AdminEvent("1", "Title", "Desc", "Cat", "Loc", "Date", 10, 10.0),
            "token"
        )
        Assert.assertTrue(success)
    }

    @Test
    fun `cancelAdminEvent primary success`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200))
        val (success, _) = SupabaseClient.cancelAdminEvent("1", "token")
        Assert.assertTrue(success)
    }

    @Test
    fun `cancelAdminEvent fallback success`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(400)) // Primary fails
        mockServer.enqueue(MockResponse().setResponseCode(200)) // Fallback succeeds
        val (success, msg) = SupabaseClient.cancelAdminEvent("1", "token")
        Assert.assertTrue(success)
        Assert.assertTrue(msg.contains("Fallback", ignoreCase = true) || msg.contains("set to 0"))
    }

    @Test
    fun `cancelAdminEvent total failure`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(400)) // Primary fails
        mockServer.enqueue(MockResponse().setResponseCode(400)) // Fallback fails
        val (success, _) = SupabaseClient.cancelAdminEvent("1", "token")
        Assert.assertFalse(success)
    }
}
