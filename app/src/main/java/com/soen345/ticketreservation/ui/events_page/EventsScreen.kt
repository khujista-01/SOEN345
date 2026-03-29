package com.soen345.ticketreservation.ui.events_page

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.soen345.ticketreservation.data.SupabaseClient
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
fun EventsLoadingScreen(
    userId: String,
    userAccessToken: String
) {
    val scope = rememberCoroutineScope()
    var events by remember { mutableStateOf<List<Event>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(userAccessToken) {
        scope.launch {
            loading = true
            error = null
            try {
                val fetchedEvents = SupabaseClient.fetchEvents(userAccessToken)
                if (fetchedEvents != null) {
                    events = fetchedEvents
                    error = null
                } else {
                    error = "Failed to load events from server."
                    events = emptyList()
                }
            } catch (e: Exception) {
                error = "Error loading events: ${e.message}"
                events = emptyList()
            } finally {
                loading = false
            }
        }
    }

    when {
        loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        error != null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(error!!, style = MaterialTheme.typography.bodyLarge)
            }
        }
        else -> {
            EventsScreen(
                events = events.orEmpty(),
                userId = userId,
                userAccessToken = userAccessToken
            )
        }
    }
}
