package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.HardeningItem
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cyberDataStore by preferencesDataStore(name = "cyber_ai_preferences")

class SecurityPreferencesRepository(private val context: Context) {

    private object Keys {
        val COMPLETED_HARDENING_IDS = stringSetPreferencesKey("completed_hardening_ids")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val themeModeFlow: Flow<AppThemeMode> = context.cyberDataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE]
        AppThemeMode.entries.firstOrNull { it.name == raw } ?: AppThemeMode.TACTICAL_DARK
    }

    val hardeningItemsFlow: Flow<List<HardeningItem>> = context.cyberDataStore.data.map { prefs ->
        val completed = prefs[Keys.COMPLETED_HARDENING_IDS] ?: emptySet()
        DEFAULT_HARDENING_CONTROLS.map { item ->
            item.copy(isCompleted = item.id in completed)
        }
    }

    suspend fun toggleHardeningItem(id: String) {
        context.cyberDataStore.edit { prefs ->
            val current = prefs[Keys.COMPLETED_HARDENING_IDS]?.toMutableSet() ?: mutableSetOf()
            if (id in current) {
                current.remove(id)
            } else {
                current.add(id)
            }
            prefs[Keys.COMPLETED_HARDENING_IDS] = current
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.cyberDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    companion object {
        val DEFAULT_HARDENING_CONTROLS = listOf(
            HardeningItem(
                id = "passkeys_mfa",
                title = "Phishing-Resistant MFA & Passkeys",
                description = "Enable FIDO2 passkeys or hardware security keys on primary email, banking, and cloud accounts.",
                category = "Identity",
                weight = 25
            ),
            HardeningItem(
                id = "password_manager",
                title = "Unique 16+ Char Passwords",
                description = "Replace reused passwords with unique high-entropy credentials stored in an audited vault.",
                category = "Identity",
                weight = 20
            ),
            HardeningItem(
                id = "private_dns",
                title = "Encrypted DNS-over-TLS",
                description = "Configure Private DNS on Android to prevent plain-text DNS snooping and block malicious domains.",
                category = "Network",
                weight = 15
            ),
            HardeningItem(
                id = "os_security_patches",
                title = "Verify OS & Play System Updates",
                description = "Keep Android security patches and Google Play system updates current to close zero-day CVEs.",
                category = "Device",
                weight = 15
            ),
            HardeningItem(
                id = "app_permissions_audit",
                title = "Least-Privilege App Permissions",
                description = "Audit Camera, Location, Microphone, and Accessibility access; revoke unused privileges.",
                category = "Privacy",
                weight = 15
            ),
            HardeningItem(
                id = "recovery_codes_offline",
                title = "Offline MFA Recovery Codes",
                description = "Store backup 2FA recovery codes securely offline to prevent account lockouts.",
                category = "Resilience",
                weight = 10
            )
        )
    }
}
