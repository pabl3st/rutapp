package com.pabl3st.rutapp.fcm

import android.content.Context
import android.provider.Settings
import com.google.firebase.messaging.FirebaseMessaging
import com.pabl3st.rutapp.BuildConfig
import com.pabl3st.rutapp.data.network.PushRegisterRequest
import com.pabl3st.rutapp.data.network.RutasApiService
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val session: SessionManager,
    private val api:     RutasApiService,
) {
    // Llamado cuando Firebase emite un nuevo token (rotación automática)
    suspend fun onTokenRefresh(token: String) {
        uploadToken(token)
    }

    // Obtener token actual y registrarlo — llamar tras login exitoso
    suspend fun uploadCurrentToken() {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            uploadToken(token)
        }
    }

    private suspend fun uploadToken(fcmToken: String) {
        val authToken = session.token ?: return
        val deviceId  = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"

        runCatching {
            api.pushRegister(
                token = authToken,
                body  = PushRegisterRequest(
                    fcmToken   = fcmToken,
                    deviceId   = deviceId,
                    platform   = "android",
                    appVersion = BuildConfig.VERSION_NAME,
                ),
            )
        }
        // Fallo silencioso — si no hay red se registrará en el próximo arranque
    }
}
