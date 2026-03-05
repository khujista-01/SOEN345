package com.soen345.ticketreservation.data

import android.util.Log
import com.soen345.ticketreservation.BuildConfig
import com.soen345.ticketreservation.ui.events_page.Event
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object SupabaseClient {

    private const val TAG = "SUPABASE_TEST"
    private val http = OkHttpClient()

    fun upsertUserProfile(
        accessToken: String,
        userId: String,
        email: String,
        fullName: String?,
        phone: String?
    ): Pair<Int, String> {

        val url =
            "${BuildConfig.SUPABASE_URL}/rest/v1/users?on_conflict=id"

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
            "${BuildConfig.SUPABASE_URL}/rest/v1/events?select=*"

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
}