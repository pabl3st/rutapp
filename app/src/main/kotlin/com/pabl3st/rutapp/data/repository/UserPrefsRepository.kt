package com.pabl3st.rutapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pabl3st.rutapp.data.network.RutasApiService
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ── Modelo de preferencias de usuario ────────────────────────
data class UserPrefs(
    // Generales
    val language:             String  = "es",           // es | en
    val timezone:             String  = "Europe/Madrid",

    // Formulario de visita — controla qué campos se muestran
    val showVisitDuration:    Boolean = true,
    val showNextAction:       Boolean = true,
    val showPhotos:           Boolean = true,
    val requireResult:        Boolean = true,

    // Notificaciones
    val pushEnabled:          Boolean = true,
    val autoSync:             Boolean = true,
    val jornadaReminder:      Boolean = false,
    val jornadaReminderHour:  Int     = 9,
)

private val Context.userPrefsStore: DataStore<Preferences>
        by preferencesDataStore(name = "rutasapp_user_prefs")

@Singleton
class UserPrefsRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val session: SessionManager,
    private val api:     RutasApiService,
) {
    // ── Claves DataStore ──────────────────────────────────────
    private object K {
        val LANGUAGE          = stringPreferencesKey("language")
        val TIMEZONE          = stringPreferencesKey("timezone")
        val SHOW_DURATION     = booleanPreferencesKey("show_visit_duration")
        val SHOW_NEXT_ACTION  = booleanPreferencesKey("show_next_action")
        val SHOW_PHOTOS       = booleanPreferencesKey("show_photos")
        val REQUIRE_RESULT    = booleanPreferencesKey("require_result")
        val PUSH_ENABLED      = booleanPreferencesKey("push_enabled")
        val AUTO_SYNC         = booleanPreferencesKey("auto_sync")
        val JORNADA_REMINDER  = booleanPreferencesKey("jornada_reminder")
        val JORNADA_HOUR      = intPreferencesKey("jornada_reminder_hour")
    }

    // ── Flow reactivo — la UI observa esto ───────────────────
    val prefs: Flow<UserPrefs> = ctx.userPrefsStore.data.map { p ->
        UserPrefs(
            language            = p[K.LANGUAGE]         ?: "es",
            timezone            = p[K.TIMEZONE]         ?: "Europe/Madrid",
            showVisitDuration   = p[K.SHOW_DURATION]    ?: true,
            showNextAction      = p[K.SHOW_NEXT_ACTION] ?: true,
            showPhotos          = p[K.SHOW_PHOTOS]       ?: true,
            requireResult       = p[K.REQUIRE_RESULT]   ?: true,
            pushEnabled         = p[K.PUSH_ENABLED]     ?: true,
            autoSync            = p[K.AUTO_SYNC]        ?: true,
            jornadaReminder     = p[K.JORNADA_REMINDER] ?: false,
            jornadaReminderHour = p[K.JORNADA_HOUR]     ?: 9,
        )
    }

    // ── Actualizar una preferencia y sincronizar ──────────────
    // Uso: prefsRepo.update { copy(pushEnabled = false) }
    suspend fun update(transform: UserPrefs.() -> UserPrefs) {
        val current = prefs.first()
        val updated = current.transform()
        persist(updated)
        syncToServer(updated)   // fire-and-forget — el usuario no espera
    }

    // ── Persistir en DataStore local ──────────────────────────
    private suspend fun persist(p: UserPrefs) {
        ctx.userPrefsStore.edit { store ->
            store[K.LANGUAGE]         = p.language
            store[K.TIMEZONE]         = p.timezone
            store[K.SHOW_DURATION]    = p.showVisitDuration
            store[K.SHOW_NEXT_ACTION] = p.showNextAction
            store[K.SHOW_PHOTOS]      = p.showPhotos
            store[K.REQUIRE_RESULT]   = p.requireResult
            store[K.PUSH_ENABLED]     = p.pushEnabled
            store[K.AUTO_SYNC]        = p.autoSync
            store[K.JORNADA_REMINDER] = p.jornadaReminder
            store[K.JORNADA_HOUR]     = p.jornadaReminderHour
        }
    }

    // ── Sincronizar con user_prefs del servidor ───────────────
    // No bloquea: error de red → preferencias siguen en DataStore
    private suspend fun syncToServer(p: UserPrefs) {
        val token = session.token ?: return
        runCatching {
            api.updateUserPrefs(
                token = token,
                body  = mapOf(
                    "prefs" to mapOf(
                        "language"              to p.language,
                        "timezone"              to p.timezone,
                        "show_visit_duration"   to p.showVisitDuration,
                        "show_next_action"      to p.showNextAction,
                        "show_photos"           to p.showPhotos,
                        "require_result"        to p.requireResult,
                        "push_enabled"          to p.pushEnabled,
                        "auto_sync"             to p.autoSync,
                        "jornada_reminder"      to p.jornadaReminder,
                        "jornada_reminder_hour" to p.jornadaReminderHour,
                    )
                )
            )
        }
        // Fallo silencioso — DataStore es la fuente de verdad
    }
}
