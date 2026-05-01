package com.pabl3st.rutapp.data.session

import android.content.Context
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestiona el token de sesión y datos del usuario autenticado.
 * Token en EncryptedSharedPreferences (Android Keystore AES-256).
 * Datos no sensibles en SharedPreferences normales.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // MasterKeys.getOrCreate — API estable en security-crypto 1.1.0
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val securePrefs = EncryptedSharedPreferences.create(
        "rutasapp_secure",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val prefs = context.getSharedPreferences("rutasapp_session", Context.MODE_PRIVATE)

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

    // ── Sync cursor ──────────────────────────────────────────
    var lastSyncTimestamp: String
        get()      = prefs.getString(KEY_LAST_SYNC, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_SYNC, value).apply()

    // ── Device ID estable ────────────────────────────────────
    val deviceId: String
        get() = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID
        ) ?: "unknown"

    // ── Save full auth response ───────────────────────────────
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
        securePrefs.edit().clear().apply()
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
    }
}
