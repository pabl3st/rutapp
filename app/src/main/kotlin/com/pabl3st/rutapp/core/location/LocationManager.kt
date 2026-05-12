package com.pabl3st.rutapp.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // FusedLocationProviderClient es thread-safe — OK construir en constructor
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    // ── Última posición conocida (rápido, sin esperar fix nuevo) ─
    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): Location? = suspendCancellableCoroutine { cont ->
        fusedClient.lastLocation
            .addOnSuccessListener { loc -> cont.resume(loc) {} }
            .addOnFailureListener { cont.resume(null) {} }
    }

    // ── Flow de actualizaciones continuas ─────────────────────
    // Activo solo mientras el mapa está en pantalla (scope del ViewModel).
    @SuppressLint("MissingPermission")
    fun locationUpdates(
        intervalMs: Long  = 5_000L,   // cada 5 segundos
        minDistance: Float = 10f,      // mínimo 10 metros de movimiento
    ): Flow<Location> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateDistanceMeters(minDistance)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }.catch { /* GPS no disponible — fallar silenciosamente */ }

    // ── Distancia entre dos puntos en metros ──────────────────
    fun distanceBetween(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double,
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }
}
