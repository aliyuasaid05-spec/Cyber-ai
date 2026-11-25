package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.domain.RiskLevel
import com.example.domain.ScanCategory
import com.example.domain.ThreatReport
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

private const val TAG = "CyberAiRepository"
private const val AI_TIMEOUT_MS = 60_000L

class CyberAiRepository {

    private val threatSchema = Schema.obj(
        properties = mapOf(
            "riskLevel" to Schema.enumeration(
                values = listOf("CRITICAL", "HIGH", "MEDIUM", "LOW", "SAFE"),
                description = "Overall security risk classification"
            ),
            "riskScore" to Schema.integer(
                description = "Numeric risk score from 0 (completely safe) to 100 (confirmed active threat)"
            ),
            "threatType" to Schema.string(
                description = "Short threat classification such as Credential Phishing, Lookalike Domain, Command Injection, Benign Communication, etc."
            ),
            "summary" to Schema.string(
                description = "Clear 2-3 sentence defensive verdict explaining what was found and why it is or is not risky"
            ),
            "indicators" to Schema.array(
                items = Schema.string(),
                description = "Specific suspicious indicators, red flags, domains, or technical findings observed in the input"
            ),
            "remediationSteps" to Schema.array(
                items = Schema.string(),
                description = "Concrete, ordered defensive steps the user should take next"
            ),
            "confidenceNote" to Schema.string(
                description = "Brief note on certainty and any context limitations"
            )
        )
    )

    private val threatScannerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = threatSchema
                temperature = 0.2f
            },
            systemInstruction = content {
                text(
                    "You are Cyber AI, a defensive cybersecurity threat analyst. " +
                        "Inspect user-submitted text, URLs, emails, SMS messages, logs, code snippets, or screenshots strictly for defensive triage. " +
                        "Identify phishing tactics, social engineering urgency, homoglyph/lookalike domains, suspicious redirects, " +
                        "malicious script behaviors, credential harvesting, or software vulnerabilities. " +
                        "Never fabricate findings not present in the submitted input. Provide practical, calm, defensive remediation steps."
                )
            }
        )
    }

    private val copilotModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.4f
            },
            systemInstruction = content {
                text(
                    "You are Cyber AI Copilot, an expert defensive cybersecurity analyst and incident responder. " +
                        "Help users protect accounts, triage suspicious messages or breaches, harden devices, understand CVEs, and write secure code. " +
                        "Always stay strictly defensive and ethical; refuse requests to create malware, exploits, or attacks against targets, " +
                        "and pivot constructively to defense and mitigation. Keep responses structured, clear, and concise."
                )
            }
        )
    }

    private var activeChat: Chat? = null

    private fun getOrCreateChat(): Chat {
        val existing = activeChat
        if (existing != null) return existing
        return copilotModel.startChat().also { activeChat = it }
    }

    fun resetChatSession() {
        activeChat = null
    }

    suspend fun analyzeThreat(
        inputText: String,
        category: ScanCategory,
        attachmentBitmap: Bitmap?
    ): ThreatReport {
        val trimmed = inputText.trim()
        require(trimmed.isNotEmpty() || attachmentBitmap != null) {
            "Provide text, a URL, code, or a screenshot to scan."
        }

        val prompt = buildString {
            appendLine("Perform a defensive cybersecurity risk assessment.")
            appendLine("Requested scan mode: ${category.label} (${category.hint}).")
            if (trimmed.isNotEmpty()) {
                appendLine("Submitted artifact:")
                appendLine(trimmed)
            } else {
                appendLine("Analyze the attached screenshot thoroughly for phishing, spoofing, malware warnings, scam cues, or security risks.")
            }
        }

        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                if (attachmentBitmap != null) {
                    threatScannerModel.generateContent(
                        content {
                            image(attachmentBitmap)
                            text(prompt)
                        }
                    )
                } else {
                    threatScannerModel.generateContent(prompt)
                }
            }

            val jsonRaw = response.text
                ?: throw IllegalStateException("Cyber AI returned an empty response.")

            parseThreatReport(
                jsonString = jsonRaw,
                inputPreview = trimmed.ifEmpty { "Screenshot attachment" },
                category = category,
                hasAttachment = attachmentBitmap != null
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Threat scan timed out after ${AI_TIMEOUT_MS}ms", e)
            throw IllegalStateException("Threat scan timed out after 60 seconds. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Threat scan failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Threat analysis failed.", e)
        }
    }

    fun sendCopilotMessageStream(userMessage: String): Flow<String> = flow {
        val chat = getOrCreateChat()
        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                chat.sendMessageStream(userMessage).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        accumulated.append(chunkText)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Copilot returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Copilot stream timed out", e)
            throw IllegalStateException("Response timed out after 60 seconds. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Copilot stream failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to reach Cyber AI Copilot.", e)
        }
    }

    private fun parseThreatReport(
        jsonString: String,
        inputPreview: String,
        category: ScanCategory,
        hasAttachment: Boolean
    ): ThreatReport {
        val json = JSONObject(jsonString)
        val riskLevel = RiskLevel.fromRaw(json.optString("riskLevel", "MEDIUM"))
        val riskScore = json.optInt("riskScore", 50).coerceIn(0, 100)
        val threatType = json.optString("threatType", "Security Assessment")
        val summary = json.optString("summary", "Analysis complete.")
        val confidenceNote = json.optString("confidenceNote", "")

        val indicatorsArray = json.optJSONArray("indicators")
        val indicators = if (indicatorsArray != null) {
            (0 until indicatorsArray.length())
                .mapNotNull { idx -> indicatorsArray.optString(idx).takeIf { it.isNotBlank() } }
        } else {
            emptyList()
        }

        val stepsArray = json.optJSONArray("remediationSteps")
        val remediationSteps = if (stepsArray != null) {
            (0 until stepsArray.length())
                .mapNotNull { idx -> stepsArray.optString(idx).takeIf { it.isNotBlank() } }
        } else {
            emptyList()
        }

        return ThreatReport(
            id = UUID.randomUUID().toString(),
            timestampMillis = System.currentTimeMillis(),
            inputPreview = inputPreview.take(120),
            category = category,
            riskLevel = riskLevel,
            riskScore = riskScore,
            threatType = threatType,
            summary = summary,
            indicators = indicators,
            remediationSteps = remediationSteps,
            confidenceNote = confidenceNote,
            hasAttachment = hasAttachment
        )
    }

    suspend fun decodeScaledBitmap(context: Context, uri: Uri, maxEdgePx: Int = 1280): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        val width = info.size.width
                        val height = info.size.height
                        val maxDim = maxOf(width, height)
                        if (maxDim > maxEdgePx) {
                            val scale = maxEdgePx.toFloat() / maxDim.toFloat()
                            decoder.setTargetSize(
                                (width * scale).toInt().coerceAtLeast(1),
                                (height * scale).toInt().coerceAtLeast(1)
                            )
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val bmp = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    val maxDim = maxOf(bmp.width, bmp.height)
                    if (maxDim > maxEdgePx) {
                        val scale = maxEdgePx.toFloat() / maxDim.toFloat()
                        Bitmap.createScaledBitmap(
                            bmp,
                            (bmp.width * scale).toInt().coerceAtLeast(1),
                            (bmp.height * scale).toInt().coerceAtLeast(1),
                            true
                        )
                    } else {
                        bmp
                    }
                }
                rawBitmap
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode selected screenshot", e)
                null
            }
        }
    }
}
