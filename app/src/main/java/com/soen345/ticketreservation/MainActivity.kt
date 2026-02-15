package com.soen345.ticketreservation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.soen345.ticketreservation.ui.theme.TicketReservationTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.soen345.ticketreservation.data.SupabaseClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TicketReservationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        lifecycleScope.launch {
            val jsonBody = """
                {
                    "email": "test@example.com",
                    "password_hash": "fakehash123"
                }
            """.trimIndent()

            val request = SupabaseClient.insertUserRequest(jsonBody)

            withContext(Dispatchers.IO) {
                SupabaseClient.http().newCall(request).execute().use { response ->
                    android.util.Log.d("SUPABASE_TEST", "CODE=${response.code}")
                    android.util.Log.d("SUPABASE_TEST", "BODY=${response.body?.string()}")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TicketReservationTheme {
        Greeting("Android")
    }
}