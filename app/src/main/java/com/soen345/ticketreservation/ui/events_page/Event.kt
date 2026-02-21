package com.soen345.ticketreservation.ui.events_page

data class Event(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val location: String,
    val date: String,
    val availableTickets: Int,
    val price: Double
)