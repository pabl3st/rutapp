package com.pabl3st.rutapp.feature.home

import android.content.Context
import com.pabl3st.rutapp.core.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.core.location.LocationForegroundService
import com.pabl3st.rutapp.data.local.entity.DaySessionEntity
import com.pabl3st.rutapp.data.repository.JornadaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JornadaUiState(
    val session:    DaySessionEntity? = null,
    val elapsedMs:  Long              = 0L,
    val distanceKm: Double            = 0.0,
    val isLocating: Boolean           = false,
)

@HiltViewModel
class JornadaViewModel @Inject constructor(
    private val jornadaRepo: JornadaRepository,
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
            jornadaRepo.finish(routeUid, jornadaRepo.todayStr())
            stopGpsService()
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
                _ui.update { it.copy(elapsedMs = jornadaRepo.elapsedMs(session)) }
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
