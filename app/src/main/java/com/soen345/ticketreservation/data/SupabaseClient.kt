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

    private const val TAG = "SUPABASE_TEST"
    private val http = OkHttpClient()
    //private const val BASE_URL = "${BuildConfig.SUPABASE_URL}/rest/v1"
    var BASE_URL = "${BuildConfig.SUPABASE_URL}/rest/v1"
    fun getBaseUrl() = BASE_URL // optional getter
    fun upsertUserProfile(
        accessToken: String,
        userId: String,
        email: String,
        fullName: String?,
        phone: String?
    ): Pair<Int, String> {

        val url =
            //"${BuildConfig.SUPABASE_URL}/rest/v1/users?on_conflict=id"
            "$BASE_URL/users?on_conflict=id"

        val json = JSONObject().apply {
            put("id", userId)                 // MUST match auth.uid()
            put("email", email)
            put("full_name", fullName ?: JSONObject.NULL)
            put("phone", phone ?: JSONObject.NULL)
        }

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
            .build()

        Log.d(TAG, "REQUEST upsert users: authPresent=${accessToken.isNotBlank()} userId=$userId email=$email")

        http.newCall(req).execute().use { resp ->
            val respBody = resp.body?.string().orEmpty()
            Log.d(TAG, "CODE=${resp.code}")
            Log.d(TAG, "BODY=$respBody")
            return resp.code to respBody
        }
    }

    //added this for browsing events
    fun fetchEvents(accessToken: String): List<Event>? {

        val url =
            //"${BuildConfig.SUPABASE_URL}/rest/v1/events?select=*"
            "$BASE_URL/events?select=*"
        val req = Request.Builder()
            .url(url)
            .get()
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        Log.d(TAG, "REQUEST fetch events")

        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            Log.d(TAG, "CODE=${resp.code}")
            Log.d(TAG, "BODY=$body")

            if (!resp.isSuccessful) return null

            val jsonArray = org.json.JSONArray(body)
            val events = mutableListOf<Event>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

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

            return events
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
            val body = adminEventToJson(event, includeId = true)
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


    suspend fun insertReservation(eventId: String, userId: String, accessToken: String): Boolean {
        val json = JSONObject().apply {
            put("event_id", eventId)
            put("user_id", userId)
        }

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/reservations") // Your table name
            .addHeader("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imx4Y3N3dWlqcnB3ZmVndmlmdHB3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzExNzIyMTQsImV4cCI6MjA4Njc0ODIxNH0.J_VXqZljBs_nkGApzF4PwhCVx-K0es5p474iOvTPYiY")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(requestBody)
            .build()

        return try {
            http.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error inserting reservation", e)
            false
        }
    }

    suspend fun deleteReservation(eventId: String, userId: String, accessToken: String): Boolean {
        val request = Request.Builder()
            .url("$BASE_URL/reservations?event_id=eq.$eventId&user_id=eq.$userId")
            .addHeader("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imx4Y3N3dWlqcnB3ZmVndmlmdHB3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzExNzIyMTQsImV4cCI6MjA4Njc0ODIxNH0.J_VXqZljBs_nkGApzF4PwhCVx-K0es5p474iOvTPYiY")
            .addHeader("Authorization", "Bearer $accessToken")
            .delete()
            .build()

        return try {
            http.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error deleting reservation", e)
            false
        }
    }
}
