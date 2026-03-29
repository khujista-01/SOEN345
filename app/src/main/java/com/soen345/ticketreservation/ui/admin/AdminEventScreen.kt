package com.soen345.ticketreservation.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soen345.ticketreservation.admin.AdminEvent
import com.soen345.ticketreservation.admin.AdminEventManager

@Composable
fun AdminEventScreen(
    manager: AdminEventManager,
    onBackToNormal: () -> Unit
) {
    var id by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var availableTickets by remember { mutableStateOf("") }

    var events by remember { mutableStateOf(manager.getAllEvents()) }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun refreshEvents() {
        events = manager.getAllEvents()
    }

    fun buildEventFromForm(): AdminEvent {
        val parsedPrice = price.toDoubleOrNull()
            ?: throw IllegalArgumentException("Price must be a valid number")
        val parsedTickets = availableTickets.toIntOrNull()
            ?: throw IllegalArgumentException("Available tickets must be a valid integer")

        return AdminEvent(
            id = id,
            title = title,
            description = description,
            categoryId = categoryId,
            location = location,
            date = date,
            availableTickets = parsedTickets,
            price = parsedPrice
        )
    }

    fun showSuccess(text: String) {
        isError = false
        message = text
    }

    fun showError(text: String) {
        isError = true
        message = text
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Admin Event Management",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = id,
            onValueChange = { id = it },
            label = { Text("id") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("description") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = categoryId,
            onValueChange = { categoryId = it },
            label = { Text("categoryId") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("location") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("date") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("price") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = availableTickets,
            onValueChange = { availableTickets = it },
            label = { Text("availableTickets") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    try {
                        manager.addEvent(buildEventFromForm())
                        refreshEvents()
                        showSuccess("Event added successfully.")
                    } catch (e: Exception) {
                        showError(e.message ?: "Failed to add event.")
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Add Event")
            }

            Button(
                onClick = {
                    try {
                        manager.editEvent(buildEventFromForm())
                        refreshEvents()
                        showSuccess("Event edited successfully.")
                    } catch (e: Exception) {
                        showError(e.message ?: "Failed to edit event.")
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Edit Event")
            }
        }

        Button(
            onClick = {
                try {
                    manager.cancelEvent(id)
                    refreshEvents()
                    showSuccess("Event cancelled successfully.")
                } catch (e: Exception) {
                    showError(e.message ?: "Failed to cancel event.")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel Event")
        }

        Button(
            onClick = onBackToNormal,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Normal View")
        }

        if (message.isNotBlank()) {
            Text(
                text = message,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "All Events",
            style = MaterialTheme.typography.titleMedium
        )

        events.forEach { event ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("ID: ${event.id}")
                    Text("Title: ${event.title}")
                    Text("Description: ${event.description}")
                    Text("Category: ${event.categoryId}")
                    Text("Location: ${event.location}")
                    Text("Date: ${event.date}")
                    Text("Price: ${event.price}")
                    Text("Available Tickets: ${event.availableTickets}")
                    Text("Cancelled: ${event.isCancelled}")
                }
            }
        }
    }
}