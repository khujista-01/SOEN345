package com.soen345.ticketreservation.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.soen345.ticketreservation.admin.AdminConstants

@Composable
fun AdminGateScreen(
    onAdminAccessGranted: () -> Unit,
    onBackToNormal: () -> Unit
) {
    var adminCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Admin Access",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = "Enter admin code to access event management",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = adminCode,
            onValueChange = {
                adminCode = it
                errorMessage = ""
            },
            label = { Text("Admin Code") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                if (adminCode == AdminConstants.ADMIN_CODE) {
                    onAdminAccessGranted()
                } else {
                    errorMessage = "Invalid admin code"
                    adminCode = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text("Submit")
        }

        TextButton(
            onClick = onBackToNormal,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Normal View")
        }
    }
}
