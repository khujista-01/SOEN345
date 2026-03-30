package com.soen345.ticketreservation.ui.events_page

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth

@Composable
fun TicketButton(
    isReserved: Boolean,
    onReservedChange: (Boolean) -> Unit
) {
    Button(
        onClick = { onReservedChange(!isReserved) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (isReserved) "Cancel Reservation" else "Reserve Ticket")
    }
}