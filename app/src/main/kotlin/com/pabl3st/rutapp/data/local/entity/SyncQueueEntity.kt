package com.pabl3st.rutapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entity: String,        // "route" | "stop"
    val entityUid: String,     // uid de la entidad
    val operation: String,     // "create" | "update" | "delete"
    val payload: String,       // JSON serializado de la entidad
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int  = 0,
    val lastError: String? = null,
)
