package com.pabl3st.rutapp.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat

// ── Estado del permiso GPS ────────────────────────────────────
enum class LocationPermissionState {
    Granted,           // permiso concedido
    Denied,            // denegado — puede volver a pedir
    PermanentlyDenied, // denegado para siempre — ir a ajustes del sistema
}

fun Context.locationPermissionState(): LocationPermissionState {
    val fine   = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
    return if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED)
        LocationPermissionState.Granted
    else
        LocationPermissionState.Denied
}

// ── Composable helper para solicitar permiso ──────────────────
@Composable
fun rememberLocationPermissionLauncher(
    onGranted: () -> Unit,
    onDenied:  () -> Unit,
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) onGranted() else onDenied()
    }

    return {
        launcher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ))
    }
}
