package com.pabl3st.rutapp.data.repository

import com.pabl3st.rutapp.data.network.AccountUserDto
import com.pabl3st.rutapp.data.network.DeactivateUserRequest
import com.pabl3st.rutapp.data.network.InviteUserRequest
import com.pabl3st.rutapp.data.network.RutasApiService
import com.pabl3st.rutapp.data.network.UpdateRoleRequest
import com.pabl3st.rutapp.data.session.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

/** Nivel numérico de autoridad — cuanto mayor, más permisos */
fun roleLevelOf(role: String): Int = when (role) {
    "viewer"  -> 1
    "agent"   -> 2
    "manager" -> 3
    "admin"   -> 4
    "owner"   -> 5
    "god"     -> 6
    else      -> 0
}

@Singleton
class AdminRepository @Inject constructor(
    private val api:     RutasApiService,
    private val session: SessionManager,
) {
    val isOwnerOrAdmin: Boolean
        get() = session.userRole in setOf("owner", "admin")

    val isGod: Boolean
        get() = session.userRole == "god"

    val canManageUsers: Boolean
        get() = session.userRole in setOf("god", "owner", "admin")

    val isOwner: Boolean
        get() = session.userRole == "owner"

    suspend fun listUsers(): AuthResult<List<AccountUserDto>> = runCatching {
        val resp = api.usersList(token = "Bearer ${session.token}")
        if (resp.isSuccessful) {
            val body = resp.body()
            if (body?.success == true) AuthResult.Success(body.users)
            else AuthResult.Error(body?.message ?: "Error al cargar usuarios")
        } else {
            AuthResult.Error("HTTP ${resp.code()}")
        }
    }.getOrElse { AuthResult.Error(it.message ?: "Error de red") }

    suspend fun inviteUser(email: String, role: String): AuthResult<String> = runCatching {
        val resp = api.inviteUser(
            token = "Bearer ${session.token}",
            body  = InviteUserRequest(email = email.trim(), role = role),
        )
        if (resp.isSuccessful) {
            val body = resp.body()
            if (body?.success == true) AuthResult.Success(body.message)
            else AuthResult.Error(body?.message ?: "Error al invitar usuario")
        } else {
            AuthResult.Error("HTTP ${resp.code()}")
        }
    }.getOrElse { AuthResult.Error(it.message ?: "Error de red") }

    suspend fun updateRole(targetUserId: Int, role: String): AuthResult<String> = runCatching {
        val resp = api.updateRole(
            token = "Bearer ${session.token}",
            body  = UpdateRoleRequest(targetUserId = targetUserId, role = role),
        )
        if (resp.isSuccessful) {
            val body = resp.body()
            if (body?.success == true) AuthResult.Success(body.message)
            else AuthResult.Error(body?.message ?: "Error al actualizar rol")
        } else {
            AuthResult.Error("HTTP ${resp.code()}")
        }
    }.getOrElse { AuthResult.Error(it.message ?: "Error de red") }

    suspend fun deactivateUser(targetUserId: Int): AuthResult<String> = runCatching {
        val resp = api.deactivateUser(
            token = "Bearer ${session.token}",
            body  = DeactivateUserRequest(targetUserId = targetUserId),
        )
        if (resp.isSuccessful) {
            val body = resp.body()
            if (body?.success == true) AuthResult.Success(body.message)
            else AuthResult.Error(body?.message ?: "Error al desactivar usuario")
        } else {
            AuthResult.Error("HTTP ${resp.code()}")
        }
    }.getOrElse { AuthResult.Error(it.message ?: "Error de red") }

    val availableRoles: List<String>
        get() = when {
            isGod          -> listOf("god", "owner", "admin", "manager", "agent", "viewer")
            isOwner        -> listOf("admin", "manager", "agent", "viewer")
            isOwnerOrAdmin -> listOf("manager", "agent", "viewer")
            else           -> emptyList()
        }

    suspend fun godSetRole(targetUserId: Int, role: String): AuthResult<String> = runCatching {
        val resp = api.godSetRole(
            token = "Bearer \${session.token}",
            body  = com.pabl3st.rutapp.data.network.GodSetRoleRequest(userId = targetUserId, role = role),
        )
        if (resp.isSuccessful) {
            val body = resp.body()
            if (body?.success == true) AuthResult.Success("Rol actualizado")
            else AuthResult.Error(body?.message ?: "Error")
        } else AuthResult.Error("HTTP \${resp.code()}")
    }.getOrElse { AuthResult.Error(it.message ?: "Error de red") }

    fun roleLabel(role: String) = when (role) {
        "god"     -> "Superadmin"
        "owner"   -> "Propietario"
        "admin"   -> "Administrador"
        "manager" -> "Manager"
        "agent"   -> "Agente"
        "viewer"  -> "Solo lectura"
        else      -> role
    }
}

