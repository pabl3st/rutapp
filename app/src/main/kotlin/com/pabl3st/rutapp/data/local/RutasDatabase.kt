package com.pabl3st.rutapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pabl3st.rutapp.data.local.dao.RouteDao
import com.pabl3st.rutapp.data.local.dao.StopDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity

@Database(
    entities     = [RouteEntity::class, StopEntity::class, SyncQueueEntity::class],
    version      = 1,
    exportSchema = false,
)
abstract class RutasDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun stopDao(): StopDao
    abstract fun syncQueueDao(): SyncQueueDao
}
