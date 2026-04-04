package com.soen345.ticketreservation.ui.events_page

import android.util.Log
import com.soen345.ticketreservation.data.SupabaseClient
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
class TicketActionsTest {

    private lateinit var server: MockWebServer

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        SupabaseClient.BASE_URL = server.url("/rest/v1").toString().removeSuffix("/")

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `reserveTicket returns true on success`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody("""[{"event_id":"event1","user_id":"user1"}]""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"available_tickets":10}]""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"available_tickets":9}]""")
        )

        val result = TicketActions.reserveTicket("event1", "user1", "token")

        Assert.assertTrue(result)
    }

    @Test
    fun `reserveTicket returns false on failure`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(400))

        val result = TicketActions.reserveTicket("event1", "user1", "token")

        Assert.assertFalse(result)
    }

    @Test
    fun `cancelReservation returns true on success`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"event_id":"event1","user_id":"user1"}]""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"available_tickets":9}]""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"available_tickets":10}]""")
        )

        val result = TicketActions.cancelReservation("event1", "user1", "token")

        Assert.assertTrue(result)
    }
}