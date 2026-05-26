package com.pabl3st.rutapp.feature.home

import android.content.Context
import com.pabl3st.rutapp.core.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.core.location.LocationForegroundService
import com.pabl3st.rutapp.data.local.entity.DaySessionEntity
import com.pabl3st.rutapp.data.repository.JornadaRepository
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JornadaUiState(
    val session:          DaySessionEntity? = null,
    val elapsedMs:        Long              = 0L,
    val distanceKm:       Double            = 0.0,
    val isLocating:       Boolean           = false,
    val showReopenDialog: Boolean           = false,
    // Resumen de jornada — se muestra al finalizar
    val summary:          JornadaSummary?   = null,
)

/** Datos del resumen mostrado al cerrar la jornada */
data class JornadaSummary(
    val elapsedMs:     Long,
    val distanceKm:    Double,
    val stopsTotal:    Int,
    val stopsDone:     Int,
    val stopsSkipped:  Int,
    val stopsPending:  Int,
)

@HiltViewModel
class JornadaViewModel @Inject constructor(
    private val jornadaRepo: JornadaRepository,
    private val routeRepo:   RouteRepository,
    private val stopRepo:    StopRepository,
    @ApplicationContext private val appContext: Context,
) : BaseViewModel() {

    private val _ui = MutableStateFlow(JornadaUiState())
    val ui: StateFlow<JornadaUiState> = _ui.asStateFlow()

    private var tickJob:  Job? = null
    private var routeUid: String = ""

    fun init(routeUid: String) {
        if (this.routeUid == routeUid) return
        this.routeUid = routeUid
        val dateStr = jornadaRepo.todayStr()

        viewModelScope.launch {
            jornadaRepo.observe(routeUid, dateStr).collect { session ->
                _ui.update {
                    it.copy(
                        session    = session,
                        distanceKm = session?.distanceKm ?: 0.0,
                    )
                }
                when {
                    session?.state == "running" -> {
                        startTick(session)
                        _ui.update { it.copy(isLocating = true) }
                    }
                    else -> {
                        stopTick()
                        _ui.update {
                            it.copy(
                                isLocating = false,
                                elapsedMs  = session?.elapsedMs ?: 0L,
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Controles de jornada ──────────────────────────────────

    fun start() {
        viewModelScope.launch {
            jornadaRepo.start(routeUid, jornadaRepo.todayStr())
            startGpsService()
        }
    }

    fun pause() {
        viewModelScope.launch {
            jornadaRepo.pause(routeUid, jornadaRepo.todayStr())
            stopGpsService()
        }
    }

    fun resume() {
        viewModelScope.launch {
            jornadaRepo.resume(routeUid, jornadaRepo.todayStr())
            startGpsService()
        }
    }

    fun finish() {
        viewModelScope.launch {
            // Capturar métricas ANTES de cerrar — la sesión sigue accesible
            val current     = _ui.value
            val elapsedNow  = current.session?.let { jornadaRepo.elapsedMs(it) } ?: current.elapsedMs
            val distanceNow = current.session?.distanceKm ?: current.distanceKm

            jornadaRepo.finish(routeUid, jornadaRepo.todayStr())
            stopGpsService()

            val stops = stopRepo.getByRoute(routeUid)
            val done    = stops.count { it.status == "done" }
            val skipped = stops.count { it.status == "skipped" }
            val pending = stops.count { it.status == "pending" || it.status == "visiting" }

            // Sincronizar route.status: si todos done/skipped → ruta done
            if (stops.isNotEmpty() && stops.all { it.status == "done" || it.status == "skipped" }) {
                routeRepo.markDone(routeUid)
            }

            // Mostrar resumen de jornada
            _ui.update { it.copy(summary = JornadaSummary(
                elapsedMs    = elapsedNow,
                distanceKm   = distanceNow,
                stopsTotal   = stops.size,
                stopsDone    = done,
                stopsSkipped = skipped,
                stopsPending = pending,
            )) }
        }
    }

    /** Cierra el diálogo de resumen */
    fun dismissSummary() = _ui.update { it.copy(summary = null) }

    fun onReopenRequest() = _ui.update { it.copy(showReopenDialog = true) }
    fun onReopenDismiss() = _ui.update { it.copy(showReopenDialog = false) }

    fun confirmReopen() {
        _ui.update { it.copy(showReopenDialog = false) }
        viewModelScope.launch {
            jornadaRepo.reopen(routeUid, jornadaRepo.todayStr())
            startGpsService()
        }
    }

    // ── Foreground Service GPS ────────────────────────────────

    private fun startGpsService() {
        val intent = LocationForegroundService.startIntent(appContext, routeUid)
        appContext.startForegroundService(intent)
    }

    private fun stopGpsService() {
        val intent = LocationForegroundService.stopIntent(appContext)
        appContext.startService(intent)   // STOP_ACTION lo gestiona el propio servicio
    }

    // ── Tick del timer ────────────────────────────────────────

    private fun startTick(session: DaySessionEntity) {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                // Usar la sesión más fresca del UiState, no el snapshot inicial
                // Esto es crítico para que pause/resume actualicen el timer correctamente
                val current = _ui.value.session ?: session
                _ui.update { it.copy(elapsedMs = jornadaRepo.elapsedMs(current)) }
                delay(1_000L)
            }
        }
    }

    private fun stopTick() {
        tickJob?.cancel()
        tickJob = null
    }

    // ── Formato mm:ss o h:mm:ss ───────────────────────────────

    fun formatElapsed(ms: Long): String {
        val s   = (ms / 1000).toInt()
        val h   = s / 3600
        val m   = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec)
        else "%02d:%02d".format(m, sec)
    }

    override fun onCoroutineError(t: Throwable) {
        // Errores de GPS no deben crashar el VM — se ignoran silenciosamente
    }
}
