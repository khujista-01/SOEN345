package com.soen345.ticketreservation.ui.events_page

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun EventsScreen(events: List<Event>, userId: String, userAccessToken: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(events) { event ->
            EventCard(event = event, userId = userId, userAccessToken = userAccessToken)
        }
    }
}

@Composable
fun EventCard(event: Event, userId: String, userAccessToken: String) {
    var isReserved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Text(event.title, style = MaterialTheme.typography.titleLarge)
            Text(event.category, style = MaterialTheme.typography.labelMedium)
            Text(event.description, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(8.dp))

            Text("Location: ${event.location}")
            Text("Date: ${event.date}")
            Text("🎟 Tickets: ${event.availableTickets}")
            Text("💲Amount ${event.price}")

            Spacer(modifier = Modifier.height(12.dp))

            TicketButton(
                isReserved = isReserved,
                onReservedChange = { newState ->
                    scope.launch {
                        val success =
                            if (newState)
                                TicketActions.reserveTicket(event.id, userId, userAccessToken)
                            else
                                TicketActions.cancelReservation(event.id, userId, userAccessToken)

                        if (success) {
                            isReserved = newState
                        }
                    }
                }
            )
        }
    }
}
@Composable
fun EventsDummyScreen(
    userId: String,
    userAccessToken: String
) {
    val dummyEvents = listOf(
        Event("67ffa809-e247-434d-b5bd-2f01d4f4400c", "Rock Concert", "Join the biggest rock concert of the year!", "Music", "Montreal Arena", "2026-03-10", 120, 49.99),
        Event("86590715-2712-4564-a91a-04bb25fceda9", "Tech Conference", "Explore AI and Robotics innovations.", "Conference", "Concordia University", "2026-04-05", 50, 99.0),
        Event("8ceb1c37-b7ac-40fd-b596-c0a1b33dfec1", "Food Festival", "Taste dishes from around the world.", "Food & Drink", "Old Port", "2026-05-20", 200, 15.0)
    )

    EventsScreen(
        events = dummyEvents,
        userId = userId,
        userAccessToken = userAccessToken
    )
}
