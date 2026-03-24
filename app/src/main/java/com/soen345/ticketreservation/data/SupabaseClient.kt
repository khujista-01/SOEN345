package com.soen345.ticketreservation.data

import android.util.Log
import com.soen345.ticketreservation.BuildConfig
import com.soen345.ticketreservation.ui.events_page.Event
import com.soen345.ticketreservation.admin.AdminEvent
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray

object SupabaseClient {

    private const val TAG = "SUPABASE_TEST"
    private val http = OkHttpClient()
    //private const val BASE_URL = "${BuildConfig.SUPABASE_URL}/rest/v1"
    var BASE_URL = "${BuildConfig.SUPABASE_URL}/rest/v1"
    fun getBaseUrl() = BASE_URL // optional getter

    fun fetchCurrentUserIsAdmin(accessToken: String, userId: String): Pair<Int, Boolean> {
        val url = "$BASE_URL/users?id=eq.$userId&select=is_admin&limit=1"

        val req = Request.Builder()
            .url(url)
            .get()
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Accept", "application/json")
            .build()

        return try {
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                Log.d(TAG, "FETCH USER ADMIN CODE=${resp.code}")
                Log.d(TAG, "FETCH USER ADMIN BODY=$raw")

                if (!resp.isSuccessful) return resp.code to false

                val arr = JSONArray(raw)
                if (arr.length() == 0) return resp.code to false

                val isAdmin = arr.getJSONObject(0).optBoolean("is_admin", false)
                resp.code to isAdmin
            }
        } catch (e: Exception) {
            Log.d(TAG, "fetchCurrentUserIsAdmin exception=${e.message}")
            0 to false
        }
    }

    private fun adminEventFromJson(obj: JSONObject): AdminEvent {
        return AdminEvent(
            id = obj.optString("id"),
            title = obj.optString("title"),
            description = obj.optString("description"),
            location = obj.optString("location"),
            date = obj.optString("date"),
            availableTickets = obj.optInt("available_tickets"),
            price = obj.optDouble("price"),
            categoryId = obj.optString("category"),
            isCancelled = obj.optBoolean("is_cancelled", false)
        )
    }

    fun fetchAdminEvents(accessToken: String): Pair<Int, List<AdminEvent>> {
        val url = "$BASE_URL/events?select=*"

        val req = Request.Builder()
            .url(url)
            .get()
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Accept", "application/json")
            .build()

        return try {
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                Log.d(TAG, "FETCH ADMIN EVENTS CODE=${resp.code}")
                Log.d(TAG, "FETCH ADMIN EVENTS BODY=$raw")

                if (!resp.isSuccessful) return resp.code to emptyList()

                val arr = JSONArray(raw)
                val events = mutableListOf<AdminEvent>()
                for (i in 0 until arr.length()) {
                    events.add(adminEventFromJson(arr.getJSONObject(i)))
                }
                resp.code to events
            }
        } catch (e: Exception) {
            Log.d(TAG, "fetchAdminEvents exception=${e.message}")
            0 to emptyList()
        }
    }

    fun addAdminEvent(accessToken: String, event: AdminEvent): Pair<Int, String> {
        val url = "$BASE_URL/events"

        val bodyJson = JSONObject().apply {
            put("id", event.id)
            put("title", event.title)
            put("description", event.description)
            put("location", event.location)
            put("date", event.date)
            put("available_tickets", event.availableTickets)
            put("price", event.price)
            put("category", event.categoryId)
            put("is_cancelled", event.isCancelled)
        }.toString()

        val req = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .addHeader("Accept", "application/json")
            .build()

        return try {
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                Log.d(TAG, "ADD ADMIN EVENT CODE=${resp.code}")
                Log.d(TAG, "ADD ADMIN EVENT BODY=$raw")
                resp.code to raw
            }
        } catch (e: Exception) {
            Log.d(TAG, "addAdminEvent exception=${e.message}")
            0 to (e.message ?: "")
        }
    }

    fun updateAdminEvent(accessToken: String, event: AdminEvent): Pair<Int, String> {
        val url = "$BASE_URL/events?id=eq.${event.id}"

        val bodyJson = JSONObject().apply {
            put("title", event.title)
            put("description", event.description)
            put("location", event.location)
            put("date", event.date)
            put("available_tickets", event.availableTickets)
            put("price", event.price)
            put("category", event.categoryId)
            put("is_cancelled", event.isCancelled)
        }.toString()

        val req = Request.Builder()
            .url(url)
            .method("PATCH", bodyJson.toRequestBody("application/json".toMediaType()))
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .addHeader("Accept", "application/json")
            .build()

        return try {
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                Log.d(TAG, "UPDATE ADMIN EVENT CODE=${resp.code}")
                Log.d(TAG, "UPDATE ADMIN EVENT BODY=$raw")
                resp.code to raw
            }
        } catch (e: Exception) {
            Log.d(TAG, "updateAdminEvent exception=${e.message}")
            0 to (e.message ?: "")
        }
    }

    fun cancelAdminEvent(accessToken: String, eventId: String): Pair<Int, String> {
        val url = "$BASE_URL/events?id=eq.$eventId"
        val bodyJson = JSONObject().apply {
            put("is_cancelled", true)
        }.toString()

        val req = Request.Builder()
            .url(url)
            .method("PATCH", bodyJson.toRequestBody("application/json".toMediaType()))
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .addHeader("Accept", "application/json")
            .build()

        return try {
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                Log.d(TAG, "CANCEL ADMIN EVENT CODE=${resp.code}")
                Log.d(TAG, "CANCEL ADMIN EVENT BODY=$raw")
                resp.code to raw
            }
        } catch (e: Exception) {
            Log.d(TAG, "cancelAdminEvent exception=${e.message}")
            0 to (e.message ?: "")
        }
    }

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
