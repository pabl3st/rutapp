package com.pabl3st.rutapp.data.repository

import com.pabl3st.rutapp.data.network.AccountUserDto
import com.pabl3st.rutapp.data.network.AssignManagerRequest
import com.pabl3st.rutapp.data.network.DeactivateUserRequest
import com.pabl3st.rutapp.data.network.InviteDto
import com.pabl3st.rutapp.data.network.InviteUserRequest
import com.pabl3st.rutapp.data.network.ReactivateUserRequest
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
        val resp = api.usersList(token = session.token ?: "")
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
            token = session.token ?: "",
            body  = InviteUserRequest(email = email.trim(), role = role),
        )
        if (resp.isSuccessful) {
            val body = resp.body()
            if (body?.success == true)
                AuthResult.Success(body.code ?: body.message)
            else AuthResult.Error(body?.message ?: "Error al invitar usuario")
        } else {
            AuthResult.Error("HTTP ${resp.code()}")
        }
    }.getOrElse { AuthResult.Error(it.message ?: "Error de red") }

    suspend fun listInvites(): AuthResult<List<InviteDto>> = runCatching {
        val resp = api.inviteList(token = session.token ?: "")
        if (resp.isSuccessful && resp.body()?.success == true)
            AuthResult.Success(resp.body()!!.invites)
        else AuthResult.Error("HTTP ${resp.code()}")
    }.getOrElse { AuthResult.Error(it.message ?: "Error de red") }

    suspend fun deleteInvite(inviteId: Int): AuthResult<String> = runCatching {
        val resp = api.deleteInvite(
            token = session.token ?: "",
            body  = mapOf("invite_id" to inviteId),
        )
        if (resp.isSuccessful && resp.body()?.success == true)
            AuthResult.Success("Invitación eliminada")
        else AuthResult.Error("HTTP ${resp.code()}")
    }.getOrElse { AuthResult.Error(it.message ?: "Error de red") }

    suspend fun updateRole(targetUserId: Int, role: String): AuthResult<String> = runCatching {
        val resp = api.updateRole(
            token = session.token ?: "",
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
            token = session.token ?: "",
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

    // Roles asignables vía update_role (god/owner no son asignables por diseño del servidor)
    // Roles a los que este usuario puede asignar un supervisor
    // (solo puede gestionar usuarios de nivel inferior al propio)
    fun canManageSupervisorOf(targetRole: String): Boolean {
        val myLevel     = roleLevel(session.userRole)
        val targetLevel = roleLevel(targetRole)
        return myLevel > targetLevel
    }

    // Usuarios que pueden ser supervisores del target dado
    // El supervisor debe tener nivel > target y <= caller (salvo owner/god que ve todos)
    fun validManagersFor(users: List<AccountUserDto>, targetRole: String): List<AccountUserDto> {
        val targetLevel = roleLevel(targetRole)
        val myLevel     = roleLevel(session.userRole)
        return users.filter { u ->
            val uLevel = roleLevel(u.role)
            uLevel > targetLevel && (isOwner || isGod || uLevel <= myLevel)
        }
    }

    private fun roleLevel(role: String) =
        mapOf("viewer" to 1, "agent" to 2, "manager" to 3, "admin" to 4, "owner" to 5, "god" to 6)[role] ?: 0

    val availableRoles: List<String>
        get() = when {
            isGod          -> listOf("admin", "manager", "agent", "viewer")
            isOwner        -> listOf("admin", "manager", "agent", "viewer")
            isOwnerOrAdmin -> listOf("manager", "agent", "viewer")
            else           -> emptyList()
        }

    suspend fun assignManager(targetUserId: Int, managerId: Int?): AuthResult<String> = runCatching {
        val resp = api.assignManager(
            token = session.token ?: "",
            body  = AssignManagerRequest(targetUserId = targetUserId, managerId = managerId),
        )
        if (resp.isSuccessful && resp.body()?.success == true)
            AuthResult.Success(resp.body()!!.message)
        else AuthResult.Error(resp.body()?.message ?: "HTTP ${resp.code()}")
    }.getOrElse { AuthResult.Error(it.message ?: "Error de red") }

    suspend fun reactivateUser(targetUserId: Int): AuthResult<String> = runCatching {
        val resp = api.reactivateUser(
            token = session.token ?: "",
            body  = ReactivateUserRequest(targetUserId = targetUserId),
        )
        if (resp.isSuccessful) {
            val body = resp.body()
            if (body?.success == true) AuthResult.Success(body.message)
            else AuthResult.Error(body?.message ?: "Error al reactivar usuario")
        } else {
            AuthResult.Error("HTTP ${resp.code()}")
        }
    }.getOrElse { AuthResult.Error(it.message ?: "Error de red") }

    suspend fun godSetRole(targetUserId: Int, role: String): AuthResult<String> = runCatching {
        val resp = api.godSetRole(
            token = session.token ?: "",
            body  = com.pabl3st.rutapp.data.network.GodSetRoleRequest(userId = targetUserId, role = role),
        )
        if (resp.isSuccessful) {
            val body = resp.body()
            if (body?.success == true) AuthResult.Success("Rol actualizado")
            else AuthResult.Error(body?.message ?: "Error")
        } else AuthResult.Error("HTTP ${resp.code()}")
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


