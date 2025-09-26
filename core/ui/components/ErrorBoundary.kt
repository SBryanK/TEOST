package com.example.teost.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ErrorBoundary(
    onError: (Throwable) -> Unit = {},
    content: @Composable () -> Unit
) {
    val errorState = remember { mutableStateOf<Throwable?>(null) }
    
    if (errorState.value != null) {
        ErrorScreen(
            error = errorState.value!!,
            onRetry = { errorState.value = null }
        )
    } else {
        try {
            content()
        } catch (e: Throwable) {
            errorState.value = e
            onError(e)
        }
    }
}

@Composable
fun ErrorScreen(
    error: Throwable,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Something went wrong",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = error.message ?: "An unexpected error occurred",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "Try Again",
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorCard(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠️",
                fontSize = 20.sp
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            
            onRetry?.let {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = it) {
                    Text(
                        text = "Retry",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
