package com.soen345.ticketreservation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.soen345.ticketreservation.data.AuthClient
import com.soen345.ticketreservation.data.SupabaseClient
import com.soen345.ticketreservation.ui.theme.TicketReservationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.soen345.ticketreservation.ui.events_page.EventsDummyScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TicketReservationTheme {
                AppRoot()
            }
        }
    }
}

private enum class AuthMode { LOGIN, REGISTER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot() {
    val scope = rememberCoroutineScope()

    var session by remember { mutableStateOf<AuthClient.AuthSession?>(null) }
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Events Browsing page") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    if (session != null) {
                        TextButton(
                            onClick = {
                                val token = session?.accessToken.orEmpty()
                                if (token.isBlank()) {
                                    session = null
                                    return@TextButton
                                }
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        AuthClient.signOut(token)
                                    }
                                    session = null
                                }
                            }
                        ) {
                            Text("Logout")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (session == null) {
                AuthTabs(mode = mode, onModeChange = { mode = it })
                Spacer(Modifier.height(14.dp))

                when (mode) {
                    AuthMode.LOGIN -> LoginCard(
                        onLoginSuccess = { session = it }
                    )

                    AuthMode.REGISTER -> RegisterCard(
                        onRegisterSuccess = {
                            if (it.accessToken.isNotBlank()) session = it
                        }
                    )
                }
            } else {
                EventsDummyScreen(
                    userId = session!!.userId,
                    userAccessToken = session!!.accessToken
                )
            }
        }
    }
}

@Composable
private fun AuthTabs(mode: AuthMode, onModeChange: (AuthMode) -> Unit) {
    TabRow(selectedTabIndex = if (mode == AuthMode.LOGIN) 0 else 1) {
        Tab(
            selected = mode == AuthMode.LOGIN,
            onClick = { onModeChange(AuthMode.LOGIN) },
            text = { Text("Login") }
        )
        Tab(
            selected = mode == AuthMode.REGISTER,
            onClick = { onModeChange(AuthMode.REGISTER) },
            text = { Text("Register") }
        )
    }
}

@Composable
private fun LoginCard(onLoginSuccess: (AuthClient.AuthSession) -> Unit) {
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Welcome back", style = MaterialTheme.typography.titleLarge)
            Text("Login with your email and password.", style = MaterialTheme.typography.bodyMedium)

            HorizontalDivider()

            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            message?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = {
                    message = null

                    if (!isValidEmail(email)) {
                        message = "Please enter a valid email."
                        return@Button
                    }
                    if (password.length < 8) {
                        message = "Password must be at least 8 characters."
                        return@Button
                    }

                    loading = true
                    scope.launch {
                        val sess = withContext(Dispatchers.IO) {
                            AuthClient.signIn(email, password)
                        }

                        loading = false

                        if (sess == null || sess.accessToken.isBlank()) {
                            message = "Login failed. Check your email/password."
                        } else {
                            Log.d("AUTH", "Login success userId=${sess.userId} email=${sess.email}")
                            onLoginSuccess(sess)
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (loading) CircularProgressIndicator(strokeWidth = 2.dp)
                    Text(if (loading) "Logging in…" else "Login")
                }
            }

            Text(
                text = "If you can’t login after registering, check if email confirmation is enabled in Supabase Auth settings.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RegisterCard(onRegisterSuccess: (AuthClient.AuthSession) -> Unit) {
    val scope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var successNote by remember { mutableStateOf<String?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Create an account", style = MaterialTheme.typography.titleLarge)
            Text("Register with Supabase Auth, then store your profile in the database.", style = MaterialTheme.typography.bodyMedium)

            HorizontalDivider()

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full name (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it },
                label = { Text("Confirm password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            message?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            successNote?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = {
                    message = null
                    successNote = null

                    if (!isValidEmail(email)) {
                        message = "Please enter a valid email."
                        return@Button
                    }
                    if (password.length < 8) {
                        message = "Password must be at least 8 characters."
                        return@Button
                    }
                    if (password != confirm) {
                        message = "Passwords do not match."
                        return@Button
                    }

                    loading = true
                    scope.launch {
                        // 1️⃣ Sign up
                        val signUpResult = withContext(Dispatchers.IO) {
                            AuthClient.signUp(email, password)
                        }

                        if (signUpResult.first !in 200..299) {
                            loading = false
                            message = "Registration failed. Try a different email."
                            return@launch
                        }

                        // 2️⃣ Immediately sign in to obtain real access token
                        val sess = withContext(Dispatchers.IO) {
                            AuthClient.signIn(email, password)
                        }

                        if (sess == null || sess.accessToken.isBlank()) {
                            loading = false
                            message = "Registered, but login failed (no access token)."
                            return@launch
                        }

                        Log.d("AUTH", "Signup+Signin userId=${sess.userId} email=${sess.email}")

                        // 3️⃣ Upsert profile row into public.users
                        val (dbCode, dbBody) = withContext(Dispatchers.IO) {
                            SupabaseClient.upsertUserProfile(
                                accessToken = sess.accessToken,
                                userId = sess.userId,
                                email = sess.email,
                                fullName = fullName.takeIf { it.isNotBlank() },
                                phone = phone.takeIf { it.isNotBlank() }
                            )
                        }

                        loading = false

                        if (dbCode in 200..299) {
                            successNote = "Registered ✅ Profile saved ✅"
                        } else {
//                            //logcat note to find and resolve issue
                            successNote = "Registered ✅ (profile save failed — check Logcat)"
                        }

                        onRegisterSuccess(sess)
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (loading) CircularProgressIndicator(strokeWidth = 2.dp)
                    Text(if (loading) "Creating account…" else "Create account")
                }
            }

            Text(
                text = "Passwords are handled by Supabase Auth (we do not store plaintext passwords in our DB).",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
private fun isValidEmail(email: String): Boolean {
    return email.contains('@') && email.contains('.') && email.length >= 5
}