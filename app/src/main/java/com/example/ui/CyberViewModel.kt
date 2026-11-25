package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CyberAiRepository
import com.example.data.SecurityPreferencesRepository
import com.example.domain.ChatMessage
import com.example.domain.HardeningItem
import com.example.domain.HashAlgorithm
import com.example.domain.MessageRole
import com.example.domain.PasswordAnalysis
import com.example.domain.ScanCategory
import com.example.domain.ThreatReport
import com.example.ui.theme.AppThemeMode
import com.example.util.CryptoToolkit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "CyberViewModel"

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Loading : ScanUiState
    data class Success(val report: ThreatReport) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class CyberViewModel(application: Application) : AndroidViewModel(application) {

    private val aiRepository = CyberAiRepository()
    private val prefsRepository = SecurityPreferencesRepository(application.applicationContext)

    val themeMode: StateFlow<AppThemeMode> = prefsRepository.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppThemeMode.TACTICAL_DARK)

    val hardeningItems: StateFlow<List<HardeningItem>> = prefsRepository.hardeningItemsFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SecurityPreferencesRepository.DEFAULT_HARDENING_CONTROLS
        )

    val postureScore: StateFlow<Int> = hardeningItems
        .map { items -> items.filter { it.isCompleted }.sumOf { it.weight }.coerceIn(0, 100) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // Threat Scanner State
    private val _scannerInput = MutableStateFlow("")
    val scannerInput: StateFlow<String> = _scannerInput.asStateFlow()

    private val _selectedCategory = MutableStateFlow(ScanCategory.AUTO)
    val selectedCategory: StateFlow<ScanCategory> = _selectedCategory.asStateFlow()

    private val _attachedBitmap = MutableStateFlow<Bitmap?>(null)
    val attachedBitmap: StateFlow<Bitmap?> = _attachedBitmap.asStateFlow()

    private val _scanUiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanUiState: StateFlow<ScanUiState> = _scanUiState.asStateFlow()

    private val _recentScans = MutableStateFlow<List<ThreatReport>>(emptyList())
    val recentScans: StateFlow<List<ThreatReport>> = _recentScans.asStateFlow()

    // Cyber Copilot State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatStreaming = MutableStateFlow(false)
    val isChatStreaming: StateFlow<Boolean> = _isChatStreaming.asStateFlow()

    private var activeChatJob: Job? = null

    // Defense Toolkit State
    private val _passwordInput = MutableStateFlow("")
    val passwordInput: StateFlow<String> = _passwordInput.asStateFlow()

    private val _passwordAnalysis = MutableStateFlow(CryptoToolkit.analyzePassword(""))
    val passwordAnalysis: StateFlow<PasswordAnalysis> = _passwordAnalysis.asStateFlow()

    private val _cryptoInput = MutableStateFlow("")
    val cryptoInput: StateFlow<String> = _cryptoInput.asStateFlow()

    private val _selectedHashAlgorithm = MutableStateFlow(HashAlgorithm.SHA256)
    val selectedHashAlgorithm: StateFlow<HashAlgorithm> = _selectedHashAlgorithm.asStateFlow()

    private val _hashOutput = MutableStateFlow("")
    val hashOutput: StateFlow<String> = _hashOutput.asStateFlow()

    private val _base64Output = MutableStateFlow("")
    val base64Output: StateFlow<String> = _base64Output.asStateFlow()

    private val _base64Error = MutableStateFlow<String?>(null)
    val base64Error: StateFlow<String?> = _base64Error.asStateFlow()

    fun cycleThemeMode() {
        viewModelScope.launch {
            val entries = AppThemeMode.entries
            val next = entries[(themeMode.value.ordinal + 1) % entries.size]
            prefsRepository.setThemeMode(next)
        }
    }

    fun toggleHardeningItem(id: String) {
        viewModelScope.launch {
            prefsRepository.toggleHardeningItem(id)
        }
    }

    fun updateScannerInput(text: String) {
        _scannerInput.value = text
    }

    fun selectScanCategory(category: ScanCategory) {
        _selectedCategory.value = category
    }

    fun attachScreenshotUri(uri: Uri) {
        viewModelScope.launch {
            val bitmap = aiRepository.decodeScaledBitmap(getApplication(), uri)
            if (bitmap != null) {
                _attachedBitmap.value = bitmap
            } else {
                _scanUiState.value = ScanUiState.Error("Could not load selected image.")
            }
        }
    }

    fun clearAttachedScreenshot() {
        _attachedBitmap.value = null
    }

    fun openReportInScanner(report: ThreatReport) {
        _selectedCategory.value = report.category
        _scanUiState.value = ScanUiState.Success(report)
    }

    fun runThreatScan() {
        val text = _scannerInput.value
        val category = _selectedCategory.value
        val bitmap = _attachedBitmap.value

        if (text.isBlank() && bitmap == null) {
            _scanUiState.value = ScanUiState.Error("Paste suspicious content or attach a screenshot first.")
            return
        }

        viewModelScope.launch {
            _scanUiState.value = ScanUiState.Loading
            try {
                val report = aiRepository.analyzeThreat(
                    inputText = text,
                    category = category,
                    attachmentBitmap = bitmap
                )
                _scanUiState.value = ScanUiState.Success(report)
                _recentScans.update { current -> (listOf(report) + current).take(15) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Threat scan failed", e)
                _scanUiState.value = ScanUiState.Error(
                    e.localizedMessage ?: "Unable to complete threat scan."
                )
            }
        }
    }

    fun sendCopilotPrompt(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty() || _isChatStreaming.value) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            text = prompt
        )
        val modelMsgId = UUID.randomUUID().toString()
        val placeholderModelMsg = ChatMessage(
            id = modelMsgId,
            role = MessageRole.MODEL,
            text = "",
            isStreaming = true
        )

        _chatMessages.update { it + userMsg + placeholderModelMsg }
        _isChatStreaming.value = true

        activeChatJob?.cancel()
        activeChatJob = viewModelScope.launch {
            try {
                aiRepository.sendCopilotMessageStream(prompt).collect { accumulatedText ->
                    _chatMessages.update { list ->
                        list.map { msg ->
                            if (msg.id == modelMsgId) {
                                msg.copy(text = accumulatedText, isStreaming = true)
                            } else {
                                msg
                            }
                        }
                    }
                }
                _chatMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == modelMsgId) msg.copy(isStreaming = false) else msg
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Copilot response failed", e)
                val errorText = e.localizedMessage ?: "Failed to generate response."
                _chatMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == modelMsgId) {
                            msg.copy(
                                text = errorText,
                                isStreaming = false,
                                isError = true
                            )
                        } else {
                            msg
                        }
                    }
                }
            } finally {
                _isChatStreaming.value = false
            }
        }
    }

    fun askCopilotAboutReport(report: ThreatReport) {
        val contextPrompt = buildString {
            appendLine("Review this threat scan and advise me on next defensive steps:")
            appendLine("• Classification: ${report.threatType} (${report.riskLevel.label}, score ${report.riskScore}/100)")
            appendLine("• Summary: ${report.summary}")
            if (report.indicators.isNotEmpty()) {
                appendLine("• Indicators: ${report.indicators.joinToString("; ")}")
            }
        }
        sendCopilotPrompt(contextPrompt)
    }

    fun clearCopilotSession() {
        activeChatJob?.cancel()
        _isChatStreaming.value = false
        _chatMessages.value = emptyList()
        aiRepository.resetChatSession()
    }

    // Password & Crypto Toolkit Actions
    fun updatePasswordInput(value: String) {
        _passwordInput.value = value
        _passwordAnalysis.value = CryptoToolkit.analyzePassword(value)
    }

    fun generateStrongPassword(length: Int = 18, includeSymbols: Boolean = true) {
        val generated = CryptoToolkit.generateSecurePassword(length, includeSymbols)
        updatePasswordInput(generated)
    }

    fun updateCryptoInput(value: String) {
        _cryptoInput.value = value
        _base64Error.value = null
        if (value.isEmpty()) {
            _hashOutput.value = ""
        } else {
            _hashOutput.value = CryptoToolkit.computeHash(value, _selectedHashAlgorithm.value)
        }
    }

    fun selectHashAlgorithm(algorithm: HashAlgorithm) {
        _selectedHashAlgorithm.value = algorithm
        val current = _cryptoInput.value
        if (current.isNotEmpty()) {
            _hashOutput.value = CryptoToolkit.computeHash(current, algorithm)
        }
    }

    fun encodeInputToBase64() {
        val input = _cryptoInput.value
        _base64Error.value = null
        _base64Output.value = if (input.isEmpty()) "" else CryptoToolkit.encodeBase64(input)
    }

    fun decodeInputFromBase64() {
        val input = _cryptoInput.value
        if (input.isBlank()) {
            _base64Output.value = ""
            _base64Error.value = null
            return
        }
        val result = CryptoToolkit.decodeBase64(input)
        result.onSuccess { decoded ->
            _base64Error.value = null
            _base64Output.value = decoded
        }.onFailure {
            _base64Output.value = ""
            _base64Error.value = "Invalid Base64 input."
        }
    }
}
