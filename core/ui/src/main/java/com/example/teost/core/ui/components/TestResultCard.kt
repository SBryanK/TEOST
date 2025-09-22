package com.example.teost.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.teost.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TestResultCard(
    domain: String,
    testName: String,
    status: String,
    duration: Long,
    creditsUsed: Int,
    statusCode: Int? = null,
    startTime: Date,
    dnsTime: Long? = null,
    tcpTime: Long? = null,
    sslTime: Long? = null,
    ttfb: Long? = null,
    responseTime: Long? = null,
    headers: Map<String, String>? = null,
    encryptedBody: String? = null,
    encryptionAlgorithm: String? = null,
    networkLogs: List<String>? = null,
    onViewDetails: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with status
            TestResultHeader(
                domain = domain,
                testName = testName,
                status = status
            )
            
            // Basic metrics (always visible)
            TestBasicMetrics(
                duration = duration,
                creditsUsed = creditsUsed
            )
            
            // Expandable detailed metrics
            if (isExpanded) {
                TestDetailedMetrics(
                    statusCode = statusCode,
                    startTime = startTime,
                    dnsTime = dnsTime,
                    tcpTime = tcpTime,
                    sslTime = sslTime,
                    ttfb = ttfb,
                    responseTime = responseTime
                )
                
                // Network Diagnosis Logs
                if (!networkLogs.isNullOrEmpty()) {
                    NetworkDiagnosisSection(
                        logs = networkLogs,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Response Headers
                if (!headers.isNullOrEmpty()) {
                    ResponseHeadersSection(
                        headers = headers,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Encrypted Body
                if (!encryptedBody.isNullOrBlank() && !encryptionAlgorithm.isNullOrBlank()) {
                    EncryptedBodySection(
                        encryptedBody = encryptedBody,
                        algorithm = encryptionAlgorithm,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Expand/collapse indicator
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun TestResultHeader(
    domain: String,
    testName: String,
    status: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = domain,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = testName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        // Status chip
        StatusChip(status = status)
    }
}

@Composable
private fun StatusChip(status: String) {
    val backgroundColor = when (status) {
        "SUCCESS" -> Color(0xFF4CAF50)
        "FAILED" -> Color(0xFFF44336)
        "RUNNING" -> Color(0xFFFF9800)
        "PENDING" -> Color(0xFF2196F3)
        else -> Color(0xFF9E9E9E)
    }
    
    val textColor = Color.White
    val icon = when (status) {
        "SUCCESS" -> Icons.Filled.CheckCircle
        "FAILED" -> Icons.Filled.Error
        "RUNNING" -> Icons.Filled.Schedule
        "PENDING" -> Icons.Filled.Pending
        else -> Icons.AutoMirrored.Filled.Help
    }
    
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

@Composable
private fun TestBasicMetrics(
    duration: Long,
    creditsUsed: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Duration
        MetricChip(
            icon = Icons.Filled.Schedule,
            label = "Duration",
            value = "${duration}ms",
            color = TencentBlue
        )
        
        // Credits used
        MetricChip(
            icon = Icons.Filled.Star,
            label = "Credits",
            value = "$creditsUsed",
            color = StatusOrange
        )
    }
}

@Composable
private fun TestDetailedMetrics(
    statusCode: Int?,
    startTime: Date,
    dnsTime: Long? = null,
    tcpTime: Long? = null,
    sslTime: Long? = null,
    ttfb: Long? = null,
    responseTime: Long? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status code
        statusCode?.let { code ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status Code: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        code in 200..299 -> Color(0xFF4CAF50)
                        code in 300..399 -> Color(0xFFFF9800)
                        code in 400..499 -> Color(0xFFF44336)
                        else -> Color(0xFF2196F3)
                    }
                ) {
                    Text(
                        text = code.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        
        // Network Timing Metrics (same as SearchScreen)
        NetworkTimingMetrics(
            dnsTime = dnsTime,
            tcpTime = tcpTime,
            sslTime = sslTime,
            ttfb = ttfb,
            responseTime = responseTime
        )
        
        // Test time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(startTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricChip(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NetworkTimingMetrics(
    dnsTime: Long? = null,
    tcpTime: Long? = null,
    sslTime: Long? = null,
    ttfb: Long? = null,
    responseTime: Long? = null
) {
    val hasTimingData = dnsTime != null || tcpTime != null || sslTime != null || ttfb != null
    
    if (hasTimingData) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Network Diagnosis",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                dnsTime?.let { t ->
                    TimingChip(icon = Icons.Outlined.Language, title = "DNS", value = "${t}ms")
                }
                ttfb?.let { t ->
                    TimingChip(icon = Icons.Outlined.NetworkCheck, title = "TTFB", value = "${t}ms")
                }
                tcpTime?.let { t ->
                    TimingChip(icon = Icons.Outlined.Link, title = "TCP", value = "${t}ms")
                }
                sslTime?.let { t ->
                    TimingChip(icon = Icons.Outlined.Lock, title = "SSL", value = "${t}ms")
                }
                responseTime?.let { t ->
                    TimingChip(icon = Icons.Outlined.Timer, title = "Total", value = "${t}ms")
                }
            }
        }
    }
}

@Composable
private fun TimingChip(
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
private fun NetworkDiagnosisSection(
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Network Diagnosis",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${logs.size} entries",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Logs container with proper formatting
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            LazyColumn(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(logs) { logLine ->
                    Text(
                        text = logLine,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun ResponseHeadersSection(
    headers: Map<String, String>,
    modifier: Modifier = Modifier
) {
    var headersExpanded by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        // Headers toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { headersExpanded = !headersExpanded }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Response Headers",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${headers.size} headers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (headersExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (headersExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Headers content
        androidx.compose.animation.AnimatedVisibility(
            visible = headersExpanded,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                LazyColumn(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(headers.toList()) { (key, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$key:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(0.4f)
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(0.6f)
                            )
                        }
                    }
                }
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun EncryptedBodySection(
    encryptedBody: String,
    algorithm: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Section header
        Text(
            text = "Encrypted Response Body",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = StatusOrange,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Encryption details
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(StatusOrange.copy(alpha = 0.1f))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Algorithm:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = algorithm,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = StatusOrange
                    )
                }
                
                Text(
                    text = "Encrypted Data:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 100.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = encryptedBody,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}