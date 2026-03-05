package com.soen345.ticketreservation.ui.events_page

import android.util.Log
import com.soen345.ticketreservation.data.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TicketActions {

    suspend fun reserveTicket(eventId: String, userId: String, accessToken: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Call Supabase API to insert a reservation
                val result = SupabaseClient.insertReservation(eventId, userId, accessToken)
                if (result) {
                    Log.d("TicketActions", "Reserved ticket for event $eventId by user $userId")
                } else {
                    Log.e("TicketActions", "Failed to reserve ticket for event $eventId")
                }
                result
            } catch (e: Exception) {
                Log.e("TicketActions", "Error reserving ticket", e)
                false
            }
        }
    }

    suspend fun cancelReservation(eventId: String, userId: String, accessToken: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Call Supabase API to delete a reservation
                val result = SupabaseClient.deleteReservation(eventId, userId, accessToken)
                if (result) {
                    Log.d("TicketActions", "Cancelled reservation for event $eventId by user $userId")
                } else {
                    Log.e("TicketActions", "Failed to cancel reservation for event $eventId")
                }
                result
            } catch (e: Exception) {
                Log.e("TicketActions", "Error cancelling reservation", e)
                false
            }
        }
    }
}