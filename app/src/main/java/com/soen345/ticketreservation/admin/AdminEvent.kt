package com.soen345.ticketreservation.admin

data class AdminEvent(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val date: String,
    val availableTickets: Int,
    val price: Double,
    val categoryId: String,
    val isCancelled: Boolean = false
)