package com.soen345.ticketreservation.data

import com.soen345.ticketreservation.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object SupabaseClient {
    private val client = OkHttpClient()
    private val json = "application/json; charset=utf-8".toMediaType()

    fun http(): OkHttpClient = client

    fun insertUserRequest(bodyJson: String): Request {
        val url = "${BuildConfig.SUPABASE_URL}/rest/v1/users"

        return Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody(json))
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .build()
    }
}