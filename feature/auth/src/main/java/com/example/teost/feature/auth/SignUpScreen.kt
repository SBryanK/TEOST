package com.example.teost.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.example.teost.util.Validation
import com.example.teost.presentation.screens.LanguageViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onNavigateToLogin: () -> Unit = {},
    onNavigateToMain: () -> Unit = {},
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val signUpState by viewModel.signUpState.collectAsStateWithLifecycle()
    
    LaunchedEffect(signUpState) {
        when (signUpState) {
            is Resource.Success -> {
                onNavigateToMain()
            }
            else -> {}
        }
    }
    
    SignUpScreenContent(
        onNavigateToLogin = onNavigateToLogin,
        onSignUp = { email, password, confirmPassword, displayName ->
            viewModel.signUp(email, password, confirmPassword, displayName)
        },
        signUpState = signUpState,
        selectedLang = "", // centralized in Profile; hide toggle here
        onToggleLanguage = { }
    )
}

@Composable
private fun SignUpScreenContent(
    onNavigateToLogin: () -> Unit = {},
    onSignUp: (String, String, String, String) -> Unit = { _,_,_,_ -> },
    signUpState: Resource<com.example.teost.data.model.User>? = null,
    selectedLang: String = "en",
    onToggleLanguage: (String) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    var email by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.height(60.dp))
            
            // Logo
            Image(
                painter = painterResource(id = R.drawable.teoc_logo),
                contentDescription = stringResource(id = R.string.edgeone_logo),
                modifier = Modifier.size(150.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Language toggle moved to Profile

            // Title
            Text(
                text = stringResource(R.string.signup_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.signup_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Name Field
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(R.string.full_name_required)) },
                placeholder = { Text(stringResource(R.string.full_name_placeholder)) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = stringResource(id = R.string.cd_name),
                        tint = TencentBlue
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
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
            // Helper removed per request
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email_hint)) },
                placeholder = { Text(stringResource(R.string.email_placeholder)) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Email,
                        contentDescription = stringResource(id = R.string.cd_email),
                        tint = TencentBlue
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                shape = com.example.teost.core.ui.theme.TextFieldShape,
                modifier = Modifier.fillMaxWidth(),
                isError = email.isNotBlank() && !Validation.isLikelyEmail(email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TencentBlue,
                    focusedLabelColor = TencentBlue,
                    cursorColor = TencentBlue
                )
            )
            if (email.isNotBlank() && !Validation.isLikelyEmail(email)) {
                Text(
                    text = stringResource(R.string.email_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password_hint)) },
                placeholder = { Text(stringResource(R.string.password_placeholder)) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = stringResource(id = R.string.cd_password),
                        tint = TencentBlue
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                shape = com.example.teost.core.ui.theme.TextFieldShape,
                modifier = Modifier.fillMaxWidth(),
                isError = password.isNotBlank() && password.length < 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TencentBlue,
                    focusedLabelColor = TencentBlue,
                    cursorColor = TencentBlue
                )
            )
            if (password.isNotBlank() && password.length < 6) {
                Text(
                    text = stringResource(R.string.password_too_short),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Sign Up Button
            val emailValid = Validation.isLikelyEmail(email)
            val passwordValid = password.length >= 6
            val confirmValid = confirmPassword == password && confirmPassword.isNotBlank()
            val canSubmit = emailValid && passwordValid && confirmValid && displayName.isNotBlank() && signUpState !is Resource.Loading
            Button(
                onClick = { onSignUp(email, password, confirmPassword, displayName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = canSubmit,
                shape = com.example.teost.core.ui.theme.ButtonShape,
                colors = com.example.teost.core.ui.theme.PrimaryCtaColors()
            ) {
                if (signUpState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.signup_button),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Error Message
            AnimatedVisibility(visible = signUpState is Resource.Error) {
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
                        text = (signUpState as? Resource.Error)?.message ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Divider with OR
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.or_separator),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Login Link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.already_have_account).replace(" Login", ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        text = stringResource(R.string.login_button),
                        color = TencentBlue,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SignUpScreenPreview() {
    EdgeOneTheme {
        SignUpScreenContent(signUpState = null)
    }
}

// Dark preview disabled per light-only requirement