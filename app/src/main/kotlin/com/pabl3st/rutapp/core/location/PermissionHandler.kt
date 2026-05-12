package com.pabl3st.rutapp.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
// onPermanentlyDenied: el usuario marcó "No volver a preguntar" — hay que
// dirigirlo a Ajustes del sistema manualmente.
@Composable
fun rememberLocationPermissionLauncher(
    onGranted:          () -> Unit,
    onDenied:           () -> Unit,
    onPermanentlyDenied: (() -> Unit)? = null,
): () -> Unit {
    val activity = LocalContext.current as? androidx.activity.ComponentActivity
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            onGranted()
        } else {
            // Si shouldShowRequestPermissionRationale devuelve false tras la denegación
            // significa que el usuario eligió "No volver a preguntar"
            val canAskAgain = activity?.shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) ?: true
            if (!canAskAgain && onPermanentlyDenied != null) {
                onPermanentlyDenied()
            } else {
                onDenied()
            }
        }
    }

    return {
        launcher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ))
    }
}
