package com.example.domain

enum class ScanCategory(val label: String, val hint: String) {
    AUTO("Auto-Detect", "Automatic triage of text, links, logs, or screenshots"),
    PHISHING("Phishing / SMS", "Emails, DMs, urgent bank/delivery alerts, social engineering"),
    URL("URL / Domain", "Suspicious links, homoglyphs, redirects, shorteners"),
    CODE("Code / Script", "Shell commands, PowerShell, JS, SQL, or app snippets"),
    LOG("Log / Incident", "Auth failures, firewall alerts, stack traces, headers")
}

enum class RiskLevel(val label: String) {
    CRITICAL("CRITICAL"),
    HIGH("HIGH"),
    MEDIUM("MEDIUM"),
    LOW("LOW"),
    SAFE("SAFE");

    companion object {
        fun fromRaw(raw: String): RiskLevel {
            return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: MEDIUM
        }
    }
}

data class ThreatReport(
    val id: String,
    val timestampMillis: Long,
    val inputPreview: String,
    val category: ScanCategory,
    val riskLevel: RiskLevel,
    val riskScore: Int,
    val threatType: String,
    val summary: String,
    val indicators: List<String>,
    val remediationSteps: List<String>,
    val confidenceNote: String,
    val hasAttachment: Boolean = false
)

enum class MessageRole {
    USER,
    MODEL
}

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val text: String,
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class HardeningItem(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val weight: Int,
    val isCompleted: Boolean = false
)

data class PasswordAnalysis(
    val length: Int,
    val entropyBits: Double,
    val strengthLabel: String,
    val scoreFraction: Float,
    val estimatedCrackTime: String,
    val feedback: List<String>
)

enum class HashAlgorithm(val label: String, val jcaName: String) {
    SHA256("SHA-256", "SHA-256"),
    SHA512("SHA-512", "SHA-512"),
    MD5("MD5", "MD5")
}
