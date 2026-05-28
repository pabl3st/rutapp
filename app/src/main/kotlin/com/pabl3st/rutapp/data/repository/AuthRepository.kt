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
    /** True si este login pertenece a una cuenta distinta a la anterior en
     *  este móvil (Room ya fue limpiado en parseAuthResponse). El AuthViewModel
     *  debe disparar syncFullDownload() en lugar de syncIncremental() para
     *  repoblar Room desde cero. */
    val wasAccountSwitch: Boolean = false,
)

@Singleton
class AuthRepository @Inject constructor(
    private val api:           RutasApiService,
    private val session:       SessionManager,
    private val userPrefsRepo: UserPrefsRepository,
    private val fcmTokenRepo:  FcmTokenRepository,
    private val database:      com.pabl3st.rutapp.data.local.RutasDatabase,
    // dagger.Lazy evita el ciclo Hilt: SyncRepository depende de varios repos
    // y AuthRepository es solo Singleton — lazy se resuelve en runtime.
    private val syncRepoLazy:  dagger.Lazy<SyncRepository>,
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
        // Subir lo pendiente antes de cerrar — pero con timeout para no
        // bloquear al usuario más de 5s si la red está mala. Si caduca,
        // los datos quedan en sync_queue para el próximo login.
        runCatching {
            kotlinx.coroutines.withTimeout(5_000) {
                syncRepoLazy.get().syncUploadOnly()
            }
        }
        runCatching { api.logout(token = token) }
        // Logout ligero: solo borrar token. accountId/role/managedAgentIds/
        // lastSync sobreviven para que el próximo login del MISMO usuario
        // tenga sus datos al instante. parseAuthResponse decide si limpiar
        // hierarchy (cambio de userId mismo accountId) o Room completo
        // (cambio de accountId).
        session.clearTokenOnly()
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
            // Actualizar token FCM en el servidor (puede haber rotado desde el último login)
            fcmTokenRepo.uploadCurrentToken()
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
    private suspend fun parseAuthResponse(
        code: Int,
        body: com.pabl3st.rutapp.data.network.AuthResponse?,
    ): AuthResult<AuthSuccess> {
        if (body == null || !body.ok || body.token == null || body.user == null || body.account == null) {
            return AuthResult.Error(body?.error ?: "Error desconocido", code)
        }
        // P5 (mayo 2026): si el login pertenece a una cuenta DIFERENTE a la
        // anterior en este móvil, limpiar Room para evitar contaminación entre
        // cuentas (datos de otra empresa visibles tras logout/login). Si es la
        // misma cuenta (logout/login para cambiar de rol), Room se mantiene
        // para que el nuevo rol vea los datos al instante sin esperar a un
        // delta_sync completo.
        val previousAccountId = session.accountId
        val previousUserId    = session.userId
        val newAccountId      = body.account.id
        val newUserId         = body.user.id
        val isAccountSwitch   = previousAccountId != 0 && previousAccountId != newAccountId
        // Cambio de USUARIO dentro de la MISMA cuenta (ej: logout owner → login admin).
        // El subárbol del nuevo usuario es diferente — no podemos reusar
        // managedAgentIds del anterior. Hay que limpiar y dejar que el primer
        // delta_sync los repueble.
        val isUserSwitchSameAccount = !isAccountSwitch &&
                                       previousUserId != 0 &&
                                       previousUserId != newUserId
        if (isAccountSwitch) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                database.clearAllTables()
            }
        } else if (isUserSwitchSameAccount) {
            // No tocar Room (los datos son visibles para varios roles de la
            // misma cuenta), pero invalidar la jerarquía cacheada y forzar
            // re-sync para descargar el subárbol correcto del nuevo usuario.
            session.clearManagedHierarchy()
        }
        // Persistir en SessionManager
        session.saveAuth(
            token           = body.token,
            userId          = body.user.id,
            userName        = body.user.username,
            userEmail       = body.user.email,
            userRole        = body.user.role,
            userDisplayName = body.user.name,
            accountId       = newAccountId,
            accountType     = body.account.type,
            accountName     = body.account.name,
        )
        val success = buildSuccess(
            body.token, body.user.id, body.user.username, body.user.email,
            body.user.role, body.user.name, body.account.id, body.account.type,
            body.account.name, wasAccountSwitch = isAccountSwitch,
        )
        return AuthResult.Success(success)
    }

    private fun buildSuccess(
        token: String, userId: Int, userName: String, userEmail: String,
        userRole: String, userDisplayName: String, accountId: Int,
        accountType: String, accountName: String,
        wasAccountSwitch: Boolean = false,
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
        wasAccountSwitch = wasAccountSwitch,
    )
}
