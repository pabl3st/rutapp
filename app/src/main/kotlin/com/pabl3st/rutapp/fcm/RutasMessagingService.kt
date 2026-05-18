package com.pabl3st.rutapp.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pabl3st.rutapp.MainActivity
import com.pabl3st.rutapp.R
import androidx.work.WorkManager
import com.pabl3st.rutapp.sync.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RutasMessagingService : FirebaseMessagingService() {

    @Inject lateinit var fcmTokenRepository: FcmTokenRepository
    @Inject lateinit var workManager: WorkManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch { fcmTokenRepository.onTokenRefresh(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data

        // Sync inmediato para type=sync y cualquier notificación de ruta
        val shouldSync = data["type"] in listOf("sync", "route_assigned", "route_reassigned")
        if (shouldSync) {
            workManager.enqueueUniqueWork(
                SyncWorker.WORK_NAME_ONDEMAND,
                androidx.work.ExistingWorkPolicy.REPLACE,
                SyncWorker.onDemandRequest(),
            )
        }

        // Notificaciones con navegación a pantalla concreta
        val title = message.notification?.title ?: data["title"] ?: return
        val body  = message.notification?.body  ?: data["body"]  ?: return
        val type  = data["type"] ?: ""

        val deepLinkIntent = buildDeepLinkIntent(type, data)
        showNotification(title, body, deepLinkIntent)
    }

    /**
     * Construye un Intent que lleva al usuario a la pantalla correcta
     * según el tipo de notificación y los datos del payload.
     *
     * Tipos soportados:
     *  - route_assigned  → RouteDetail/{routeUid}
     *  - route_reassigned → RouteDetail/{routeUid}
     *  - sync            → Home (sin navegación específica)
     *  - (otros)         → Home
     */
    private fun buildDeepLinkIntent(type: String, data: Map<String, String>): Intent {
        val base = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return when (type) {
            "route_assigned", "route_reassigned" -> {
                val routeUid = data["route_uid"]
                if (!routeUid.isNullOrBlank()) {
                    base.apply {
                        putExtra(EXTRA_DEEP_LINK_TYPE, type)
                        putExtra(EXTRA_DEEP_LINK_ROUTE_UID, routeUid)
                    }
                } else base
            }
            else -> base
        }
    }

    private fun showNotification(title: String, body: String, intent: Intent) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "RutasApp",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Notificaciones de RutasApp" }
        manager.createNotificationChannel(channel)

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),  // requestCode único para no sobrescribir
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID                = "rutasapp_default"
        const val EXTRA_DEEP_LINK_TYPE      = "deep_link_type"
        const val EXTRA_DEEP_LINK_ROUTE_UID = "deep_link_route_uid"
    }
}
