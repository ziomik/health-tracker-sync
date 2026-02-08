package com.healthtracker.sync

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.*
import com.healthtracker.sync.work.HealthSyncWorker
import java.util.concurrent.TimeUnit

class HealthSyncApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Crea notification channel per sync updates
        createNotificationChannel()
        
        // Schedule periodic sync work
        scheduleSyncWork()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SYNC_CHANNEL_ID,
                "Health Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifiche sincronizzazione dati salute"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)  // WiFi o dati mobili
            .setRequiresBatteryNotLow(true)  // Evita sync con batteria scarica
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<HealthSyncWorker>(
            3, TimeUnit.HOURS  // Sync ogni 3 ore
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    companion object {
        const val SYNC_CHANNEL_ID = "health_sync_channel"
        const val SYNC_WORK_NAME = "health_sync_work"
    }
}
