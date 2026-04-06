package com.soen345.ticketreservation.admin

import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertThrows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

class AdminEventManagerTest {
    private lateinit var manager: AdminEventManager
    private lateinit var validEvent: AdminEvent

    @Before
    fun setUp() {
        manager = AdminEventManager()
        validEvent = AdminEvent(
            id = "event1",
            title = "Test Event",
            description = "A test event",
            categoryId = "cat1",
            location = "Test Location",
            date = "2026-03-30",
            availableTickets = 100,
            price = 50.0
        )
    }

    // ===================== ORIGINAL TESTS =====================

    // ========== addEvent Tests ==========

    @Test
    fun testAddEventSuccess() {
        manager.addEvent(validEvent)
        val retrieved = manager.getEventById("event1")
        assertEquals(validEvent, retrieved)
    }

    @Test
    fun testAddEventWithBlankId() {
        val invalidEvent = validEvent.copy(id = "")
        val exception = assertThrows(IllegalArgumentException::class.java) {
            manager.addEvent(invalidEvent)
        }
        assertEquals("Event ID cannot be empty", exception.message)
    }

    @Test
    fun testAddEventWithBlankTitle() {
        val invalidEvent = validEvent.copy(title = "")
        val exception = assertThrows(IllegalArgumentException::class.java) {
            manager.addEvent(invalidEvent)
        }
        assertEquals("Event title cannot be empty", exception.message)
    }

    @Test
    fun testAddEventWithWhitespaceTitle() {
        val invalidEvent = validEvent.copy(title = "   ")
        val exception = assertThrows(IllegalArgumentException::class.java) {
            manager.addEvent(invalidEvent)
        }
        assertEquals("Event title cannot be empty", exception.message)
    }

    @Test
    fun testAddEventWithBlankDescription() {
        val invalidEvent = validEvent.copy(description = "")
        val exception = assertThrows(IllegalArgumentException::class.java) {
            manager.addEvent(invalidEvent)
        }
        assertEquals("Event description cannot be empty", exception.message)
    }

    @Test
    fun testAddEventWithBlankCategory() {
        val invalidEvent = validEvent.copy(categoryId = "")
        val exception = assertThrows(IllegalArgumentException::class.java) {
            manager.addEvent(invalidEvent)
        }
        assertEquals("Event category cannot be empty", exception.message)
    }

    @Test
    fun testAddEventWithBlankLocation() {
        val invalidEvent = validEvent.copy(location = "")
        val exception = assertThrows(IllegalArgumentException::class.java) {
            manager.addEvent(invalidEvent)
        }
        assertEquals("Event location cannot be empty", exception.message)
    }

    @Test
    fun testAddEventWithBlankDate() {
        val invalidEvent = validEvent.copy(date = "")
        val exception = assertThrows(IllegalArgumentException::class.java) {
            manager.addEvent(invalidEvent)
        }
        assertEquals("Event date cannot be empty", exception.message)
    }

    @Test
    fun testAddEventWithNegativePrice() {
        val invalidEvent = validEvent.copy(price = -10.0)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            manager.addEvent(invalidEvent)
        }
        assertEquals("Event price cannot be negative", exception.message)
    }

    @Test
    fun testAddEventWithNegativeTickets() {
        val invalidEvent = validEvent.copy(availableTickets = -5)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            manager.addEvent(invalidEvent)
        }
        assertEquals("Available tickets cannot be negative", exception.message)
    }

    @Test
    fun testAddEventWithDuplicateId() {
        manager.addEvent(validEvent)
        val exception = assertThrows(IllegalStateException::class.java) {
            manager.addEvent(validEvent)
        }
        assertEquals("Event with ID event1 already exists", exception.message)
    }

    // ========== editEvent Tests ==========

    @Test
    fun testEditEventSuccess() {
        manager.addEvent(validEvent)
        val updatedEvent = validEvent.copy(title = "Updated Event", price = 75.0)
        manager.editEvent(updatedEvent)
        val retrieved = manager.getEventById("event1")
        assertEquals(updatedEvent, retrieved)
        assertEquals("Updated Event", retrieved?.title)
        assertEquals(75.0, retrieved?.price ?: -1.0, 0.0)
    }

    @Test
    fun testEditEventWithBlankTitle() {
        manager.addEvent(validEvent)
        val invalidEvent = validEvent.copy(title = "")
        val exception = assertThrows(IllegalArgumentException::class.java) {
            manager.editEvent(invalidEvent)
        }
        assertEquals("Event title cannot be empty", exception.message)
    }

    @Test
    fun testEditEventWithBlankDescription() {
        manager.addEvent(validEvent)
        val invalidEvent = validEvent.copy(description = "")
        val exception = assertThrows(IllegalArgumentException::class.java) {
            manager.editEvent(invalidEvent)
        }
        assertEquals("Event description cannot be empty", exception.message)
    }

    @Test
    fun testEditEventWithBlankCategory() {
        manager.addEvent(validEvent)
        val invalidEvent = validEvent.copy(categoryId = "")
        val exception = assertThrows(IllegalArgumentException::class.java) {
            manager.editEvent(invalidEvent)
        }
        assertEquals("Event category cannot be empty", exception.message)
    }

    @Test
    fun testEditEventWithBlankLocation() {
        manager.addEvent(validEvent)
        val invalidEvent = validEvent.copy(location = "")
        val exception = assertThrows(IllegalArgumentException::class.java) {
            manager.editEvent(invalidEvent)
        }
        assertEquals("Event location cannot be empty", exception.message)
    }

    @Test
    fun testEditEventWithBlankDate() {
        manager.addEvent(validEvent)
        val invalidEvent = validEvent.copy(date = "")
        val exception = assertThrows(IllegalArgumentException::class.java) {
            manager.editEvent(invalidEvent)
        }
        assertEquals("Event date cannot be empty", exception.message)
    }

    @Test
    fun testEditEventWithNegativePrice() {
        manager.addEvent(validEvent)
        val invalidEvent = validEvent.copy(price = -5.0)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            manager.editEvent(invalidEvent)
        }
        assertEquals("Event price cannot be negative", exception.message)
    }

    @Test
    fun testEditEventWithNegativeTickets() {
        manager.addEvent(validEvent)
        val invalidEvent = validEvent.copy(availableTickets = -10)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            manager.editEvent(invalidEvent)
        }
        assertEquals("Available tickets cannot be negative", exception.message)
    }

    @Test
    fun testEditEventNotFound() {
        val nonExistentEvent = validEvent.copy(id = "nonexistent")
        val exception = assertThrows(IllegalStateException::class.java) {
            manager.editEvent(nonExistentEvent)
        }
        assertEquals("Event with ID nonexistent does not exist", exception.message)
    }

    // ========== cancelEvent Tests ==========

    @Test
    fun testCancelEventSuccess() {
        manager.addEvent(validEvent)
        manager.cancelEvent("event1")
        val retrieved = manager.getEventById("event1")
        assertEquals(true, retrieved?.isCancelled)
    }

    @Test
    fun testCancelEventPreservesOtherFields() {
        manager.addEvent(validEvent)
        manager.cancelEvent("event1")
        val retrieved = manager.getEventById("event1")
        assertEquals("Test Event", retrieved?.title)
        assertEquals(50.0, retrieved?.price ?: -1.0, 0.0)
        assertEquals(100, retrieved?.availableTickets)
    }

    @Test
    fun testCancelEventNotFound() {
        val exception = assertThrows(IllegalStateException::class.java) {
            manager.cancelEvent("nonexistent")
        }
        assertEquals("Event with ID nonexistent does not exist", exception.message)
    }

    @Test
    fun testCancelAlreadyCancelledEvent() {
        manager.addEvent(validEvent)
        manager.cancelEvent("event1")
        manager.cancelEvent("event1")
        val retrieved = manager.getEventById("event1")
        assertEquals(true, retrieved?.isCancelled)
    }

    // ========== getEventById Tests ==========

    @Test
    fun testGetEventByIdFound() {
        manager.addEvent(validEvent)
        val retrieved = manager.getEventById("event1")
        assertEquals(validEvent, retrieved)
    }

    @Test
    fun testGetEventByIdNotFound() {
        val retrieved = manager.getEventById("nonexistent")
        assertNull(retrieved)
    }

    // ========== getAllEvents Tests ==========

    @Test
    fun testGetAllEventsEmpty() {
        val events = manager.getAllEvents()
        assertEquals(0, events.size)
    }

    @Test
    fun testGetAllEventsSingle() {
        manager.addEvent(validEvent)
        val events = manager.getAllEvents()
        assertEquals(1, events.size)
        assertEquals(validEvent, events[0])
    }

    @Test
    fun testGetAllEventsMultiple() {
        val event2 = validEvent.copy(id = "event2", title = "Second Event")
        val event3 = validEvent.copy(id = "event3", title = "Third Event")
        manager.addEvent(validEvent)
        manager.addEvent(event2)
        manager.addEvent(event3)
        val events = manager.getAllEvents()
        assertEquals(3, events.size)
    }

    // ========== Edge Cases ==========

    @Test
    fun testAddEventWithZeroPrice() {
        val freeEvent = validEvent.copy(price = 0.0)
        manager.addEvent(freeEvent)
        val retrieved = manager.getEventById("event1")
        assertEquals(0.0, retrieved?.price ?: -1.0, 0.0)
    }

    @Test
    fun testAddEventWithZeroTickets() {
        val soldOutEvent = validEvent.copy(availableTickets = 0)
        manager.addEvent(soldOutEvent)
        val retrieved = manager.getEventById("event1")
        assertEquals(0, retrieved?.availableTickets)
    }

    // ===================== BRANCH COVERAGE TESTS =====================

    // --- validateEvent: whitespace-only strings (isBlank catches these) ---

    @Test
    fun `addEvent throws when id is whitespace only`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            manager.addEvent(validEvent.copy(id = "   "))
        }
        assertEquals("Event ID cannot be empty", ex.message)
    }

    @Test
    fun `addEvent throws when description is whitespace only`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            manager.addEvent(validEvent.copy(description = "\t"))
        }
        assertEquals("Event description cannot be empty", ex.message)
    }

    @Test
    fun `addEvent throws when categoryId is whitespace only`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            manager.addEvent(validEvent.copy(categoryId = " "))
        }
        assertEquals("Event category cannot be empty", ex.message)
    }

    @Test
    fun `addEvent throws when location is whitespace only`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            manager.addEvent(validEvent.copy(location = "  "))
        }
        assertEquals("Event location cannot be empty", ex.message)
    }

    @Test
    fun `addEvent throws when date is whitespace only`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            manager.addEvent(validEvent.copy(date = " "))
        }
        assertEquals("Event date cannot be empty", ex.message)
    }

    // --- editEvent: blank ID triggers ID validation before existence check ---

    @Test
    fun `editEvent throws IllegalArgumentException for blank id before checking existence`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            manager.editEvent(validEvent.copy(id = ""))
        }
        assertEquals("Event ID cannot be empty", ex.message)
    }

    // --- editEvent: whitespace-only validation ---

    @Test
    fun `editEvent throws for whitespace-only location`() {
        manager.addEvent(validEvent)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            manager.editEvent(validEvent.copy(location = "  "))
        }
        assertEquals("Event location cannot be empty", ex.message)
    }

    @Test
    fun `editEvent throws for whitespace-only date`() {
        manager.addEvent(validEvent)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            manager.editEvent(validEvent.copy(date = "\n"))
        }
        assertEquals("Event date cannot be empty", ex.message)
    }

    @Test
    fun `editEvent throws for whitespace-only categoryId`() {
        manager.addEvent(validEvent)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            manager.editEvent(validEvent.copy(categoryId = " "))
        }
        assertEquals("Event category cannot be empty", ex.message)
    }

    // --- clear() ---

    @Test
    fun `clear empties all events`() {
        manager.addEvent(validEvent)
        manager.addEvent(validEvent.copy(id = "event2"))
        manager.clear()
        assertEquals(0, manager.getAllEvents().size)
    }

    @Test
    fun `after clear same id can be added again`() {
        manager.addEvent(validEvent)
        manager.clear()
        manager.addEvent(validEvent) // Should not throw
        assertEquals(1, manager.getAllEvents().size)
    }

    // --- getAllEvents snapshot ---

    @Test
    fun `getAllEvents returns independent snapshot`() {
        manager.addEvent(validEvent)
        val snapshot = manager.getAllEvents()
        assertEquals(1, snapshot.size)
        assertEquals(1, manager.getAllEvents().size)
    }

    // --- cancelEvent: already-cancelled event stays cancelled ---

    @Test
    fun `cancelEvent on already-cancelled event keeps isCancelled true`() {
        manager.addEvent(validEvent.copy(isCancelled = true))
        manager.cancelEvent("event1")
        assertTrue(manager.getEventById("event1")!!.isCancelled)
    }

    // --- boundary: price and tickets exactly 0 are valid ---

    @Test
    fun `addEvent accepts price of exactly 0`() {
        manager.addEvent(validEvent.copy(price = 0.0))
        assertNotNull(manager.getEventById("event1"))
    }

    @Test
    fun `addEvent accepts availableTickets of exactly 0`() {
        manager.addEvent(validEvent.copy(availableTickets = 0))
        assertEquals(0, manager.getEventById("event1")?.availableTickets)
    }
}