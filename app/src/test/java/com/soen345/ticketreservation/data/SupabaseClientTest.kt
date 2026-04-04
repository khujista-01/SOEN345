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
import org.junit.Assert.*
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

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        try { mockServer.shutdown() } catch (_: Exception) {}
    }

    // ===================== ORIGINAL TESTS =====================

    @Test
    fun `test fetchEvents returns a list`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

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

        mockServer.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val result = SupabaseClient.fetchEvents(accessToken = "fake-token", userId = "user1")

        Assert.assertNull(result.errorMessage)
        Assert.assertNotNull(result.events)
        Assert.assertEquals(1, result.events!!.size)
        Assert.assertEquals("Concert", result.events!![0].title)
    }

    @Test
    fun `fetchEvents returns error when fields are missing`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

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

        mockServer.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val result = SupabaseClient.fetchEvents("fake-token", "user1")

        Assert.assertNotNull(result.errorMessage)
        Assert.assertTrue(result.errorMessage!!.contains("parse", ignoreCase = true))
    }

    @Test
    fun `fetchEvents returns error on network exception`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        mockServer.shutdown()

        val result = SupabaseClient.fetchEvents("fake-token", "user1")

        Assert.assertNotNull(result.errorMessage)
    }

    @Test
    fun `test upsertUserProfile returns code and body`() {
        val mockResponseBody = """{"id":"123","email":"test@test.com"}"""
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody(mockResponseBody))

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
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"event_id":"event1","user_id":"user1"}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":10}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":9}]"""))

        val result = SupabaseClient.insertReservation("event1", "user1", "fake-token")
        Assert.assertTrue(result)
    }

    @Test
    fun `insertReservation returns false when ticket update fails and rolls back`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"id":1}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":10}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(400))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":1}]"""))

        val result = SupabaseClient.insertReservation("event1", "user1", "fake-token")
        Assert.assertFalse(result)
    }

    @Test
    fun `test deleteReservation returns true on success`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"event_id":"event1","user_id":"user1"}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":9}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":10}]"""))

        val result = SupabaseClient.deleteReservation("event1", "user1", "fake-token")
        Assert.assertTrue(result)
    }

    @Test
    fun `deleteReservation returns false when rollback occurs`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":1}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(500))
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"id":1}]"""))

        val result = SupabaseClient.deleteReservation("event1", "user1", "fake-token")
        Assert.assertFalse(result)
    }

    @Test
    fun `test sendConfirmationEmail success`() = runBlocking {
        val event = Event("1", "Concert", "Fun", "Music", "Hall A", "2026-03-10", 50, 30.0)

        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"message":"Email sent"}"""))

        val (code, msg) = SupabaseClient.sendConfirmationEmail(
            userEmail = "test@test.com",
            userName = "Test User",
            event = event,
            accessToken = "fake-token"
        )

        Assert.assertEquals(200, code)
        Assert.assertTrue("Expected success message but got: $msg", msg.contains("Ticket email sent", ignoreCase = true))
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
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":"1","title":"Admin Event"}]"""))
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
            AdminEvent("1", "Title", "Desc", "Cat", "Loc", "Date", 10, 10.0), "token"
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
        mockServer.enqueue(MockResponse().setResponseCode(400))
        mockServer.enqueue(MockResponse().setResponseCode(200))
        val (success, msg) = SupabaseClient.cancelAdminEvent("1", "token")
        Assert.assertTrue(success)
        Assert.assertTrue(msg.contains("Fallback", ignoreCase = true) || msg.contains("set to 0"))
    }

    @Test
    fun `cancelAdminEvent total failure`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(400))
        mockServer.enqueue(MockResponse().setResponseCode(400))
        val (success, _) = SupabaseClient.cancelAdminEvent("1", "token")
        Assert.assertFalse(success)
    }

    // ===================== BRANCH COVERAGE TESTS =====================

    // --- insertReservation branches ---

    @Test
    fun `insertReservation returns false when inserted rows is zero`() = runBlocking {
        // insertReservationRowOnly: 201 but empty array → insertedRows == 0 → false
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("[]"))

        val result = SupabaseClient.insertReservation("event1", "user1", "token")
        assertFalse(result)
    }

    @Test
    fun `insertReservation returns false when reservation insert is not successful`() = runBlocking {
        // Non-2xx response from insertReservationRowOnly
        mockServer.enqueue(MockResponse().setResponseCode(409).setBody("conflict"))

        val result = SupabaseClient.insertReservation("event1", "user1", "token")
        assertFalse(result)
    }

    @Test
    fun `insertReservation returns false when available tickets would go negative`() = runBlocking {
        // 0 tickets + (-1) = -1 < 0 → rollback and return false
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"id":1}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":0}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":1}]"""))

        val result = SupabaseClient.insertReservation("event1", "user1", "token")
        assertFalse(result)
    }

    @Test
    fun `insertReservation returns false when patch verification count mismatches`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"id":1}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":5}]"""))
        // Patch returns wrong count (99 instead of 4)
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":99}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":1}]"""))

        val result = SupabaseClient.insertReservation("event1", "user1", "token")
        assertFalse(result)
    }

    @Test
    fun `insertReservation returns false when current ticket count cannot be fetched`() = runBlocking {
        // 200 but empty array → fetchCurrentAvailableTickets returns null
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"id":1}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":1}]"""))

        val result = SupabaseClient.insertReservation("event1", "user1", "token")
        assertFalse(result)
    }

    @Test
    fun `insertReservation returns false when patch body is not valid JSON array`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"id":1}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":5}]"""))
        // Invalid JSON body → returnedCount = Int.MIN_VALUE → mismatch → false
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("not-json"))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":1}]"""))

        val result = SupabaseClient.insertReservation("event1", "user1", "token")
        assertFalse(result)
    }

    // --- deleteReservation branches ---

    @Test
    fun `deleteReservation returns false when deleted rows is zero`() = runBlocking {
        // 200 but empty array → deletedRows == 0 → false
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val result = SupabaseClient.deleteReservation("event1", "user1", "token")
        assertFalse(result)
    }

    @Test
    fun `deleteReservation returns false when ticket count fetch fails with non-200`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":1}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(404))
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"id":1}]"""))

        val result = SupabaseClient.deleteReservation("event1", "user1", "token")
        assertFalse(result)
    }

    // --- fetchEvents branches ---

    @Test
    fun `fetchEvents marks event as reserved when user has reservation`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"event_id":"42"}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{
            "id":"42","title":"Rock Night","description":"Great show",
            "category":"Music","location":"Stadium","date":"2026-06-01",
            "available_tickets":100,"price":25.0
        }]"""))

        val result = SupabaseClient.fetchEvents("token", "user1")
        assertNull(result.errorMessage)
        assertTrue("Event should be marked as reserved", result.events!![0].isReservedByCurrentUser)
    }

    @Test
    fun `fetchEvents does not mark event as reserved when user has no reservation`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{
            "id":"42","title":"Rock Night","description":"Great show",
            "category":"Music","location":"Stadium","date":"2026-06-01",
            "available_tickets":100,"price":25.0
        }]"""))

        val result = SupabaseClient.fetchEvents("token", "user1")
        assertFalse("Event should NOT be marked as reserved", result.events!![0].isReservedByCurrentUser)
    }

    @Test
    fun `fetchEvents returns error when HTTP response is not successful`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        mockServer.enqueue(MockResponse().setResponseCode(403).setBody("forbidden"))

        val result = SupabaseClient.fetchEvents("token", "user1")
        assertNotNull(result.errorMessage)
        assertNull(result.events)
    }

    @Test
    fun `fetchEvents handles reserved events fetch failure gracefully`() = runBlocking {
        // Reserved fetch fails → falls back to empty set, events still returned
        mockServer.enqueue(MockResponse().setResponseCode(500))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{
            "id":"1","title":"Jazz Night","description":"Smooth",
            "category":"Music","location":"Club","date":"2026-07-01",
            "available_tickets":30,"price":15.0
        }]"""))

        val result = SupabaseClient.fetchEvents("token", "user1")
        assertNull(result.errorMessage)
        assertNotNull(result.events)
        assertFalse(result.events!![0].isReservedByCurrentUser)
    }

    @Test
    fun `fetchEvents skips blank event_id in reserved set`() = runBlocking {
        // Blank event_id should be excluded from reserved set
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"event_id":""}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{
            "id":"1","title":"Jazz","description":"Cool",
            "category":"Music","location":"Venue","date":"2026-07-01",
            "available_tickets":10,"price":10.0
        }]"""))

        val result = SupabaseClient.fetchEvents("token", "user1")
        assertNotNull(result.events)
        assertFalse(result.events!![0].isReservedByCurrentUser)
    }

    // --- fetchAdminEvents branches ---

    @Test
    fun `fetchAdminEvents returns error on network exception`() = runBlocking {
        mockServer.shutdown()
        val (events, error) = SupabaseClient.fetchAdminEvents("token")
        assertNull(events)
        assertNotNull(error)
    }

    // --- updateAdminEvent branches ---

    @Test
    fun `updateAdminEvent returns true on success`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200))
        val event = AdminEvent("1", "Title", "Desc", "Cat", "Loc", "2026-01-01", 10, 20.0)
        val (success, _) = SupabaseClient.updateAdminEvent(event, "token")
        assertTrue(success)
    }

    @Test
    fun `updateAdminEvent returns false on failure`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(400).setBody("bad request"))
        val event = AdminEvent("1", "Title", "Desc", "Cat", "Loc", "2026-01-01", 10, 20.0)
        val (success, msg) = SupabaseClient.updateAdminEvent(event, "token")
        assertFalse(success)
        assertTrue(msg.contains("400"))
    }

    @Test
    fun `updateAdminEvent returns false on network exception`() = runBlocking {
        mockServer.shutdown()
        val event = AdminEvent("1", "Title", "Desc", "Cat", "Loc", "2026-01-01", 10, 20.0)
        val (success, _) = SupabaseClient.updateAdminEvent(event, "token")
        assertFalse(success)
    }

    // --- insertAdminEvent branches ---

    @Test
    fun `insertAdminEvent returns false on failure`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(500).setBody("error"))
        val event = AdminEvent("1", "Title", "Desc", "Cat", "Loc", "2026-01-01", 10, 20.0)
        val (success, msg) = SupabaseClient.insertAdminEvent(event, "token")
        assertFalse(success)
        assertTrue(msg.contains("500"))
    }

    @Test
    fun `insertAdminEvent returns false on network exception`() = runBlocking {
        mockServer.shutdown()
        val event = AdminEvent("1", "Title", "Desc", "Cat", "Loc", "2026-01-01", 10, 20.0)
        val (success, _) = SupabaseClient.insertAdminEvent(event, "token")
        assertFalse(success)
    }

    // --- sendConfirmationEmail branches ---

    @Test
    fun `sendConfirmationEmail returns failure message on non-200 response`() = runBlocking {
        val event = Event("1", "Concert", "Fun", "Music", "Hall A", "2026-03-10", 50, 30.0)
        mockServer.enqueue(MockResponse().setResponseCode(500).setBody("internal error"))

        val (code, msg) = SupabaseClient.sendConfirmationEmail("test@test.com", "User", event, "token")
        assertEquals(500, code)
        assertTrue(msg.contains("Email failed", ignoreCase = true))
    }

    @Test
    fun `sendConfirmationEmail uses User as fallback when userName is null`() = runBlocking {
        val event = Event("1", "Concert", "Fun", "Music", "Hall A", "2026-03-10", 50, 30.0)
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))

        val (code, msg) = SupabaseClient.sendConfirmationEmail("test@test.com", null, event, "token")
        assertEquals(200, code)
        assertTrue(msg.contains("Ticket email sent", ignoreCase = true))
    }

    // --- upsertUserProfile branches ---

    @Test
    fun `upsertUserProfile with all fields non-null returns 200`() {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"u1"}"""))
        val (code, _) = SupabaseClient.upsertUserProfile("token", "u1", "a@b.com", "Alice", "555-1234")
        assertEquals(200, code)
    }

    @Test
    fun `upsertUserProfile with null fullName and phone returns 200`() {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"u1"}"""))
        val (code, _) = SupabaseClient.upsertUserProfile("token", "u1", "a@b.com", null, null)
        assertEquals(200, code)
    }
}