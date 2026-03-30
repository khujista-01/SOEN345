package com.soen345.ticketreservation.admin

class AdminEventManager {
    private val events = mutableMapOf<String, AdminEvent>()

    /**
     * Adds a new event to the manager.
     *
     * @param event The event to add.
     * @throws IllegalArgumentException if title is empty, price < 0, or availableTickets < 0.
     * @throws IllegalStateException if an event with the same ID already exists.
     */
    fun addEvent(event: AdminEvent) {
        validateEvent(event)
        
        if (events.containsKey(event.id)) {
            throw IllegalStateException("Event with ID ${event.id} already exists")
        }
        
        events[event.id] = event
    }

    /**
     * Edits an existing event.
     *
     * @param event The updated event.
     * @throws IllegalArgumentException if title is empty, price < 0, or availableTickets < 0.
     * @throws IllegalStateException if the event does not exist.
     */
    fun editEvent(event: AdminEvent) {
        validateEvent(event)
        
        if (!events.containsKey(event.id)) {
            throw IllegalStateException("Event with ID ${event.id} does not exist")
        }
        
        events[event.id] = event
    }

    /**
     * Cancels an event by ID.
     *
     * @param eventId The ID of the event to cancel.
     * @throws IllegalStateException if the event does not exist.
     */
    fun cancelEvent(eventId: String) {
        val event = events[eventId] ?: throw IllegalStateException("Event with ID $eventId does not exist")
        events[eventId] = event.copy(isCancelled = true)
    }

    /**
     * Retrieves an event by ID.
     *
     * @param eventId The ID of the event to retrieve.
     * @return The event if found, null otherwise.
     */
    fun getEventById(eventId: String): AdminEvent? {
        return events[eventId]
    }

    /**
     * Retrieves all events.
     *
     * @return A list of all events.
     */
    fun getAllEvents(): List<AdminEvent> {
        return events.values.toList()
    }

    /**
     * Clears all events from the manager (useful for testing).
     */
    fun clear() {
        events.clear()
    }

    private fun validateEvent(event: AdminEvent) {
        if (event.id.isBlank()) {
            throw IllegalArgumentException("Event ID cannot be empty")
        }
        
        if (event.title.isBlank()) {
            throw IllegalArgumentException("Event title cannot be empty")
        }
        
        if (event.description.isBlank()) {
            throw IllegalArgumentException("Event description cannot be empty")
        }
        
        if (event.categoryId.isBlank()) {
            throw IllegalArgumentException("Event category cannot be empty")
        }
        
        if (event.location.isBlank()) {
            throw IllegalArgumentException("Event location cannot be empty")
        }
        
        if (event.date.isBlank()) {
            throw IllegalArgumentException("Event date cannot be empty")
        }
        
        if (event.price < 0) {
            throw IllegalArgumentException("Event price cannot be negative")
        }
        
        if (event.availableTickets < 0) {
            throw IllegalArgumentException("Available tickets cannot be negative")
        }
    }
}
