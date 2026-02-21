package com.soen345.ticketreservation.ui.events_page

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import kotlinx.coroutines.launch

@Composable
fun TicketButton(eventId: String, userId: String, accessToken: String) {
    var reserved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            scope.launch {
                if (!reserved) {
                    // Reserve ticket
                    val success = TicketActions.reserveTicket(eventId, userId, accessToken)
                    if (success) reserved = true
                } else {
                    // Cancel reservation
                    val success = TicketActions.cancelReservation(eventId, userId, accessToken)
                    if (success) reserved = false
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (reserved) "Cancel Reservation" else "Reserve Ticket")
    }
}