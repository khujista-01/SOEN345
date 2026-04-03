package com.soen345.ticketreservation.data

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
    // AFTER — safe, falls back to empty string if anything goes wrong
    internal var BASE_URL: String = runCatching { BuildConfig.SUPABASE_URL as? String }.getOrNull() ?: ""
    internal var ANON_KEY: String = runCatching { BuildConfig.SUPABASE_ANON_KEY as? String }.getOrNull() ?: ""
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
            println("[$TAG] Using BASE_URL=$BASE_URL")
            val req = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("apikey", ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                println("[$TAG] signUp code=${resp.code} body=$body")
                resp.code to body
            }
        } catch (e: Exception) {
            println("[$TAG] signUp crashed: ${e.message}")
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
                println("[$TAG] signIn code=${resp.code} body=$body")
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
            println("[$TAG] signIn crashed: ${e.message}")
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
                println("[$TAG] signOut code=${resp.code} body=$body")
                resp.code to body
            }
        } catch (e: Exception) {
            println("[$TAG] signOut crashed: ${e.message}")
            0 to (e.message ?: "")
        }
    }
}