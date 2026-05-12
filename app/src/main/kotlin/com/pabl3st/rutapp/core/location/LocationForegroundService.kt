package com.pabl3st.rutapp.core.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pabl3st.rutapp.MainActivity
import com.pabl3st.rutapp.R
import com.pabl3st.rutapp.data.repository.JornadaRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground Service que mantiene el GPS activo mientras la jornada está en estado "running".
 * Se arranca desde JornadaViewModel.start()/resume() y se detiene en pause()/finish().
 *
 * Comunicación:
 *  - Intent action START_ACTION con extra EXTRA_ROUTE_UID  → inicia tracking
 *  - Intent action STOP_ACTION                             → detiene tracking y el servicio
 */
@AndroidEntryPoint
class LocationForegroundService : Service() {

    @Inject lateinit var locationMgr:  LocationManager
    @Inject lateinit var jornadaRepo:  JornadaRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Ciclo de vida ─────────────────────────────────────────

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            START_ACTION -> {
                val routeUid = intent.getStringExtra(EXTRA_ROUTE_UID) ?: run {
                    stopSelf(); return START_NOT_STICKY
                }
                startForeground(NOTIFICATION_ID, buildNotification())
                startTracking(routeUid)
            }
            STOP_ACTION -> {
                serviceScope.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    // ── GPS tracking ──────────────────────────────────────────

    private fun startTracking(routeUid: String) {
        val dateStr = jornadaRepo.todayStr()
        serviceScope.launch {
            locationMgr.locationUpdates(
                intervalMs  = 10_000L,   // cada 10 segundos
                minDistance = 20f,        // mínimo 20 metros de movimiento
            ).collect { loc ->
                jornadaRepo.updateGps(routeUid, dateStr, loc.latitude, loc.longitude)
            }
        }
    }

    // ── Notificación persistente ──────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Jornada activa",
            NotificationManager.IMPORTANCE_LOW,    // LOW = sin sonido, pero visible
        ).apply {
            description = "Tracking GPS de la jornada de trabajo"
            setShowBadge(false)
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jornada en curso")
            .setContentText("GPS activo — toca para abrir RutasApp")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    // ── Companion ─────────────────────────────────────────────

    companion object {
        const val CHANNEL_ID       = "jornada_gps"
        const val NOTIFICATION_ID  = 1001
        const val START_ACTION     = "com.pabl3st.rutapp.action.GPS_START"
        const val STOP_ACTION      = "com.pabl3st.rutapp.action.GPS_STOP"
        const val EXTRA_ROUTE_UID  = "route_uid"

        fun startIntent(ctx: Context, routeUid: String): Intent =
            Intent(ctx, LocationForegroundService::class.java).apply {
                action = START_ACTION
                putExtra(EXTRA_ROUTE_UID, routeUid)
            }

        fun stopIntent(ctx: Context): Intent =
            Intent(ctx, LocationForegroundService::class.java).apply {
                action = STOP_ACTION
            }
    }
}
