package com.example.teost.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.teost.presentation.screens.LanguageViewModel
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import com.example.teost.core.ui.R
import com.example.teost.core.ui.theme.EdgeOneTheme
import com.example.teost.core.ui.theme.ErrorDark
import com.example.teost.core.ui.theme.ErrorLight
import com.example.teost.core.ui.theme.TencentBlue
import com.example.teost.util.Resource
import com.example.teost.util.Validation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    onNavigateToEmailVerification: () -> Unit = {},
    onNavigateToMain: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    var prefilledEmail by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadRememberedEmail { saved -> prefilledEmail = saved }
    }

    LaunchedEffect(loginState) {
        when (loginState) {
            is Resource.Success -> onNavigateToMain()
            else -> {}
        }
    }

    LoginScreenContent(
        onNavigateToSignUp = onNavigateToSignUp,
        onNavigateToForgotPassword = onNavigateToForgotPassword,
        onSignIn = { email, password, remember -> viewModel.signIn(email, password, remember) },
        loginState = loginState,
        initialEmail = prefilledEmail
    )
}

@Composable
private fun LoginScreenContent(
    onNavigateToSignUp: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    onSignIn: (String, String, Boolean) -> Unit = { _, _, _ -> },
    loginState: Resource<com.example.teost.data.model.User>? = null,
    initialEmail: String = ""
) {
    val focusManager = LocalFocusManager.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberMe by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialEmail) {
        if (initialEmail.isNotBlank()) {
            email = initialEmail
            rememberMe = true
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // moved language toggle below subtitle per spec

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            Image(
                painter = painterResource(id = R.drawable.teoc_logo),
                contentDescription = stringResource(id = com.example.teost.core.ui.R.string.edgeone_logo),
                modifier = Modifier.size(160.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(id = com.example.teost.core.ui.R.string.login_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Language toggle (below subtitle, smaller)
            val langVm: LanguageViewModel = hiltViewModel()
            val selectedLang by langVm.selectedLanguage.collectAsStateWithLifecycle()
            val isEn = selectedLang.startsWith("en")
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = isEn,
                    onClick = { if (!isEn) langVm.setLanguage("en") },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text(stringResource(id = com.example.teost.core.ui.R.string.lang_en)) }
                SegmentedButton(
                    selected = !isEn,
                    onClick = { if (isEn) langVm.setLanguage("zh-Hans") },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text(stringResource(id = com.example.teost.core.ui.R.string.lang_zh)) }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email_hint)) },
                placeholder = { Text(stringResource(id = com.example.teost.core.ui.R.string.email_placeholder)) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Email,
                        contentDescription = stringResource(id = com.example.teost.core.ui.R.string.cd_email),
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
                    cursorColor = TencentBlue,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    errorLabelColor = MaterialTheme.colorScheme.error,
                    errorSupportingTextColor = MaterialTheme.colorScheme.error
                )
            )
            if (email.isNotBlank() && !Validation.isLikelyEmail(email)) {
                Text(
                    text = stringResource(id = com.example.teost.core.ui.R.string.email_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password_hint)) },
                placeholder = { Text(stringResource(id = com.example.teost.core.ui.R.string.password_placeholder)) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = stringResource(id = com.example.teost.core.ui.R.string.cd_password),
                        tint = TencentBlue
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) stringResource(id = com.example.teost.core.ui.R.string.hide_password) else stringResource(id = com.example.teost.core.ui.R.string.show_password)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onSignIn(email, password, rememberMe)
                    }
                ),
                singleLine = true,
                shape = com.example.teost.core.ui.theme.TextFieldShape,
                modifier = Modifier.fillMaxWidth(),
                isError = password.isNotBlank() && password.length < 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TencentBlue,
                    focusedLabelColor = TencentBlue,
                    cursorColor = TencentBlue,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    errorLabelColor = MaterialTheme.colorScheme.error,
                    errorSupportingTextColor = MaterialTheme.colorScheme.error
                )
            )
            if (password.isNotBlank() && password.length < 6) {
                Text(
                    text = stringResource(id = com.example.teost.core.ui.R.string.password_too_short),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = TencentBlue,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = stringResource(id = com.example.teost.core.ui.R.string.remember_me),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = onNavigateToForgotPassword) {
                    Text(
                        text = stringResource(R.string.forgot_password),
                        color = TencentBlue,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val emailValid = Validation.isLikelyEmail(email)
            val canSubmit = emailValid && password.length >= 6 && loginState !is Resource.Loading
            Button(
                onClick = { onSignIn(email, password, rememberMe) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = canSubmit,
                shape = com.example.teost.core.ui.theme.ButtonShape,
                colors = com.example.teost.core.ui.theme.PrimaryCtaColors()
            ) {
                if (loginState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.login_button),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            AnimatedVisibility(visible = loginState is Resource.Error) {
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
                        text = (loginState as? Resource.Error)?.message ?: "Authentication failed. Please check your email and password.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(id = com.example.teost.core.ui.R.string.or_separator),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dont_have_account).replace(" Sign Up", ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onNavigateToSignUp) {
                    Text(
                        text = stringResource(R.string.signup_button),
                        color = TencentBlue,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    EdgeOneTheme {
        LoginScreenContent(
            onSignIn = { _, _, _ -> },
            loginState = null
        )
    }
}
