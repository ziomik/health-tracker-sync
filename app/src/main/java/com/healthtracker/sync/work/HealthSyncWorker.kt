package com.healthtracker.sync.work

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthtracker.sync.HealthSyncApp
import com.healthtracker.sync.R
import com.healthtracker.sync.api.HealthTrackerApi
import com.healthtracker.sync.data.PreferencesManager
import com.healthtracker.sync.health.HealthConnectManager
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * Worker per sync automatica in background
 */
class HealthSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val prefsManager = PreferencesManager(context)
    private val healthManager = HealthConnectManager(context)
    private val notificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    override suspend fun doWork(): Result {
        return try {
            // Verifica configurazione
            val deviceId = prefsManager.deviceId.first()
            val apiToken = prefsManager.apiToken.first()
            val serverUrl = prefsManager.serverUrl.first()
            
            if (deviceId == null || apiToken == null || serverUrl == null) {
                return Result.failure()  // Non configurato
            }
            
            // Inizializza API
            HealthTrackerApi.initialize(serverUrl)
            
            // Mostra notifica sync in corso
            showSyncNotification("Sincronizzazione in corso...")
            
            // Leggi dati da Health Connect
            val vitals = healthManager.readVitalsData()
            val sleep = healthManager.readSleepData()
            val activity = healthManager.readActivityData()
            
            // Sync con server
            val response = HealthTrackerApi.syncData(
                deviceId = deviceId,
                apiToken = apiToken,
                vitals = vitals.takeIf { it.isNotEmpty() },
                sleep = sleep.takeIf { it.isNotEmpty() },
                activity = activity.takeIf { it.isNotEmpty() }
            )
            
            if (response.isSuccess) {
                val data = response.getOrNull()
                val totalRecords = data?.totalRecords ?: 0
                
                // Aggiorna last sync
                prefsManager.updateLastSync(Instant.now().toString())
                
                // Notifica successo
                showSyncNotification(
                    "✅ Sincronizzati $totalRecords record",
                    isOngoing = false
                )
                
                Result.success()
            } else {
                // Notifica errore
                showSyncNotification(
                    "❌ Sync fallito: ${response.exceptionOrNull()?.message}",
                    isOngoing = false
                )
                
                Result.retry()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            
            showSyncNotification(
                "❌ Errore: ${e.message}",
                isOngoing = false
            )
            
            Result.failure()
        }
    }
    
    private fun showSyncNotification(text: String, isOngoing: Boolean = true) {
        val notification = NotificationCompat.Builder(applicationContext, HealthSyncApp.SYNC_CHANNEL_ID)
            .setContentTitle("Health Tracker Sync")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_sync)  // TODO: add icon
            .setOngoing(isOngoing)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
        
        // Rimuovi notifica dopo 5 sec se non ongoing
        if (!isOngoing) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                notificationManager.cancel(NOTIFICATION_ID)
            }, 5000)
        }
    }
    
    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
