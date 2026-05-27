package com.pabl3st.rutapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pabl3st.rutapp.data.local.dao.BusinessProfileDao
import com.pabl3st.rutapp.data.local.dao.DaySessionDao
import com.pabl3st.rutapp.data.local.dao.KpiDefinitionDao
import com.pabl3st.rutapp.data.local.dao.KpiValueDao
import com.pabl3st.rutapp.data.local.dao.RouteDao
import com.pabl3st.rutapp.data.local.dao.StopDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.local.entity.BusinessProfileEntity
import com.pabl3st.rutapp.data.local.entity.DaySessionEntity
import com.pabl3st.rutapp.data.local.entity.KpiDefinitionEntity
import com.pabl3st.rutapp.data.local.entity.KpiValueEntity
import com.pabl3st.rutapp.data.local.dao.VisitPhotoDao
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity
import com.pabl3st.rutapp.data.local.entity.VisitPhotoEntity

@androidx.room.TypeConverters(RutasTypeConverters::class)
@Database(
    entities = [
        RouteEntity::class,
        StopEntity::class,
        SyncQueueEntity::class,
        DaySessionEntity::class,
        KpiDefinitionEntity::class,
        BusinessProfileEntity::class,
        KpiValueEntity::class,
        VisitPhotoEntity::class,
    ],
    version      = 15,
    exportSchema = false,
)
abstract class RutasDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun stopDao(): StopDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun daySessionDao(): DaySessionDao
    abstract fun kpiDefinitionDao(): KpiDefinitionDao
    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun kpiValueDao(): KpiValueDao
    abstract fun visitPhotoDao(): VisitPhotoDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stops ADD COLUMN externalId   TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE stops ADD COLUMN contactName  TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE stops ADD COLUMN contactPhone TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE stops ADD COLUMN visitResult  TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE stops ADD COLUMN nextAction   TEXT DEFAULT NULL")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stops ADD COLUMN visitFrequency INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE stops ADD COLUMN priority INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE stops ADD COLUMN segment TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE stops ADD COLUMN accountStatus TEXT DEFAULT 'active'")
                db.execSQL("ALTER TABLE stops ADD COLUMN openingHours TEXT DEFAULT NULL")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS day_sessions (
                        routeUid TEXT NOT NULL, dateStr TEXT NOT NULL,
                        state TEXT NOT NULL DEFAULT 'idle',
                        startedAt INTEGER, pausedAt INTEGER,
                        elapsedMs INTEGER NOT NULL DEFAULT 0,
                        distanceKm REAL NOT NULL DEFAULT 0.0,
                        lastLat REAL, lastLng REAL,
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (routeUid, dateStr)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_day_sessions_dateStr ON day_sessions (dateStr)")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS kpi_definitions (
                        id TEXT NOT NULL PRIMARY KEY,
                        accountId INTEGER NOT NULL DEFAULT 0,
                        sector TEXT NOT NULL,
                        label TEXT NOT NULL,
                        type TEXT NOT NULL,
                        unit TEXT,
                        options TEXT,
                        required INTEGER NOT NULL DEFAULT 0,
                        visible INTEGER NOT NULL DEFAULT 1,
                        orderIndex INTEGER NOT NULL DEFAULT 0,
                        section TEXT NOT NULL DEFAULT 'general',
                        isSystem INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_kpi_sector ON kpi_definitions (sector)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_kpi_account ON kpi_definitions (accountId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS business_profiles (
                        accountId INTEGER NOT NULL PRIMARY KEY,
                        sector TEXT NOT NULL DEFAULT 'custom',
                        name TEXT NOT NULL DEFAULT 'Mi negocio',
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS kpi_values (
                        stopUid    TEXT NOT NULL,
                        kpiId      TEXT NOT NULL,
                        valueText  TEXT NOT NULL,
                        syncStatus TEXT NOT NULL DEFAULT 'pending',
                        PRIMARY KEY (stopUid, kpiId)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_kpi_values_stop ON kpi_values (stopUid)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_kpi_values_sync ON kpi_values (syncStatus)")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Añadir campo pdvOpen a stops — persiste si el PDV estaba abierto en la visita
                db.execSQL("ALTER TABLE stops ADD COLUMN pdvOpen INTEGER NOT NULL DEFAULT 1")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stops ADD COLUMN pdvInactive INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Fechas de visita programadas por ruta (JSON array)
                db.execSQL("ALTER TABLE routes ADD COLUMN scheduledDates TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS visit_photos (
                        uid        TEXT NOT NULL PRIMARY KEY,
                        stopUid    TEXT NOT NULL,
                        localPath  TEXT NOT NULL,
                        serverUrl  TEXT,
                        syncStatus TEXT NOT NULL DEFAULT 'pending',
                        createdAt  INTEGER NOT NULL DEFAULT 0,
                        lastError  TEXT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_vp_stop   ON visit_photos (stopUid)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_vp_sync   ON visit_photos (syncStatus)")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stops ADD COLUMN checkInTs INTEGER")
                db.execSQL("ALTER TABLE stops ADD COLUMN checkOutTs INTEGER")
                db.execSQL("ALTER TABLE stops ADD COLUMN gpsLatVisit REAL")
                db.execSQL("ALTER TABLE stops ADD COLUMN gpsLngVisit REAL")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Fecha de visita concreta del stop (un stop por fecha del schedule)
                db.execSQL("ALTER TABLE stops ADD COLUMN dateAssigned TEXT")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Separar dirección en street/postalCode/city.
                // Address se conserva para compatibilidad y display.
                db.execSQL("ALTER TABLE stops ADD COLUMN street TEXT")
                db.execSQL("ALTER TABLE stops ADD COLUMN postalCode TEXT")
                db.execSQL("ALTER TABLE stops ADD COLUMN city TEXT")

                // Back-fill: extraer CP (5 dígitos) de address y separar street/city.
                // SQLite no tiene REGEXP nativo. Recorremos por código en Kotlin
                // no es posible aquí — usamos LIKE+SUBSTR para casos típicos:
                //   "CALLE X 7, 46320, SINARCAS"  →  street="CALLE X 7"  cp="46320"  city="SINARCAS"
                // Limitación SQLite: solo CPs con coma delante. Direcciones libres
                // sin CP detectable quedan con street=address y resto NULL.
                db.execSQL("""
                    UPDATE stops
                    SET postalCode = SUBSTR(address,
                                       INSTR(address || ',', ', ') + 2,
                                       5)
                    WHERE address IS NOT NULL
                      AND address LIKE '%, %'
                      AND LENGTH(address) >= INSTR(address || ',', ', ') + 6
                      AND postalCode IS NULL
                """.trimIndent())

                // Si extrajimos algo que no parece CP (no 5 dígitos), limpiar
                db.execSQL("""
                    UPDATE stops
                    SET postalCode = NULL
                    WHERE postalCode IS NOT NULL
                      AND (LENGTH(postalCode) != 5
                           OR CAST(postalCode AS INTEGER) = 0)
                """.trimIndent())

                // street = lo anterior al ", CP"
                db.execSQL("""
                    UPDATE stops
                    SET street = TRIM(SUBSTR(address, 1, INSTR(address, ', ' || postalCode) - 1))
                    WHERE postalCode IS NOT NULL AND street IS NULL
                """.trimIndent())

                // city = lo posterior a "CP, "
                db.execSQL("""
                    UPDATE stops
                    SET city = TRIM(SUBSTR(address, INSTR(address, postalCode) + 6))
                    WHERE postalCode IS NOT NULL AND city IS NULL
                """.trimIndent())

                // Direcciones sin CP detectable → street = address completo
                db.execSQL("""
                    UPDATE stops
                    SET street = address
                    WHERE address IS NOT NULL
                      AND postalCode IS NULL
                      AND street IS NULL
                """.trimIndent())
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Índice UNIQUE en sync_queue — last-write-wins, evita duplicados al servidor
                // DROP+RECREATE porque SQLite no soporta ADD UNIQUE INDEX inline
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_sq_unique ON sync_queue (entity, entityUid, operation)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Índices para las queries más frecuentes — evita full scans con 149+ paradas
                // routes: observeByDate, observeToday
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_routes_user_date ON routes (userId, dateAssigned)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_routes_account_date ON routes (accountId, dateAssigned)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_routes_sync ON routes (syncStatus)")
                // stops: observeByRoute, observeByRouteUids, getPendingSync
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_stops_route ON stops (routeUid)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_stops_sync ON stops (syncStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_stops_account ON stops (accountId, deletedAt)")
                // kpi_values: getByStop, getByStopsInMonth
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_kv_stop ON kpi_values (stopUid)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_kv_sync ON kpi_values (syncStatus)")
                // sync_queue: getNext50 (ya ordenado por createdAt)
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_sq_created ON sync_queue (createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_sq_attempts ON sync_queue (attempts)")
            }
        }
    }
}
