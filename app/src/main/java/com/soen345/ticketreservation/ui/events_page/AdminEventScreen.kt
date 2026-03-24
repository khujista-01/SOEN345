package com.soen345.ticketreservation.ui.events_page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soen345.ticketreservation.admin.AdminEvent
import com.soen345.ticketreservation.data.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun AdminEventScreen(accessToken: String, isAdmin: Boolean) {
    if (!isAdmin) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Admin access required.", style = MaterialTheme.typography.titleMedium)
            Text("You do not have permission to manage events.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val events = remember { mutableStateListOf<AdminEvent>() }

    var eventId by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var availableTickets by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("academic") }

    var editingEventId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun clearForm() {
        eventId = ""
        title = ""
        description = ""
        location = ""
        date = ""
        availableTickets = ""
        price = ""
        categoryId = "academic"
        editingEventId = null
    }

    fun buildEventFromForm(idToUse: String): AdminEvent? {
        val tickets = availableTickets.toIntOrNull()
        val priceValue = price.toDoubleOrNull()

        if (title.isBlank() || description.isBlank() || location.isBlank() || date.isBlank()) {
            message = "Please fill in all required fields."
            return null
        }

        if (tickets == null || tickets < 0) {
            message = "Available tickets must be 0 or more."
            return null
        }

        if (priceValue == null || priceValue < 0.0) {
            message = "Price must be 0 or more."
            return null
        }

        if (categoryId.isBlank()) {
            message = "Category ID is required."
            return null
        }

        return AdminEvent(
            id = idToUse,
            title = title,
            description = description,
            location = location,
            date = date,
            availableTickets = tickets,
            price = priceValue,
            categoryId = categoryId,
            isCancelled = false
        )
    }

    fun replaceEventInList(updatedEvent: AdminEvent) {
        val index = events.indexOfFirst { it.id == updatedEvent.id }
        if (index != -1) {
            events[index] = updatedEvent
        }
    }

    LaunchedEffect(accessToken) {
        val (_, remoteEvents) = withContext(Dispatchers.IO) {
            SupabaseClient.fetchAdminEvents(accessToken)
        }
        events.clear()
        events.addAll(remoteEvents)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Admin Event Management", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Administrators can add, edit, and cancel events.",
            style = MaterialTheme.typography.bodyMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    if (editingEventId == null) "Add New Event" else "Edit Event",
                    style = MaterialTheme.typography.titleMedium
                )

                if (editingEventId == null) {
                    OutlinedTextField(
                        value = eventId,
                        onValueChange = { eventId = it },
                        label = { Text("Event ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Text("Editing event ID: $editingEventId")
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("YYYY-MM-DD") }
                )

                OutlinedTextField(
                    value = availableTickets,
                    onValueChange = { availableTickets = it },
                    label = { Text("Available Tickets") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = categoryId,
                    onValueChange = { categoryId = it },
                    label = { Text("Category ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("academic / workshop / concert / sports") }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = isAdmin,
                        onClick = {
                            message = ""

                            if (editingEventId == null) {
                                val finalId = if (eventId.isBlank()) UUID.randomUUID().toString() else eventId
                                val newEvent = buildEventFromForm(finalId) ?: return@Button

                                scope.launch {
                                    val (code, _) = withContext(Dispatchers.IO) {
                                        SupabaseClient.addAdminEvent(accessToken, newEvent)
                                    }
                                    if (code in 200..299) {
                                        events.add(newEvent)
                                        message = "Event added successfully."
                                        clearForm()
                                    } else {
                                        message = "Could not add event. Check values, permissions, or duplicate ID."
                                    }
                                }
                            } else {
                                val updatedEvent = buildEventFromForm(editingEventId!!) ?: return@Button

                                scope.launch {
                                    val (code, _) = withContext(Dispatchers.IO) {
                                        SupabaseClient.updateAdminEvent(accessToken, updatedEvent)
                                    }
                                    if (code in 200..299) {
                                        replaceEventInList(updatedEvent)
                                        message = "Event updated successfully."
                                        clearForm()
                                    } else {
                                        message = "Could not edit event."
                                    }
                                }
                            }
                        }
                    ) {
                        Text(if (editingEventId == null) "Add Event" else "Save Changes")
                    }

                    if (editingEventId != null) {
                        Button(onClick = {
                            clearForm()
                            message = "Edit canceled."
                        }) {
                            Text("Cancel Edit")
                        }
                    }
                }

                if (message.isNotBlank()) {
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        HorizontalDivider()

        Text("Existing Events", style = MaterialTheme.typography.titleMedium)

        if (events.isEmpty()) {
            Text("No events available yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(events, key = { it.id }) { event ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(event.title, style = MaterialTheme.typography.titleMedium)
                            Text("ID: ${event.id}")
                            Text("Description: ${event.description}")
                            Text("Location: ${event.location}")
                            Text("Date: ${event.date}")
                            Text("Tickets: ${event.availableTickets}")
                            Text("Price: ${event.price}")
                            Text("Category ID: ${event.categoryId}")
                            Text(
                                if (event.isCancelled) "Status: Cancelled" else "Status: Active"
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        editingEventId = event.id
                                        eventId = event.id
                                        title = event.title
                                        description = event.description
                                        location = event.location
                                        date = event.date
                                        availableTickets = event.availableTickets.toString()
                                        price = event.price.toString()
                                        categoryId = event.categoryId
                                        message = "Editing ${event.id}"
                                    },
                                    enabled = isAdmin && !event.isCancelled
                                ) {
                                    Text("Edit")
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            val (code, _) = withContext(Dispatchers.IO) {
                                                SupabaseClient.cancelAdminEvent(accessToken, event.id)
                                            }
                                            if (code in 200..299) {
                                                val updated = event.copy(isCancelled = true)
                                                replaceEventInList(updated)
                                                message = "Event cancelled successfully."
                                            } else {
                                                message = "Could not cancel event."
                                            }
                                        }
                                    },
                                    enabled = isAdmin && !event.isCancelled
                                ) {
                                    Text("Cancel Event")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}