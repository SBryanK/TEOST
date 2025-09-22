package com.example.teost.presentation.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.teost.core.ui.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.example.teost.presentation.screens.StubScreen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.teost.core.ui.theme.EdgeOneTheme
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.teost.presentation.screens.LanguageViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToCredits: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val vm: ProfileViewModel = hiltViewModel()
    val email by vm.email.collectAsState()
    val langVm: LanguageViewModel = hiltViewModel()
    val selectedLang by langVm.selectedLanguage.collectAsStateWithLifecycle()
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val snack = remember { SnackbarHostState() }
    var justToggled by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(justToggled) {
        if (justToggled) {
            val msg = if (selectedLang.startsWith("en"))
                ctx.getString(com.example.teost.core.ui.R.string.language_set_english)
            else
                ctx.getString(com.example.teost.core.ui.R.string.language_updated)
            snack.showSnackbar(message = msg)
            justToggled = false
        }
    }
    val scope = rememberCoroutineScope()
    Scaffold(topBar = {}, snackbarHost = { SnackbarHost(snack) }, contentWindowInsets = WindowInsets(0.dp)) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 40.dp, // Increased top margin
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.nav_profile),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        painter = painterResource(id = R.drawable.teoc_logo),
                        contentDescription = null,
                        modifier = Modifier.size(150.dp) // Increased by 20% (125 * 1.2 = 150)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (email.isNotBlank()) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val isEn = selectedLang.startsWith("en")
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = isEn,
                            onClick = { if (!isEn) { langVm.setLanguage("en"); justToggled = true } },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.lang_en)) }
                        SegmentedButton(
                            selected = !isEn,
                            onClick = { if (isEn) { langVm.setLanguage("zh-Hans"); justToggled = true } },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.lang_zh)) }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item {
                ProfileMenuItemCard(
                    icon = Icons.Filled.VerifiedUser,
                    label = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.user_credits),
                    onClick = onNavigateToCredits
                )
            }
            item {
                ProfileMenuItemCard(
                    icon = Icons.Filled.Lock,
                    label = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.privacy_policy),
                    onClick = onNavigateToPrivacy
                )
            }
            item {
                ProfileMenuItemCard(
                    icon = Icons.Filled.Info,
                    label = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help),
                    onClick = onNavigateToHelp
                )
            }
            item {
                ProfileMenuItemCard(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    label = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.logout),
                    onClick = { showLogoutConfirm = true }
                )
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.sign_out_title)) },
            text = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.sign_out_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    vm.logout { ok -> if (ok) onLogout() }
                }) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.sign_out)) }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.cancel)) } }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    EdgeOneTheme { ProfileScreen() }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading icon container
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun ProfileMenuItemCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = com.example.teost.core.ui.theme.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp) // Increased by 20% (40 * 1.2 = 48)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp) // Increased icon size by 20% (24 * 1.2 = 28.8 ≈ 28)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: CreditsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.user_credits)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.back))
                }
            }
        )
    }) { padding ->
        Box(Modifier.padding(padding)) {
            CreditsScreenContent(
                state = state,
                onRequest = { callback -> viewModel.requestTokens(callback) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreenContent(
    state: CreditsViewModel.UiState,
    onRequest: (onResult: (Boolean, String) -> Unit) -> Unit = {}
) {
    val snack = remember { SnackbarHostState() }
    var isSubmitting by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snack.showSnackbar(msg)
            snackbarMessage = null
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.user_credits)) }) },
        snackbarHost = { SnackbarHost(snack) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card { 
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.label_used), style = MaterialTheme.typography.labelMedium)
                        Text(state.used.toString(), style = MaterialTheme.typography.headlineMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.label_remaining), style = MaterialTheme.typography.labelMedium)
                        Text(state.remaining.toString(), style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
            Button(
                enabled = !isSubmitting,
                onClick = {
                    isSubmitting = true
                    onRequest { ok, msg ->
                        isSubmitting = false
                        if (ok) {
                            snackbarMessage = context.getString(com.example.teost.core.ui.R.string.credit_request_submitted, msg)
                        } else {
                            snackbarMessage = msg
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.request_credits)) }
            Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.request_credits_hint))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreditsScreenPreview() {
    EdgeOneTheme {
        CreditsScreenContent(state = CreditsViewModel.UiState(used = 12, remaining = 88))
    }
}

// FavoritesScreen and SettingsScreen removed per product decision

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit = {}
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.back)) }
            }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_body),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item { HelpSection(title = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_quick_start), bullets = listOf(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_quick_start_points))) }
            item { HelpSection(title = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_navigation), bullets = listOf(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_nav_points))) }
            item { HelpSection(title = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_tests_screen), bullets = listOf(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_tests_points))) }
            item { HelpSection(title = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_configure), bullets = listOf(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_configure_points))) }
            item { HelpSection(title = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_flow), bullets = listOf(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_flow_points))) }
            item { HelpSection(title = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_search), bullets = listOf(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_search_points))) }
            item { HelpSection(title = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_results), bullets = listOf(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_results_points))) }
            item { HelpSection(title = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_import_export), bullets = listOf(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_import_export_points))) }
            item { HelpSection(title = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_profile_language), bullets = listOf(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_profile_language_points))) }
            item { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_faq_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            item { FaqItemInline(q = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_faq_q1), a = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_faq_a1)) }
            item { FaqItemInline(q = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_faq_q2), a = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_faq_a2)) }
            item { FaqItemInline(q = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_faq_q3), a = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_faq_a3)) }
            item { FaqItemInline(q = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_faq_q4), a = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_faq_a4)) }
            item { FaqItemInline(q = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_faq_q5), a = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_faq_a5)) }
            item { FaqItemInline(q = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_faq_q6), a = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_faq_a6)) }
            item { FaqItemInline(q = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_faq_q7), a = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.help_faq_a7)) }
        }
    }
}

@Composable
private fun HelpSection(title: String, bullets: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        bullets.forEach { b -> Text("• $b", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FaqItemInline(q: String, a: String) {
    var expanded by remember { mutableStateOf(false) }
    ElevatedCard(
        shape = com.example.teost.core.ui.theme.CardShape,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(q, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (expanded) {
                Spacer(Modifier.height(6.dp))
                Text(a, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onNavigateBack: () -> Unit = {}
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.privacy_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.back)) }
            }
        )
    }) { padding ->
        val context = androidx.compose.ui.platform.LocalContext.current
        val policyText = remember {
            runCatching {
                context.resources.openRawResource(com.example.teost.core.ui.R.raw.privacy_policy)
                    .bufferedReader()
                    .use { it.readText() }
            }.getOrNull()
        }
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (policyText != null) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text(
                                text = policyText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                Text(
                    androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.privacy_body),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// About screen removed per product decision
