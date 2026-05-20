package com.pabl3st.rutapp.feature.rutas

import com.pabl3st.rutapp.core.BaseViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.core.map.MapProvider
import com.pabl3st.rutapp.data.repository.StopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditarParadaUiState(
    val name:          String  = "",
    val externalId:    String  = "",
    val address:       String  = "",
    val lat:           String  = "",
    val lng:           String  = "",
    val contactName:   String  = "",
    val contactPhone:  String  = "",
    val openingHours:  String  = "",
    val notes:         String  = "",
    val segment:       String  = "",
    val visitFrequency:String  = "",
    val priority:      Int     = 3,
    val isLoading:     Boolean = true,
    val isGeocoding:   Boolean = false,
    val isSaving:      Boolean = false,
    val saved:         Boolean = false,
    val error:         String? = null,
)

@HiltViewModel
class EditarParadaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stopRepo:    StopRepository,
    private val mapProvider: MapProvider,
) : BaseViewModel() {

    private val stopUid: String = checkNotNull(savedStateHandle["stopUid"])

    private val _ui = MutableStateFlow(EditarParadaUiState())
    val ui: StateFlow<EditarParadaUiState> = _ui.asStateFlow()

    init { loadStop() }

    private fun loadStop() {
        viewModelScope.launch {
            val stop = stopRepo.getByUid(stopUid)
            if (stop == null) {
                _ui.update { it.copy(isLoading = false, error = "Parada no encontrada") }
                return@launch
            }
            _ui.update { it.copy(
                name           = stop.name,
                externalId     = stop.externalId ?: "",
                address        = stop.address ?: "",
                lat            = stop.lat?.let { "%.6f".format(it) } ?: "",
                lng            = stop.lng?.let { "%.6f".format(it) } ?: "",
                contactName    = stop.contactName ?: "",
                contactPhone   = stop.contactPhone ?: "",
                openingHours   = stop.openingHours ?: "",
                notes          = stop.notes ?: "",
                segment        = stop.segment ?: "",
                visitFrequency = stop.visitFrequency?.toString() ?: "",
                priority       = stop.priority.coerceIn(1, 5),
                isLoading      = false,
            ) }
        }
    }

    fun onNameChange(v: String)           = _ui.update { it.copy(name = v, error = null) }
    fun onExternalIdChange(v: String)     = _ui.update { it.copy(externalId = v) }
    fun onAddressChange(v: String)        = _ui.update { it.copy(address = v) }
    fun onLatChange(v: String)            = _ui.update { it.copy(lat = v) }
    fun onLngChange(v: String)            = _ui.update { it.copy(lng = v) }
    fun onContactNameChange(v: String)    = _ui.update { it.copy(contactName = v) }
    fun onContactPhoneChange(v: String)   = _ui.update { it.copy(contactPhone = v) }
    fun onOpeningHoursChange(v: String)   = _ui.update { it.copy(openingHours = v) }
    fun onNotesChange(v: String)          = _ui.update { it.copy(notes = v) }
    fun onSegmentChange(v: String)        = _ui.update { it.copy(segment = v) }
    fun onVisitFrequencyChange(v: String) = _ui.update { it.copy(visitFrequency = v) }
    fun onPriorityChange(v: Int)          = _ui.update { it.copy(priority = v.coerceIn(1, 5)) }

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

    fun save() {
        val name = _ui.value.name.trim()
        if (name.isBlank()) {
            _ui.update { it.copy(error = "El nombre es obligatorio") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(isSaving = true, error = null) }
            try {
                val lat  = _ui.value.lat.toDoubleOrNull()
                val lng  = _ui.value.lng.toDoubleOrNull()
                val freq = _ui.value.visitFrequency.toIntOrNull()
                stopRepo.updateStop(
                    uid            = stopUid,
                    name           = name,
                    externalId     = _ui.value.externalId.trim().ifEmpty { null },
                    address        = _ui.value.address.trim().ifEmpty { null },
                    lat            = lat,
                    lng            = lng,
                    notes          = _ui.value.notes.trim().ifEmpty { null },
                    contactName    = _ui.value.contactName.trim().ifEmpty { null },
                    contactPhone   = _ui.value.contactPhone.trim().ifEmpty { null },
                    visitFrequency = freq,
                    priority       = _ui.value.priority,
                    segment        = _ui.value.segment.trim().ifEmpty { null },
                    openingHours   = _ui.value.openingHours.trim().ifEmpty { null },
                )
                // Geocodificar en background si hay dirección pero no coords
                val address = _ui.value.address.trim()
                if (lat == null && address.isNotBlank()) {
                    launch { stopRepo.geocodeAddress(stopUid, address, mapProvider::geocode) }
                }
                _ui.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Exception) {
                _ui.update { it.copy(isSaving = false, error = e.message ?: "Error al guardar") }
            }
        }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
}
