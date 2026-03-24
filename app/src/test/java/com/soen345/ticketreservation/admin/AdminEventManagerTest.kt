package com.soen345.ticketreservation.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminEventManagerTest {

    @Test
    fun `adding a valid event succeeds`() {
        val manager = FakeInMemoryAdminEventManager()
        val event = validEvent(id = "event-1")

        val added = manager.addEvent(event)

        assertTrue("Expected a valid event to be added", added)
        assertNotNull("Expected event to be stored after add", manager.getEventById("event-1"))
    }

    @Test
    fun `adding an event with empty title fails`() {
        val manager = FakeInMemoryAdminEventManager()
        val event = validEvent(id = "event-2", title = "")

        val added = manager.addEvent(event)

        assertFalse("Expected add to fail when title is empty", added)
        assertFalse("Expected no event stored when add fails", manager.contains("event-2"))
    }

    @Test
    fun `adding an event with invalid price fails`() {
        val manager = FakeInMemoryAdminEventManager()
        val event = validEvent(id = "event-3", price = -10.0)

        val added = manager.addEvent(event)

        assertFalse("Expected add to fail when price is negative", added)
        assertFalse("Expected no event stored when add fails", manager.contains("event-3"))
    }

    @Test
    fun `adding an event with negative available tickets fails`() {
        val manager = FakeInMemoryAdminEventManager()
        val event = validEvent(id = "event-4", availableTickets = -1)

        val added = manager.addEvent(event)

        assertFalse("Expected add to fail when available tickets is negative", added)
        assertFalse("Expected no event stored when add fails", manager.contains("event-4"))
    }

    @Test
    fun `editing an existing event updates its fields correctly`() {
        val manager = FakeInMemoryAdminEventManager()
        manager.addEvent(validEvent(id = "event-5"))

        val updatedEvent = AdminEvent(
            id = "event-5",
            title = "Edited Title",
            description = "Edited Description",
            category = "Workshop",
            location = "EV Building",
            date = "2026-04-15",
            availableTickets = 75,
            price = 49.99,
            isCancelled = false
        )

        val edited = manager.editEvent(updatedEvent)
        val stored = manager.getEventById("event-5")

        assertTrue("Expected edit to succeed for an existing event", edited)
        assertNotNull("Expected edited event to still exist", stored)
        assertEquals("Edited Title", stored?.title)
        assertEquals("Edited Description", stored?.description)
        assertEquals("Workshop", stored?.category)
        assertEquals("EV Building", stored?.location)
        assertEquals("2026-04-15", stored?.date)
        assertEquals(75, stored?.availableTickets)
        assertEquals(49.99, stored?.price ?: 0.0, 0.0001)
    }

    @Test
    fun `editing a non-existing event fails`() {
        val manager = FakeInMemoryAdminEventManager()
        val updatedEvent = validEvent(id = "missing-event", title = "Updated")

        val edited = manager.editEvent(updatedEvent)

        assertFalse("Expected edit to fail for a non-existing event", edited)
    }

    @Test
    fun `canceling an existing event marks it as canceled`() {
        val manager = FakeInMemoryAdminEventManager()
        manager.addEvent(validEvent(id = "event-6"))

        val cancelled = manager.cancelEvent("event-6")
        val stored = manager.getEventById("event-6")

        assertTrue("Expected cancel to succeed for existing event", cancelled)
        assertTrue("Expected event to be marked as cancelled", stored?.isCancelled == true)
    }

    @Test
    fun `canceling a non-existing event fails`() {
        val manager = FakeInMemoryAdminEventManager()

        val cancelled = manager.cancelEvent("missing-event")

        assertFalse("Expected cancel to fail for non-existing event", cancelled)
    }

    private fun validEvent(
        id: String,
        title: String = "SOEN 345 Guest Speaker",
        description: String = "Software engineering best practices talk.",
        category: String = "Academic",
        location: String = "Hall Building",
        date: String = "2026-03-30",
        availableTickets: Int = 100,
        price: Double = 15.0,
        isCancelled: Boolean = false
    ): AdminEvent {
        return AdminEvent(
            id = id,
            title = title,
            description = description,
            category = category,
            location = location,
            date = date,
            availableTickets = availableTickets,
            price = price,
            isCancelled = isCancelled
        )
    }
}

data class AdminEvent(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val location: String,
    val date: String,
    val availableTickets: Int,
    val price: Double,
    val isCancelled: Boolean
)

class FakeInMemoryAdminEventManager {

    private val events = mutableMapOf<String, AdminEvent>()

    fun addEvent(event: AdminEvent): Boolean {
        if (!isValid(event)) return false
        if (events.containsKey(event.id)) return false

        events[event.id] = event
        return true
    }

    fun editEvent(updatedEvent: AdminEvent): Boolean {
        if (!isValid(updatedEvent)) return false
        if (!events.containsKey(updatedEvent.id)) return false

        events[updatedEvent.id] = updatedEvent
        return true
    }

    fun cancelEvent(eventId: String): Boolean {
        val existing = events[eventId] ?: return false
        events[eventId] = existing.copy(isCancelled = true)
        return true
    }

    fun getEventById(eventId: String): AdminEvent? = events[eventId]

    fun contains(eventId: String): Boolean = events.containsKey(eventId)

    private fun isValid(event: AdminEvent): Boolean {
        return event.title.isNotBlank() &&
            event.price >= 0.0 &&
            event.availableTickets >= 0
    }
}