package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ScanCategory
import com.example.domain.ThreatReport
import com.example.ui.ScanUiState
import com.example.ui.components.ThreatReportCard

@Composable
fun ThreatScanScreen(
    inputText: String,
    selectedCategory: ScanCategory,
    attachedBitmap: Bitmap?,
    scanUiState: ScanUiState,
    onInputChange: (String) -> Unit,
    onSelectCategory: (ScanCategory) -> Unit,
    onAttachImageUri: (android.net.Uri) -> Unit,
    onRemoveImage: () -> Unit,
    onRunScan: () -> Unit,
    onAskCopilot: (ThreatReport) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onAttachImageUri(uri)
        }
    }

    val isLoading = scanUiState is ScanUiState.Loading

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.scanner_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.scanner_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScanCategory.entries.forEach { category ->
                    FilterChip(
                        selected = category == selectedCategory,
                        onClick = { onSelectCategory(category) },
                        label = { Text(category.label) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("scan_category_${category.name.lowercase()}")
                    )
                }
            }
        }

        // Input Box
        item {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                label = { Text(stringResource(R.string.scanner_input_label)) },
                placeholder = { Text(stringResource(R.string.scanner_input_placeholder)) },
                minLines = 4,
                maxLines = 8,
                trailingIcon = {
                    if (inputText.isNotEmpty()) {
                        IconButton(
                            onClick = { onInputChange("") },
                            modifier = Modifier.testTag("clear_scan_input_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear input"
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scanner_input_field")
            )
        }

        // Attachment Preview
        if (attachedBitmap != null) {
            item {
                val imageBitmap = remember(attachedBitmap) { attachedBitmap.asImageBitmap() }
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Selected screenshot for security scan",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(MaterialTheme.shapes.small)
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            IconButton(
                                onClick = onRemoveImage,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("remove_screenshot_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.action_remove_screenshot)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Row: Attach Screenshot + Run Scan
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("attach_screenshot_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_attach_screenshot))
                }

                Button(
                    onClick = onRunScan,
                    enabled = !isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("run_threat_scan_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_scanning))
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_run_scan))
                    }
                }
            }
        }

        // Result / Error States
        when (scanUiState) {
            is ScanUiState.Error -> {
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = "Scan error",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = scanUiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            is ScanUiState.Success -> {
                item {
                    val report = scanUiState.report
                    ThreatReportCard(
                        report = report,
                        onCopyReport = {
                            val formatted = buildString {
                                appendLine("[${report.riskLevel.label} ${report.riskScore}/100] ${report.threatType}")
                                appendLine(report.summary)
                                if (report.indicators.isNotEmpty()) {
                                    appendLine("\nIndicators:")
                                    report.indicators.forEach { appendLine("- $it") }
                                }
                                if (report.remediationSteps.isNotEmpty()) {
                                    appendLine("\nRemediation:")
                                    report.remediationSteps.forEachIndexed { idx, s -> appendLine("${idx + 1}. $s") }
                                }
                            }
                            clipboard.setText(AnnotatedString(formatted))
                            onShowMessage("Threat report copied")
                        },
                        onAskCopilot = { onAskCopilot(report) },
                        modifier = Modifier.testTag("threat_report_card")
                    )
                }
            }

            ScanUiState.Idle, ScanUiState.Loading -> Unit
        }
    }
}
