package com.dietcoach.app.data.secrets

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dietcoach.app.BuildConfig

class ApiKeyStore(context: Context) {
    private val appContext = context.applicationContext

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            "secret_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getApiKey(): String {
        val override = prefs.getString(KEY, null)?.trim().orEmpty()
        if (override.isNotEmpty()) return override
        return BuildConfig.DASHSCOPE_API_KEY.trim()
    }

    fun setApiKey(value: String) {
        prefs.edit().putString(KEY, value.trim()).apply()
    }

    fun clearOverride() {
        prefs.edit().remove(KEY).apply()
    }

    fun hasAnyKey(): Boolean = getApiKey().isNotBlank()

    companion object {
        private const val KEY = "dashscope_api_key"
    }
}
