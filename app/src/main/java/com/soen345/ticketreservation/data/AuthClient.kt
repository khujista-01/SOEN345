package com.soen345.ticketreservation.data

import android.util.Log
import com.soen345.ticketreservation.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object AuthClient {

    private const val TAG = "AUTH"
    private val http = OkHttpClient()

    data class AuthSession(
        val userId: String,
        val email: String,
        val accessToken: String
    )

    fun signUp(email: String, password: String): Pair<Int, String> {
        val url = "${BuildConfig.SUPABASE_URL}/auth/v1/signup"

        val json = JSONObject().apply {
            put("email", email)
            put("password", password)
        }

        val req = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .build()

        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            Log.d(TAG, "signUp code=${resp.code} body=$body")
            return resp.code to body
        }
    }

    fun signIn(email: String, password: String): AuthSession? {
        val url = "${BuildConfig.SUPABASE_URL}/auth/v1/token?grant_type=password"

        val json = JSONObject().apply {
            put("email", email)
            put("password", password)
        }

        val req = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .build()

        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            Log.d(TAG, "signIn code=${resp.code} body=$body")

            if (!resp.isSuccessful) return null

            val obj = JSONObject(body)
            val accessToken = obj.optString("access_token", "")
            val userObj = obj.optJSONObject("user")
            val userId = userObj?.optString("id", "") ?: ""
            val em = userObj?.optString("email", "") ?: email

            if (accessToken.isBlank() || userId.isBlank()) return null
            return AuthSession(userId = userId, email = em, accessToken = accessToken)
        }
    }
    fun signOut(accessToken: String): Pair<Int, String> {
        val url = "${BuildConfig.SUPABASE_URL}/auth/v1/logout"

        // Supabase logout expects a POST; an empty JSON body is fine.
        val emptyBody = "{}".toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url(url)
            .post(emptyBody)
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .build()

        return try {
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                Log.d(TAG, "signOut code=${resp.code} body=$body")
                resp.code to body
            }
        } catch (e: Exception) {
            Log.d(TAG, "signOut exception=${e.message}")
            0 to (e.message ?: "")
        }
    }
}