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
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity

@Database(
    entities = [
        RouteEntity::class,
        StopEntity::class,
        SyncQueueEntity::class,
        DaySessionEntity::class,
        KpiDefinitionEntity::class,
        BusinessProfileEntity::class,
        KpiValueEntity::class,
    ],
    version      = 6,
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
            }
        }
    }
}
