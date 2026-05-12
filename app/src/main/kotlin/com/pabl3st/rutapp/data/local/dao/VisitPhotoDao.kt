package com.pabl3st.rutapp.data.local.dao

import androidx.room.*
import com.pabl3st.rutapp.data.local.entity.VisitPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitPhotoDao {

    @Query("SELECT * FROM visit_photos WHERE stopUid = :stopUid ORDER BY createdAt ASC")
    fun observeByStop(stopUid: String): Flow<List<VisitPhotoEntity>>

    @Query("SELECT * FROM visit_photos WHERE stopUid = :stopUid ORDER BY createdAt ASC")
    suspend fun getByStop(stopUid: String): List<VisitPhotoEntity>

    @Query("SELECT * FROM visit_photos WHERE syncStatus = 'pending' ORDER BY createdAt ASC LIMIT 10")
    suspend fun getPending(): List<VisitPhotoEntity>

    @Upsert
    suspend fun upsert(photo: VisitPhotoEntity)

    @Upsert
    suspend fun upsertAll(photos: List<VisitPhotoEntity>)

    @Query("UPDATE visit_photos SET syncStatus = :status, serverUrl = :url, lastError = :error WHERE uid = :uid")
    suspend fun updateSync(uid: String, status: String, url: String?, error: String?)

    @Query("DELETE FROM visit_photos WHERE uid = :uid")
    suspend fun delete(uid: String)

    @Query("DELETE FROM visit_photos WHERE stopUid = :stopUid")
    suspend fun deleteByStop(stopUid: String)
}
