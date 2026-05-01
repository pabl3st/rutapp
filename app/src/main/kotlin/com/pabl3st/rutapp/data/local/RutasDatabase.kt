package com.pabl3st.rutapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pabl3st.rutapp.data.local.dao.RouteDao
import com.pabl3st.rutapp.data.local.dao.StopDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity

@Database(
    entities     = [RouteEntity::class, StopEntity::class, SyncQueueEntity::class],
    version      = 2,
    exportSchema = false,
)
abstract class RutasDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun stopDao(): StopDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        // v1 → v2: añadir external_id, contact_name, contact_phone, visit_result, next_action
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stops ADD COLUMN externalId   TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE stops ADD COLUMN contactName  TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE stops ADD COLUMN contactPhone TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE stops ADD COLUMN visitResult  TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE stops ADD COLUMN nextAction   TEXT DEFAULT NULL")
            }
        }
    }
}
