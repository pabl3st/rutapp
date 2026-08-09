package com.pabl3st.rutapp.data.session

import com.pabl3st.rutapp.core.UserRole

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // ── Señal reactiva de sesión ──────────────────────────────
    /**
     * Se emite cada vez que cambia algo que decide QUÉ DATOS ve el usuario:
     * userId, rol, cuenta o la jerarquía (managedAgentIds).
     *
     * BUG que resuelve (ago 2026): SessionManager era estado plano, y
     * RouteRepository.observeAll() elegía la consulta UNA sola vez, al
     * construir el Flow. Si un ViewModel nacía antes de que delta_sync
     * rellenara managedAgentIds, quedaba cableado a listOf(userId) para
     * siempre: la jerarquía llegaba después y el Flow ya no se rehacía.
     *
     * Síntoma observado: el Calendario del tab inferior (que reutiliza su
     * ViewModel vía saveState/restoreState) mostraba el mes vacío, mientras
     * que el mismo Calendario abierto desde el menú — ViewModel nuevo, foto
     * actual de la sesión — sí mostraba las rutas. Reiniciar la app lo
     * "arreglaba", que es la pista de que era una foto vieja y no un fallo
     * de datos.
     *
     * Los repositorios deben usar flatMapLatest sobre esta señal en lugar de
     * leer la sesión una única vez.
     */
    private val _sessionScope = MutableStateFlow(currentScope())
    val sessionScope: StateFlow<SessionScope> = _sessionScope.asStateFlow()

    data class SessionScope(
        val userId:          Int,
        val accountId:       Int,
        val userRole:        String,
        val managedAgentIds: List<Int>,
    )

    private fun currentScope() = SessionScope(
        userId          = prefs.getInt(KEY_USER_ID, 0),
        accountId       = prefs.getInt(KEY_ACCOUNT_ID, 0),
        userRole        = prefs.getString(KEY_USER_ROLE, "") ?: "",
        managedAgentIds = (prefs.getString(KEY_MANAGED_AGENTS, "") ?: "")
            .split(",").mapNotNull { it.trim().toIntOrNull() },
    )

    /** Recalcula y emite si algo del alcance cambió. Idempotente. */
    private fun refreshScope() {
        val next = currentScope()
        if (next != _sessionScope.value) _sessionScope.value = next
    }

    /** IDs de agentes que reportan directamente a este manager.
     *  Vacío para roles distintos de manager. */
    var managedAgentIds: List<Int>
        get() {
            val raw = prefs.getString(KEY_MANAGED_AGENTS, "") ?: ""
            return if (raw.isBlank()) emptyList()
                   else raw.split(",").mapNotNull { it.trim().toIntOrNull() }
        }
        set(value) {
            prefs.edit().putString(KEY_MANAGED_AGENTS, value.joinToString(",")).apply()
            refreshScope()
        }

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
        refreshScope()   // login / cambio de usuario: rehacer consultas dependientes
    }

    /** Limpieza nuclear: borra TODO (token, datos del usuario, managedAgentIds,
     *  lastSync, etc.). Solo se usa cuando es seguro descartar todo el estado
     *  cacheado — por ejemplo, al cambiar a una cuenta totalmente diferente,
     *  donde mantener algo del usuario anterior sería contaminación. */
    fun clear() {
        try { securePrefs.edit().clear().apply() } catch (_: Exception) {}
        prefs.edit().clear().apply()
        refreshScope()
    }

    /** Logout "ligero": borra SOLO el token de autenticación.
     *
     *  Antes (bug mayo 2026): el logout llamaba a clear() y borraba todo,
     *  incluido managedAgentIds. El siguiente login del MISMO usuario (o de
     *  otro usuario en la misma cuenta) arrancaba con managedAgentIds=[]
     *  → RouteRepository.observeAll() caía al fallback [session.userId]
     *  → admin/manager no veía las rutas asignadas a su subárbol hasta que
     *  delta_sync repoblaba managedAgentIds (2-15s, o nunca si red mala).
     *
     *  Ahora: solo se borra el token. accountId, role, lastSync y
     *  managedAgentIds sobreviven al logout para que el próximo login del
     *  MISMO usuario tenga los datos al instante. Si en el próximo login
     *  cambia el userId o accountId, AuthRepository.parseAuthResponse limpia
     *  lo que toque (P5 para cuenta nueva, lógica adicional para usuario
     *  nuevo de la misma cuenta). */
    fun clearTokenOnly() {
        try { securePrefs.edit().remove(KEY_TOKEN).apply() } catch (_: Exception) {}
    }

    /** Limpia el cache de jerarquía (managedAgentIds, lastSync) sin tocar el
     *  resto de la sesión. Se usa cuando el siguiente login es de un usuario
     *  DISTINTO en la misma cuenta — el subárbol del nuevo no es el del
     *  anterior, así que arrancar con la lista vieja sería incorrecto. */
    fun clearManagedHierarchy() {
        prefs.edit()
            .remove(KEY_MANAGED_AGENTS)
            .remove(KEY_LAST_SYNC)
            .remove(KEY_LAST_FULL_SYNC)
            .apply()
        refreshScope()
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
