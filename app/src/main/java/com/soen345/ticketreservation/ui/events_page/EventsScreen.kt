package com.soen345.ticketreservation.ui.events_page

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun EventsScreen(events: List<Event>) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(events) { event ->
            EventCard(event)
        }
    }
}
@Composable
fun EventsDummyScreen() {
    val dummyEvents = listOf(
        Event(
            id = "1",
            title = "Rock Concert",
            description = "Join the biggest rock concert of the year!",
            category = "Music",
            location = "Montreal Arena",
            date = "2026-03-10",
            availableTickets = 120,
            price = 49.99
        ),
        Event(
            id = "2",
            title = "Tech Conference",
            description = "Explore AI and Robotics innovations.",
            category = "Conference",
            location = "Concordia University",
            date = "2026-04-05",
            availableTickets = 50,
            price = 99.0
        ),
        Event(
            id = "3",
            title = "Food Festival",
            description = "Taste dishes from around the world.",
            category = "Food & Drink",
            location = "Old Port",
            date = "2026-05-20",
            availableTickets = 200,
            price = 15.0
        )
    )

    EventsScreen(events = dummyEvents)
}
@Composable
fun EventCard(event: Event) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text = event.title,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = event.category,
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Location: ${event.location}")
            Text("Date: ${event.date}")
            Text("🎟 Tickets: ${event.availableTickets}")
            Text("💲Amount ${event.price}")
        }
    }
}