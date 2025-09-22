package com.example.teost.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.teost.core.ui.R
import com.example.teost.core.ui.theme.*
import com.example.teost.util.Resource
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val resetPasswordState by viewModel.resetPasswordState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    
    var email by rememberSaveable { mutableStateOf("") }
    var emailSent by rememberSaveable { mutableStateOf(false) }
    
    LaunchedEffect(resetPasswordState) {
        when (resetPasswordState) {
            is Resource.Success -> {
                emailSent = true
            }
            else -> {}
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, top = 20.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(id = com.example.teost.core.ui.R.string.back),
                        tint = TencentBlue
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Logo
            Image(
                painter = painterResource(id = R.drawable.teoc_logo),
                contentDescription = stringResource(id = com.example.teost.core.ui.R.string.edgeone_logo),
                modifier = Modifier.size(148.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (emailSent) {
                // Success State
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = com.example.teost.core.ui.theme.CardShape
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = stringResource(id = com.example.teost.core.ui.R.string.success),
                            tint = SuccessDark,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = stringResource(id = com.example.teost.core.ui.R.string.reset_email_hint),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = stringResource(id = com.example.teost.core.ui.R.string.reset_email_check_inbox, email),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        OutlinedButton(
                            onClick = { 
                                emailSent = false
                                viewModel.clearError()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = com.example.teost.core.ui.theme.ButtonShape,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            border = com.example.teost.core.ui.theme.OutlinedBrandBorder()
                        ) {
                            Text(
                                text = stringResource(id = com.example.teost.core.ui.R.string.send_another_email),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                // Input State
                Text(
                    text = stringResource(id = com.example.teost.core.ui.R.string.forgot_password_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(id = com.example.teost.core.ui.R.string.forgot_password_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(id = com.example.teost.core.ui.R.string.email_hint)) },
                    placeholder = { Text(stringResource(id = com.example.teost.core.ui.R.string.email_input_placeholder)) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Email,
                            contentDescription = stringResource(id = com.example.teost.core.ui.R.string.cd_email),
                            tint = TencentBlue
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            focusManager.clearFocus()
                            viewModel.sendPasswordResetEmail(email)
                        }
                    ),
                    singleLine = true,
                    shape = com.example.teost.core.ui.theme.TextFieldShape,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TencentBlue,
                        focusedLabelColor = TencentBlue,
                        cursorColor = TencentBlue
                    )
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Send Reset Email Button
                Button(
                    onClick = { viewModel.sendPasswordResetEmail(email) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = email.isNotBlank() && resetPasswordState !is Resource.Loading,
                    shape = com.example.teost.core.ui.theme.ButtonShape,
                    colors = com.example.teost.core.ui.theme.PrimaryCtaColors()
                ) {
                    if (resetPasswordState is Resource.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(id = com.example.teost.core.ui.R.string.send_reset_email),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                // Error Message
                AnimatedVisibility(visible = resetPasswordState is Resource.Error) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = (resetPasswordState as? Resource.Error)?.message ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Back to Login Link
            TextButton(onClick = onNavigateBack) {
                Text(
                    text = stringResource(id = com.example.teost.core.ui.R.string.back_to_login),
                    color = TencentBlue,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ForgotPasswordScreenPreview() {
    EdgeOneTheme {
        ForgotPasswordScreen()
    }
}

// Dark preview disabled per light-only requirement