package com.soen345.ticketreservation.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import com.soen345.ticketreservation.BuildConfig

object AuthClient {

    private const val TAG = "AUTH"
    internal var BASE_URL = BuildConfig.SUPABASE_URL      // ← use BuildConfig
    private val ANON_KEY = BuildConfig.SUPABASE_ANON_KEY  // ← use BuildConfig

    internal var http: OkHttpClient = OkHttpClient()

    data class AuthSession(
        val userId: String,
        val email: String,
        val accessToken: String
    )

    suspend fun signUp(email: String, password: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/auth/v1/signup"

            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
            }

            Log.d(TAG, "Using BASE_URL=$BASE_URL")

            val req = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("apikey", ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .build()

            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                Log.d(TAG, "signUp code=${resp.code} body=$body")
                resp.code to body
            }
        } catch (e: Exception) {
            Log.e(TAG, "signUp crashed", e)
            0 to (e.message ?: "error")
        }
    }

    suspend fun signIn(email: String, password: String): AuthSession? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/auth/v1/token?grant_type=password"

            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
            }

            val req = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("apikey", ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .build()

            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                Log.d(TAG, "signIn code=${resp.code} body=$body")

                if (!resp.isSuccessful) return@withContext null

                val obj = JSONObject(body)
                val accessToken = obj.optString("access_token", "")
                val userObj = obj.optJSONObject("user")
                val userId = userObj?.optString("id", "") ?: ""
                val em = userObj?.optString("email", "") ?: email

                if (accessToken.isBlank() || userId.isBlank()) return@withContext null
                AuthSession(userId, em, accessToken)
            }
        } catch (e: Exception) {
            Log.e(TAG, "signIn crashed", e)
            null
        }
    }

    suspend fun signOut(accessToken: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/auth/v1/logout"
            val emptyBody = "{}".toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url(url)
                .post(emptyBody)
                .addHeader("apikey", ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .build()

            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                Log.d(TAG, "signOut code=${resp.code} body=$body")
                resp.code to body
            }
        } catch (e: Exception) {
            Log.e(TAG, "signOut crashed", e)
            0 to (e.message ?: "")
        }
    }
}