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
            .addMigrations(RutasDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideRouteDao(db: RutasDatabase) = db.routeDao()

    @Provides
    fun provideStopDao(db: RutasDatabase) = db.stopDao()

    @Provides
    fun provideSyncQueueDao(db: RutasDatabase) = db.syncQueueDao()
}

