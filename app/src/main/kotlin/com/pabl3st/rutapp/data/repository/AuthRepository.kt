package com.pabl3st.rutapp.data.repository

import android.content.Context
import android.os.Build
import com.pabl3st.rutapp.BuildConfig
import com.pabl3st.rutapp.data.network.LoginRequest
import com.pabl3st.rutapp.data.network.RegisterCompanyRequest
import com.pabl3st.rutapp.data.network.RegisterIndividualRequest
import com.pabl3st.rutapp.data.network.RegisterWithInviteRequest
import com.pabl3st.rutapp.data.network.RutasApiService
import com.pabl3st.rutapp.data.session.SessionManager
import com.pabl3st.rutapp.data.repository.UserPrefsRepository
import com.pabl3st.rutapp.fcm.FcmTokenRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val code: Int = 0) : AuthResult<Nothing>()
}

data class AuthSuccess(
    val token: String,
    val userId: Int,
    val userName: String,
    val userEmail: String,
    val userRole: String,
    val userDisplayName: String,
    val accountId: Int,
    val accountType: String,
    val accountName: String,
    val isCompany: Boolean,
)

@Singleton
class AuthRepository @Inject constructor(
    private val api:           RutasApiService,
    private val session:       SessionManager,
    private val userPrefsRepo: UserPrefsRepository,
    private val fcmTokenRepo:  FcmTokenRepository,
    @ApplicationContext private val context: Context,
) {
    private val deviceName: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    private val appVersion: String
        get() = BuildConfig.VERSION_NAME

    // ── Register individual ──────────────────────────────────
    suspend fun registerIndividual(
        name: String,
        username: String,
        email: String,
        password: String,
    ): AuthResult<AuthSuccess> = runCatching {
        val resp = api.registerIndividual(
            body = RegisterIndividualRequest(
                name       = name,
                username   = username,
                email      = email,
                password   = password,
                deviceId   = session.deviceId,
                deviceName = deviceName,
                appVersion = appVersion,
            )
        )
        parseAuthResponse(resp.code(), resp.body())
    }.getOrElse { AuthResult.Error("Error de conexión: ${it.message}") }

    // ── Register company ─────────────────────────────────────
    suspend fun registerCompany(
        companyName: String,
        name: String,
        username: String,
        email: String,
        password: String,
    ): AuthResult<AuthSuccess> = runCatching {
        val resp = api.registerCompany(
            body = RegisterCompanyRequest(
                companyName = companyName,
                name        = name,
                username    = username,
                email       = email,
                password    = password,
                deviceId    = session.deviceId,
                deviceName  = deviceName,
                appVersion  = appVersion,
            )
        )
        parseAuthResponse(resp.code(), resp.body())
    }.getOrElse { AuthResult.Error("Error de conexión: ${it.message}") }

    // ── Register with invite ─────────────────────────────────
    suspend fun registerWithInvite(
        inviteCode: String,
        name: String,
        username: String,
        email: String,
        password: String,
    ): AuthResult<AuthSuccess> = runCatching {
        val resp = api.registerWithInvite(
            body = RegisterWithInviteRequest(
                inviteCode = inviteCode,
                name       = name,
                username   = username,
                email      = email,
                password   = password,
                deviceId   = session.deviceId,
                deviceName = deviceName,
                appVersion = appVersion,
            )
        )
        parseAuthResponse(resp.code(), resp.body())
    }.getOrElse { AuthResult.Error("Error de conexión: ${it.message}") }

    // ── Login ─────────────────────────────────────────────────
    suspend fun login(
        credential: String,
        password: String,
    ): AuthResult<AuthSuccess> = runCatching {
        val resp = api.login(
            body = LoginRequest(
                username   = credential,
                password   = password,
                deviceId   = session.deviceId,
                deviceName = deviceName,
                appVersion = appVersion,
            )
        )
        val result = parseAuthResponse(resp.code(), resp.body())
        if (result is AuthResult.Success) fcmTokenRepo.uploadCurrentToken()
        result
    }.getOrElse { AuthResult.Error("Error de conexión: ${it.message}") }
    // ── Logout ────────────────────────────────────────────────
    suspend fun logout() {
        val token = session.token ?: return
        runCatching { api.logout(token = token) }
        session.clear()
    }

    // ── Me (verificar sesión al arrancar) ─────────────────────
    suspend fun verifySession(): AuthResult<AuthSuccess> {
        val token = session.token ?: return AuthResult.Error("Sin sesión", 401)
        return runCatching {
            val resp = api.me(token = token)
            if (!resp.isSuccessful || resp.body()?.ok != true) {
                session.clear()
                return AuthResult.Error(resp.body()?.error ?: "Sesión expirada", resp.code())
            }
            val body = resp.body()!!
            // Actualizar token si el servidor envió uno nuevo
            body.newToken?.let { session.token = it }

            val user    = body.user!!
            val account = body.account!!
            val success = buildSuccess(token, user.id, user.username, user.email,
                user.role, user.name, account.id, account.type, account.name)
            session.saveAuth(
                token           = session.token!!,
                userId          = user.id,
                userName        = user.username,
                userEmail       = user.email,
                userRole        = user.role,
                userDisplayName = user.name,
                accountId       = account.id,
                accountType     = account.type,
                accountName     = account.name,
            )
            // Restaurar vacaciones y prefs desde el servidor (recuperación tras reinstalación)
            userPrefsRepo.restoreFromServer(body.prefs)
            AuthResult.Success(success)
        }.getOrElse {
            // En modo offline usar datos cacheados
            if (session.isLoggedIn) {
                AuthResult.Success(AuthSuccess(
                    token           = session.token!!,
                    userId          = session.userId,
                    userName        = session.userName,
                    userEmail       = session.userEmail,
                    userRole        = session.userRole,
                    userDisplayName = session.userDisplayName,
                    accountId       = session.accountId,
                    accountType     = session.accountType,
                    accountName     = session.accountName,
                    isCompany       = session.isCompany,
                ))
            } else {
                AuthResult.Error("Sin conexión y sin sesión guardada")
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────
    private fun parseAuthResponse(
        code: Int,
        body: com.pabl3st.rutapp.data.network.AuthResponse?,
    ): AuthResult<AuthSuccess> {
        if (body == null || !body.ok || body.token == null || body.user == null || body.account == null) {
            return AuthResult.Error(body?.error ?: "Error desconocido", code)
        }
        val success = buildSuccess(
            body.token, body.user.id, body.user.username, body.user.email,
            body.user.role, body.user.name, body.account.id, body.account.type, body.account.name,
        )
        // Persistir en SessionManager
        session.saveAuth(
            token           = body.token,
            userId          = body.user.id,
            userName        = body.user.username,
            userEmail       = body.user.email,
            userRole        = body.user.role,
            userDisplayName = body.user.name,
            accountId       = body.account.id,
            accountType     = body.account.type,
            accountName     = body.account.name,
        )
        return AuthResult.Success(success)
    }

    private fun buildSuccess(
        token: String, userId: Int, userName: String, userEmail: String,
        userRole: String, userDisplayName: String, accountId: Int,
        accountType: String, accountName: String,
    ) = AuthSuccess(
        token           = token,
        userId          = userId,
        userName        = userName,
        userEmail       = userEmail,
        userRole        = userRole,
        userDisplayName = userDisplayName,
        accountId       = accountId,
        accountType     = accountType,
        accountName     = accountName,
        isCompany       = accountType == "company",
    )
}
