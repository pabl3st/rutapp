package com.pabl3st.rutapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Foto tomada durante una visita.
 * Se persiste en Room al guardar la visita y se sube al servidor
 * en background via SyncWorker (entidad "photo" en SyncQueue).
 */
@Entity(
    tableName = "visit_photos",
    indices   = [
        androidx.room.Index("stopUid"),
        androidx.room.Index("syncStatus"),
    ],
)
data class VisitPhotoEntity(
    @PrimaryKey val uid: String,          // UUID local
    val stopUid:    String,               // stop al que pertenece
    val localPath:  String,               // content:// URI de MediaStore
    val serverUrl:  String?  = null,      // URL tras upload exitoso
    val syncStatus: String   = "pending", // pending | uploading | synced | error
    val createdAt:  Long     = System.currentTimeMillis(),
    val lastError:  String?  = null,
)
