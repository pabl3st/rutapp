package com.pabl3st.rutapp.feature.rutas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.core.map.MapProvider
import com.pabl3st.rutapp.data.repository.StopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CrearParadaUiState(
    val name: String          = "",
    val externalId: String    = "",
    val address: String       = "",
    val lat: String           = "",
    val lng: String           = "",
    val contactName: String   = "",
    val contactPhone: String  = "",
    val notes: String         = "",
    val priority: Int         = 3,
    val isGeocoding: Boolean  = false,
    val isSaving: Boolean     = false,
    val savedUid: String?     = null,
    val error: String?        = null,
)

@HiltViewModel
class CrearParadaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stopRepo:    StopRepository,
    private val mapProvider: MapProvider,
) : ViewModel() {

    private val routeUid: String = checkNotNull(savedStateHandle["routeUid"])

    private val _ui = MutableStateFlow(CrearParadaUiState())
    val ui: StateFlow<CrearParadaUiState> = _ui.asStateFlow()

    // ── Campos ────────────────────────────────────────────────
    fun onNameChange(v: String)         = _ui.update { it.copy(name = v, error = null) }
    fun onExternalIdChange(v: String)   = _ui.update { it.copy(externalId = v) }
    fun onAddressChange(v: String)      = _ui.update { it.copy(address = v) }
    fun onLatChange(v: String)          = _ui.update { it.copy(lat = v) }
    fun onLngChange(v: String)          = _ui.update { it.copy(lng = v) }
    fun onContactNameChange(v: String)  = _ui.update { it.copy(contactName = v) }
    fun onContactPhoneChange(v: String) = _ui.update { it.copy(contactPhone = v) }
    fun onNotesChange(v: String)        = _ui.update { it.copy(notes = v) }
    fun onPriorityChange(v: Int)        = _ui.update { it.copy(priority = v.coerceIn(1, 5)) }

    // ── Geocodificar dirección ────────────────────────────────
    fun geocodeAddress() {
        val address = _ui.value.address.trim()
        if (address.isBlank()) return
        viewModelScope.launch {
            _ui.update { it.copy(isGeocoding = true) }
            val result = mapProvider.geocode(address)
            if (result != null) {
                _ui.update { it.copy(
                    lat         = "%.6f".format(result.lat),
                    lng         = "%.6f".format(result.lng),
                    isGeocoding = false,
                ) }
            } else {
                _ui.update { it.copy(isGeocoding = false) }
            }
        }
    }

    // ── Guardar ───────────────────────────────────────────────
    fun save() {
        val name = _ui.value.name.trim()
        if (name.isBlank()) {
            _ui.update { it.copy(error = "El nombre es obligatorio") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(isSaving = true, error = null) }
            try {
                val lat = _ui.value.lat.toDoubleOrNull()
                val lng = _ui.value.lng.toDoubleOrNull()
                val stop = stopRepo.createStop(
                    routeUid     = routeUid,
                    name         = name,
                    externalId   = _ui.value.externalId.trim().ifEmpty { null },
                    address      = _ui.value.address.trim().ifEmpty { null },
                    lat          = lat,
                    lng          = lng,
                    notes        = _ui.value.notes.trim().ifEmpty { null },
                    contactName  = _ui.value.contactName.trim().ifEmpty { null },
                    contactPhone = _ui.value.contactPhone.trim().ifEmpty { null },
                )
                // Geocodificar en background si hay dirección pero no coords
                val address = _ui.value.address.trim()
                if (lat == null && address.isNotBlank()) {
                    launch { stopRepo.geocodeAddress(stop.uid, address, mapProvider::geocode) }
                }
                _ui.update { it.copy(isSaving = false, savedUid = stop.uid) }
            } catch (e: Exception) {
                _ui.update { it.copy(isSaving = false, error = e.message ?: "Error al guardar") }
            }
        }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
}
