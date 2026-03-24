package com.soen345.ticketreservation.admin

class AdminEventManager {

    private val events = mutableMapOf<String, AdminEvent>()

    fun addEvent(event: AdminEvent): Boolean {
        if (!isValid(event)) return false
        if (events.containsKey(event.id)) return false

        events[event.id] = event
        return true
    }

    fun editEvent(updatedEvent: AdminEvent): Boolean {
        if (!events.containsKey(updatedEvent.id)) return false
        if (!isValid(updatedEvent)) return false

        events[updatedEvent.id] = updatedEvent
        return true
    }

    fun cancelEvent(eventId: String): Boolean {
        val existingEvent = events[eventId] ?: return false
        events[eventId] = existingEvent.copy(isCancelled = true)
        return true
    }

    fun getEventById(eventId: String): AdminEvent? {
        return events[eventId]
    }

    fun contains(eventId: String): Boolean {
        return events.containsKey(eventId)
    }

    private fun isValid(event: AdminEvent): Boolean {
        return event.title.isNotBlank() &&
                event.price >= 0.0 &&
                event.availableTickets >= 0
    }
}