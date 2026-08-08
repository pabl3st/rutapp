package com.pabl3st.rutapp.core.permission

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestión centralizada de permisos runtime.
 *
 * Estrategia:
 * - CRÍTICOS (notificaciones, localización): pedidos en el splash de onboarding,
 *   uno a uno con explicación. No se piden de nuevo salvo que el usuario los revoque.
 * - OPCIONALES (cámara, almacenamiento): pedidos justo antes del primer uso real,
 *   nunca en el splash.
 * - BACKGROUND_LOCATION: pedido solo al activar el tracking de jornada.
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    private val prefs: SharedPreferences by lazy {
        ctx.getSharedPreferences("rutasapp_permissions", Context.MODE_PRIVATE)
    }

    // ── Grupos de permisos ────────────────────────────────────

    /** Permisos que se piden en el onboarding — uno a uno */
    val onboardingPermissions: List<AppPermission> = buildList {
        // 1. Notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(AppPermission.Notifications)
        }
        // 2. Localización precisa
        add(AppPermission.LocationFine)
    }

    /** Permiso de cámara — pedir solo al intentar hacer foto */
    val cameraPermission = AppPermission.Camera

    /** Permiso de almacenamiento — pedir solo al importar/exportar XLS */
    val storagePermission: AppPermission
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            AppPermission.MediaImages
        else
            AppPermission.Storage

    /** Localización en background — pedir solo al iniciar jornada con tracking */
    val backgroundLocationPermission = AppPermission.LocationBackground

    // ── Checks ───────────────────────────────────────────────

    fun isGranted(permission: AppPermission): Boolean =
        permission.manifestPermissions.all { mp ->
            ContextCompat.checkSelfPermission(ctx, mp) == PackageManager.PERMISSION_GRANTED
        }

    fun areOnboardingPermissionsComplete(): Boolean =
        onboardingPermissions.all { isGranted(it) || wasPermanentlyDenied(it) }

    fun wasPermanentlyDenied(permission: AppPermission): Boolean =
        prefs.getBoolean("denied_${permission.key}", false)

    fun markPermanentlyDenied(permission: AppPermission) {
        prefs.edit().putBoolean("denied_${permission.key}", true).apply()
    }

    fun wasOnboardingShown(): Boolean =
        prefs.getBoolean("onboarding_shown", false)

    fun markOnboardingShown() {
        prefs.edit().putBoolean("onboarding_shown", true).apply()
    }

    fun isLocationAvailable(): Boolean = isGranted(AppPermission.LocationFine) ||
        isGranted(AppPermission.LocationCoarse)

    fun isCameraAvailable(): Boolean = isGranted(AppPermission.Camera)

    fun isStorageAvailable(): Boolean = isGranted(storagePermission)

    // ── Localizacion en background ────────────────────────────

    /** El permiso de background solo aplica en Android 10+ (Q). En < Q se hereda del foreground. */
    fun backgroundLocationApplies(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** True si ya tenemos background, o si no aplica (heredado del foreground en < Q). */
    fun isBackgroundLocationGranted(): Boolean =
        !backgroundLocationApplies() || isGranted(AppPermission.LocationBackground)

    fun wasBackgroundPromptShown(): Boolean =
        prefs.getBoolean("background_prompt_shown", false)

    fun markBackgroundPromptShown() {
        prefs.edit().putBoolean("background_prompt_shown", true).apply()
    }

    // ── Exencion de optimizacion de bateria ───────────────────

    /**
     * True si la app esta exenta de la optimizacion de bateria (o no hay PowerManager).
     * Sin exencion, el SO puede matar el ForegroundService de GPS durante la jornada.
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }

    /** Intent para pedir al usuario la exencion directa (dialogo del sistema). */
    @SuppressLint("BatteryLife")
    fun batteryOptimizationRequestIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${ctx.packageName}")
        }

    /** Intent de respaldo: lista de ajustes de bateria (si el directo no esta disponible). */
    fun batteryOptimizationSettingsIntent(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    fun wasBatteryPromptShown(): Boolean =
        prefs.getBoolean("battery_prompt_shown", false)

    fun markBatteryPromptShown() {
        prefs.edit().putBoolean("battery_prompt_shown", true).apply()
    }
}

// ── Modelo de permisos ────────────────────────────────────────

sealed class AppPermission(
    val key: String,
    val manifestPermissions: List<String>,
    val title: String,
    val rationale: String,
    val icon: String,           // nombre de MaterialIcon
    val isCritical: Boolean,    // true = sin él la app pierde funcionalidad core
) {
    /** Notificaciones push (Android 13+) */
    data object Notifications : AppPermission(
        key                  = "notifications",
        manifestPermissions  = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        else emptyList(),
        title                = "Notificaciones",
        rationale            = "Recibe avisos de jornada, sincronización y cambios en tus rutas.",
        icon                 = "Notifications",
        isCritical           = false,
    )

    /** GPS preciso — imprescindible para tracking y mapa */
    data object LocationFine : AppPermission(
        key                  = "location_fine",
        manifestPermissions  = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ),
        title                = "Ubicación precisa",
        rationale            = "Necesaria para ver tu posición en el mapa, ordenar rutas por proximidad y registrar check-ins automáticos.",
        icon                 = "LocationOn",
        isCritical           = true,
    )

    /** GPS de baja precisión */
    data object LocationCoarse : AppPermission(
        key                  = "location_coarse",
        manifestPermissions  = listOf(Manifest.permission.ACCESS_COARSE_LOCATION),
        title                = "Ubicación aproximada",
        rationale            = "Permite ver tu zona en el mapa.",
        icon                 = "MyLocation",
        isCritical           = false,
    )

    /** GPS en segundo plano — solo para tracking de jornada */
    data object LocationBackground : AppPermission(
        key                  = "location_background",
        manifestPermissions  = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        else emptyList(),
        title                = "Ubicación en segundo plano",
        rationale            = "Permite registrar tu recorrido durante la jornada aunque la app esté minimizada.",
        icon                 = "Route",
        isCritical           = false,
    )

    /** Cámara — fotos en formularios de visita */
    data object Camera : AppPermission(
        key                  = "camera",
        manifestPermissions  = listOf(Manifest.permission.CAMERA),
        title                = "Cámara",
        rationale            = "Para añadir fotos a los informes de visita.",
        icon                 = "Camera",
        isCritical           = false,
    )

    /** Almacenamiento (Android < 13) */
    data object Storage : AppPermission(
        key                  = "storage",
        manifestPermissions  = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                @Suppress("DEPRECATION")
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        else emptyList(),
        title                = "Almacenamiento",
        rationale            = "Para importar y exportar ficheros de rutas y KPIs.",
        icon                 = "FolderOpen",
        isCritical           = false,
    )

    /** Acceso a imágenes (Android 13+) */
    data object MediaImages : AppPermission(
        key                  = "media_images",
        manifestPermissions  = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            listOf(Manifest.permission.READ_MEDIA_IMAGES)
        else emptyList(),
        title                = "Archivos multimedia",
        rationale            = "Para importar ficheros XLS y adjuntar imágenes a visitas.",
        icon                 = "PermMedia",
        isCritical           = false,
    )
}
