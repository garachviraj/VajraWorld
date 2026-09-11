package com.vajraworld.defender.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vajraworld.defender.data.local.VajraDatabase
import com.vajraworld.defender.data.repository.VajraRepository

class TelemetrySyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = VajraDatabase.getInstance(applicationContext)
        val repository = VajraRepository(database.dao())
        val refreshResult = repository.refreshIncidents()
        return if (refreshResult.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
