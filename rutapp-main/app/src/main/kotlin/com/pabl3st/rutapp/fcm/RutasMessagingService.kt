package com.pabl3st.rutapp.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pabl3st.rutapp.MainActivity
import com.pabl3st.rutapp.R
import com.pabl3st.rutapp.sync.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Tipos de payload FCM reconocidos ─────────────────────────
// Enviados desde el servidor en el campo 'data' del mensaje FCM
// type=sync_now      → forzar sync inmediato (nueva ruta asignada, etc.)
// type=new_route     → notificación + sync (ruta asignada hoy)
// type=route_update  → notificación + sync (cambio en ruta existente)
// type=message       → notificación informativa pura

@AndroidEntryPoint
class RutasMessagingService : FirebaseMessagingService() {

    @Inject lateinit var fcmTokenRepository: FcmTokenRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch { fcmTokenRepository.onTokenRefresh(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data  = message.data
        val type  = data["type"] ?: "message"
        val title = message.notification?.title ?: data["title"] ?: "RutasApp"
        val body  = message.notification?.body  ?: data["body"]

        when (type) {
            "sync_now" -> {
                // Sync silencioso — sin notificación visible
                triggerImmediateSync()
            }
            "new_route" -> {
                // Nueva ruta asignada — notificación + sync
                triggerImmediateSync()
                body?.let { showNotification(title, it, CHANNEL_ROUTES) }
            }
            "route_update" -> {
                // Cambio en ruta — notificación + sync
                triggerImmediateSync()
                body?.let { showNotification(title, it, CHANNEL_ROUTES) }
            }
            else -> {
                // Notificación informativa
                body?.let { showNotification(title, it, CHANNEL_DEFAULT) }
            }
        }
    }

    // ── Lanzar SyncWorker inmediato vía WorkManager ───────────
    private fun triggerImmediateSync() {
        WorkManager.getInstance(applicationContext)
            .enqueue(
                OneTimeWorkRequestBuilder<SyncWorker>()
                    .addTag("fcm_triggered")
                    .build()
            )
    }

    private fun showNotification(title: String, body: String, channelId: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal si no existe
        val channelName = if (channelId == CHANNEL_ROUTES) "Rutas" else "General"
        manager.createNotificationChannel(
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Notificaciones RutasApp — $channelName" }
        )

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )

        manager.notify(
            System.currentTimeMillis().toInt(),
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
        )
    }

    companion object {
        const val CHANNEL_DEFAULT = "rutasapp_default"
        const val CHANNEL_ROUTES  = "rutasapp_routes"
    }
}
