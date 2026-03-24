package com.soen345.ticketreservation.admin

class AdminEventManager {

    private val events = mutableMapOf<String, AdminEvent>()

    private val categories = mutableMapOf(
        "academic" to EventCategory("academic", "Academic"),
        "workshop" to EventCategory("workshop", "Workshop"),
        "concert" to EventCategory("concert", "Concert"),
        "sports" to EventCategory("sports", "Sports")
    )

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
        val existing = events[eventId] ?: return false
        events[eventId] = existing.copy(isCancelled = true)
        return true
    }

    fun getEventById(eventId: String): AdminEvent? {
        return events[eventId]
    }

    fun contains(eventId: String): Boolean {
        return events.containsKey(eventId)
    }

    fun getAllCategories(): List<EventCategory> {
        return categories.values.toList()
    }

    private fun isValid(event: AdminEvent): Boolean {
        return event.title.isNotBlank() &&
                event.description.isNotBlank() &&
                event.location.isNotBlank() &&
                event.date.isNotBlank() &&
                event.price >= 0.0 &&
                event.availableTickets >= 0 &&
                categories.containsKey(event.categoryId)
    }
}