package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.HardeningItem
import com.example.domain.HashAlgorithm
import com.example.domain.PasswordAnalysis
import com.example.ui.theme.CyberMonoTextStyle
import com.example.ui.theme.ThreatCritical
import com.example.ui.theme.ThreatMedium
import com.example.ui.theme.ThreatSafe

@Composable
fun ToolkitScreen(
    initialTab: Int,
    passwordInput: String,
    passwordAnalysis: PasswordAnalysis,
    cryptoInput: String,
    selectedHashAlgorithm: HashAlgorithm,
    hashOutput: String,
    base64Output: String,
    base64Error: String?,
    hardeningItems: List<HardeningItem>,
    onPasswordChange: (String) -> Unit,
    onGeneratePassword: () -> Unit,
    onCryptoInputChange: (String) -> Unit,
    onSelectHashAlgorithm: (HashAlgorithm) -> Unit,
    onEncodeBase64: () -> Unit,
    onDecodeBase64: () -> Unit,
    onToggleHardeningItem: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab) }
    val tabs = listOf(
        stringResource(R.string.tab_passwords),
        stringResource(R.string.tab_crypto),
        stringResource(R.string.tab_hardening)
    )

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.toolkit_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.toolkit_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, title ->
                    SegmentedButton(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("toolkit_tab_$index")
                    ) {
                        Text(text = title, maxLines = 1)
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> PasswordLabTab(
                passwordInput = passwordInput,
                analysis = passwordAnalysis,
                onPasswordChange = onPasswordChange,
                onGeneratePassword = onGeneratePassword,
                onShowMessage = onShowMessage
            )
            1 -> HashAndBase64Tab(
                cryptoInput = cryptoInput,
                selectedAlgorithm = selectedHashAlgorithm,
                hashOutput = hashOutput,
                base64Output = base64Output,
                base64Error = base64Error,
                onCryptoInputChange = onCryptoInputChange,
                onSelectAlgorithm = onSelectHashAlgorithm,
                onEncodeBase64 = onEncodeBase64,
                onDecodeBase64 = onDecodeBase64,
                onShowMessage = onShowMessage
            )
            2 -> HardeningTab(
                items = hardeningItems,
                onToggleItem = onToggleHardeningItem
            )
        }
    }
}

@Composable
private fun PasswordLabTab(
    passwordInput: String,
    analysis: PasswordAnalysis,
    onPasswordChange: (String) -> Unit,
    onGeneratePassword: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    val meterColor = when {
        analysis.scoreFraction >= 0.8f -> ThreatSafe
        analysis.scoreFraction >= 0.55f -> MaterialTheme.colorScheme.primary
        analysis.scoreFraction >= 0.3f -> ThreatMedium
        else -> ThreatCritical
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            OutlinedTextField(
                value = passwordInput,
                onValueChange = onPasswordChange,
                label = { Text("Password or passphrase") },
                placeholder = { Text("Type or generate a credential...") },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row {
                        IconButton(
                            onClick = { showPassword = !showPassword },
                            modifier = Modifier.testTag("toggle_password_visibility")
                        ) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                        if (passwordInput.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(passwordInput))
                                    onShowMessage("Password copied")
                                },
                                modifier = Modifier.testTag("copy_password_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = stringResource(R.string.cd_copy)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("password_lab_input")
            )
        }

        item {
            Button(
                onClick = {
                    showPassword = true
                    onGeneratePassword()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("generate_password_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Autorenew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Generate 18-Char CSPRNG Password")
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = analysis.strengthLabel,
                            style = MaterialTheme.typography.titleLarge,
                            color = meterColor
                        )
                        Text(
                            text = String.format("%.1f bits entropy", analysis.entropyBits),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LinearProgressIndicator(
                        progress = { analysis.scoreFraction },
                        color = meterColor,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Length: ${analysis.length} chars",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Offline GPU crack: ${analysis.estimatedCrackTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    analysis.feedback.forEach { tip ->
                        Text(
                            text = "• $tip",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HashAndBase64Tab(
    cryptoInput: String,
    selectedAlgorithm: HashAlgorithm,
    hashOutput: String,
    base64Output: String,
    base64Error: String?,
    onCryptoInputChange: (String) -> Unit,
    onSelectAlgorithm: (HashAlgorithm) -> Unit,
    onEncodeBase64: () -> Unit,
    onDecodeBase64: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            OutlinedTextField(
                value = cryptoInput,
                onValueChange = onCryptoInputChange,
                label = { Text("Plaintext or encoded payload") },
                placeholder = { Text("Enter text to hash or Base64 encode/decode...") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("crypto_input_field")
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Cryptographic Digest",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HashAlgorithm.entries.forEach { algo ->
                        FilterChip(
                            selected = algo == selectedAlgorithm,
                            onClick = { onSelectAlgorithm(algo) },
                            label = { Text(algo.label) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("hash_algo_${algo.name.lowercase()}")
                        )
                    }
                }
                if (hashOutput.isNotEmpty()) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = hashOutput,
                                style = CyberMonoTextStyle,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(hashOutput))
                                    onShowMessage("${selectedAlgorithm.label} digest copied")
                                },
                                modifier = Modifier.testTag("copy_hash_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = stringResource(R.string.cd_copy)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Base64 Payload Inspector",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onEncodeBase64,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("base64_encode_button")
                    ) {
                        Text("Encode")
                    }
                    OutlinedButton(
                        onClick = onDecodeBase64,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("base64_decode_button")
                    ) {
                        Text("Decode")
                    }
                }

                if (base64Error != null) {
                    Text(
                        text = base64Error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (base64Output.isNotEmpty()) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = base64Output,
                                style = CyberMonoTextStyle,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(base64Output))
                                    onShowMessage("Base64 output copied")
                                },
                                modifier = Modifier.testTag("copy_base64_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = stringResource(R.string.cd_copy)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HardeningTab(
    items: List<HardeningItem>,
    onToggleItem: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clickable { onToggleItem(item.id) }
                    .testTag("hardening_item_${item.id}"),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = if (item.isCompleted) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (item.isCompleted) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (item.isCompleted) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Filled.RadioButtonUnchecked
                        },
                        contentDescription = if (item.isCompleted) "Completed" else "Incomplete",
                        tint = if (item.isCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp).padding(top = 2.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "+${item.weight}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
