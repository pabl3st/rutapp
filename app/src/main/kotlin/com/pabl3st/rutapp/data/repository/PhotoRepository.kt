package com.pabl3st.rutapp.data.repository

import android.content.Context
import android.net.Uri
import com.pabl3st.rutapp.data.local.dao.VisitPhotoDao
import com.pabl3st.rutapp.data.local.entity.VisitPhotoEntity
import com.pabl3st.rutapp.data.network.RutasApiService
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val photoDao: VisitPhotoDao,
    private val api:      RutasApiService,
    private val session:  SessionManager,
) {

    fun observeByStop(stopUid: String): Flow<List<VisitPhotoEntity>> =
        photoDao.observeByStop(stopUid)

    // ── Persistir fotos tomadas en la visita ──────────────────
    // Llama desde VisitaViewModel.saveVisit() con los Uris de la sesión.
    // Convierte cada Uri en una VisitPhotoEntity y la guarda en Room.
    suspend fun savePhotos(stopUid: String, uris: List<Uri>): List<VisitPhotoEntity> {
        val entities = uris.map { uri ->
            VisitPhotoEntity(
                uid       = UUID.randomUUID().toString(),
                stopUid   = stopUid,
                localPath = uri.toString(),
            )
        }
        if (entities.isNotEmpty()) photoDao.upsertAll(entities)
        return entities
    }

    // ── Subir fotos pendientes al servidor ────────────────────
    // Llamado desde SyncRepository.uploadPending() para procesar la cola.
    // Devuelve true si todos los uploads tuvieron éxito (o no había pendientes).
    suspend fun uploadPending(): Boolean {
        val token   = session.token ?: return false
        val pending = photoDao.getPending()
        if (pending.isEmpty()) return true

        var allOk = true
        for (photo in pending) {
            val ok = uploadPhoto(token, photo)
            if (!ok) allOk = false
        }
        return allOk
    }

    private suspend fun uploadPhoto(token: String, photo: VisitPhotoEntity): Boolean {
        return runCatching {
            val uri   = Uri.parse(photo.localPath)
            val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return false

            val mimeType    = ctx.contentResolver.getType(uri) ?: "image/jpeg"
            val extension   = if (mimeType.contains("png")) "png" else "jpg"
            val filePart    = MultipartBody.Part.createFormData(
                name     = "file",
                filename = "photo_${photo.uid}.$extension",
                body     = bytes.toRequestBody(mimeType.toMediaTypeOrNull()),
            )
            val stopUidPart  = photo.stopUid.toRequestBody("text/plain".toMediaTypeOrNull())
            val photoUidPart = photo.uid.toRequestBody("text/plain".toMediaTypeOrNull())

            val resp = api.fileUpload(
                token    = token,
                stopUid  = stopUidPart,
                photoUid = photoUidPart,
                file     = filePart,
            )

            if (resp.isSuccessful && resp.body()?.ok == true) {
                val url = resp.body()?.url
                photoDao.updateSync(photo.uid, "synced", url, null)
                true
            } else {
                val err = resp.body()?.error ?: "HTTP ${resp.code()}"
                photoDao.updateSync(photo.uid, "error", null, err)
                false
            }
        }.getOrElse { e ->
            photoDao.updateSync(photo.uid, "error", null, e.message)
            false
        }
    }
}
