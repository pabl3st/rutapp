package com.pabl3st.rutapp.feature.perfil

import com.pabl3st.rutapp.core.BaseViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.local.entity.BusinessProfileEntity
import com.pabl3st.rutapp.data.local.entity.KpiDefinitionEntity
import com.pabl3st.rutapp.data.repository.BusinessProfileRepository
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BusinessProfileUiState(
    val profile:      BusinessProfileEntity?    = null,
    val kpis:         List<KpiDefinitionEntity> = emptyList(),
    val canEditSector: Boolean                  = false,
    val isLoading: Boolean                      = true,
    val showSectorPicker: Boolean               = false,
    val showAddKpiDialog: Boolean               = false,
    // campos del diálogo nuevo KPI
    val newKpiLabel: String                     = "",
    val newKpiType: String                      = "number",
    val newKpiUnit: String                      = "",
    val newKpiSection: String                   = "general",
    val newKpiRequired: Boolean                 = false,
    val error: String?                          = null,
)

@HiltViewModel
class BusinessProfileViewModel @Inject constructor(
    private val repo:    BusinessProfileRepository,
    private val session: SessionManager,
) : BaseViewModel() {

    private val _ui = MutableStateFlow(BusinessProfileUiState(
        canEditSector = session.userRole in listOf("owner", "admin", "god"),
    ))
    val ui: StateFlow<BusinessProfileUiState> = _ui.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            repo.ensureCommonKpis()
            val profile = repo.getOrCreateProfile()
            repo.seedKpisIfNeeded(profile.sector)

            repo.observeProfile()
                .filterNotNull()
                .flatMapLatest { p ->
                    repo.observeAllKpis(p.sector).map { kpis -> p to kpis }
                }
                .catch { e -> _ui.update { it.copy(error = e.message, isLoading = false) } }
                .collect { (p, kpis) ->
                    _ui.update { it.copy(profile = p, kpis = kpis, isLoading = false) }
                }
        }
    }

    fun onShowSectorPicker()  = _ui.update { it.copy(showSectorPicker = true) }
    fun onDismissSectorPicker() = _ui.update { it.copy(showSectorPicker = false) }

    fun onSelectSector(sector: String) {
        viewModelScope.launch {
            repo.setSector(sector)
            _ui.update { it.copy(showSectorPicker = false) }
        }
    }

    fun onToggleKpiVisible(id: String, visible: Boolean) {
        viewModelScope.launch { repo.setKpiVisible(id, visible) }
    }

    fun onShowAddKpiDialog()    = _ui.update { it.copy(showAddKpiDialog = true, newKpiLabel = "", newKpiType = "number", newKpiUnit = "", error = null) }
    fun onDismissAddKpiDialog() = _ui.update { it.copy(showAddKpiDialog = false) }
    fun onNewKpiLabelChange(v: String)   = _ui.update { it.copy(newKpiLabel = v, error = null) }
    fun onNewKpiTypeChange(v: String)    = _ui.update { it.copy(newKpiType = v) }
    fun onNewKpiUnitChange(v: String)    = _ui.update { it.copy(newKpiUnit = v) }
    fun onNewKpiSectionChange(v: String) = _ui.update { it.copy(newKpiSection = v) }
    fun onNewKpiRequiredChange(v: Boolean) = _ui.update { it.copy(newKpiRequired = v) }

    fun saveCustomKpi() {
        val s = _ui.value
        if (s.newKpiLabel.isBlank()) {
            _ui.update { it.copy(error = "El nombre es obligatorio") }
            return
        }
        viewModelScope.launch {
            repo.addCustomKpi(
                label    = s.newKpiLabel.trim(),
                type     = s.newKpiType,
                unit     = s.newKpiUnit.trim().ifEmpty { null },
                options  = null,
                required = s.newKpiRequired,
                section  = s.newKpiSection,
            )
            _ui.update { it.copy(showAddKpiDialog = false) }
        }
    }

    fun deleteCustomKpi(id: String) {
        viewModelScope.launch { repo.deleteCustomKpi(id) }
    }

    fun sectorLabel(sector: String) = repo.sectorLabel(sector)
    val sectors get() = repo.sectors
    override fun onCoroutineError(t: Throwable) {
        _ui.update { it.copy(
            isLoading = false,
            error     = t.message ?: "Error inesperado",
        )}
    }

}
