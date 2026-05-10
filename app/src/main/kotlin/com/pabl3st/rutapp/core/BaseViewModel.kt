package com.pabl3st.rutapp.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * ViewModel base con CoroutineExceptionHandler que evita crashes silenciosos.
 * Sobreescribir onCoroutineError() para manejar errores en el UiState.
 */
abstract class BaseViewModel : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        onCoroutineError(throwable)
    }

    /**
     * Callback cuando una coroutine lanza una excepción no capturada.
     * Implementar en subclases para actualizar el UiState con el error.
     */
    open fun onCoroutineError(t: Throwable) {
        // Subclases deben sobreescribir para mostrar error en UI
        // Por defecto: log sin crash
        android.util.Log.e("BaseViewModel", "Uncaught coroutine error: ${t.message}", t)
    }

    /** Launch con manejo automático de excepciones */
    protected fun launchSafe(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(exceptionHandler, block = block)
    }
}
