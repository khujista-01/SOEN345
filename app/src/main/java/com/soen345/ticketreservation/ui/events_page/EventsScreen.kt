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
import kotlinx.coroutines.launch
import com.soen345.ticketreservation.data.SupabaseClient
import android.util.Log
@Composable
fun EventsScreen(
    events: List<Event>,
    userId: String,
    userAccessToken: String,
    userEmail: String,
    onReservationChanged: suspend () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }

    // Filtering logic
    val filteredEvents = events.filter { event ->
        (searchText.isEmpty() ||
                event.title.contains(searchText, ignoreCase = true) ||
                event.description.contains(searchText, ignoreCase = true)) &&
                (selectedDate.isEmpty() || event.date.contains(selectedDate)) &&
                (selectedLocation.isEmpty() || event.location.contains(selectedLocation, ignoreCase = true)) &&
                (selectedCategory.isEmpty() || event.category.contains(selectedCategory, ignoreCase = true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        EventFiltersCompact(
            searchText = searchText,
            onSearchChange = { searchText = it },
            selectedDate = selectedDate,
            onDateChange = { selectedDate = it },
            selectedLocation = selectedLocation,
            onLocationChange = { selectedLocation = it },
            selectedCategory = selectedCategory,
            onCategoryChange = { selectedCategory = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filteredEvents) { event ->
                EventCard(
                    event = event,
                    userId = userId,
                    userAccessToken = userAccessToken,
                    userEmail = userEmail,
                    onReservationChanged = onReservationChanged
                )
            }
        }
    }
}
@Composable
fun EventFiltersCompact(
    searchText: String,
    onSearchChange: (String) -> Unit,
    selectedDate: String,
    onDateChange: (String) -> Unit,
    selectedLocation: String,
    onLocationChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // Search field on its own row
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChange,
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall
        )

        // Filters in a row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = selectedDate,
                onValueChange = onDateChange,
                label = { Text("Date") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = selectedLocation,
                onValueChange = onLocationChange,
                label = { Text("Location") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = onCategoryChange,
                label = { Text("Category") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
        }
    }
}
@Composable
fun EventCard(
    event: Event,
    userId: String,
    userAccessToken: String,
    userEmail: String,
    onReservationChanged: suspend () -> Unit
) {
    var isReserved by remember(event.id, event.isReservedByCurrentUser) {
        mutableStateOf(event.isReservedByCurrentUser)
    }
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }

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

            var isProcessing by remember { mutableStateOf(false) }

            Button(
                onClick = {
                    if (isProcessing) return@Button

                    scope.launch {
                        isProcessing = true

                        val action = if (!isReserved) "reserve" else "cancel"
                        val success = if (!isReserved) {
                            SupabaseClient.insertReservation(event.id, userId, userAccessToken)
                        } else {
                            SupabaseClient.deleteReservation(event.id, userId, userAccessToken)
                        }

                        if (success) {
                            Log.d("EVENTS", "Ticket $action success for eventId=${event.id}, userId=$userId")
                            isReserved = !isReserved
                            Log.d("EVENTS", "Triggering events refresh after $action for eventId=${event.id}")
                            onReservationChanged()

                            // Send confirmation email only when reserving
                            if (isReserved) {
                                try {
                                    val (code, msg) = SupabaseClient.sendConfirmationEmail(
                                        userEmail = userEmail,
                                        userName = "User",
                                        event = event,
                                        accessToken = userAccessToken
                                    )
                                    Log.d("EMAIL", msg)
                                } catch (e: Exception) {
                                    Log.e("EMAIL", "Failed to send email", e)
                                }
                                showDialog = true
                            }
                        } else {
                            Log.e("EVENTS", "Ticket $action failed for eventId=${event.id}, userId=$userId")
                        }

                        isProcessing = false
                    }
                },
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        isProcessing -> "Processing…"
                        isReserved -> "Cancel Reservation"
                        else -> "Reserve Ticket"
                    }
                )
            }
        }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("OK")
                    }
                },
                title = { Text("Confirmation") },
                text = {
                    Text("Your ticket has been reserved and a confirmation email has been sent!")
                }
            )
        }
    }
}

@Composable
fun EventsLoadingScreen(
    userId: String,
    userAccessToken: String,
    userEmail: String
 
) {
    var events by remember { mutableStateOf<List<Event>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    val refreshEventsFromSupabase: suspend () -> Unit = {
        Log.d("EVENTS", "Events refresh triggered")
        val result = SupabaseClient.fetchEvents(userAccessToken, userId)
        if (result.errorMessage == null) {
            events = result.events.orEmpty()
            error = null
            val ticketSnapshot = events
                ?.joinToString(separator = ", ") { "${it.id}:${it.availableTickets}:reserved=${it.isReservedByCurrentUser}" }
                .orEmpty()
            Log.d("EVENTS", "Events refresh success. count=${events?.size ?: 0} tickets=[$ticketSnapshot]")
        } else {
            error = result.errorMessage
            Log.e("EVENTS", "Events refresh failed: ${result.errorMessage}")
        }
    }

    LaunchedEffect(userAccessToken) {
        loading = true
        error = null

        refreshEventsFromSupabase()
        loading = false
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
                Text(
                    text = error ?: "Failed to load events.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        else -> {
            EventsScreen(
                events = events.orEmpty(),
                userId = userId,
                userAccessToken = userAccessToken,
                userEmail = userEmail,
                onReservationChanged = refreshEventsFromSupabase
            )
        }
    }
}
