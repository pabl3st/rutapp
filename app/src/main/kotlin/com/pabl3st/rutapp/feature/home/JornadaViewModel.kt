package com.pabl3st.rutapp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.core.location.LocationManager
import com.pabl3st.rutapp.data.local.entity.DaySessionEntity
import com.pabl3st.rutapp.data.repository.JornadaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val locationMgr: LocationManager,
) : ViewModel() {

    private val _ui = MutableStateFlow(JornadaUiState())
    val ui: StateFlow<JornadaUiState> = _ui.asStateFlow()

    private var tickJob:     Job? = null
    private var locationJob: Job? = null
    private var routeUid:    String = ""

    fun init(routeUid: String) {
        if (this.routeUid == routeUid) return
        this.routeUid = routeUid
        val dateStr = jornadaRepo.todayStr()

        viewModelScope.launch {
            jornadaRepo.observe(routeUid, dateStr).collect { session ->
                _ui.update { it.copy(
                    session    = session,
                    distanceKm = session?.distanceKm ?: 0.0,
                ) }
                if (session?.state == "running") {
                    startTick(session)
                    startLocationUpdates()
                } else {
                    stopTick()
                    stopLocationUpdates()
                    if (session != null) {
                        _ui.update { it.copy(elapsedMs = session.elapsedMs) }
                    }
                }
            }
        }
    }

    fun start() {
        viewModelScope.launch {
            jornadaRepo.start(routeUid, jornadaRepo.todayStr())
        }
    }

    fun pause() {
        viewModelScope.launch {
            jornadaRepo.pause(routeUid, jornadaRepo.todayStr())
        }
    }

    fun resume() {
        viewModelScope.launch {
            jornadaRepo.resume(routeUid, jornadaRepo.todayStr())
        }
    }

    fun finish() {
        viewModelScope.launch {
            jornadaRepo.finish(routeUid, jornadaRepo.todayStr())
        }
    }

    private fun startTick(session: DaySessionEntity) {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                _ui.update { it.copy(elapsedMs = jornadaRepo.elapsedMs(session)) }
                delay(1_000L)
            }
        }
    }

    private fun stopTick() { tickJob?.cancel(); tickJob = null }

    private fun startLocationUpdates() {
        if (locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            _ui.update { it.copy(isLocating = true) }
            locationMgr.locationUpdates(intervalMs = 10_000L, minDistance = 30f).collect { loc ->
                jornadaRepo.updateGps(routeUid, jornadaRepo.todayStr(), loc.latitude, loc.longitude)
            }
        }
    }

    private fun stopLocationUpdates() {
        locationJob?.cancel()
        locationJob = null
        _ui.update { it.copy(isLocating = false) }
    }

    // Formato mm:ss o h:mm:ss
    fun formatElapsed(ms: Long): String {
        val s = (ms / 1000).toInt()
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec)
        else "%02d:%02d".format(m, sec)
    }
}
