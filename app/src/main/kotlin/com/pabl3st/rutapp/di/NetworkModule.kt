package com.pabl3st.rutapp.di

import com.pabl3st.rutapp.data.network.RutasApiService
import com.squareup.moshi.Moshi
import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import com.pabl3st.rutapp.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Base URL debe terminar en "/" — Retrofit lanza IllegalArgumentException si no
    private const val BASE_URL = "https://mejoresiagratis.com/"

    @Provides
    @Singleton
    // Todos los DTOs usan @JsonClass(generateAdapter = true) -> adaptadores KSP.
    // Sin fallback reflexivo: un DTO sin anotar debe fallar en tests, no en produccion.
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        sessionManager: com.pabl3st.rutapp.data.session.SessionManager,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // BODY solo en debug — evita loguear tokens y datos sensibles en producción
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }
        // Auth interceptor — añadir token a cada request automáticamente
        val authInterceptor = okhttp3.Interceptor { chain ->
            val token = sessionManager.token
            val req   = if (token != null)
                chain.request().newBuilder()
                    .header("X-Auth-Token", token)
                    .build()
            else chain.request()
            chain.proceed(req)
        }

        // Authenticator — refresca sesión automáticamente cuando el servidor devuelve 401
        val tokenAuthenticator = object : Authenticator {
            override fun authenticate(route: Route?, response: Response): Request? {
                // Evitar bucle infinito: si ya intentamos con el token actual, rendirse
                if (response.request.header("X-Auth-Token") == sessionManager.token) {
                    return null  // ya reintentamos — logout implícito (SyncWorker lo gestiona)
                }
                // Token nuevo del servidor si viene en la respuesta
                val newToken = response.header("X-New-Token")
                return if (newToken != null) {
                    sessionManager.token = newToken
                    response.request.newBuilder()
                        .header("X-Auth-Token", newToken)
                        .build()
                } else null
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .authenticator(tokenAuthenticator)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): RutasApiService =
        retrofit.create(RutasApiService::class.java)

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext ctx: Context): WorkManager =
        WorkManager.getInstance(ctx)
}
