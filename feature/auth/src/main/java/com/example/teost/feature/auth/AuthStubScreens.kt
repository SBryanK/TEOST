package com.example.teost.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.teost.core.ui.theme.EdgeOneTheme
import androidx.compose.ui.res.stringResource

// Moved to separate files:
// - SignUpScreen.kt
// - ForgotPasswordScreen.kt

// Email verification flow removed per product decision

@Composable
fun EmailVerificationContent(
    verified: Boolean,
    cooldown: Int,
    onResend: () -> Unit,
    onRefresh: () -> Unit,
    onVerifiedNavigate: () -> Unit = {}
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(id = com.example.teost.core.ui.R.string.verify_email_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(id = com.example.teost.core.ui.R.string.verify_email_message))

        if (verified) {
            Text(stringResource(id = com.example.teost.core.ui.R.string.email_verified_redirecting))
            LaunchedEffect(verified) { onVerifiedNavigate() }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onResend, enabled = cooldown == 0) {
                Text(if (cooldown == 0) "Resend Email" else "Resend in ${cooldown}s")
            }
            OutlinedButton(onClick = onRefresh, enabled = !verified) { Text(stringResource(id = com.example.teost.core.ui.R.string.refresh_status)) }
        }
    }
}

// Preview removed
