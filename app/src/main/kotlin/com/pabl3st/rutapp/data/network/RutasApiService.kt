package com.pabl3st.rutapp.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

const val API_PATH = "rutasproapk/api.php"

// ── Request bodies ───────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class RegisterIndividualRequest(
    val name: String,
    val username: String,
    val email: String,
    val password: String,
    @Json(name = "device_id")   val deviceId: String,
    @Json(name = "device_name") val deviceName: String?,
    @Json(name = "app_version") val appVersion: String?,
    @Json(name = "fcm_token")   val fcmToken: String? = null,
)

@JsonClass(generateAdapter = true)
data class RegisterCompanyRequest(
    @Json(name = "company_name") val companyName: String,
    val name: String,
    val username: String,
    val email: String,
    val password: String,
    @Json(name = "device_id")   val deviceId: String,
    @Json(name = "device_name") val deviceName: String?,
    @Json(name = "app_version") val appVersion: String?,
    @Json(name = "fcm_token")   val fcmToken: String? = null,
)

@JsonClass(generateAdapter = true)
data class RegisterWithInviteRequest(
    @Json(name = "invite_code") val inviteCode: String,
    val name: String,
    val username: String,
    val email: String,
    val password: String,
    @Json(name = "device_id")   val deviceId: String,
    @Json(name = "device_name") val deviceName: String?,
    @Json(name = "app_version") val appVersion: String?,
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val username: String,
    val password: String,
    @Json(name = "device_id")   val deviceId: String,
    @Json(name = "device_name") val deviceName: String?,
    @Json(name = "app_version") val appVersion: String?,
    @Json(name = "fcm_token")   val fcmToken: String? = null,
)

@JsonClass(generateAdapter = true)
data class TokenRefreshRequest(
    @Json(name = "fcm_token") val fcmToken: String? = null,
)

@JsonClass(generateAdapter = true)
data class LogoutRequest(
    @Json(name = "clear_fcm") val clearFcm: Boolean = true,
)

// ── Response models ──────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ApiError(val ok: Boolean, val error: String?)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val ok: Boolean,
    val token: String?,
    @Json(name = "expires_in_days") val expiresInDays: Int?,
    val user: UserDto?,
    val account: AccountDto?,
    val prefs: Map<String, Any>?,
    val error: String?,
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: Int,
    val username: String,
    val email: String,
    val name: String,
    val role: String,
    @Json(name = "avatar_url")  val avatarUrl: String?,
    @Json(name = "account_id")  val accountId: Int,
    @Json(name = "created_at")  val createdAt: String?,
)

@JsonClass(generateAdapter = true)
data class AccountDto(
    val id: Int,
    val type: String,
    val name: String,
    val slug: String,
    val plan: String,
    @Json(name = "plus_config")  val plusConfig: Map<String, Any>?,
    @Json(name = "form_config")  val formConfig: List<Map<String, Any>>?,
    @Json(name = "ai_settings")  val aiSettings: Map<String, Any>?,
)

@JsonClass(generateAdapter = true)
data class MeResponse(
    val ok: Boolean,
    val user: UserDto?,
    val account: AccountDto?,
    val prefs: Map<String, Any>?,
    @Json(name = "new_token")    val newToken: String?,
    @Json(name = "server_time")  val serverTime: String?,
    val error: String?,
)

@JsonClass(generateAdapter = true)
data class HealthResponse(
    val ok: Boolean,
    val db: Boolean,
    val version: String?,
    @Json(name = "server_time") val serverTime: String?,
)


// ── S02 DTOs ─────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class RouteDto(
    val id: Int?,
    val uid: String,
    @Json(name = "user_id")         val userId: Int = 0,
    val name: String,
    @Json(name = "date_assigned")    val dateAssigned: String,
    @Json(name = "scheduled_dates")  val scheduledDates: String? = null,
    val status: String,
    val notes: String?,
    @Json(name = "stop_count")  val stopCount: Int = 0,
    @Json(name = "done_count")  val doneCount: Int = 0,
    @Json(name = "created_at")  val createdAt: String,
    @Json(name = "updated_at")  val updatedAt: String,
    @Json(name = "deleted_at")  val deletedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class BusinessProfileSyncDto(
    val sector: String = "custom",
    val name:   String = "Mi negocio",
)

@JsonClass(generateAdapter = true)
data class KpiDefinitionSyncDto(
    val id:          String,
    @Json(name = "account_id") val accountId: Int = 0,
    val sector:      String = "common",
    val label:       String,
    val type:        String = "text",
    val unit:        String? = null,
    val options:     String? = null,
    @Json(name = "is_system")   val isSystem:   Int = 0,
    val visible:     Int = 1,
    val required:    Int = 0,
    @Json(name = "order_index") val orderIndex: Int = 0,
    val section:     String = "general",
)

@JsonClass(generateAdapter = true)
data class StopDto(
    val id: Int?,
    val uid: String,
    @Json(name = "route_uid")     val routeUid: String?,
    val name: String,
    @Json(name = "external_id")   val externalId: String?   = null,
    val address: String?,
    val street: String?      = null,
    @Json(name = "postal_code") val postalCode: String? = null,
    val city: String?        = null,
    val lat: Double?,
    val lng: Double?,
    @Json(name = "order_index")   val orderIndex: Int        = 0,
    val status: String,
    val notes: String?,
    @Json(name = "contact_name")  val contactName: String?  = null,
    @Json(name = "contact_phone") val contactPhone: String? = null,
    @Json(name = "visited_at")    val visitedAt: String?,
    @Json(name = "visit_result")  val visitResult: String?  = null,
    @Json(name = "next_action")   val nextAction: String?   = null,
    @Json(name = "pdv_open")         val pdvOpen: Boolean        = true,
    @Json(name = "pdv_inactive")     val pdvInactive: Boolean    = false,
    @Json(name = "visit_frequency")  val visitFrequency: String? = null,
    val priority: Int?               = null,
    val segment: String?             = null,
    @Json(name = "account_status")   val accountStatus: String?  = null,
    @Json(name = "created_at")       val createdAt: String,
    @Json(name = "updated_at")    val updatedAt: String,
    @Json(name = "deleted_at")    val deletedAt: String?    = null,
    @Json(name = "date_assigned") val dateAssigned: String?  = null,
    @Json(name = "check_in_ts")   val checkInTs:  Long?      = null,
    @Json(name = "check_out_ts")  val checkOutTs: Long?      = null,
    @Json(name = "gps_lat_visit") val gpsLatVisit: Double?   = null,
    @Json(name = "gps_lng_visit") val gpsLngVisit: Double?   = null,
)

@JsonClass(generateAdapter = true)
data class RoutesListResponse(
    val ok: Boolean,
    val routes: List<RouteDto>?,
    @Json(name = "server_time") val serverTime: String?,
    val error: String?,
)

@JsonClass(generateAdapter = true)
data class DeltaSyncResponse(
    val ok: Boolean,
    val routes: List<RouteDto>?,
    val stops: List<StopDto>?,
    @Json(name = "day_sessions")     val daySessions:     List<DaySessionDto>?      = null,
    @Json(name = "kpi_values")       val kpiValues:       List<KpiValueDto>?         = null,
    @Json(name = "business_profile") val businessProfile: BusinessProfileSyncDto?    = null,
    @Json(name = "kpi_definitions")  val kpiDefinitions:   List<KpiDefinitionSyncDto>? = null,
    @Json(name = "managed_agent_ids") val managedAgentIds: List<Int>?                  = null,
    @Json(name = "server_time")       val serverTime:      String?,
    val error: String?,
)

@JsonClass(generateAdapter = true)
data class SyncOperation(
    val entity: String,
    val uid: String,
    val operation: String,
    val data: Map<String, Any?>,
)

@JsonClass(generateAdapter = true)
data class BatchSyncRequest(
    val operations: List<SyncOperation>,
)

@JsonClass(generateAdapter = true)
data class BatchSyncResult(
    val uid: String,
    val entity: String,
    @Json(name = "server_id") val serverId: Int? = null,
    val deleted: Boolean = false,
    val error: String? = null,
)

@JsonClass(generateAdapter = true)
data class BatchSyncResponse(
    val ok: Boolean,
    val synced: List<BatchSyncResult>?,
    val errors: List<BatchSyncResult>?,
    @Json(name = "server_time") val serverTime: String?,
    val error: String?,
)

// ── S08/S09 DTOs — jornada y KPI values ─────────────────────

@JsonClass(generateAdapter = true)
data class DaySessionDto(
    @Json(name = "route_uid")    val routeUid:   String,
    @Json(name = "date_str")     val dateStr:    String,
    val state:                                   String,
    @Json(name = "started_at")   val startedAt:  Long?,
    @Json(name = "elapsed_ms")   val elapsedMs:  Long,
    @Json(name = "distance_km")  val distanceKm: Double,
    @Json(name = "last_lat")     val lastLat:    Double?,
    @Json(name = "last_lng")     val lastLng:    Double?,
    @Json(name = "updated_at")   val updatedAt:  Long,
)

@JsonClass(generateAdapter = true)
data class KpiValueDto(
    @Json(name = "stop_uid")    val stopUid:   String,
    @Json(name = "kpi_id")      val kpiId:     String,
    @Json(name = "value_text")  val valueText: String?,
    @Json(name = "updated_at")  val updatedAt: String?,
)

// ── Historial de reasignación de rutas ──────────────────────

@JsonClass(generateAdapter = true)
data class RouteAssignmentDto(
    val id:                                 Long    = 0,
    @Json(name = "route_uid")        val routeUid:       String  = "",
    @Json(name = "route_name")       val routeName:      String  = "",
    @Json(name = "from_user_id")     val fromUserId:     Int?    = null,
    @Json(name = "from_user_name")   val fromUserName:   String? = null,
    @Json(name = "to_user_id")       val toUserId:       Int     = 0,
    @Json(name = "to_user_name")     val toUserName:     String  = "",
    @Json(name = "assigned_by_id")   val assignedById:   Int     = 0,
    @Json(name = "assigned_by_name") val assignedByName: String  = "",
    val reason:                              String? = null,
    @Json(name = "created_at")       val createdAt:      String  = "",
)

@JsonClass(generateAdapter = true)
data class RouteHistoryResponse(
    val ok:      Boolean,
    val history: List<RouteAssignmentDto> = emptyList(),
    val error:   String?                  = null,
)

@JsonClass(generateAdapter = true)
data class BulkAssignResponse(
    val ok:         Boolean,
    val reassigned: Int          = 0,
    val skipped:    List<String> = emptyList(),
    val error:      String?      = null,
)


// ── S14 Admin DTOs ───────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class AccountUserDto(
    @Json(name = "user_id")      val userId:      Int,
    @Json(name = "username")     val username:    String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "email")        val email:       String,
    @Json(name = "role")         val role:        String,
    @Json(name = "is_active")    val isActive:    Boolean = true,
    @Json(name = "manager_id")   val managerId:   Int?    = null,
    @Json(name = "manager_name") val managerName: String? = null,
    @Json(name = "created_at")   val createdAt:   String  = "",
)

@JsonClass(generateAdapter = true)
data class AssignManagerRequest(
    @Json(name = "target_user_id") val targetUserId: Int,
    @Json(name = "manager_id")     val managerId:    Int?,   // null = quitar supervisor
)

@JsonClass(generateAdapter = true)
data class UsersListResponse(
    val success: Boolean,
    val users:   List<AccountUserDto> = emptyList(),
    val message: String = "",
)

@JsonClass(generateAdapter = true)
data class InviteUserRequest(
    val email: String,
    val role:  String = "agent",
)

@JsonClass(generateAdapter = true)
data class UpdateRoleRequest(
    @Json(name = "target_user_id") val targetUserId: Int,
    val role: String,
)

@JsonClass(generateAdapter = true)
data class ReactivateUserRequest(
    @Json(name = "target_user_id") val targetUserId: Int,
)

@JsonClass(generateAdapter = true)
data class DeactivateUserRequest(
    @Json(name = "target_user_id") val targetUserId: Int,
)

@JsonClass(generateAdapter = true)
data class AdminActionResponse(
    val success: Boolean,
    val message: String = "",
    val code:    String? = null,   // código de invitación (solo en invite_user)
)

@JsonClass(generateAdapter = true)
data class InviteListResponse(
    val success: Boolean                    = false,
    val invites: List<InviteDto>            = emptyList(),
)

@JsonClass(generateAdapter = true)
data class InviteDto(
    val id:              Int    = 0,
    val code:            String = "",
    @Json(name = "role_to_assign") val roleToAssign: String = "",
    @Json(name = "uses_left")      val usesLeft:     Int    = 0,
    @Json(name = "expires_at")     val expiresAt:    String = "",
    @Json(name = "created_at")     val createdAt:    String = "",
)

@JsonClass(generateAdapter = true)
data class BaseResponse(
    val ok: Boolean,
    val error: String? = null,
)

// ── stats_month DTOs ─────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class StatsMonthVisits(
    @Json(name = "total_stops")    val totalStops:    Int = 0,
    @Json(name = "done_stops")     val doneStops:     Int = 0,
    @Json(name = "skipped_stops")  val skippedStops:  Int = 0,
    @Json(name = "pending_stops")  val pendingStops:  Int = 0,
    @Json(name = "contacted")      val contacted:     Int = 0,
    @Json(name = "not_home")       val notHome:       Int = 0,
    @Json(name = "return_visit")   val returnVisit:   Int = 0,
    @Json(name = "rejected")       val rejected:      Int = 0,
    @Json(name = "active_agents")  val activeAgents:  Int = 0,
    @Json(name = "total_routes")   val totalRoutes:   Int = 0,
    @Json(name = "done_routes")    val doneRoutes:    Int = 0,
)

@JsonClass(generateAdapter = true)
data class StatsMonthKpi(
    @Json(name = "kpi_id")        val kpiId:       String  = "",
    @Json(name = "label")         val label:       String  = "",
    @Json(name = "type")          val type:        String  = "",
    @Json(name = "unit")          val unit:        String? = null,
    @Json(name = "section")       val section:     String  = "general",
    @Json(name = "count_entries") val countEntries: Int    = 0,
    @Json(name = "total_value")   val totalValue:  Double  = 0.0,
    @Json(name = "true_count")    val trueCount:   Int     = 0,
)

@JsonClass(generateAdapter = true)
data class StatsMonthAgent(
    @Json(name = "user_id")    val userId:     Int    = 0,
    @Json(name = "name")       val name:       String = "",
    @Json(name = "username")   val username:   String = "",
    @Json(name = "total_stops") val totalStops: Int   = 0,
    @Json(name = "done_stops")  val doneStops:  Int   = 0,
    @Json(name = "contacted")   val contacted:  Int   = 0,
)

@JsonClass(generateAdapter = true)
data class StatsMonthResponse(
    val ok:              Boolean                = false,
    val month:           String                 = "",
    val visits:          StatsMonthVisits?      = null,
    @Json(name = "kpi_aggregates") val kpiAggregates: List<StatsMonthKpi>   = emptyList(),
    val agents:          List<StatsMonthAgent>  = emptyList(),
    val error:           String?                = null,
)

// ── account_config_save DTOs ─────────────────────────────────
@JsonClass(generateAdapter = true)
data class AccountConfigSaveRequest(
    @Json(name = "name")        val name:       String? = null,
    @Json(name = "plus_config") val plusConfig:  Any?   = null,
    @Json(name = "form_config") val formConfig:  Any?   = null,
    @Json(name = "ai_settings") val aiSettings:  Any?   = null,
)

@JsonClass(generateAdapter = true)
data class AccountConfigSaveResponse(
    val ok:      Boolean = false,
    val account: AccountData? = null,
    val error:   String? = null,
)

@JsonClass(generateAdapter = true)
data class AccountData(
    val id:    Int    = 0,
    val name:  String = "",
    val type:  String = "",
    val slug:  String = "",
    val plan:  String = "",
)

@JsonClass(generateAdapter = true)
data class PushRegisterRequest(
    @Json(name = "fcm_token")   val fcmToken:   String,
    @Json(name = "device_id")   val deviceId:   String,
    @Json(name = "device_name") val deviceName: String? = null,
    @Json(name = "platform")    val platform:   String  = "android",
    @Json(name = "app_version") val appVersion: String? = null,
)

@JsonClass(generateAdapter = true)
data class FileUploadResponse(
    val ok: Boolean,
    val url: String?   = null,   // URL pública de la foto subida
    val path: String?  = null,   // path relativo en el servidor
    val error: String? = null,
)

// ── Retrofit interface ───────────────────────────────────────


// ── God Dashboard DTOs ────────────────────────────────────────
data class GodStatsResponse(
    @Json(name = "ok")             val success:       Boolean,
    @Json(name = "message")        val message:       String  = "",
    @Json(name = "total_accounts") val totalAccounts: Int     = 0,
    @Json(name = "total_users")    val totalUsers:    Int     = 0,
    @Json(name = "total_routes")   val totalRoutes:   Int     = 0,
    @Json(name = "total_stops")    val totalStops:    Int     = 0,
    @Json(name = "total_reports")  val totalReports:  Int     = 0,
    @Json(name = "top_accounts")   val topAccounts:   List<GodAccountDto>  = emptyList(),
    @Json(name = "recent_users")   val recentUsers:   List<GodUserDto>     = emptyList(),
)

data class GodAccountDto(
    @Json(name = "id")            val id:           Int,
    @Json(name = "name")          val name:         String,
    @Json(name = "type")          val type:         String,
    @Json(name = "user_count")    val userCount:    Int     = 0,
    @Json(name = "route_count")   val routeCount:   Int     = 0,
    @Json(name = "last_activity") val lastActivity: String? = null,
    @Json(name = "plan")          val plan:         String  = "free",
)

data class GodUserDto(
    @Json(name = "id")           val id:          Int,
    @Json(name = "username")     val username:    String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "email")        val email:       String,
    @Json(name = "role")         val role:        String,
    @Json(name = "is_active")    val isActive:    Boolean = true,
    @Json(name = "created_at")   val createdAt:   String,
    @Json(name = "account_id")   val accountId:   Int     = 0,
    @Json(name = "account_name")  val accountName:  String  = "",
    @Json(name = "last_login_at") val lastLoginAt:  String? = null,
    @Json(name = "avatar_url")    val avatarUrl:    String? = null,
)

data class GodUsersResponse(
    @Json(name = "ok")      val success: Boolean,
    @Json(name = "users")   val users:   List<GodUserDto> = emptyList(),
)

data class GodSetRoleRequest(
    @Json(name = "user_id") val userId: Int,
    @Json(name = "role")    val role:   String,
)

// ─── S20 DTOs ──────────────────────────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class AgentOverviewDto(
    @Json(name = "user_id")       val userId:       Int,
    val name:                                        String,
    val username:                                    String,
    val role:                                        String,
    @Json(name = "avatar_url")    val avatarUrl:    String?,
    @Json(name = "jornada_state") val jornadaState: String?,  // running|paused|done|null
    @Json(name = "elapsed_ms")    val elapsedMs:    Long     = 0L,
    @Json(name = "distance_km")   val distanceKm:   Double   = 0.0,
    @Json(name = "last_lat")      val lastLat:      Double?  = null,
    @Json(name = "last_lng")      val lastLng:      Double?  = null,
    @Json(name = "last_gps_at")   val lastGpsAt:    String?  = null,
    @Json(name = "stops_total")   val stopsTotal:   Int      = 0,
    @Json(name = "stops_done")    val stopsDone:    Int      = 0,
    @Json(name = "stops_skipped") val stopsSkipped: Int      = 0,
    @Json(name = "stops_contacted") val stopsContacted: Int  = 0,
    @Json(name = "month_total")   val monthTotal:   Int      = 0,
    @Json(name = "month_done")    val monthDone:    Int      = 0,
    @Json(name = "month_contacted") val monthContacted: Int  = 0,
) {
    val progressToday: Float get() = if (stopsTotal > 0) stopsDone.toFloat() / stopsTotal else 0f
    val isActive: Boolean     get() = jornadaState == "running"
    val isPaused: Boolean     get() = jornadaState == "paused"
    val isDone:   Boolean     get() = jornadaState == "done"
}

@JsonClass(generateAdapter = true)
data class TeamOverviewResponse(
    val ok:     Boolean,
    val agents: List<AgentOverviewDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class AgentTodayRouteDto(
    val uid:            String,
    val name:           String,
    val status:         String,
    @Json(name = "date_assigned")  val dateAssigned:  String?,
    @Json(name = "total_stops")    val totalStops:    Int = 0,
    @Json(name = "done_stops")     val doneStops:     Int = 0,
    @Json(name = "skipped_stops")  val skippedStops:  Int = 0,
    @Json(name = "pending_stops")  val pendingStops:  Int = 0,
)

@JsonClass(generateAdapter = true)
data class AgentRecentVisitDto(
    val uid:          String,
    val name:         String,
    @Json(name = "visit_result")  val visitResult:  String?,
    @Json(name = "visited_at")    val visitedAt:    String?,
    @Json(name = "next_action")   val nextAction:   String?,
    @Json(name = "gps_lat_visit") val gpsLat:       Double?,
    @Json(name = "gps_lng_visit") val gpsLng:       Double?,
    @Json(name = "route_name")    val routeName:    String?,
)

@JsonClass(generateAdapter = true)
data class AgentMonthKpisDto(
    val total:     Int = 0,
    val done:      Int = 0,
    val contacted: Int = 0,
    @Json(name = "not_home") val notHome: Int = 0,
    val rejected:  Int = 0,
) {
    val completionRate: Float get() = if (total > 0) done.toFloat() / total else 0f
    val contactRate:    Float get() = if (done  > 0) contacted.toFloat() / done else 0f
}

@JsonClass(generateAdapter = true)
data class AgentDetailResponse(
    val ok:             Boolean,
    val agent:          UserDto?,
    val jornada:        Map<String, Any?>?     = null,
    @Json(name = "today_routes")   val todayRoutes:   List<AgentTodayRouteDto> = emptyList(),
    @Json(name = "recent_visits")  val recentVisits:  List<AgentRecentVisitDto> = emptyList(),
    @Json(name = "month_kpis")     val monthKpis:     AgentMonthKpisDto?   = null,
)

interface RutasApiService {

    @POST(API_PATH)
    suspend fun registerIndividual(
        @Query("action") action: String = "register_individual",
        @Body body: RegisterIndividualRequest,
    ): Response<AuthResponse>

    @POST(API_PATH)
    suspend fun registerCompany(
        @Query("action") action: String = "register_company",
        @Body body: RegisterCompanyRequest,
    ): Response<AuthResponse>

    @POST(API_PATH)
    suspend fun registerWithInvite(
        @Query("action") action: String = "register_with_invite",
        @Body body: RegisterWithInviteRequest,
    ): Response<AuthResponse>

    @POST(API_PATH)
    suspend fun login(
        @Query("action") action: String = "login",
        @Body body: LoginRequest,
    ): Response<AuthResponse>

    @POST(API_PATH)
    suspend fun logout(
        @Query("action") action: String = "logout",
        @Header("X-Auth-Token") token: String,
        @Body body: LogoutRequest = LogoutRequest(),
    ): Response<ApiError>

    @GET(API_PATH)
    suspend fun me(
        @Query("action") action: String = "me",
        @Header("X-Auth-Token") token: String,
    ): Response<MeResponse>

    @POST(API_PATH)
    suspend fun tokenRefresh(
        @Query("action") action: String = "token_refresh",
        @Header("X-Auth-Token") token: String,
        @Body body: TokenRefreshRequest = TokenRefreshRequest(),
    ): Response<MeResponse>

    @GET(API_PATH)
    suspend fun health(
        @Query("action") action: String = "health",
    ): Response<HealthResponse>

    @GET(API_PATH)
    suspend fun routesList(
        @Query("action") action: String = "routes_list",
        @Header("X-Auth-Token") token: String,
        @Query("date")  date: String?  = null,
        @Query("since") since: String? = null,
    ): Response<RoutesListResponse>

    @GET(API_PATH)
    suspend fun deltaSync(
        @Query("action") action: String = "delta_sync",
        @Header("X-Auth-Token") token: String,
        @Query("since") since: String,
    ): Response<DeltaSyncResponse>

    @POST(API_PATH)
    suspend fun batchSync(
        @Query("action") action: String = "batch_sync",
        @Header("X-Auth-Token") token: String,
        @Body body: BatchSyncRequest,
    ): Response<BatchSyncResponse>

    @POST(API_PATH)
    suspend fun assignRoute(
        @Query("action")        action: String = "assign_route",
        @Header("X-Auth-Token") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
    ): retrofit2.Response<BaseResponse>

    @GET(API_PATH)
    suspend fun routeHistory(
        @Query("action")        action: String = "route_history",
        @Query("route_uid")     routeUid: String,
        @Header("X-Auth-Token") token: String,
    ): retrofit2.Response<RouteHistoryResponse>

    @POST(API_PATH)
    suspend fun assignRoutesBulk(
        @Query("action")        action: String = "assign_routes_bulk",
        @Header("X-Auth-Token") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
    ): retrofit2.Response<BulkAssignResponse>

    @POST(API_PATH)
    suspend fun kpiDefSave(
        @Query("action")        action: String = "kpi_def_save",
        @Header("X-Auth-Token") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
    ): retrofit2.Response<BaseResponse>

    @GET(API_PATH)
    suspend fun teamOverview(
        @Query("action")        action: String = "team_overview",
        @Header("X-Auth-Token") token: String,
    ): retrofit2.Response<TeamOverviewResponse>

    @GET(API_PATH)
    suspend fun agentDetail(
        @Query("action")        action: String = "agent_detail",
        @Header("X-Auth-Token") token: String,
        @Query("user_id")       userId: Int,
    ): retrofit2.Response<AgentDetailResponse>

    @POST(API_PATH)
    suspend fun clearRoutes(
        @Query("action")        action: String = "clear_routes",
        @Header("X-Auth-Token") token: String,
    ): retrofit2.Response<BaseResponse>

    // ── S14 Admin endpoints ───────────────────────────────────
    @POST(API_PATH)
    suspend fun updateUserPrefs(
        @Query("action")         action: String = "update_user_prefs",
        @Header("X-Auth-Token")  token: String,
        @Body                    body: Map<String, @JvmSuppressWildcards Any>,
    ): retrofit2.Response<BaseResponse>

    @GET(API_PATH)
    suspend fun usersList(
        @Query("action") action: String = "users_list",
        @Header("X-Auth-Token") token: String,
    ): Response<UsersListResponse>

    @POST(API_PATH)
    suspend fun inviteUser(
        @Query("action") action: String = "invite_user",
        @Header("X-Auth-Token") token: String,
        @Body body: InviteUserRequest,
    ): Response<AdminActionResponse>

    @POST(API_PATH)
    suspend fun updateRole(
        @Query("action") action: String = "update_role",
        @Header("X-Auth-Token") token: String,
        @Body body: UpdateRoleRequest,
    ): Response<AdminActionResponse>

    @POST(API_PATH)
    suspend fun deactivateUser(
        @Query("action") action: String = "deactivate_user",
        @Header("X-Auth-Token") token: String,
        @Body body: DeactivateUserRequest,
    ): Response<AdminActionResponse>

    @POST(API_PATH)
    suspend fun reactivateUser(
        @Query("action") action: String = "reactivate_user",
        @Header("X-Auth-Token") token: String,
        @Body body: ReactivateUserRequest,
    ): Response<AdminActionResponse>

    @GET(API_PATH)
    suspend fun inviteList(
        @Query("action") action: String = "invite_list",
        @Header("X-Auth-Token") token: String,
    ): Response<InviteListResponse>

    @POST(API_PATH)
    suspend fun deleteInvite(
        @Query("action")        action: String = "invite_delete",
        @Header("X-Auth-Token") token:  String,
        @Body                   body:   Map<String, Int>,
    ): Response<AdminActionResponse>


    // ── stats_month — agregados del mes para manager/owner ───
    @GET(API_PATH)
    suspend fun statsMonth(
        @Query("action")          action:       String = "stats_month",
        @Header("X-Auth-Token")   token:        String,
        @Query("month")           month:        String,
        @Query("target_user_id")  targetUserId: Int?   = null,
    ): Response<StatsMonthResponse>

    // ── account_config_save — actualiza nombre/config de cuenta ─
    @POST(API_PATH)
    suspend fun accountConfigSave(
        @Query("action")        action: String = "account_config_save",
        @Header("X-Auth-Token") token:  String,
        @Body                   body:   AccountConfigSaveRequest,
    ): Response<AccountConfigSaveResponse>

    // ── assign_manager — asigna/quita supervisor a un usuario ──
    @POST(API_PATH)
    suspend fun assignManager(
        @Query("action")        action: String = "assign_manager",
        @Header("X-Auth-Token") token:  String,
        @Body                   body:   AssignManagerRequest,
    ): Response<AdminActionResponse>

    // ── push_register — registro token FCM por dispositivo ───
    @POST(API_PATH)
    suspend fun pushRegister(
        @Query("action")        action: String = "push_register",
        @Header("X-Auth-Token") token:  String,
        @Body                   body:   PushRegisterRequest,
    ): Response<BaseResponse>

    // ── file_upload — subida de fotos de visita ──────────────
    @Multipart
    @POST(API_PATH)
    suspend fun fileUpload(
        @Query("action")         action: String = "file_upload",
        @Header("X-Auth-Token")  token: String,
        @Part("stop_uid")        stopUid: RequestBody,
        @Part("photo_uid")       photoUid: RequestBody,
        @Part                    file: MultipartBody.Part,
    ): Response<FileUploadResponse>

    // ── God Dashboard ─────────────────────────────────────────
    @GET(API_PATH)
    suspend fun godStats(
        @Header("X-Auth-Token") token: String,
        @Query("action") action: String = "god_stats",
    ): Response<GodStatsResponse>

    @POST(API_PATH)
    suspend fun godUsersAll(
        @Header("X-Auth-Token") token: String,
        @Query("action") action: String = "god_users_all",
        @Body body: Map<String, @JvmSuppressWildcards Any?> = emptyMap(),
    ): Response<GodUsersResponse>

    @POST(API_PATH)
    suspend fun godSetRole(
        @Header("X-Auth-Token") token: String,
        @Query("action") action: String = "god_set_role",
        @Body body: GodSetRoleRequest,
    ): Response<AdminActionResponse>

}




