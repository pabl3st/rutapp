package com.pabl3st.rutapp.di

import android.content.Context
import com.pabl3st.rutapp.core.map.MapConfig
import com.pabl3st.rutapp.core.map.MapProvider
import com.pabl3st.rutapp.core.map.MapProviderFactory
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapModule {

    @Provides
    @Singleton
    fun provideMapConfig(session: SessionManager): MapConfig {
        // Por ahora siempre MapLibre (sin key).
        // En S07 leeremos provider y apiKey desde account.plus_config
        return MapProviderFactory.fieldSalesConfig()
    }

    @Provides
    @Singleton
    fun provideMapProvider(
        @ApplicationContext ctx: Context,
        config: MapConfig,
    ): MapProvider = MapProviderFactory.create(ctx, config)
}
