package com.soen345.ticketreservation.admin

import org.junit.Assert.assertEquals
import org.junit.Test

class AdminEventTest {

    @Test
    fun `test AdminEvent properties`() {
        val event = AdminEvent(
            id = "1",
            title = "Concert",
            description = "Live Music",
            categoryId = "cat1",
            location = "Montreal",
            date = "2023-12-01",
            availableTickets = 100,
            price = 50.0,
            isCancelled = false
        )

        assertEquals("1", event.id)
        assertEquals("Concert", event.title)
        assertEquals("Live Music", event.description)
        assertEquals("cat1", event.categoryId)
        assertEquals("Montreal", event.location)
        assertEquals("2023-12-01", event.date)
        assertEquals(100, event.availableTickets)
        assertEquals(50.0, event.price, 0.0)
        assertEquals(false, event.isCancelled)
    }

    @Test
    fun `test AdminEvent default isCancelled`() {
        val event = AdminEvent(
            id = "1",
            title = "Concert",
            description = "Live Music",
            categoryId = "cat1",
            location = "Montreal",
            date = "2023-12-01",
            availableTickets = 100,
            price = 50.0
        )
        assertEquals(false, event.isCancelled)
    }

    @Test
    fun `test AdminEvent copy`() {
        val event = AdminEvent("1", "T", "D", "C", "L", "Date", 10, 5.0)
        val cancelledEvent = event.copy(isCancelled = true)
        
        assertEquals(true, cancelledEvent.isCancelled)
        assertEquals("1", cancelledEvent.id)
    }
}
