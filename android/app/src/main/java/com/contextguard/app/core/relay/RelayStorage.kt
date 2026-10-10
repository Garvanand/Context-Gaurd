package com.contextguard.app.core.relay

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Manages private persistent storage of relay device identity and credentials.
 */
class RelayStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var deviceId: String
        get() {
            var id = prefs.getString(KEY_DEVICE_ID, null)
            if (id == null) {
                id = "dev_" + UUID.randomUUID().toString().replace("-", "").take(16)
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }
            return id
        }
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    var deviceToken: String?
        get() = prefs.getString(KEY_DEVICE_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_TOKEN, value).apply()

    var pairedAt: String?
        get() = prefs.getString(KEY_PAIRED_AT, null)
        set(value) = prefs.edit().putString(KEY_PAIRED_AT, value).apply()

    var isTelemetrySyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_TELEMETRY_SYNC, true)
        set(value) = prefs.edit().putBoolean(KEY_TELEMETRY_SYNC, value).apply()

    val isPaired: Boolean
        get() = !deviceToken.isNullOrBlank()

    fun savePairing(id: String, token: String, timestamp: String) {
        prefs.edit()
            .putString(KEY_DEVICE_ID, id)
            .putString(KEY_DEVICE_TOKEN, token)
            .putString(KEY_PAIRED_AT, timestamp)
            .apply()
    }

    fun clearPairing() {
        prefs.edit()
            .remove(KEY_DEVICE_TOKEN)
            .remove(KEY_PAIRED_AT)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "contextguard_relay_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_TOKEN = "device_token"
        private const val KEY_PAIRED_AT = "paired_at"
        private const val KEY_TELEMETRY_SYNC = "telemetry_sync_enabled"

        @Volatile
        private var instance: RelayStorage? = null

        fun getInstance(context: Context): RelayStorage {
            return instance ?: synchronized(this) {
                instance ?: RelayStorage(context.applicationContext).also { instance = it }
            }
        }
    }
}
