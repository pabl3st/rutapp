package com.pabl3st.rutapp.di

import android.content.Context
import com.pabl3st.rutapp.core.location.LocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Provides
    @Singleton
    fun provideLocationManager(@ApplicationContext ctx: Context): LocationManager =
        LocationManager(ctx)
}
