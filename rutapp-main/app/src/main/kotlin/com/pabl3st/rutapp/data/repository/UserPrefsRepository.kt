package com.pabl3st.rutapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pabl3st.rutapp.data.network.RutasApiService
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ── Preferencias del usuario — persistidas en DataStore local
// y sincronizadas con user_prefs del servidor en background
// ─────────────────────────────────────────────────────────────
data class UserPrefs(
    // ── Generales ────────────────────────────────────────────
    val language:          String  = "es",        // es|en
    val timezone:          String  = "Europe/Madrid",

    // ── Formulario de visita ──────────────────────────────────
    // Controla qué secciones aparecen en VisitaScreen además de KPIs
    val showVisitDuration: Boolean = true,         // mostrar campo duración
    val showNextAction:    Boolean = true,         // mostrar campo próxima acción
    val showPhotos:        Boolean = true,         // mostrar sección fotos
    val requireResult:     Boolean = true,         // resultado visita obligatorio

    // ── Notificaciones ────────────────────────────────────────
    val pushEnabled:       Boolean = true,         // notificaciones push
    val autoSync:          Boolean = true,         // sync automático cada 15min
    val jornadaReminder:   Boolean = false,        // recordatorio inicio jornada
    val jornadaReminderHour: Int   = 9,            // hora del recordatorio (9:00)
)

private val Context.userPrefsDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "rutasapp_user_prefs")

@Singleton
class UserPrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val session: SessionManager,
    private val api:     RutasApiService,
) {
    private object Keys {
        val LANGUAGE           = stringPreferencesKey("language")
        val TIMEZONE           = stringPreferencesKey("timezone")
        val SHOW_DURATION      = booleanPreferencesKey("show_visit_duration")
        val SHOW_NEXT_ACTION   = booleanPreferencesKey("show_next_action")
        val SHOW_PHOTOS        = booleanPreferencesKey("show_photos")
        val REQUIRE_RESULT     = booleanPreferencesKey("require_result")
        val PUSH_ENABLED       = booleanPreferencesKey("push_enabled")
        val AUTO_SYNC          = booleanPreferencesKey("auto_sync")
        val JORNADA_REMINDER   = booleanPreferencesKey("jornada_reminder")
        val JORNADA_HOUR       = stringPreferencesKey("jornada_reminder_hour")
    }

    val prefs: Flow<UserPrefs> = context.userPrefsDataStore.data.map { p ->
        UserPrefs(
            language          = p[Keys.LANGUAGE]         ?: "es",
            timezone          = p[Keys.TIMEZONE]         ?: "Europe/Madrid",
            showVisitDuration = p[Keys.SHOW_DURATION]    ?: true,
            showNextAction    = p[Keys.SHOW_NEXT_ACTION] ?: true,
            showPhotos        = p[Keys.SHOW_PHOTOS]      ?: true,
            requireResult     = p[Keys.REQUIRE_RESULT]   ?: true,
            pushEnabled       = p[Keys.PUSH_ENABLED]     ?: true,
            autoSync          = p[Keys.AUTO_SYNC]        ?: true,
            jornadaReminder   = p[Keys.JORNADA_REMINDER] ?: false,
            jornadaReminderHour = p[Keys.JORNADA_HOUR]?.toIntOrNull() ?: 9,
        )
    }

    suspend fun update(transform: UserPrefs.() -> UserPrefs) {
        val current = getCurrentSnapshot()
        val updated = current.transform()
        save(updated)
        syncToServer(updated)
    }

    private suspend fun save(p: UserPrefs) {
        context.userPrefsDataStore.edit { store ->
            store[Keys.LANGUAGE]         = p.language
            store[Keys.TIMEZONE]         = p.timezone
            store[Keys.SHOW_DURATION]    = p.showVisitDuration
            store[Keys.SHOW_NEXT_ACTION] = p.showNextAction
            store[Keys.SHOW_PHOTOS]      = p.showPhotos
            store[Keys.REQUIRE_RESULT]   = p.requireResult
            store[Keys.PUSH_ENABLED]     = p.pushEnabled
            store[Keys.AUTO_SYNC]        = p.autoSync
            store[Keys.JORNADA_REMINDER] = p.jornadaReminder
            store[Keys.JORNADA_HOUR]     = p.jornadaReminderHour.toString()
        }
    }

    // ── Sync al servidor — user_prefs tabla ───────────────────
    private suspend fun syncToServer(p: UserPrefs) {
        val token = session.token ?: return
        val json  = buildServerJson(p)
        runCatching {
            api.updateUserPrefs(token = token, body = mapOf("prefs" to json))
        }
    }

    // Construye el JSON para servidor en formato compatible con user_prefs.prefs
    private fun buildServerJson(p: UserPrefs): Map<String, Any> = mapOf(
        "language"            to p.language,
        "timezone"            to p.timezone,
        "show_visit_duration" to p.showVisitDuration,
        "show_next_action"    to p.showNextAction,
        "show_photos"         to p.showPhotos,
        "require_result"      to p.requireResult,
        "push_enabled"        to p.pushEnabled,
        "auto_sync"           to p.autoSync,
        "jornada_reminder"    to p.jornadaReminder,
        "jornada_reminder_hour" to p.jornadaReminderHour,
    )

    // Snapshot puntual para la transformación
    private suspend fun getCurrentSnapshot(): UserPrefs {
        var result = UserPrefs()
        context.userPrefsDataStore.data.collect { p ->
            result = UserPrefs(
                language          = p[Keys.LANGUAGE]         ?: "es",
                timezone          = p[Keys.TIMEZONE]         ?: "Europe/Madrid",
                showVisitDuration = p[Keys.SHOW_DURATION]    ?: true,
                showNextAction    = p[Keys.SHOW_NEXT_ACTION] ?: true,
                showPhotos        = p[Keys.SHOW_PHOTOS]      ?: true,
                requireResult     = p[Keys.REQUIRE_RESULT]   ?: true,
                pushEnabled       = p[Keys.PUSH_ENABLED]     ?: true,
                autoSync          = p[Keys.AUTO_SYNC]        ?: true,
                jornadaReminder   = p[Keys.JORNADA_REMINDER] ?: false,
                jornadaReminderHour = p[Keys.JORNADA_HOUR]?.toIntOrNull() ?: 9,
            )
            return@collect
        }
        return result
    }
}
