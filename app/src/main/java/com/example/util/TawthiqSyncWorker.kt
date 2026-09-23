package com.example.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class TawthiqSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "TawthiqSyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Executing WorkManager periodic sync check...")
        try {
            // Perform direct sync safely via WorkManager
            TawthiqBackgroundSyncManager.performBackgroundSync(appContext)

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in TawthiqSyncWorker", e)
            return Result.retry()
        }
    }
}
