package com.soen345.ticketreservation.ui.events_page

import org.junit.Assert.assertEquals
import org.junit.Test

class EventTest {

    @Test
    fun `test Event properties`() {
        val event = Event(
            id = "e1",
            title = "Tech Talk",
            description = "AI and Future",
            category = "Technology",
            location = "Hall A",
            date = "2024-05-10",
            availableTickets = 50,
            price = 0.0,
            isReservedByCurrentUser = true
        )

        assertEquals("e1", event.id)
        assertEquals("Tech Talk", event.title)
        assertEquals("AI and Future", event.description)
        assertEquals("Technology", event.category)
        assertEquals("Hall A", event.location)
        assertEquals("2024-05-10", event.date)
        assertEquals(50, event.availableTickets)
        assertEquals(0.0, event.price, 0.0)
        assertEquals(true, event.isReservedByCurrentUser)
    }

    @Test
    fun `test Event default isReservedByCurrentUser`() {
        val event = Event(
            id = "e1",
            title = "Tech Talk",
            description = "AI and Future",
            category = "Technology",
            location = "Hall A",
            date = "2024-05-10",
            availableTickets = 50,
            price = 0.0
        )
        assertEquals(false, event.isReservedByCurrentUser)
    }
}
