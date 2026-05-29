package com.pabl3st.rutapp.data.repository

import com.pabl3st.rutapp.data.network.AgentDetailResponse
import com.pabl3st.rutapp.data.network.AgentOverviewDto
import com.pabl3st.rutapp.data.network.RutasApiService
import com.pabl3st.rutapp.data.session.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio para datos de equipo — S20.
 * Roles que pueden usarlo: manager (sus agentes directos), admin/owner/god (toda la cuenta).
 * Nota: agent NO tiene acceso — roleLevel < 3 es rechazado por el servidor.
 */
@Singleton
class TeamRepository @Inject constructor(
    private val api:     RutasApiService,
    private val session: SessionManager,
) {
    /** Lista de agentes con estado de jornada, GPS y stops de hoy.
     *  @param forUserId si no es null, devuelve el equipo de ese usuario
     *                   (drill-down). El usuario debe estar en el subárbol
     *                   descendente del caller. */
    suspend fun teamOverview(forUserId: Int? = null): Result<List<AgentOverviewDto>> = runCatching {
        val token = session.token ?: error("Sin sesión activa")
        val resp  = api.teamOverview(token = token, forUserId = forUserId)
        if (!resp.isSuccessful) error("Error del servidor: ${resp.code()}")
        resp.body()?.agents ?: emptyList()
    }

    /** Detalle completo de un agente — jornada, rutas de hoy, visitas recientes, KPIs del mes. */
    suspend fun agentDetail(userId: Int): Result<AgentDetailResponse> = runCatching {
        val token = session.token ?: error("Sin sesión activa")
        val resp  = api.agentDetail(token = token, userId = userId)
        if (!resp.isSuccessful) error("Error del servidor: ${resp.code()}")
        resp.body() ?: error("Respuesta vacía")
    }
}
