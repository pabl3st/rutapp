@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.visita

import com.pabl3st.rutapp.core.ui.theme.RutasColors

import android.Manifest
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NextPlan
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pabl3st.rutapp.data.local.entity.KpiDefinitionEntity
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

private val VISIT_RESULTS = listOf(
    Triple("contactado",  "Contactado",    Icons.Default.CheckCircle),
    Triple("no_estaba",   "No estaba",     Icons.Default.PersonOff),
    Triple("volvemos",    "Volvemos",      Icons.Default.Replay),
    Triple("rechazado",   "Rechazado",     Icons.Default.Cancel),
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VisitaScreen(
    stopUid: String,
    onBack: () -> Unit,
    vm: VisitaViewModel = hiltViewModel(),
) {
    val ui            by vm.ui.collectAsStateWithLifecycle()
    val snackbarHost   = remember { SnackbarHostState() }

    LaunchedEffect(ui.saved) { if (ui.saved) onBack() }
    LaunchedEffect(ui.error) {
        ui.error?.let { msg ->
            snackbarHost.showSnackbar(msg, duration = SnackbarDuration.Short)
            vm.clearError()
        }
    }

    if (ui.showCamera) {
        CameraScreen(onPhotoTaken = vm::onPhotoTaken, onDismiss = vm::onHideCamera)
        return
    }

    Scaffold(
        modifier = Modifier.semantics { testTag = "visita-screen" },
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(ui.stop?.name ?: "Visita")
                        if (ui.isEditingPreviousVisit) {
                            Text(
                                "Editando visita anterior",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick  = vm::saveVisit,
                    // Guardar requiere resultado seleccionado — si está en blanco el botón está desactivado
                    enabled  = !ui.isSaving && ui.selectedResult.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                        .semantics { testTag = "visita-save-button" },
                ) {
                    if (ui.isSaving) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (ui.selectedResult.isBlank()) "Selecciona un resultado" else "Guardar visita")
                }
            }
        }
    ) { padding ->
        when {
            ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ui.stop == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Parada no encontrada", color = MaterialTheme.colorScheme.error)
            }
            else -> {
                val stop = ui.stop!!
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            stop.externalId?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                            Text(stop.name, style = MaterialTheme.typography.titleMedium)
                            val ctxForNav = LocalContext.current
                            stop.address?.let { addr ->
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Default.Place, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text(addr, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f), maxLines = 2,
                                        overflow = TextOverflow.Ellipsis)
                                    // Botón navegar a Maps
                                    if (stop.lat != null && stop.lng != null) {
                                        IconButton(
                                            onClick = {
                                                val uri = android.net.Uri.parse(
                                                    "geo:${stop.lat},${stop.lng}?q=${stop.lat},${stop.lng}(${android.net.Uri.encode(stop.name)})"
                                                )
                                                ctxForNav.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                                            },
                                            modifier = Modifier.size(28.dp),
                                        ) {
                                            Icon(Icons.Default.Navigation, "Navegar",
                                                Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                            stop.contactName?.let { contact ->
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text(contact, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            stop.contactPhone?.let { phone ->
                                Spacer(Modifier.height(4.dp))
                                val context = LocalContext.current
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_DIAL,
                                                android.net.Uri.parse("tel:$phone")
                                            )
                                            context.startActivity(intent)
                                        }
                                        .padding(vertical = 2.dp),
                                ) {
                                    Icon(Icons.Default.Phone, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text  = phone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            // Horario de apertura
                            stop.openingHours?.takeIf { it.isNotBlank() }?.let { hours ->
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, null, Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text(hours, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            // Segmento + prioridad
                            val hasMeta = (stop.segment != null) || (stop.priority in 1..3)
                            if (hasMeta) {
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    stop.segment?.takeIf { it.isNotBlank() }?.let { seg ->
                                        Surface(
                                            shape = MaterialTheme.shapes.extraSmall,
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                        ) {
                                            Text(seg, style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                    if (stop.priority in 1..3) {
                                        val pColor = when (stop.priority) {
                                            1 -> MaterialTheme.colorScheme.error
                                            2 -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                        Surface(
                                            shape = MaterialTheme.shapes.extraSmall,
                                            color = pColor.copy(alpha = 0.15f),
                                        ) {
                                            Text("P${stop.priority}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = pColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Estado PDV — 3 estados ─────────────────────
                    Text("Estado del PDV", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

                    // Banner PDV inactivo cuando está marcado
                    if (ui.pdvInactive) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors   = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Block, null, Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("PDV marcado como inactivo",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.error)
                                    Text("El estado del account se actualizará a 'inactivo'",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Botón Abierto
                        val openSel = ui.storeOpen == true && !ui.pdvInactive
                        OutlinedButton(
                            onClick  = { vm.onStoreOpenChange(true) },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (openSel) MaterialTheme.colorScheme.primaryContainer
                                                 else MaterialTheme.colorScheme.surface,
                            ),
                            border = BorderStroke(
                                if (openSel) 2.dp else 1.dp,
                                if (openSel) MaterialTheme.colorScheme.primary
                                             else MaterialTheme.colorScheme.outlineVariant,
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Default.Store, null, Modifier.size(14.dp),
                                tint = if (openSel) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(3.dp))
                            Text("Abierto", style = MaterialTheme.typography.labelSmall,
                                color = if (openSel) MaterialTheme.colorScheme.primary
                                         else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        // Botón Cerrado hoy
                        val closedSel = ui.storeOpen == false && !ui.pdvInactive
                        OutlinedButton(
                            onClick  = { vm.onStoreOpenChange(false) },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (closedSel) MaterialTheme.colorScheme.errorContainer
                                                  else MaterialTheme.colorScheme.surface,
                            ),
                            border = BorderStroke(
                                if (closedSel) 2.dp else 1.dp,
                                if (closedSel) MaterialTheme.colorScheme.error
                                               else MaterialTheme.colorScheme.outlineVariant,
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Default.StoreMallDirectory, null, Modifier.size(14.dp),
                                tint = if (closedSel) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(3.dp))
                            Text("Hoy cerrado", style = MaterialTheme.typography.labelSmall,
                                color = if (closedSel) MaterialTheme.colorScheme.error
                                         else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        // Botón PDV Inactivo
                        OutlinedButton(
                            onClick  = { vm.onPdvInactiveToggle() },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (ui.pdvInactive)
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.surface,
                            ),
                            border = BorderStroke(
                                if (ui.pdvInactive) 2.dp else 1.dp,
                                if (ui.pdvInactive) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.outlineVariant,
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Default.Block, null, Modifier.size(14.dp),
                                tint = if (ui.pdvInactive) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(3.dp))
                            Text("Inactivo", style = MaterialTheme.typography.labelSmall,
                                color = if (ui.pdvInactive) MaterialTheme.colorScheme.error
                                         else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Text("Resultado", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VISIT_RESULTS.forEach { (value, label, icon) ->
                            val selected = ui.selectedResult == value
                            Card(
                                onClick = { vm.onResultChange(value) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                                border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon, null, Modifier.size(20.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(12.dp))
                                    Text(label, style = MaterialTheme.typography.bodyMedium, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                    if (selected) { Spacer(Modifier.weight(1f)); Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                                }
                            }
                        }
                    }

                    if (ui.prefs.showPhotos) {
                        Text("Fotos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        PhotosSection(photos = ui.photos, onAddPhoto = vm::onShowCamera, onRemovePhoto = vm::onRemovePhoto)
                    }


                    // ── KPIs dinámicos del sector ─────────────────────
                    if (ui.kpiFields.isNotEmpty()) {
                        Text("Datos de visita", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        ui.kpiFields.forEach { kpi ->
                            KpiField(
                                kpi      = kpi,
                                value    = ui.kpiValues[kpi.id] ?: "",
                                onChange = { vm.onKpiValueChange(kpi.id, it) },
                            )
                        }
                    }

                                        OutlinedTextField(value = ui.notes, onValueChange = { if (it.length <= 500) vm.onNotesChange(it) }, label = { Text("Notas de la visita") }, placeholder = { Text("Observaciones, incidencias...") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6)

                    if (ui.prefs.showNextAction) {
                        OutlinedTextField(value = ui.nextAction, onValueChange = { if (it.length <= 255) vm.onNextActionChange(it) }, label = { Text("Próxima acción") }, placeholder = { Text("Qué hacer en la siguiente visita...") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, leadingIcon = { Icon(Icons.AutoMirrored.Filled.NextPlan, null, Modifier.size(18.dp)) })
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PhotosSection(photos: List<Uri>, onAddPhoto: () -> Unit, onRemovePhoto: (Uri) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 8.dp)) {
        item {
            Box(
                modifier = Modifier.size(80.dp).clip(MaterialTheme.shapes.small)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                    .clickable(onClick = onAddPhoto),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, "Añadir foto", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("Foto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        items(photos) { uri ->
            Box(Modifier.size(80.dp).clip(MaterialTheme.shapes.small)) {
                AsyncImage(model = uri, contentDescription = "Foto visita", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                IconButton(
                    onClick  = { onRemovePhoto(uri) },
                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(RutasColors.Dark950.copy(alpha = 0.5f), CircleShape),
                ) { Icon(Icons.Default.Close, "Eliminar foto", tint = RutasColors.TextDark100, modifier = Modifier.size(14.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CameraScreen(onPhotoTaken: (Uri) -> Unit, onDismiss: () -> Unit) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) { if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest() }

    if (!cameraPermission.status.isGranted) {
        Box(Modifier.fillMaxSize().background(RutasColors.Dark950), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Permiso de cámara necesario", color = RutasColors.TextDark100, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { cameraPermission.launchPermissionRequest() }) { Text("Conceder permiso") }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss) { Text("Cancelar", color = RutasColors.TextDark100) }
            }
        }
        return
    }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor     = remember { Executors.newSingleThreadExecutor() }

    Box(Modifier.fillMaxSize().background(RutasColors.Dark950)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                ProcessCameraProvider.getInstance(ctx).addListener({
                    val provider = ProcessCameraProvider.getInstance(ctx).get()
                    val preview  = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().padding(bottom = 48.dp), contentAlignment = Alignment.BottomCenter) {
            Row(horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cancelar", tint = RutasColors.TextDark100, modifier = Modifier.size(32.dp)) }
                Box(
                    modifier = Modifier.size(72.dp).background(RutasColors.TextDark100, CircleShape).clickable {
                        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
                        val cv   = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, "RutasApp_$name")
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RutasApp")
                        }
                        imageCapture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv).build(),
                            executor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(out: ImageCapture.OutputFileResults) { out.savedUri?.let { onPhotoTaken(it) } }
                                override fun onError(e: ImageCaptureException) {}
                            }
                        )
                    },
                    contentAlignment = Alignment.Center,
                ) { Box(Modifier.size(60.dp).background(RutasColors.TextDark100, CircleShape).border(2.dp, RutasColors.Dark600, CircleShape)) }
                Spacer(Modifier.size(32.dp))
            }
        }
    }
}

// ── Campo de KPI dinámico ─────────────────────────────────────
// Renderiza el input correcto según el tipo del KPI:
// number → teclado numérico decimal
// boolean → Switch
// select → LazyRow de FilterChips con las opciones
// text → campo de texto libre
@Composable
private fun KpiField(
    kpi:      KpiDefinitionEntity,
    value:    String,
    onChange: (String) -> Unit,
) {
    val label = if (kpi.required) "${kpi.label} *" else kpi.label
    when (kpi.type) {
        "boolean" -> Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(
                checked         = value == "true",
                onCheckedChange = { onChange(if (it) "true" else "false") },
            )
        }
        "select" -> {
            val options = runCatching {
                kpi.options
                    ?.trim('[', ']')
                    ?.split(",")
                    ?.map { it.trim().trim('"') }
                    ?: emptyList()
            }.getOrDefault(emptyList())
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(options) { opt ->
                        FilterChip(
                            selected = value == opt,
                            onClick  = { onChange(opt) },
                            label    = { Text(opt, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        }
        "number" -> OutlinedTextField(
            value           = value,
            onValueChange   = onChange,
            label           = { Text(label) },
            modifier        = Modifier.fillMaxWidth(),
            singleLine      = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            suffix          = kpi.unit?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        )
        else -> OutlinedTextField(
            value         = value,
            onValueChange = onChange,
            label         = { Text(label) },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
        )
    }
}



