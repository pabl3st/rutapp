package com.pabl3st.rutapp.data.session

import com.pabl3st.rutapp.core.UserRole

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // Lazy — NO se inicializa en el constructor (hilo principal de Hilt)
    // Se inicializa la primera vez que se accede, desde una coroutine
    // EncryptedSharedPreferences deprecated pero sigue siendo la mejor opción
    // para almacenamiento seguro de tokens. Migrar a DataStore+EncryptedFile en S18+.
    @Suppress("DEPRECATION")
    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "rutasapp_secure",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            // Fallback: prefs normales si Keystore no disponible (emuladores sin PIN)
            Log.w("SessionManager", "EncryptedSharedPreferences no disponible, usando fallback: ${e.message}")
            context.getSharedPreferences("rutasapp_secure_fallback", Context.MODE_PRIVATE)
        }
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("rutasapp_session", Context.MODE_PRIVATE)
    }

    // ── Token ────────────────────────────────────────────────
    var token: String?
        get()      = securePrefs.getString(KEY_TOKEN, null)
        set(value) = securePrefs.edit().apply {
            if (value != null) putString(KEY_TOKEN, value) else remove(KEY_TOKEN)
        }.apply()

    val isLoggedIn: Boolean get() = !token.isNullOrEmpty()

    // ── User ─────────────────────────────────────────────────
    var userId: Int
        get()      = prefs.getInt(KEY_USER_ID, 0)
        set(value) = prefs.edit().putInt(KEY_USER_ID, value).apply()

    var userName: String
        get()      = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userEmail: String
        get()      = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userRole: String
        get()      = prefs.getString(KEY_USER_ROLE, "agent") ?: "agent"
        set(value) = prefs.edit().putString(KEY_USER_ROLE, value).apply()

    var userDisplayName: String
        get()      = prefs.getString(KEY_USER_DISPLAY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_DISPLAY_NAME, value).apply()

    // ── Account ──────────────────────────────────────────────
    var accountId: Int
        get()      = prefs.getInt(KEY_ACCOUNT_ID, 0)
        set(value) = prefs.edit().putInt(KEY_ACCOUNT_ID, value).apply()

    var accountType: String
        get()      = prefs.getString(KEY_ACCOUNT_TYPE, "individual") ?: "individual"
        set(value) = prefs.edit().putString(KEY_ACCOUNT_TYPE, value).apply()

    var accountName: String
        get()      = prefs.getString(KEY_ACCOUNT_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACCOUNT_NAME, value).apply()

    val isCompany: Boolean get() = accountType == "company"
    val isGod: Boolean     get() = UserRole.from(userRole).isGod

    var lastSyncTimestamp: String
        get()      = prefs.getString(KEY_LAST_SYNC, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_SYNC, value).apply()

    /** Epoch ms del último full-sync (delta_sync con since=época).
     *  Sirve para forzar periódicamente una descarga completa y recuperar
     *  cambios hechos directamente en BD que el sync incremental se salta. */
    var lastFullSyncMs: Long
        get()      = prefs.getLong(KEY_LAST_FULL_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_FULL_SYNC, value).apply()

    /** IDs de agentes que reportan directamente a este manager.
     *  Vacío para roles distintos de manager. */
    var managedAgentIds: List<Int>
        get() {
            val raw = prefs.getString(KEY_MANAGED_AGENTS, "") ?: ""
            return if (raw.isBlank()) emptyList()
                   else raw.split(",").mapNotNull { it.trim().toIntOrNull() }
        }
        set(value) { prefs.edit().putString(KEY_MANAGED_AGENTS, value.joinToString(",")).apply() }

    val deviceId: String
        get() = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID
        ) ?: "unknown"

    fun saveAuth(
        token: String,
        userId: Int,
        userName: String,
        userEmail: String,
        userRole: String,
        userDisplayName: String,
        accountId: Int,
        accountType: String,
        accountName: String,
    ) {
        this.token           = token
        this.userId          = userId
        this.userName        = userName
        this.userEmail       = userEmail
        this.userRole        = userRole
        this.userDisplayName = userDisplayName
        this.accountId       = accountId
        this.accountType     = accountType
        this.accountName     = accountName
    }

    fun clear() {
        try { securePrefs.edit().clear().apply() } catch (_: Exception) {}
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN             = "token"
        private const val KEY_USER_ID           = "user_id"
        private const val KEY_USER_NAME         = "user_name"
        private const val KEY_USER_EMAIL        = "user_email"
        private const val KEY_USER_ROLE         = "user_role"
        private const val KEY_USER_DISPLAY_NAME = "user_display_name"
        private const val KEY_ACCOUNT_ID        = "account_id"
        private const val KEY_ACCOUNT_TYPE      = "account_type"
        private const val KEY_ACCOUNT_NAME      = "account_name"
        private const val KEY_LAST_SYNC         = "last_sync"
        private const val KEY_LAST_FULL_SYNC    = "last_full_sync"
        private const val KEY_MANAGED_AGENTS    = "managed_agent_ids"
    }
}
