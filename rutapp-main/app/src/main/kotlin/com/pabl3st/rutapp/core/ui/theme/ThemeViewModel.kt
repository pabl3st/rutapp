package com.pabl3st.rutapp.core.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepo: ThemeRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themeRepo.themeMode
        .stateIn(
            scope            = viewModelScope,
            started          = SharingStarted.Eagerly,
            initialValue     = ThemeMode.SYSTEM,
        )

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { themeRepo.setTheme(mode) }
    }
}
