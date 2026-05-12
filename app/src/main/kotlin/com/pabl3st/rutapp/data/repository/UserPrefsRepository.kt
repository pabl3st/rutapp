package com.pabl3st.rutapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pabl3st.rutapp.data.local.entity.StopTagConfig
import com.pabl3st.rutapp.data.local.entity.TagCondition
import com.pabl3st.rutapp.data.network.RutasApiService
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
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

    // ── Tags configurables de stops ──────────────────────────
    // Lista de tags personalizados que el owner define.
    // Se persiste serializado a JSON en DataStore.
    val stopTags:             List<StopTagConfig> = emptyList(),

    // ── Umbral de KPI para alertas ───────────────────────────
    // El owner puede definir un umbral numérico genérico (ej: activaciones mínimas).
    // Se usa en condiciones KPI_ABOVE / KPI_BELOW de los tags.
    val kpiThreshold:         Double = 0.0,

    // ── Días de vacaciones del usuario ───────────────────────
    // Set de fechas ISO ("YYYY-MM-DD") marcadas como vacaciones.
    val vacationDays:         Set<String> = emptySet(),
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
        val STOP_TAGS         = stringPreferencesKey("stop_tags_json")   // JSON array
        val KPI_THRESHOLD     = stringPreferencesKey("kpi_threshold")    // Double as string
        val VACATION_DAYS     = stringPreferencesKey("vacation_days_json") // JSON array de fechas ISO
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
            stopTags            = deserializeTags(p[K.STOP_TAGS] ?: "[]"),
            kpiThreshold        = p[K.KPI_THRESHOLD]?.toDoubleOrNull() ?: 0.0,
            vacationDays        = deserializeVacations(p[K.VACATION_DAYS] ?: "[]"),
        )
    }

    // ── Serialización de tags ─────────────────────────────────
    private fun serializeTags(tags: List<StopTagConfig>): String = runCatching {
        val arr = JSONArray()
        tags.forEach { t ->
            arr.put(JSONObject().apply {
                put("id",                t.id)
                put("name",              t.name)
                put("icon",              t.icon)
                put("colorHex",          t.colorHex)
                put("textColorHex",      t.textColorHex)
                put("condition",         t.condition.name)
                put("conditionValue",    t.conditionValue ?: JSONObject.NULL)
                put("conditionKpiId",    t.conditionKpiId ?: JSONObject.NULL)
                put("conditionThreshold",t.conditionThreshold)
                put("enabled",           t.enabled)
            })
        }
        arr.toString()
    }.getOrDefault("[]")

    private fun deserializeTags(json: String): List<StopTagConfig> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            StopTagConfig(
                id                 = o.getString("id"),
                name               = o.getString("name"),
                icon               = o.optString("icon", "Label"),
                colorHex           = o.optString("colorHex", "#e2e8f0"),
                textColorHex       = o.optString("textColorHex", "#475569"),
                condition          = runCatching { TagCondition.valueOf(o.getString("condition")) }
                                         .getOrDefault(TagCondition.ALWAYS),
                conditionValue     = o.optString("conditionValue").ifBlank { null },
                conditionKpiId     = o.optString("conditionKpiId").ifBlank { null },
                conditionThreshold = o.optDouble("conditionThreshold", 0.0),
                enabled            = o.optBoolean("enabled", true),
            )
        }
    }.getOrDefault(emptyList())

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
            store[K.STOP_TAGS]        = serializeTags(p.stopTags)
            store[K.KPI_THRESHOLD]    = p.kpiThreshold.toString()
            store[K.VACATION_DAYS]    = serializeVacations(p.vacationDays)
        }
    }

    // ── Vacaciones ───────────────────────────────────────────
    private fun serializeVacations(days: Set<String>): String =
        org.json.JSONArray(days.toList()).toString()

    private fun deserializeVacations(json: String): Set<String> = runCatching {
        val arr = org.json.JSONArray(json)
        (0 until arr.length()).map { arr.getString(it) }.toSet()
    }.getOrDefault(emptySet())

    /** Añade o quita un día de vacaciones y persiste. */
    suspend fun toggleVacationDay(dateStr: String) {
        val current = prefs.first()
        val updated = if (dateStr in current.vacationDays)
            current.copy(vacationDays = current.vacationDays - dateStr)
        else
            current.copy(vacationDays = current.vacationDays + dateStr)
        persist(updated)
    }

    fun isVacationDay(prefs: UserPrefs, dateStr: String): Boolean =
        dateStr in prefs.vacationDays

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
                        "vacation_days"         to p.vacationDays.toList(),
                    )
                )
            )
        }
        // Fallo silencioso — DataStore es la fuente de verdad
    }

    // ── Restaurar prefs desde el servidor al hacer login ─────────
    // Llama /me → prefs y persiste en DataStore local.
    // Si el servidor no tiene vacation_days (primer login), DataStore gana.
    suspend fun restoreFromServer(serverPrefs: Map<String, Any>?) {
        if (serverPrefs == null) return
        val current = prefs.first()
        // Restaurar vacation_days desde el servidor (fuente de verdad tras reinstalación)
        val serverVacations = (serverPrefs["vacation_days"] as? List<*>)?.filterIsInstance<String>()?.toSet()
            ?: emptySet()
        // Merge: unión de días locales y del servidor (ambos son válidos)
        val merged = current.vacationDays + serverVacations
        if (merged != current.vacationDays) {
            persist(current.copy(vacationDays = merged))
        }
    }
}

