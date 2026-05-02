package com.pabl3st.rutapp.data.local.dao

import androidx.room.*
import com.pabl3st.rutapp.data.local.entity.BusinessProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessProfileDao {

    @Query("SELECT * FROM business_profiles WHERE accountId = :accountId LIMIT 1")
    fun observe(accountId: Int): Flow<BusinessProfileEntity?>

    @Query("SELECT * FROM business_profiles WHERE accountId = :accountId LIMIT 1")
    suspend fun get(accountId: Int): BusinessProfileEntity?

    @Upsert
    suspend fun upsert(profile: BusinessProfileEntity)
}
