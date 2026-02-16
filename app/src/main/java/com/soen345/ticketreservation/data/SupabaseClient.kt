package com.soen345.ticketreservation.data

import android.util.Log
import com.soen345.ticketreservation.BuildConfig
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
}