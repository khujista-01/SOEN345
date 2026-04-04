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

    // ================================================================
    // insertReservation
    // Branch map:
    //   L35: if (!reservationInserted)        → true  = early return false
    //                                          → false = continue
    //   L42: if (!ticketUpdateSucceeded)       → true  = rollback + return false
    //                                          → false = return true
    // ================================================================

    @Test
    fun `insertReservation returns true on success`() = runBlocking {
        // L35 false, L42 false → true
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"event_id":"event1","user_id":"user1"}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":10}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":9}]"""))

        assertTrue(SupabaseClient.insertReservation("event1", "user1", "fake-token"))
    }

    @Test
    fun `insertReservation returns false when reservation row insert fails non 2xx`() = runBlocking {
        // L35 true (non-2xx) → early return false
        mockServer.enqueue(MockResponse().setResponseCode(409).setBody("conflict"))

        assertFalse(SupabaseClient.insertReservation("event1", "user1", "token"))
    }

    @Test
    fun `insertReservation returns false when reservation row insert returns empty array`() = runBlocking {
        // L35 true (insertedRows == 0) → early return false
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("[]"))

        assertFalse(SupabaseClient.insertReservation("event1", "user1", "token"))
    }

    @Test
    fun `insertReservation returns false when reservation insert body is not JSON array`() = runBlocking {
        // insertReservationRowOnly: JSON parse fails → insertedRows = 0 → false
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("not-json"))

        assertFalse(SupabaseClient.insertReservation("event1", "user1", "token"))
    }

    @Test
    fun `insertReservation returns false when ticket update fails and rolls back`() = runBlocking {
        // L35 false, L42 true → rollback deleteReservationRowOnly, return false
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"id":1}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":10}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(400)) // patch fails
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":1}]""")) // rollback

        assertFalse(SupabaseClient.insertReservation("event1", "user1", "fake-token"))
    }

    @Test
    fun `insertReservation returns false when available tickets would go negative`() = runBlocking {
        // updateEventTicketCount: newCount < 0 → false → L42 true → rollback
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"id":1}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":0}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":1}]""")) // rollback

        assertFalse(SupabaseClient.insertReservation("event1", "user1", "token"))
    }

    @Test
    fun `insertReservation returns false when current ticket count cannot be fetched empty array`() = runBlocking {
        // fetchCurrentAvailableTickets: empty array → null → updateEventTicketCount false
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"id":1}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":1}]""")) // rollback

        assertFalse(SupabaseClient.insertReservation("event1", "user1", "token"))
    }

    @Test
    fun `insertReservation returns false when fetchCurrentAvailableTickets gets non-200`() = runBlocking {
        // fetchCurrentAvailableTickets: non-2xx → null → updateEventTicketCount false
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"id":1}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(500))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":1}]""")) // rollback

        assertFalse(SupabaseClient.insertReservation("event1", "user1", "token"))
    }

    @Test
    fun `insertReservation returns false when patch verification count mismatches`() = runBlocking {
        // patch 200 but returned count != expected → success=false → L42 true
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"id":1}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":5}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":99}]""")) // wrong count
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":1}]""")) // rollback

        assertFalse(SupabaseClient.insertReservation("event1", "user1", "token"))
    }

    @Test
    fun `insertReservation returns false when patch response body is not valid JSON`() = runBlocking {
        // patch body not JSON → returnedCount = Int.MIN_VALUE → mismatch → false
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"id":1}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":5}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("not-json"))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":1}]""")) // rollback

        assertFalse(SupabaseClient.insertReservation("event1", "user1", "token"))
    }

    @Test
    fun `insertReservation returns false when patch array is empty`() = runBlocking {
        // patch 200 but arr.length() == 0 → returnedCount = Int.MIN_VALUE → false
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"id":1}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":5}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("[]")) // empty patch response
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":1}]""")) // rollback

        assertFalse(SupabaseClient.insertReservation("event1", "user1", "token"))
    }

    @Test
    fun `insertReservation returns false on network exception`() = runBlocking {
        mockServer.shutdown()
        assertFalse(SupabaseClient.insertReservation("event1", "user1", "token"))
    }

    // ================================================================
    // deleteReservation
    // Branch map:
    //   L105: if (!reservationDeleted)         → true  = early return false
    //                                           → false = continue
    //   L112: if (!ticketUpdateSucceeded)       → true  = rollback + return false
    //                                           → false = return true
    // ================================================================

    @Test
    fun `deleteReservation returns true on success`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"event_id":"event1","user_id":"user1"}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":9}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"available_tickets":10}]"""))

        assertTrue(SupabaseClient.deleteReservation("event1", "user1", "fake-token"))
    }

    @Test
    fun `deleteReservation returns false when deleted rows is zero`() = runBlocking {
        // L105 true (deletedRows == 0)
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        assertFalse(SupabaseClient.deleteReservation("event1", "user1", "token"))
    }

    @Test
    fun `deleteReservation returns false when delete row response is non-2xx`() = runBlocking {
        // L105 true (non-2xx)
        mockServer.enqueue(MockResponse().setResponseCode(404))

        assertFalse(SupabaseClient.deleteReservation("event1", "user1", "token"))
    }

    @Test
    fun `deleteReservation returns false when delete row body is not valid JSON`() = runBlocking {
        // JSON parse fails → deletedRows = 0 → false
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("not-json"))

        assertFalse(SupabaseClient.deleteReservation("event1", "user1", "token"))
    }

    @Test
    fun `deleteReservation returns false when ticket count fetch fails and rolls back`() = runBlocking {
        // L105 false, L112 true → rollback
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":1}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(500))
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""[{"id":1}]""")) // rollback

        assertFalse(SupabaseClient.deleteReservation("event1", "user1", "fake-token"))
    }

    @Test
    fun `deleteReservation returns false on network exception`() = runBlocking {
        mockServer.shutdown()
        assertFalse(SupabaseClient.deleteReservation("event1", "user1", "token"))
    }

    // ================================================================
    // sendConfirmationEmail
    // Branch map:
    //   L87: if (response.isSuccessful)  → true = success / false = failure
    //   userName null                    → coerced to "User"
    //   Exception                        → 500
    // ================================================================

    @Test
    fun `sendConfirmationEmail returns success message on 200`() = runBlocking {
        val event = Event("1", "Concert", "Fun", "Music", "Hall A", "2026-03-10", 50, 30.0)
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"message":"Email sent"}"""))

        val (code, msg) = SupabaseClient.sendConfirmationEmail("test@test.com", "Test User", event, "token")
        assertEquals(200, code)
        assertTrue(msg.contains("Ticket email sent", ignoreCase = true))
    }

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

    @Test
    fun `sendConfirmationEmail returns 500 on network exception`() = runBlocking {
        val event = Event("1", "Concert", "Fun", "Music", "Hall A", "2026-03-10", 50, 30.0)
        mockServer.shutdown()

        val (code, _) = SupabaseClient.sendConfirmationEmail("test@test.com", null, event, "token")
        assertEquals(500, code)
    }

    // ================================================================
    // fetchEvents
    // Branch map:
    //   L295: if (!resp.isSuccessful)          → true  = error result
    //   L308: if (missingFields.isNotEmpty())  → true  = parse error
    //   L355: if (!response.isSuccessful)      → true  = emptySet
    //   L365: if (eventId.isNotBlank())        → true/false = add or skip
    //   Exception                              → error result
    // ================================================================

    @Test
    fun `fetchEvents returns list with isReservedByCurrentUser true`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"event_id":"42"}]"""))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{
            "id":"42","title":"Rock Night","description":"Great show",
            "category":"Music","location":"Stadium","date":"2026-06-01",
            "available_tickets":100,"price":25.0
        }]"""))

        val result = SupabaseClient.fetchEvents("token", "user1")
        assertNull(result.errorMessage)
        assertTrue(result.events!![0].isReservedByCurrentUser)
    }

    @Test
    fun `fetchEvents returns list with isReservedByCurrentUser false when no reservations`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{
            "id":"42","title":"Rock Night","description":"Great show",
            "category":"Music","location":"Stadium","date":"2026-06-01",
            "available_tickets":100,"price":25.0
        }]"""))

        val result = SupabaseClient.fetchEvents("token", "user1")
        assertFalse(result.events!![0].isReservedByCurrentUser)
    }

    @Test
    fun `fetchEvents skips blank event_id in reserved set`() = runBlocking {
        // L365 false → blank eventId skipped
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

    @Test
    fun `fetchEvents returns error when HTTP response is not successful`() = runBlocking {
        // L295 true
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        mockServer.enqueue(MockResponse().setResponseCode(403).setBody("forbidden"))

        val result = SupabaseClient.fetchEvents("token", "user1")
        assertNotNull(result.errorMessage)
        assertNull(result.events)
    }

    @Test
    fun `fetchEvents returns error when required field is missing`() = runBlocking {
        // L308 true → missing "price"
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{
            "id":"1","title":"Concert","description":"Fun",
            "category":"Music","location":"Hall A","date":"2026-03-10",
            "available_tickets":50
        }]"""))

        val result = SupabaseClient.fetchEvents("fake-token", "user1")
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("parse", ignoreCase = true))
    }

    @Test
    fun `fetchEvents handles reserved events fetch failure gracefully`() = runBlocking {
        // L355 true → emptySet, events still returned
        mockServer.enqueue(MockResponse().setResponseCode(500))
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{
            "id":"1","title":"Jazz Night","description":"Smooth",
            "category":"Music","location":"Club","date":"2026-07-01",
            "available_tickets":30,"price":15.0
        }]"""))

        val result = SupabaseClient.fetchEvents("token", "user1")
        assertNull(result.errorMessage)
        assertNotNull(result.events)
    }

    @Test
    fun `fetchEvents returns error on network exception`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        mockServer.shutdown()

        val result = SupabaseClient.fetchEvents("fake-token", "user1")
        assertNotNull(result.errorMessage)
    }

    // ================================================================
    // fetchAdminEvents
    // Branch map:
    //   L386: if (!resp.isSuccessful)  → true = null + error / false = parse
    //   Exception                      → null + error
    // ================================================================

    @Test
    fun `fetchAdminEvents returns events on success`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":"1","title":"Admin Event"}]"""))
        val (events, error) = SupabaseClient.fetchAdminEvents("token")
        assertNull(error)
        assertEquals(1, events?.size)
        assertEquals("Admin Event", events?.get(0)?.title)
    }

    @Test
    fun `fetchAdminEvents returns error on non-200`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(404))
        val (events, error) = SupabaseClient.fetchAdminEvents("token")
        assertNull(events)
        assertNotNull(error)
    }

    @Test
    fun `fetchAdminEvents returns error on network exception`() = runBlocking {
        mockServer.shutdown()
        val (events, error) = SupabaseClient.fetchAdminEvents("token")
        assertNull(events)
        assertNotNull(error)
    }

    // ================================================================
    // insertAdminEvent / updateAdminEvent  (via performWriteRequest)
    // Branch map:
    //   L512: if (response.isSuccessful)  → true/false
    //   L497: if (includeId)              → true (updateAdmin) / false (insertAdmin)
    //   Exception                         → failure pair
    // ================================================================

    @Test
    fun `insertAdminEvent returns true on success`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(201))
        val (success, _) = SupabaseClient.insertAdminEvent(
            AdminEvent("1", "Title", "Desc", "Cat", "Loc", "Date", 10, 10.0), "token"
        )
        assertTrue(success)
    }

    @Test
    fun `insertAdminEvent returns false on non-2xx response`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(500).setBody("error"))
        val (success, msg) = SupabaseClient.insertAdminEvent(
            AdminEvent("1", "Title", "Desc", "Cat", "Loc", "Date", 10, 10.0), "token"
        )
        assertFalse(success)
        assertTrue(msg.contains("500"))
    }

    @Test
    fun `insertAdminEvent returns false on network exception`() = runBlocking {
        mockServer.shutdown()
        val (success, _) = SupabaseClient.insertAdminEvent(
            AdminEvent("1", "Title", "Desc", "Cat", "Loc", "Date", 10, 10.0), "token"
        )
        assertFalse(success)
    }

    @Test
    fun `updateAdminEvent returns true on success`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200))
        val (success, _) = SupabaseClient.updateAdminEvent(
            AdminEvent("1", "Title", "Desc", "Cat", "Loc", "2026-01-01", 10, 20.0), "token"
        )
        assertTrue(success)
    }

    @Test
    fun `updateAdminEvent returns false on non-2xx response`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(400).setBody("bad request"))
        val (success, msg) = SupabaseClient.updateAdminEvent(
            AdminEvent("1", "Title", "Desc", "Cat", "Loc", "2026-01-01", 10, 20.0), "token"
        )
        assertFalse(success)
        assertTrue(msg.contains("400"))
    }

    @Test
    fun `updateAdminEvent returns false on network exception`() = runBlocking {
        mockServer.shutdown()
        val (success, _) = SupabaseClient.updateAdminEvent(
            AdminEvent("1", "Title", "Desc", "Cat", "Loc", "2026-01-01", 10, 20.0), "token"
        )
        assertFalse(success)
    }

    // ================================================================
    // cancelAdminEvent
    // Branch map:
    //   L468: if (flagSuccess)      → true = return early / false = try fallback
    //   L488: if (fallbackSuccess)  → true = success / false = total failure
    // ================================================================

    @Test
    fun `cancelAdminEvent returns true when primary succeeds`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200))
        val (success, _) = SupabaseClient.cancelAdminEvent("1", "token")
        assertTrue(success)
    }

    @Test
    fun `cancelAdminEvent returns true when fallback succeeds`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(400)) // primary fails
        mockServer.enqueue(MockResponse().setResponseCode(200)) // fallback succeeds
        val (success, msg) = SupabaseClient.cancelAdminEvent("1", "token")
        assertTrue(success)
        assertTrue(msg.contains("set to 0") || msg.contains("Fallback", ignoreCase = true))
    }

    @Test
    fun `cancelAdminEvent returns false when both primary and fallback fail`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(400))
        mockServer.enqueue(MockResponse().setResponseCode(400))
        val (success, _) = SupabaseClient.cancelAdminEvent("1", "token")
        assertFalse(success)
    }

    // ================================================================
    // upsertUserProfile
    // Branch map:
    //   fullName null → JSONObject.NULL
    //   phone null    → JSONObject.NULL
    // ================================================================

    @Test
    fun `upsertUserProfile with all fields non-null`() {
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":"u1"}"""))
        val (code, _) = SupabaseClient.upsertUserProfile("token", "u1", "a@b.com", "Alice", "555-1234")
        assertEquals(201, code)
    }

    @Test
    fun `upsertUserProfile with null fullName and null phone`() {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"u1"}"""))
        val (code, _) = SupabaseClient.upsertUserProfile("token", "u1", "a@b.com", null, null)
        assertEquals(200, code)
    }

    @Test
    fun `upsertUserProfile with null fullName only`() {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"u1"}"""))
        val (code, _) = SupabaseClient.upsertUserProfile("token", "u1", "a@b.com", null, "555-0000")
        assertEquals(200, code)
    }

    @Test
    fun `upsertUserProfile with null phone only`() {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"u1"}"""))
        val (code, _) = SupabaseClient.upsertUserProfile("token", "u1", "a@b.com", "Bob", null)
        assertEquals(200, code)
    }
}