package com.soen345.ticketreservation.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soen345.ticketreservation.admin.AdminEvent
import com.soen345.ticketreservation.admin.AdminEventManager

@Composable
fun AdminEventScreen(
    manager: AdminEventManager,
    onBackToNormal: () -> Unit
) {
    var events by remember { mutableStateOf(manager.getAllEvents()) }
    var showAddForm by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Admin Event Management",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = { showAddForm = !showAddForm }
        ) {
            Text(if (showAddForm) "Cancel" else "Add New Event")
        }

        if (successMessage.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Text(
                    text = successMessage,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(3000)
                successMessage = ""
            }
        }

        if (errorMessage.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        if (showAddForm) {
            AddEventForm(
                onEventCreated = { newEvent ->
                    try {
                        manager.addEvent(newEvent)
                        events = manager.getAllEvents()
                        successMessage = "Event added successfully"
                        showAddForm = false
                        errorMessage = ""
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message}"
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = "Events (${events.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        events.forEach { event ->
            EventAdminCard(
                event = event,
                onEdit = { updatedEvent ->
                    try {
                        manager.editEvent(updatedEvent)
                        events = manager.getAllEvents()
                        successMessage = "Event updated successfully"
                        errorMessage = ""
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message}"
                    }
                },
                onCancel = {
                    try {
                        manager.cancelEvent(event.id)
                        events = manager.getAllEvents()
                        successMessage = "Event cancelled successfully"
                        errorMessage = ""
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message}"
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = onBackToNormal,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Normal View")
        }
    }
}

@Composable
private fun AddEventForm(onEventCreated: (AdminEvent) -> Unit) {
    var id by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var ticketsStr by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = id,
                onValueChange = { id = it },
                label = { Text("Event ID") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = categoryId,
                onValueChange = { categoryId = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = ticketsStr,
                onValueChange = { ticketsStr = it },
                label = { Text("Available Tickets") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = priceStr,
                onValueChange = { priceStr = it },
                label = { Text("Price") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    try {
                        val tickets = ticketsStr.toIntOrNull() ?: throw IllegalArgumentException("Invalid tickets")
                        val price = priceStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid price")
                        val event = AdminEvent(
                            id = id,
                            title = title,
                            description = description,
                            categoryId = categoryId,
                            location = location,
                            date = date,
                            availableTickets = tickets,
                            price = price
                        )
                        onEventCreated(event)
                    } catch (e: Exception) {
                        // Error will be shown by parent composable
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Event")
            }
        }
    }
}

@Composable
private fun EventAdminCard(
    event: AdminEvent,
    onEdit: (AdminEvent) -> Unit,
    onCancel: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf(event.title) }
    var editedPrice by remember { mutableStateOf(event.price.toString()) }
    var editedTickets by remember { mutableStateOf(event.availableTickets.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isEditing) {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editedPrice,
                    onValueChange = { editedPrice = it },
                    label = { Text("Price") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editedTickets,
                    onValueChange = { editedTickets = it },
                    label = { Text("Tickets") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val updatedEvent = event.copy(
                                    title = editedTitle,
                                    price = editedPrice.toDouble(),
                                    availableTickets = editedTickets.toInt()
                                )
                                onEdit(updatedEvent)
                                isEditing = false
                            } catch (e: Exception) {
                                // Error will be shown by parent
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                    TextButton(
                        onClick = { isEditing = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                }
            } else {
                Text(event.title, style = MaterialTheme.typography.titleMedium)
                Text("Category: ${event.categoryId}", style = MaterialTheme.typography.bodySmall)
                Text("Location: ${event.location}", style = MaterialTheme.typography.bodySmall)
                Text("Date: ${event.date}", style = MaterialTheme.typography.bodySmall)
                Text("Tickets: ${event.availableTickets}", style = MaterialTheme.typography.bodySmall)
                Text("Price: $${event.price}", style = MaterialTheme.typography.bodySmall)
                if (event.isCancelled) {
                    Text("Status: CANCELLED", style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isEditing = true },
                        modifier = Modifier.weight(1f),
                        enabled = !event.isCancelled
                    ) {
                        Text("Edit")
                    }
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        enabled = !event.isCancelled
                    ) {
                        Text("Cancel Event")
                    }
                }
            }
        }
    }
}
