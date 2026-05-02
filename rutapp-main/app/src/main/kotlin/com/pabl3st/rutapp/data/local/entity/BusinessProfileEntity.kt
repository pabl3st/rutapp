package com.pabl3st.rutapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Perfil de negocio del account.
 * Cada account tiene exactamente un perfil activo.
 */
@Entity(tableName = "business_profiles")
data class BusinessProfileEntity(
    @PrimaryKey val accountId: Int,
    val sector: String  = "custom",        // telco|farma|distribucion|retail|custom
    val name: String    = "Mi negocio",    // nombre visible al usuario
    val updatedAt: Long = System.currentTimeMillis(),
)
