package com.example.teost.feature.search

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.saveable.rememberSaveable
// import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.min
import com.example.teost.core.ui.R
import com.example.teost.data.model.ConnectionTestResult
import com.example.teost.core.ui.theme.*
import com.example.teost.util.Resource
import com.example.teost.util.AppError
import com.example.teost.util.UrlValidator
import android.widget.Toast

// Local test tag semantics
private val TestTagKey = SemanticsPropertyKey<String>("test_tag")
private var SemanticsPropertyReceiver.testTag by TestTagKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToTests: (String?) -> Unit = {},
    onNavigateToTestsWithTargets: (String?, List<String>) -> Unit = { _, _ -> },
    viewModel: SearchViewModel = hiltViewModel()
) {
    val testState by viewModel.testState.collectAsState()
    val testResults by viewModel.testResults.collectAsState()
    LaunchedEffect(Unit) { viewModel.restoreLastResultsIfFresh() }
    SearchScreenContent(
        state = testState,
        results = testResults,
        onTest = { viewModel.testConnection(it) },
        onNavigateToTests = onNavigateToTests,
        onClearError = { viewModel.clearError() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreenContent(
    state: Resource<List<ConnectionTestResult>>?,
    results: List<ConnectionTestResult>,
    onTest: (String) -> Unit,
    onNavigateToTests: (String?) -> Unit = {},
    onNavigateToTestsWithTargets: (String?, List<String>) -> Unit = { _, _ -> },
    onClearError: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var input by rememberSaveable { mutableStateOf("") }

    val isLoading = state is Resource.Loading
    val parsedTargets by remember { derivedStateOf { UrlValidator.parseMultipleInputs(input) } }
    val allValid by remember { derivedStateOf { parsedTargets.isNotEmpty() && parsedTargets.all { UrlValidator.validate(it) is UrlValidator.ValidationResult.Valid } } }
    val isTestEnabled = allValid && !isLoading
    val canGoToTests = input.isNotBlank() && results.any { it.statusCode in 200..399 } && !isLoading

    // Optional encryption ack removed for now to simplify

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp), // Increased by 50% (16 * 1.5 = 24)
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.teoc_logo),
                    contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.teoc_logo),
                    modifier = Modifier
                        .size(162.dp)
                        .semantics { testTag = "search_logo" }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.validate_connection),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { testTag = "search_title" }
                )
            }
        }

        item(key = "input_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { testTag = "search_input_card" },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = com.example.teost.core.ui.theme.CardShape
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = {
                            Box(Modifier.fillMaxWidth()) {
                                Text(
                                    text = stringResource(R.string.enter_domain_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(textAlign = TextAlign.Center)),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (isTestEnabled) {
                                    focusManager.clearFocus(); onTest(input)
                                }
                            }
                        ),
                        singleLine = false,
                        maxLines = 5,
                        shape = com.example.teost.core.ui.theme.TextFieldShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { testTag = "search_input" },
                        enabled = !isLoading,
                        isError = state is Resource.Error && results.isEmpty(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.tip_multi_targets),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { testTag = "search_tip" }
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { focusManager.clearFocus(); onTest(input) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .semantics { testTag = "btn_test_connection" },
                        enabled = isTestEnabled,
                        shape = com.example.teost.core.ui.theme.ButtonShape,
                        colors = com.example.teost.core.ui.theme.PrimaryCtaColors()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = stringResource(R.string.testing))
                        } else {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.test_connection),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.test_connection),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp * 1.2f // Increased by 20%
                            )
                        }
                    }
                }
            }
        }

        val isValidationError = (state as? Resource.Error)?.error is AppError.Validation
        if (isValidationError && results.isEmpty()) {
            item(key = "error_card") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { testTag = "error_card" },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = (state.error as? AppError)?.message
                                ?: stringResource(R.string.error_unknown),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onClearError,
                            modifier = Modifier.semantics { testTag = "error_dismiss" }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.dismiss_error),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }


        items(results, key = { it.url }) { result ->
            ConnectionResultCardPolished(
                result = result,
                onCopyToClipboard = { text ->
                    clipboardManager.setText(AnnotatedString(text))
                    Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ConnectionResultCardPolished(
    result: ConnectionTestResult,
    onCopyToClipboard: (String) -> Unit
) {
    // Handle error cases first
    if (!result.isSuccessful && result.errorMessage != null) {
        return ErrorConnectionCard(
            result = result,
            onCopyToClipboard = onCopyToClipboard
        )
    }
    
    val statusColor = when (result.statusCode) {
        in 200..299 -> Color(0xFF00E676) // Bright green
        in 300..399 -> Color(0xFFFFD54F) // Bright amber
        in 400..499 -> Color(0xFFFF5252) // Bright red
        else -> Color(0xFFFFF176) // Bright yellow
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTag = "result_card" }
            .background(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = statusColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.domain,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = result.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        val (icon, tint) = when (result.statusCode) {
                            in 200..299 -> Icons.Outlined.CheckCircle to Color.White
                            in 300..399 -> Icons.Outlined.Refresh to Color.White
                            in 400..599 -> Icons.Outlined.Warning to Color.White
                            else -> Icons.Outlined.Info to Color.White
                        }
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${result.statusCode}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary chips (DNS/TTFB/TCP/SSL) if available
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                result.dnsTime?.let { t ->
                    MetricChip2Lines(icon = Icons.Outlined.Language, title = "DNS", value = "${t}ms")
                }
                result.ttfb?.let { t ->
                    MetricChip2Lines(icon = Icons.Outlined.NetworkCheck, title = "TTFB", value = "${t}ms")
                }
                result.tcpHandshakeTime?.let { t ->
                    MetricChip2Lines(icon = Icons.Outlined.Link, title = "TCP", value = "${t}ms")
                }
                result.sslHandshakeTime?.let { t ->
                    MetricChip2Lines(icon = Icons.Outlined.Lock, title = "SSL", value = "${t}ms")
                }
            }

            // Expandable details (meta + headers + encrypted body)
            var isExpanded by remember { mutableStateOf(false) }
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    // Network Logs (if present)
                    if (!result.logs.isNullOrEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.raw_logs),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(onClick = { 
                                try {
                                    val logText = result.logs?.filterNotNull()?.joinToString("\n") ?: ""
                                    onCopyToClipboard(logText)
                                } catch (e: Exception) {
                                    onCopyToClipboard("Error reading logs")
                                }
                            }) {
                                Icon(
                                    Icons.Outlined.ContentCopy,
                                    contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.copy),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(id = com.example.teost.core.ui.R.string.copy_all))
                            }
                        }
                        val logsScroll = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .verticalScroll(logsScroll)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = result.logs!!.joinToString("\n"),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    // Compact metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.label_response_time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${result.responseTime}ms", style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.label_request_id), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = result.requestId ?: "—", style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // DUPLICATE HEADERS COLLAPSIBLE SECTION REMOVED - keep the better one with filter below

                    Text(
                        text = stringResource(R.string.headers),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    var headerFilter by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = headerFilter,
                        onValueChange = { headerFilter = it },
                        singleLine = true,
                        placeholder = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.filter_headers_placeholder)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    val scrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(scrollState)
                    ) {
                        HeadersTable(
                            headers = result.headers,
                            filter = headerFilter,
                            onCopy = { onCopyToClipboard(it) }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                val headersText = result.headers.entries.joinToString("\n") {
                                    "${it.key}: ${it.value}"
                                }
                                onCopyToClipboard(headersText)
                            }
                        ) {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.copy),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(id = com.example.teost.core.ui.R.string.copy_all))
                        }
                    }

                }
            }

            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(R.string.show_less) else stringResource(R.string.show_more),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(if (isExpanded) R.string.show_less else R.string.show_more),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetricChip2Lines(
    icon: ImageVector,
    title: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = title, style = MaterialTheme.typography.labelSmall)
                Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun InfoChip(
    icon: ImageVector,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BodySection(
    algorithm: String,
    key: String,
    iv: String,
    ciphertext: String,
    onCopy: (String) -> Unit,
    onCopyAll: (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.label_body),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = StatusOrange,
                modifier = Modifier.size(18.dp)
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.response_body_encrypted), color = StatusOrange, style = MaterialTheme.typography.bodyMedium)
                EncryptionDetail(label = "Algorithm", value = algorithm, onCopy = null)
                EncryptionDetail(label = "Key", value = key, onCopy = onCopy)
                EncryptionDetail(label = "IV", value = iv, onCopy = onCopy)
                EncryptionDetail(label = "Ciphertext", value = ciphertext, onCopy = onCopy)
                if (onCopyAll != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onCopyAll) {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(id = com.example.teost.core.ui.R.string.copy),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(id = com.example.teost.core.ui.R.string.copy))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EncryptionDetail(label: String, value: String, onCopy: ((String) -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(100.dp))
        Text(text = value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        if (onCopy != null) {
            IconButton(onClick = { onCopy(value) }) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun BodySectionSlim(
    algorithm: String,
    ciphertext: String,
    onCopyAll: (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.label_body),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = StatusOrange,
                modifier = Modifier.size(18.dp)
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.response_body_encrypted), color = StatusOrange, style = MaterialTheme.typography.bodyMedium)
                EncryptionDetail(label = "Algorithm", value = algorithm, onCopy = null)
                EncryptionDetail(label = "Ciphertext", value = ciphertext, onCopy = null)
                if (onCopyAll != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onCopyAll) {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(id = com.example.teost.core.ui.R.string.copy),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(id = com.example.teost.core.ui.R.string.copy_all))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeadersTable(
    headers: Map<String, String>,
    filter: String,
    onCopy: ((String) -> Unit)?
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)) {
        val keyColumnWidth = 150.dp
        headers
            .asSequence()
            .filter { (k, v) ->
                val line = "$k: $v"
                filter.isBlank() || line.contains(filter, ignoreCase = true)
            }
            .forEach { (key, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = TencentBlue,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(keyColumnWidth)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (onCopy != null) {
                        IconButton(onClick = { onCopy("$key: $value") }) {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.copy_header),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
    }
}

@Composable
private fun ErrorConnectionCard(
    result: ConnectionTestResult,
    onCopyToClipboard: (String) -> Unit
) {
    val (errorIcon, errorTitle, errorDescription) = when (result.errorMessage) {
        "SERVER_NOT_FOUND" -> Triple(
            Icons.Filled.ErrorOutline,
            "The site can't be reached",
            "${result.domain} server IP address could not be found."
        )
        "NETWORK_UNREACHABLE" -> Triple(
            Icons.Filled.SignalWifiOff,
            "Connection lost",
            "Please check your internet connection and try again."
        )
        "CONNECTION_REFUSED" -> Triple(
            Icons.Filled.Block,
            "Connection refused",
            "${result.domain} refused to connect."
        )
        "CONNECTION_RESET" -> Triple(
            Icons.Filled.Refresh,
            "Connection reset",
            "The connection to ${result.domain} was reset."
        )
        else -> Triple(
            Icons.Filled.Error,
            "Connection failed",
            result.errorMessage ?: "Unable to connect to ${result.domain}."
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTag = "error_result_card" },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = errorIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = errorTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = errorDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    
                    // Show attempted URL
                    Text(
                        text = "Attempted: ${result.url}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Copy error info button
                IconButton(
                    onClick = {
                        val errorInfo = buildString {
                            appendLine("Error: $errorTitle")
                            appendLine("Target: ${result.url}")
                            appendLine("Domain: ${result.domain}")
                            appendLine("Time: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(result.timestamp)}")
                            result.logs?.let { logs ->
                                if (logs.isNotEmpty()) {
                                    appendLine("Logs:")
                                    logs.forEach { appendLine("  $it") }
                                }
                            }
                        }
                        onCopyToClipboard(errorInfo)
                    }
                ) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = "Copy error details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Show logs if available
            result.logs?.let { logs ->
                if (logs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Diagnostic Logs",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            logs.take(3).forEach { log ->
                                Text(
                                    text = log,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (logs.size > 3) {
                                Text(
                                    text = "... and ${logs.size - 3} more lines",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SearchScreenPreview() {
    EdgeOneTheme {
        SearchScreenContent(
            state = null,
            results = emptyList(),
            onTest = {},
            onNavigateToTests = {}
        )
    }
}
