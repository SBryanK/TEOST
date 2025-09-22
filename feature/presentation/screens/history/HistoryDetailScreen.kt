package com.example.teost.presentation.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.teost.core.ui.theme.EdgeOneTheme
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.runtime.saveable.rememberSaveable
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    resultId: String,
    onNavigateBack: () -> Unit = {},
    viewModel: HistoryDetailViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val ui by viewModel.ui.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(resultId) { viewModel.load(resultId) }

    var showLogsSheet by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.test_result_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.back))
                }
            }
        )
    }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Chips / key info
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(ui.category) })
                AssistChip(onClick = {}, label = { Text(ui.type) })
                ui.statusCode?.let { code -> AssistChip(onClick = {}, label = { Text(com.example.teost.core.ui.R.string.http_code_chip.let { stringId -> androidx.compose.ui.res.stringResource(id = stringId, code) }) }) }
            }

            // Summary banner
            val summary = ui.summary
            if (summary.isNotBlank()) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp, shape = com.example.teost.core.ui.theme.CardShape) {
                    Text(summary, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }

            ElevatedCard(shape = com.example.teost.core.ui.theme.CardShape) { Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.label_domain)}: ${ui.domain}")
                Text("${androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.label_test)}: ${ui.testName}")
                Text("${androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.label_status)}: ${ui.status}")
            }}

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ui.dnsTime?.let { AssistChip(onClick = {}, label = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.metric_dns_ms, it)) }) }
                ui.ttfb?.let { AssistChip(onClick = {}, label = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.metric_ttfb_ms, it)) }) }
                viewModel.getDetails()?.tcpHandshakeTime?.let { AssistChip(onClick = {}, label = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.metric_tcp_ms, it)) }) }
                viewModel.getDetails()?.sslHandshakeTime?.let { AssistChip(onClick = {}, label = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.metric_ssl_ms, it)) }) }
                // Additional chips from details
                viewModel.getDetails()?.requestsPerSecond?.let { rps ->
                    val s = java.util.Locale.ROOT.let { locale -> String.format(locale, "%.1f", rps) }
                    AssistChip(onClick = {}, label = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.metric_rps, s)) })
                }
                viewModel.getDetails()?.successRate?.let { pct ->
                    AssistChip(onClick = {}, label = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.metric_success_pct, pct.toInt())) })
                }
                viewModel.getDetails()?.errorRate?.let { pct ->
                    AssistChip(onClick = {}, label = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.metric_errors_pct, pct.toInt())) })
                }
                if (viewModel.getDetails()?.blockedByWaf == true) {
                    AssistChip(onClick = {}, label = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.chip_waf)) })
                }
                viewModel.getDetails()?.botScore?.let { bot ->
                    AssistChip(onClick = {}, label = { Text("Bot ${bot}") })
                }
            }

            // One-time confirmation for encrypted body display (if present)
            var showEncAck by rememberSaveable { mutableStateOf(true) }
            if (showEncAck && (viewModel.getDetails()?.encryptedBody != null)) {
                AlertDialog(
                    onDismissRequest = { showEncAck = false },
                    confirmButton = { TextButton(onClick = { showEncAck = false }) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.ok)) } },
                    title = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.enc_display_title)) },
                    text = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.enc_display_text)) }
                )
            }

            // Params snapshot section
            ElevatedCard(shape = com.example.teost.core.ui.theme.CardShape) { Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.label_parameters), style = MaterialTheme.typography.titleMedium)
                val params = viewModel.getParamsSnapshot()
                if (params == null) {
                    Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.no_parameters), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    ParamsSnapshot(params)
                }
            }}

            ElevatedCard(shape = com.example.teost.core.ui.theme.CardShape) { Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.headers), style = MaterialTheme.typography.titleMedium)
                HeadersTableHistory(headers = ui.headers)
            }}

            ElevatedCard(shape = com.example.teost.core.ui.theme.CardShape, modifier = Modifier.clickable { showLogsSheet = true }) { Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.raw_logs), style = MaterialTheme.typography.titleMedium)
                    val ctx = LocalContext.current
                    TextButton(onClick = {
                        try {
                            val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("logs", ui.rawLogs ?: ""))
                            Toast.makeText(ctx, com.example.teost.core.ui.R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.w("HistoryDetail", "Copy logs failed", e)
                        }
                    }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.copy), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.copy))
                    }
                }
                val scroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(scroll)
                        .padding(top = 8.dp)
                ) {
                    Text(ui.rawLogs ?: androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.no_logs), fontFamily = FontFamily.Monospace)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val ctx = LocalContext.current
                    Button(onClick = {
                        try {
                            val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("logs", ui.rawLogs ?: ""))
                            Toast.makeText(ctx, com.example.teost.core.ui.R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.w("HistoryDetail", "Copy logs failed", e)
                        }
                    }, shape = com.example.teost.core.ui.theme.ButtonShape) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.copy)) }
                }
            }}

            // Per-request logs (if present in rawLogs)
            val logs = viewModel.getRawLogs()
            if (!logs.isNullOrBlank()) {
                ElevatedCard(shape = com.example.teost.core.ui.theme.CardShape) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        var logsExpanded by rememberSaveable { mutableStateOf(false) }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Per-request logs", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val ctx = LocalContext.current
                                TextButton(onClick = {
                                    try {
                                        val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        cm.setPrimaryClip(android.content.ClipData.newPlainText("per_request_logs", logs))
                                        Toast.makeText(ctx, com.example.teost.core.ui.R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                    } catch (_: Exception) {}
                                }) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.copy)) }
                                TextButton(onClick = { logsExpanded = !logsExpanded }) {
                                    Text(if (logsExpanded) "Collapse" else "Expand")
                                }
                            }
                        }
                        val lines = remember(logs) { logs.lines() }
                        val shown = if (logsExpanded) lines else lines.take(5)
                        val scroll = rememberScrollState()
                        Column(Modifier.fillMaxWidth().heightIn(max = if (logsExpanded) 300.dp else 160.dp).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            shown.forEach { line ->
                                val isError = line.contains("FAIL", ignoreCase = true) || line.contains("ERROR", ignoreCase = true)
                                val statusMatch = Regex("\\b(HTTP \\d{3}|-> \\d{3})\\b").find(line)
                                val statusColor = when (statusMatch?.value?.takeLast(3)?.toIntOrNull()) {
                                    in 200..299 -> Color(0xFF2E7D32)
                                    in 300..399 -> Color(0xFF0277BD)
                                    in 400..499 -> Color(0xFFF9A825)
                                    in 500..599 -> Color(0xFFC62828)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                Surface(color = if (isError) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = line,
                                        color = if (isError) Color(0xFFC62828) else statusColor,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.fillMaxWidth().padding(6.dp)
                                    )
                                }
                            }
                            if (!logsExpanded && lines.size > shown.size) {
                                Text("… ${lines.size - shown.size} more lines", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        val uri = viewModel.exportResult(context, resultId)
                        uri?.let {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, it)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val chooser = context.getString(com.example.teost.core.ui.R.string.share_result)
                            context.startActivity(Intent.createChooser(share, chooser))
                        }
                    }
                }, shape = com.example.teost.core.ui.theme.ButtonShape) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.export)) }
                OutlinedButton(onClick = {
                    scope.launch {
                        val uri = viewModel.exportLogs(context, resultId)
                        uri?.let {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, it)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val chooserTitle = context.getString(com.example.teost.core.ui.R.string.export_logs)
                            context.startActivity(Intent.createChooser(share, chooserTitle))
                        }
                    }
                }, shape = com.example.teost.core.ui.theme.ButtonShape, border = com.example.teost.core.ui.theme.OutlinedBrandBorder()) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.export_logs)) }
                OutlinedButton(onClick = {
                    scope.launch {
                        val uri = viewModel.exportLogs(context, resultId)
                        uri?.let {
                            val open = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(it, "text/plain")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(open)
                        }
                    }
                }, shape = com.example.teost.core.ui.theme.ButtonShape, border = com.example.teost.core.ui.theme.OutlinedBrandBorder()) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.open_logs)) }
                OutlinedButton(onClick = onNavigateBack, shape = com.example.teost.core.ui.theme.ButtonShape, border = com.example.teost.core.ui.theme.OutlinedBrandBorder()) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.back)) }
            }
        }
    }

    if (showLogsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showLogsSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.raw_logs), style = MaterialTheme.typography.titleLarge)
                val scroll = rememberScrollState()
                Box(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(scroll)) {
                    Text(ui.rawLogs ?: "(no logs)", fontFamily = FontFamily.Monospace)
                }
                val ctx = LocalContext.current
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        try {
                            val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("logs", ui.rawLogs ?: ""))
                            Toast.makeText(ctx, com.example.teost.core.ui.R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.w("HistoryDetail", "Copy logs failed", e)
                        }
                    }, shape = com.example.teost.core.ui.theme.ButtonShape) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.copy)) }
                    OutlinedButton(onClick = {
                        scope.launch {
                            val uri = viewModel.exportLogs(ctx, resultId)
                            uri?.let {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_STREAM, it)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val chooserTitle = ctx.getString(com.example.teost.core.ui.R.string.export_logs)
                                ctx.startActivity(Intent.createChooser(share, chooserTitle))
                            }
                        }
                    }, shape = com.example.teost.core.ui.theme.ButtonShape, border = com.example.teost.core.ui.theme.OutlinedBrandBorder()) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.share_result)) }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ParamsSnapshot(params: com.example.teost.data.model.TestParameters) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        params.burstRequests?.let { Text("burst_requests: $it", fontFamily = FontFamily.Monospace) }
        params.burstIntervalMs?.let { Text("burst_interval_ms: $it", fontFamily = FontFamily.Monospace) }
        params.sustainedRpsWindow?.let { Text("sustained_window_sec: $it", fontFamily = FontFamily.Monospace) }
        params.concurrencyLevel?.let { Text("concurrent_connections: $it", fontFamily = FontFamily.Monospace) }
        params.targetPath?.let { Text("target_path: $it", fontFamily = FontFamily.Monospace) }
        params.payloadList?.takeIf { it.isNotEmpty() }?.let { Text("payload_list: ${it.size} items", fontFamily = FontFamily.Monospace) }
        params.encodingMode?.let { Text("encoding_mode: $it", fontFamily = FontFamily.Monospace) }
        params.injectionPoint?.let { Text("injection_point: $it", fontFamily = FontFamily.Monospace) }
        params.targetParam?.let { Text("target_param: $it", fontFamily = FontFamily.Monospace) }
        params.httpMethod?.let { Text("http_method: $it", fontFamily = FontFamily.Monospace) }
        params.uaProfiles?.takeIf { it.isNotEmpty() }?.let { Text("ua_profiles: ${it.joinToString()} ", fontFamily = FontFamily.Monospace) }
        params.headerMinimal?.let { Text("header_minimal: $it", fontFamily = FontFamily.Monospace) }
        params.rpsTarget?.let { Text("rps_target: $it", fontFamily = FontFamily.Monospace) }
        params.windowSec?.let { Text("window_sec: $it", fontFamily = FontFamily.Monospace) }
        params.requestPath?.let { Text("request_path: $it", fontFamily = FontFamily.Monospace) }
        params.fingerprintStrategy?.let { Text("fingerprint_mode: $it", fontFamily = FontFamily.Monospace) }
        params.cookiePolicy?.let { Text("cookie_policy: $it", fontFamily = FontFamily.Monospace) }
        params.apiEndpoint?.let { Text("api_endpoint: $it", fontFamily = FontFamily.Monospace) }
        params.authMode?.let { Text("auth_mode: $it", fontFamily = FontFamily.Monospace) }
        params.username?.let { Text("username: $it", fontFamily = FontFamily.Monospace) }
        params.passwordList?.takeIf { it.isNotEmpty() }?.let { Text("password_list: ${it.size} items", fontFamily = FontFamily.Monospace) }
        params.enumTemplate?.let { Text("enum_template: $it", fontFamily = FontFamily.Monospace) }
        params.idRange?.let { Text("id_range: ${it.joinToString()} ", fontFamily = FontFamily.Monospace) }
        params.stepSize?.let { Text("step_size: $it", fontFamily = FontFamily.Monospace) }
        params.fuzzCases?.takeIf { it.isNotEmpty() }?.let { Text("fuzz_cases: ${it.size} items", fontFamily = FontFamily.Monospace) }
        params.contentTypes?.takeIf { it.isNotEmpty() }?.let { Text("content_types: ${it.joinToString()} ", fontFamily = FontFamily.Monospace) }
        params.replayCount?.let { Text("replay_count: $it", fontFamily = FontFamily.Monospace) }
        params.requestDelayMs?.let { Text("request_delay_ms: $it", fontFamily = FontFamily.Monospace) }
        
        // Additional UI parameters
        params.queryParams?.takeIf { it.isNotEmpty() }?.let { qp ->
            Text("query_params:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
            qp.forEach { (key, value) ->
                Text("  $key: $value", fontFamily = FontFamily.Monospace, modifier = Modifier.padding(start = 16.dp))
            }
        }
        params.customHeaders?.takeIf { it.isNotEmpty() }?.let { ch ->
            Text("custom_headers:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
            ch.forEach { (key, value) ->
                Text("  $key: $value", fontFamily = FontFamily.Monospace, modifier = Modifier.padding(start = 16.dp))
            }
        }
        params.bodyTemplate?.let { Text("body_template: $it", fontFamily = FontFamily.Monospace) }
    }
}

@Composable
private fun HeadersTableHistory(headers: Map<String, String>) {
    val keyWidth = 150.dp
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        headers.forEach { (key, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(keyWidth)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryDetailPreview() { EdgeOneTheme { HistoryDetailScreen(resultId = "demo") } }
