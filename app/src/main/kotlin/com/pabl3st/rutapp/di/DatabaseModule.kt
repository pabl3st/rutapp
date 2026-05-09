package com.pabl3st.rutapp.di

import android.content.Context
import androidx.room.Room
import com.pabl3st.rutapp.data.local.RutasDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): RutasDatabase =
        Room.databaseBuilder(ctx, RutasDatabase::class.java, "rutasapp.db")
            .addMigrations(
                RutasDatabase.MIGRATION_1_2,
                RutasDatabase.MIGRATION_2_3,
                RutasDatabase.MIGRATION_3_4,
                RutasDatabase.MIGRATION_4_5,
                RutasDatabase.MIGRATION_5_6,
                RutasDatabase.MIGRATION_6_7,
            )
            .build()

    @Provides fun provideRouteDao(db: RutasDatabase)           = db.routeDao()
    @Provides fun provideStopDao(db: RutasDatabase)            = db.stopDao()
    @Provides fun provideSyncQueueDao(db: RutasDatabase)       = db.syncQueueDao()
    @Provides fun provideDaySessionDao(db: RutasDatabase)      = db.daySessionDao()
    @Provides fun provideKpiDefinitionDao(db: RutasDatabase)   = db.kpiDefinitionDao()
    @Provides fun provideBusinessProfileDao(db: RutasDatabase) = db.businessProfileDao()
    @Provides fun provideKpiValueDao(db: RutasDatabase)        = db.kpiValueDao()
}
