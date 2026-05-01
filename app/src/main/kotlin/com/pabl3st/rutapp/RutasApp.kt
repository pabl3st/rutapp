package com.pabl3st.rutapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class.
 *
 * S01: solo @HiltAndroidApp. HiltWorkerFactory se añade en S02
 * cuando se implemente WorkManager y SyncWorker.
 */
@HiltAndroidApp
class RutasApp : Application()
