package com.example.teost.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.teost.core.ui.theme.EdgeOneTheme
import com.example.teost.util.Resource
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: EmailVerificationViewModel = hiltViewModel()
) {
    val verificationState by viewModel.verificationState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    LaunchedEffect(verificationState) {
        when (val state = verificationState) {
            is Resource.Success -> {
                if (state.data == true) {
                    Toast.makeText(context, "Email verified successfully!", Toast.LENGTH_SHORT).show()
                    onNavigateToMain()
                }
            }
            is Resource.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }
    
    EmailVerificationScreenContent(
        onNavigateToLogin = onNavigateToLogin,
        onResendVerification = { viewModel.resendVerificationEmail() },
        onCheckVerification = { viewModel.checkEmailVerification() },
        verificationState = verificationState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmailVerificationScreenContent(
    onNavigateToLogin: () -> Unit,
    onResendVerification: () -> Unit,
    onCheckVerification: () -> Unit,
    verificationState: Resource<Boolean>? = null
) {
    val isLoading = verificationState is Resource.Loading
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Email Verification") },
                navigationIcon = {
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Back to Login")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Email icon
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title
            Text(
                text = "Check Your Email",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description
            Text(
                text = "We've sent a verification link to your email address. Please check your inbox and click the link to verify your account before signing in.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Check verification button
            Button(
                onClick = onCheckVerification,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("I've Verified My Email")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Resend verification button
            OutlinedButton(
                onClick = onResendVerification,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Resend Verification Email")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Help text
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Didn't receive the email?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Check your spam/junk folder\n• Make sure you entered the correct email address\n• Wait a few minutes and try resending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmailVerificationScreenPreview() {
    EdgeOneTheme {
        EmailVerificationScreenContent(
            onNavigateToLogin = {},
            onResendVerification = {},
            onCheckVerification = {}
        )
    }
}
