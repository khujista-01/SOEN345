package com.soen345.ticketreservation.data

import android.util.Log
import com.soen345.ticketreservation.BuildConfig
import com.soen345.ticketreservation.ui.events_page.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object SupabaseClient {

    private const val TAG = "SUPABASE"
    private val http = OkHttpClient()
    var BASE_URL = "${BuildConfig.SUPABASE_URL}/rest/v1"

    // ... your existing functions unchanged (upsertUserProfile, fetchEvents) ...

    suspend fun insertReservation(eventId: String, userId: String, accessToken: String): Boolean {
        return withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("event_id", eventId)
                put("user_id", userId)
            }

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/reservations")
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(requestBody)
                .build()

            try {
                http.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    Log.d(TAG, "insertReservation code=${response.code} body=$body")
                    response.isSuccessful
                }
            } catch (e: Exception) {
                Log.e(TAG, "insertReservation failed", e)
                false
            }
        }  // ✅ close withContext
    }

    /** 🎫 FIXED: Production-ready email sending */
    suspend fun sendConfirmationEmail(
        userEmail: String,
        userName: String? = "User",  // Optional name
        event: Event,
        accessToken: String
    ): Pair<Int, String> {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("userEmail", userEmail)
                    put("userName", userName ?: "User")
                    put("eventTitle", event.title)
                    put("eventDate", event.date)
                    put("eventLocation", event.location)
                    put("ticketId", "TICKET-${event.id.takeLast(4)}")  // Generate ticket ID
                    put("ticketPrice", String.format("%.2f", event.price))
                }

                val body = json.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("${BuildConfig.SUPABASE_URL}/functions/v1/send_ticket_confirmation")
                    .post(body)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build()

                http.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    Log.d("EMAIL", "sendConfirmationEmail code=${response.code} body=$bodyStr")

                    if (response.isSuccessful) {
                        response.code to "✅ Ticket email sent to $userEmail!"
                    } else {
                        response.code to "❌ Email failed: $bodyStr"
                    }
                }
            } catch (e: Exception) {
                Log.e("EMAIL", "sendConfirmationEmail crashed", e)
                500 to "Network error: ${e.message}"
            }
        }
    }

    suspend fun deleteReservation(eventId: String, userId: String, accessToken: String): Boolean {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$BASE_URL/reservations?event_id=eq.$eventId&user_id=eq.$userId")
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .delete()
                .build()

            try {
                http.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    Log.d(TAG, "deleteReservation code=${response.code} body=$body")
                    response.isSuccessful
                }
            } catch (e: Exception) {
                Log.e(TAG, "deleteReservation failed", e)
                false
            }
        }
    }
    fun fetchEvents(accessToken: String): List<Event>? {
        val request = Request.Builder()
            .url("$BASE_URL/events")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        return try {
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                Log.d(TAG, "fetchEvents code=${response.code} body=$body")
                if (!response.isSuccessful) return null

                val array = org.json.JSONArray(body)
                val events = mutableListOf<Event>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    events.add(
                        Event(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            description = obj.getString("description"),
                            category = obj.getString("category"),
                            location = obj.getString("location"),
                            date = obj.getString("date"),
                            availableTickets = obj.getInt("available_tickets"),
                            price = obj.getDouble("price")
                        )
                    )
                }
                events
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchEvents failed", e)
            null
        }
    }
    fun upsertUserProfile(
        accessToken: String,
        userId: String,
        email: String,
        fullName: String?,
        phone: String?
    ): Pair<Int, String> {

        val url = "$BASE_URL/users?on_conflict=id"

        val json = JSONObject().apply {
            put("id", userId)
            put("email", email)
            put("full_name", fullName ?: JSONObject.NULL)
            put("phone", phone ?: JSONObject.NULL)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .build()

        return http.newCall(req).execute().use { resp ->
            val respBody = resp.body?.string().orEmpty()
            Log.d("PROFILE", "upsertUserProfile code=${resp.code} body=$respBody")
            resp.code to respBody
        }
    }
}