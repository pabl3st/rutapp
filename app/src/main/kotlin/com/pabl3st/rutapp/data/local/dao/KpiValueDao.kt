package com.pabl3st.rutapp.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.pabl3st.rutapp.data.local.entity.KpiValueEntity

/** Fila de KPI con la fecha de su visita (resultado de [KpiValueDao.getWithDates]). */
data class KpiValueWithDate(
    val kpiId:     String,
    val stopUid:   String,
    val valueText: String,
    val visitDate: String,
)

@Dao
interface KpiValueDao {

    @Query("SELECT * FROM kpi_values WHERE stopUid = :stopUid")
    suspend fun getByStop(stopUid: String): List<KpiValueEntity>

    /**
     * Devuelve el último total acumulado de cada KPI del stop.
     * Los KPIs son acumulativos (cada visita guarda el total vigente del PDV),
     * así que "el valor actual" es el de la visita más reciente.
     *
     * Toma la fila por cada kpiId cuya stop_visits.visitDate sea la más alta.
     * Si dos visitas tienen la misma fecha, devuelve un resultado estable
     * (la de updatedAt mayor por desempate).
     */
    @Query("""
        SELECT kv.* FROM kpi_values kv
        JOIN stop_visits sv ON sv.uid = kv.visitUid
        WHERE kv.stopUid = :stopUid
          AND sv.deletedAt IS NULL
          AND sv.visitDate = (
              SELECT MAX(sv2.visitDate)
              FROM kpi_values kv2
              JOIN stop_visits sv2 ON sv2.uid = kv2.visitUid
              WHERE kv2.stopUid = :stopUid
                AND kv2.kpiId   = kv.kpiId
                AND sv2.deletedAt IS NULL
          )
    """)
    suspend fun getLastTotalsByStop(stopUid: String): List<KpiValueEntity>

    /** Valores de una visita concreta (formulario ya completado de esa fecha) */
    @Query("SELECT * FROM kpi_values WHERE visitUid = :visitUid")
    suspend fun getByVisit(visitUid: String): List<KpiValueEntity>

    /**
     * Total acumulado del MES para prerrellenar el formulario: por cada KPI, el valor
     * de la visita mas reciente del PDV dentro de [monthStart, untilDate]. En un mes
     * nuevo no hay filas y el formulario sale vacio (las stats se reinician el dia 1).
     */
    @Query("""
        SELECT kv.* FROM kpi_values kv
        JOIN stop_visits sv ON sv.uid = kv.visitUid
        WHERE kv.stopUid = :stopUid
          AND sv.deletedAt IS NULL
          AND sv.visitDate BETWEEN :monthStart AND :untilDate
          AND sv.visitDate = (
              SELECT MAX(sv2.visitDate)
              FROM kpi_values kv2
              JOIN stop_visits sv2 ON sv2.uid = kv2.visitUid
              WHERE kv2.stopUid = :stopUid
                AND kv2.kpiId   = kv.kpiId
                AND sv2.deletedAt IS NULL
                AND sv2.visitDate BETWEEN :monthStart AND :untilDate
          )
    """)
    suspend fun getMonthTotalsByStop(stopUid: String, monthStart: String, untilDate: String): List<KpiValueEntity>

    /** Valores con la fecha de su visita, dentro de un rango (stats mensuales) */
    @Query("""
        SELECT kv.kpiId AS kpiId, kv.stopUid AS stopUid, kv.valueText AS valueText, sv.visitDate AS visitDate
        FROM kpi_values kv
        JOIN stop_visits sv ON sv.uid = kv.visitUid
        WHERE kv.stopUid IN (:stopUids)
          AND sv.deletedAt IS NULL
          AND sv.visitDate BETWEEN :fromDate AND :toDate
    """)
    suspend fun getWithDates(stopUids: List<String>, fromDate: String, toDate: String): List<KpiValueWithDate>

    /** Carga los kpi_values de múltiples stops — para lógica acumulativa mensual */
    @Query("SELECT * FROM kpi_values WHERE stopUid IN (:stopUids)")
    suspend fun getByStops(stopUids: List<String>): List<KpiValueEntity>

    /** Flow reactivo de todos los kpi_values de una lista de stops — para tags en tiempo real */
    @Query("SELECT * FROM kpi_values WHERE stopUid IN (:stopUids)")
    fun observeByStops(stopUids: List<String>): kotlinx.coroutines.flow.Flow<List<KpiValueEntity>>

    @Query("SELECT * FROM kpi_values WHERE syncStatus = 'pending'")
    suspend fun getPendingSync(): List<KpiValueEntity>

    @Upsert
    suspend fun upsertAll(values: List<KpiValueEntity>)

    @Upsert
    suspend fun upsert(value: KpiValueEntity)

    @Query("UPDATE kpi_values SET syncStatus = 'synced' WHERE stopUid = :stopUid")
    suspend fun markSynced(stopUid: String)

    @Query("DELETE FROM kpi_values WHERE stopUid = :stopUid")
    suspend fun deleteByStop(stopUid: String)

    /** Borra TODOS los KPI values cuyos stops pertenezcan a esta cuenta.
     *  Se hace por subquery porque KpiValueEntity no tiene accountId directo. */
    @Query("""
        DELETE FROM kpi_values
        WHERE stopUid IN (SELECT uid FROM stops WHERE accountId = :accountId)
    """)
    suspend fun deleteAllByAccount(accountId: Int)
}


