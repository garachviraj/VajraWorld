package com.vajraworld.defender

import android.app.Application
import androidx.work.*
import com.vajraworld.defender.data.local.VajraDatabase
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.data.sync.TelemetrySyncWorker
import java.util.concurrent.TimeUnit

class VajraApplication : Application() {
    lateinit var database: VajraDatabase
    lateinit var repository: VajraRepository

    override fun onCreate() {
        super.onCreate()
        database = VajraDatabase.getInstance(this)
        repository = VajraRepository(database.dao())

        // Schedule periodic telemetry sync every 15 minutes
        val syncRequest = PeriodicWorkRequestBuilder<TelemetrySyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "VajraTelemetrySync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
