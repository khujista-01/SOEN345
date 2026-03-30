package com.soen345.ticketreservation.data

import android.util.Log
import com.soen345.ticketreservation.BuildConfig
import com.soen345.ticketreservation.admin.AdminEvent
import com.soen345.ticketreservation.ui.events_page.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object SupabaseClient {


    data class FetchEventsResult(
        val events: List<Event>?,
        val errorMessage: String?
    )

    private const val TAG = "SUPABASE"
    private val http = OkHttpClient()
    var BASE_URL = "${BuildConfig.SUPABASE_URL}/rest/v1"


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

    //added this for browsing events
    suspend fun fetchEvents(accessToken: String): FetchEventsResult = withContext(Dispatchers.IO) {

        val url =
            //"${BuildConfig.SUPABASE_URL}/rest/v1/events?select=*"
            "$BASE_URL/events?select=*"
        val req = Request.Builder()
            .url(url)
            .get()
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        Log.d(TAG, "REQUEST fetch events: url=$url")

        return@withContext try {
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                Log.d(TAG, "fetchEvents CODE=${resp.code}")
                Log.d(TAG, "fetchEvents BODY=$body")

                if (!resp.isSuccessful) {
                    FetchEventsResult(null, "Failed to load events (${resp.code}).")
                } else {
                    val jsonArray = org.json.JSONArray(body)
                    val events = mutableListOf<Event>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        try {
                            val requiredFields = listOf(
                                "id", "title", "description", "category", "location", "date", "available_tickets", "price"
                            )
                            val missingFields = requiredFields.filter { field -> !obj.has(field) || obj.isNull(field) }
                            if (missingFields.isNotEmpty()) {
                                throw IllegalStateException("Missing fields: ${missingFields.joinToString(", ")}")
                            }

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
                        } catch (e: Exception) {
                            Log.e(TAG, "fetchEvents parse failure at index=$i row=$obj", e)
                            return@use FetchEventsResult(
                                null,
                                "Failed to parse event data at row ${i + 1}."
                            )
                        }
                    }

                    FetchEventsResult(events, null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchEvents request failure", e)
            FetchEventsResult(null, e.message ?: "Failed to load events due to a network or parsing error.")
        }
    }

    suspend fun fetchAdminEvents(accessToken: String): Pair<List<AdminEvent>?, String?> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$BASE_URL/events?select=*")
            .get()
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        return@withContext try {
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    null to "Fetch failed (${resp.code}): $body"
                } else {
                    val jsonArray = org.json.JSONArray(body)
                    val events = mutableListOf<AdminEvent>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        events.add(
                            AdminEvent(
                                id = obj.getString("id"),
                                title = obj.optString("title"),
                                description = obj.optString("description"),
                                categoryId = obj.optString("category"),
                                location = obj.optString("location"),
                                date = obj.optString("date"),
                                availableTickets = obj.optInt("available_tickets", 0),
                                price = obj.optDouble("price", 0.0),
                                isCancelled = obj.optBoolean("is_cancelled", false)
                            )
                        )
                    }
                    events to null
                }
            }
        } catch (e: Exception) {
            null to (e.message ?: "Failed to fetch events")
        }
    }

    suspend fun insertAdminEvent(event: AdminEvent, accessToken: String): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val body = adminEventToJson(event, includeId = false)
                .toString()
                .toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url("$BASE_URL/events")
                .post(body)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build()

            return@withContext performWriteRequest(req, "insert event")
        }

    suspend fun updateAdminEvent(event: AdminEvent, accessToken: String): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val body = adminEventToJson(event, includeId = false)
                .toString()
                .toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url("$BASE_URL/events?id=eq.${event.id}")
                .patch(body)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build()

            return@withContext performWriteRequest(req, "update event")
        }

    suspend fun cancelAdminEvent(eventId: String, accessToken: String): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val cancelWithFlagBody = JSONObject()
                .put("is_cancelled", true)
                .toString()
                .toRequestBody("application/json".toMediaType())

            val cancelWithFlagReq = Request.Builder()
                .url("$BASE_URL/events?id=eq.$eventId")
                .patch(cancelWithFlagBody)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build()

            val (flagSuccess, flagMessage) = performWriteRequest(cancelWithFlagReq, "cancel event")
            if (flagSuccess) {
                return@withContext true to flagMessage
            }

            // Fallback for schemas that do not have is_cancelled.
            val fallbackBody = JSONObject()
                .put("available_tickets", 0)
                .toString()
                .toRequestBody("application/json".toMediaType())

            val fallbackReq = Request.Builder()
                .url("$BASE_URL/events?id=eq.$eventId")
                .patch(fallbackBody)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build()

            val (fallbackSuccess, fallbackMessage) = performWriteRequest(fallbackReq, "cancel event fallback")
            if (fallbackSuccess) {
                true to "Event cancelled (available_tickets set to 0)."
            } else {
                false to "Cancel failed. Primary error: $flagMessage. Fallback error: $fallbackMessage"
            }
        }

    private fun adminEventToJson(event: AdminEvent, includeId: Boolean): JSONObject {
        return JSONObject().apply {
            if (includeId) put("id", event.id)
            put("title", event.title)
            put("description", event.description)
            put("category", event.categoryId)
            put("location", event.location)
            put("date", event.date)
            put("available_tickets", event.availableTickets)
            put("price", event.price)
        }
    }

    private fun performWriteRequest(request: Request, operationName: String): Pair<Boolean, String> {
        return try {
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    true to "$operationName succeeded."
                } else {
                    false to "$operationName failed (${response.code}): $body"
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error during $operationName", e)
            false to (e.message ?: "$operationName failed")
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