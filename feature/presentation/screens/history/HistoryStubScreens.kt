package com.example.teost.presentation.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.teost.presentation.screens.StubScreen
import com.example.teost.core.ui.theme.EdgeOneTheme
import com.example.teost.core.ui.components.TestResultCard
import com.example.teost.data.model.TestCategory
import com.example.teost.data.model.TestStatus
import com.example.teost.data.model.TestType
import com.example.teost.data.model.displayTestName
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToExecution: () -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val items: LazyPagingItems<com.example.teost.data.model.TestResult> = viewModel.paged.collectAsLazyPagingItems()
    
    // Debug logging for paging data
    LaunchedEffect(items.itemCount) {
        android.util.Log.d("HistoryScreen", "Paging items count: ${items.itemCount}")
    }
    // Screen state
    var showSheet by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    // Local controls within sheet
    var selectedCategory by remember { mutableStateOf<TestCategory?>(null) }
    var selectedStatus by remember { mutableStateOf<TestStatus?>(null) }
    var selectedType by remember { mutableStateOf<TestType?>(null) }
    var domainFilter by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var fromDateText by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var toDateText by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.nav_history),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = MaterialTheme.typography.headlineMedium.fontSize * 1.2f), // Increased by 20%
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showSheet = true }) {
                Icon(Icons.Filled.FilterList, contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.filters_title))
            }
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = { viewModel.setQuery(it) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.search_field_label)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.search_field_label)) }
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = {
                    try {
                        val intent = android.content.Intent("com.example.teost.ACTION_SYNC_NOW")
                        intent.setPackage(ctx.packageName)
                        ctx.sendBroadcast(intent)
                        android.widget.Toast.makeText(ctx, ctx.getString(com.example.teost.core.ui.R.string.sync_queued), android.widget.Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {}
                }) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.sync_now)) }
                TextButton(onClick = { showExport = true }) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.export_all)) }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (items.itemCount == 0) {
                    item {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(imageVector = Icons.Outlined.Search, contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.search_field_label))
                                Text(text = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.no_results))
                                
                            }
                        }
                    }
                } else {
                    items(count = items.itemCount, key = { index -> items[index]?.id ?: index }) { index ->
                        val r = items[index]
                        if (r != null) {
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDetail(r.id) }) {
                                TestResultCard(
                                    domain = r.domain,
                                    testName = r.resultDetails.paramsSnapshot?.displayTestName() ?: r.testName,
                                    status = r.status.name,
                                    duration = r.duration,
                                    creditsUsed = r.creditsUsed,
                                    statusCode = r.resultDetails.statusCode,
                                    startTime = r.startTime,
                                    dnsTime = r.resultDetails.dnsResolutionTime,
                                    tcpTime = r.resultDetails.tcpHandshakeTime,
                                    sslTime = r.resultDetails.sslHandshakeTime,
                                    ttfb = r.resultDetails.ttfb,
                                    responseTime = r.resultDetails.responseTime,
                                    headers = r.resultDetails.headers,
                                    encryptedBody = r.resultDetails.encryptedBody,
                                    encryptionAlgorithm = r.resultDetails.encryptionAlgorithm,
                                    networkLogs = r.rawLogs?.split("\n")?.filter { it.isNotBlank() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheet for Filters
    if (showSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.filters_title), style = MaterialTheme.typography.titleLarge)
                Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.label_status), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(listOf<TestStatus?>(null) + TestStatus.entries) { st ->
                        val selected = selectedStatus == st
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedStatus = if (selected) null else st
                            },
                            label = { Text(st?.name ?: androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.any_status)) }
                        )
                    }
                }

                Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.label_category), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(listOf<TestCategory?>(null) + TestCategory.entries) { cat ->
                        val selected = selectedCategory == cat
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedCategory = if (selected) null else cat
                                selectedType = null
                            },
                            label = { Text(cat?.name ?: androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.all)) }
                        )
                    }
                }

                val typeOptions = remember(selectedCategory) { typesForCategory(selectedCategory) }
                if (typeOptions.isNotEmpty()) {
                    Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.label_type), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        items(listOf<TestType?>(null) + typeOptions) { tp ->
                            val selected = selectedType == tp
                            FilterChip(
                                selected = selected,
                                onClick = { selectedType = if (selected) null else tp },
                                label = { Text(tp?.javaClass?.simpleName ?: androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.any_type)) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = domainFilter,
                    onValueChange = { domainFilter = it },
                    label = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.domain_contains)) },
                    modifier = Modifier.fillMaxWidth()
                )
                var showFromPicker by remember { mutableStateOf(false) }
                var showToPicker by remember { mutableStateOf(false) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = fromDateText,
                        onValueChange = { },
                        label = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.from_label)) },
                        modifier = Modifier.weight(1f).clickable { showFromPicker = true },
                        enabled = false
                    )
                    OutlinedTextField(
                        value = toDateText,
                        onValueChange = { },
                        label = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.to_label)) },
                        modifier = Modifier.weight(1f).clickable { showToPicker = true },
                        enabled = false
                    )
                }
                // Quick date chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    AssistChip(
                        onClick = {
                            val now = java.util.Calendar.getInstance()
                            val df = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                            val to = df.format(now.time)
                            now.add(java.util.Calendar.DAY_OF_YEAR, -1)
                            val from = df.format(now.time)
                            fromDateText = from; toDateText = to
                        },
                        label = { Text("24h") }
                    )
                    AssistChip(
                        onClick = {
                            val now = java.util.Calendar.getInstance()
                            val df = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                            val to = df.format(now.time)
                            now.add(java.util.Calendar.DAY_OF_YEAR, -7)
                            val from = df.format(now.time)
                            fromDateText = from; toDateText = to
                        },
                        label = { Text("7d") }
                    )
                    AssistChip(
                        onClick = {
                            val now = java.util.Calendar.getInstance()
                            val df = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                            val to = df.format(now.time)
                            now.add(java.util.Calendar.DAY_OF_YEAR, -30)
                            val from = df.format(now.time)
                            fromDateText = from; toDateText = to
                        },
                        label = { Text("30d") }
                    )
                }

                if (showFromPicker) {
                    val datePickerState = rememberDatePickerState()
                    DatePickerDialog(
                        onDismissRequest = { showFromPicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val millis = datePickerState.selectedDateMillis
                                if (millis != null) {
                                    val df = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                    fromDateText = df.format(java.util.Date(millis))
                                }
                                showFromPicker = false
                            }) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.ok)) }
                        },
                        dismissButton = { TextButton(onClick = { showFromPicker = false }) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.cancel)) } }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                if (showToPicker) {
                    val datePickerState = rememberDatePickerState()
                    DatePickerDialog(
                        onDismissRequest = { showToPicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val millis = datePickerState.selectedDateMillis
                                if (millis != null) {
                                    val df = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                    toDateText = df.format(java.util.Date(millis))
                                }
                                showToPicker = false
                            }) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.ok)) }
                        },
                        dismissButton = { TextButton(onClick = { showToPicker = false }) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.cancel)) } }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = {
                        selectedCategory = null
                        selectedStatus = null
                        selectedType = null
                        domainFilter = ""
                        fromDateText = ""
                        toDateText = ""
                        viewModel.resetFilters()
                        showSheet = false
                    }) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.clear_filters)) }

                    Button(onClick = {
                        viewModel.setCategory(selectedCategory)
                        viewModel.setStatus(selectedStatus)
                        viewModel.setType(selectedType)
                        viewModel.setDomainFilter(domainFilter.ifBlank { null })
                        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        val fromDate = fromDateText.takeIf { it.isNotBlank() }?.let { runCatching { fmt.parse(it) }.getOrNull() }
                        val toDate = toDateText.takeIf { it.isNotBlank() }?.let { runCatching { fmt.parse(it) }.getOrNull() }
                        viewModel.setFromDate(fromDate)
                        viewModel.setToDate(toDate)
                        // Persist filters via ViewModel
                        viewModel.persistFilters(
                            category = selectedCategory,
                            status = selectedStatus,
                            type = selectedType,
                            from = fromDate,
                            to = toDate,
                            domainContains = domainFilter.ifBlank { null }
                        )
                        showSheet = false
                    }) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.apply)) }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showExport) {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        ExportDialog(
            formatInitial = "csv",
            onDismiss = { showExport = false },
            onExport = { format ->
                scope.launch {
                    val uri = viewModel.exportFiltered(ctx, format)
                    if (uri == null) {
                        android.widget.Toast.makeText(ctx, ctx.getString(com.example.teost.core.ui.R.string.no_results), android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = if (format == "json") "application/json" else "text/csv"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooserTitle = ctx.getString(com.example.teost.core.ui.R.string.export_all)
                        try {
                            ctx.startActivity(android.content.Intent.createChooser(share, chooserTitle))
                            android.widget.Toast.makeText(ctx, chooserTitle, android.widget.Toast.LENGTH_SHORT).show()
                        } catch (_: android.content.ActivityNotFoundException) {
                            android.widget.Toast.makeText(ctx, "No app to share export", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                showExport = false
            }
        )
    }
}

// Real screen moved to separate file; keep no-op alias if referenced in previews

@Composable
fun FilterScreen(
    onApplyFilter: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) = StubScreen("Filter Screen")

@Composable
fun ExportScreen(
    onNavigateBack: () -> Unit = {}
) = StubScreen("Export Screen")

@Composable
private fun ExportDialog(
    formatInitial: String = "csv",
    onDismiss: () -> Unit,
    onExport: (format: String) -> Unit
) {
    var format by remember { mutableStateOf(formatInitial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onExport(format) }) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.export)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.cancel)) } },
        title = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.export_all)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.select_format))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = format == "csv", onClick = { format = "csv" }, label = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.csv_label)) })
                    FilterChip(selected = format == "json", onClick = { format = "json" }, label = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.json_label)) })
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    EdgeOneTheme { HistoryScreen() }
}

@Preview(showBackground = true)
@Composable
fun HistoryDetailScreenPreview() {
    EdgeOneTheme { HistoryDetailScreen(resultId = "demo") }
}

private fun typesForCategory(category: TestCategory?): List<TestType> {
    return when (category) {
        TestCategory.DDOS_PROTECTION -> listOf(
            TestType.HttpSpike,
            TestType.ConnectionFlood,
            TestType.TcpPortReachability,
            TestType.UdpReachability,
            TestType.BasicConnectivity
        )
        TestCategory.WEB_PROTECTION -> listOf(
            TestType.SqlInjection,
            TestType.XssTest,
            TestType.PathTraversal,
            TestType.CustomRulesValidation,
            TestType.EdgeRateLimiting,
            TestType.OversizedBody
        )
        TestCategory.API_PROTECTION -> listOf(
            TestType.AuthenticationTest,
            TestType.BruteForce,
            TestType.EnumerationIdor,
            TestType.SchemaInputValidation,
            TestType.BusinessLogicAbuse
        )
        TestCategory.BOT_MANAGEMENT -> listOf(
            TestType.UserAgentAnomaly,
            TestType.CookieJsChallenge,
            TestType.WebCrawlerSimulation
        )
        // Unsupported categories return empty until implemented
        null -> emptyList()
    }
}
