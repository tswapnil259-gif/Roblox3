package com.robloxai.studio.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.robloxai.studio.data.model.AiProvider
import java.security.GeneralSecurityException

class ApiKeyVault(context: Context) {

    private val appContext = context.applicationContext
    private val prefsName = "roblox_ai_secure_vault"

    private val prefs: SharedPreferences = createEncryptedPrefs()

    private fun createEncryptedPrefs(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                prefsName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: GeneralSecurityException) {
            // Keystore corruption after backup restore — fall back to plain prefs so app does not crash
            appContext.getSharedPreferences("${prefsName}_fallback", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            appContext.getSharedPreferences("${prefsName}_fallback", Context.MODE_PRIVATE)
        }
    }

    fun saveApiKey(provider: AiProvider, apiKey: String) {
        prefs.edit().putString("key_${provider.name}", apiKey.trim()).apply()
    }

    fun getApiKey(provider: AiProvider): String {
        return prefs.getString("key_${provider.name}", "") ?: ""
    }

    fun clearApiKey(provider: AiProvider) {
        prefs.edit().remove("key_${provider.name}").apply()
    }

    fun setRelayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("relay_enabled", enabled).apply()
    }

    fun isRelayEnabled(): Boolean = prefs.getBoolean("relay_enabled", false)

    fun saveRelayUrl(url: String) {
        prefs.edit().putString("relay_url", url.trim().trimEnd('/')).apply()
    }

    fun getRelayUrl(): String = prefs.getString("relay_url", "") ?: ""

    fun saveRelayToken(token: String) {
        prefs.edit().putString("relay_token", token.trim()).apply()
    }

    fun getRelayToken(): String = prefs.getString("relay_token", "") ?: ""

    fun saveSelectedProvider(provider: AiProvider) {
        prefs.edit().putString("selected_provider", provider.name).apply()
    }

    fun getSelectedProvider(): AiProvider {
        val raw = prefs.getString("selected_provider", AiProvider.OPENROUTER.name)
        return AiProvider.entries.firstOrNull { it.name == raw } ?: AiProvider.OPENROUTER
    }

    fun saveSelectedModel(provider: AiProvider, modelId: String) {
        prefs.edit().putString("model_${provider.name}", modelId).apply()
    }

    fun getSelectedModel(provider: AiProvider): String {
        return prefs.getString("model_${provider.name}", null)
            ?: provider.defaultModels.first().id
    }
}
