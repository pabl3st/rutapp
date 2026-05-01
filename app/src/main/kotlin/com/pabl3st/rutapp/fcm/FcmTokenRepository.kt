package com.pabl3st.rutapp.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.pabl3st.rutapp.data.network.RutasApiService
import com.pabl3st.rutapp.data.session.SessionManager
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenRepository @Inject constructor(
    private val session: SessionManager,
    private val api:     RutasApiService,
) {
    // Llamado cuando Firebase emite un nuevo token
    suspend fun onTokenRefresh(token: String) {
        uploadToken(token)
    }

    // Obtener token actual y subirlo — llamar tras login
    suspend fun uploadCurrentToken() {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            uploadToken(token)
        }
    }

    private suspend fun uploadToken(fcmToken: String) {
        val authToken = session.token ?: return
        runCatching {
            api.tokenRefresh(token = authToken, body = com.pabl3st.rutapp.data.network.TokenRefreshRequest(fcmToken = fcmToken))
        }
    }
}
