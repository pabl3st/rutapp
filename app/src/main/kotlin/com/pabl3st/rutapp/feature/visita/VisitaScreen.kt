@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.visita

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
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
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.saved) { if (ui.saved) onBack() }

    if (ui.showCamera) {
        CameraScreen(onPhotoTaken = vm::onPhotoTaken, onDismiss = vm::onHideCamera)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui.stop?.name ?: "Visita") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") } },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick  = vm::saveVisit,
                    enabled  = !ui.isSaving,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (ui.isSaving) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Guardar visita")
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
                            stop.address?.let { addr ->
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text(addr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                    Text("Fotos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    PhotosSection(photos = ui.photos, onAddPhoto = vm::onShowCamera, onRemovePhoto = vm::onRemovePhoto)

                    OutlinedTextField(value = ui.notes, onValueChange = vm::onNotesChange, label = { Text("Notas de la visita") }, placeholder = { Text("Observaciones, incidencias...") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6)

                    OutlinedTextField(value = ui.nextAction, onValueChange = vm::onNextActionChange, label = { Text("Próxima acción") }, placeholder = { Text("Qué hacer en la siguiente visita...") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, leadingIcon = { Icon(Icons.Default.NextPlan, null, Modifier.size(18.dp)) })

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
                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape),
                ) { Icon(Icons.Default.Close, "Eliminar foto", tint = Color.White, modifier = Modifier.size(14.dp)) }
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
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Permiso de cámara necesario", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { cameraPermission.launchPermissionRequest() }) { Text("Conceder permiso") }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White) }
            }
        }
        return
    }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor     = remember { Executors.newSingleThreadExecutor() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
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
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cancelar", tint = Color.White, modifier = Modifier.size(32.dp)) }
                Box(
                    modifier = Modifier.size(72.dp).background(Color.White, CircleShape).clickable {
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
                ) { Box(Modifier.size(60.dp).background(Color.White, CircleShape).border(2.dp, Color.LightGray, CircleShape)) }
                Spacer(Modifier.size(32.dp))
            }
        }
    }
}
