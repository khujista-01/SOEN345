package com.soen345.ticketreservation.admin

class AdminEventManager {

	private val eventsById = mutableMapOf<String, AdminEvent>()
	private val categories = mutableMapOf(
		"academic" to EventCategory("academic", "Academic"),
		"workshop" to EventCategory("workshop", "Workshop"),
		"concert" to EventCategory("concert", "Concert"),
		"sports" to EventCategory("sports", "Sports")
	)

	fun addEvent(event: AdminEvent): Boolean {
		if (!isValid(event)) return false
		if (eventsById.containsKey(event.id)) return false

		eventsById[event.id] = event
		return true
	}

	fun editEvent(updatedEvent: AdminEvent): Boolean {
		if (!eventsById.containsKey(updatedEvent.id)) return false
		if (!isValid(updatedEvent)) return false

		eventsById[updatedEvent.id] = updatedEvent
		return true
	}

	fun cancelEvent(eventId: String): Boolean {
		val existing = eventsById[eventId] ?: return false
		eventsById[eventId] = existing.copy(isCancelled = true)
		return true
	}

	fun getEventById(eventId: String): AdminEvent? = eventsById[eventId]

	fun contains(eventId: String): Boolean = eventsById.containsKey(eventId)

	fun getAllCategories(): List<EventCategory> = categories.values.toList()

	private fun isValid(event: AdminEvent): Boolean {
		return event.id.isNotBlank() &&
			event.title.isNotBlank() &&
			event.description.isNotBlank() &&
			event.location.isNotBlank() &&
			event.date.isNotBlank() &&
			event.availableTickets >= 0 &&
			event.price >= 0.0 &&
			categories.containsKey(event.categoryId)
	}
}