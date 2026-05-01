package com.pabl3st.rutapp.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
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
    val name: String,
    @Json(name = "date_assigned") val dateAssigned: String,
    val status: String,
    val notes: String?,
    @Json(name = "stop_count")  val stopCount: Int = 0,
    @Json(name = "done_count")  val doneCount: Int = 0,
    @Json(name = "created_at")  val createdAt: String,
    @Json(name = "updated_at")  val updatedAt: String,
    @Json(name = "deleted_at")  val deletedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class StopDto(
    val id: Int?,
    val uid: String,
    @Json(name = "route_uid") val routeUid: String?,
    val name: String,
    val address: String?,
    val lat: Double?,
    val lng: Double?,
    @Json(name = "order_index") val orderIndex: Int = 0,
    val status: String,
    val notes: String?,
    @Json(name = "visited_at") val visitedAt: String?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
    @Json(name = "deleted_at") val deletedAt: String? = null,
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
    @Json(name = "server_time") val serverTime: String?,
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

// ── Retrofit interface ───────────────────────────────────────

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

}

