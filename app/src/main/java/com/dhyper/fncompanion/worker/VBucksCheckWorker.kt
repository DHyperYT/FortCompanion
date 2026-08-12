package com.dhyper.fncompanion.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dhyper.fncompanion.data.db.AppDatabase
import com.dhyper.fncompanion.data.repository.FortniteRepository
import java.util.UUID

class VBucksCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = FortniteRepository()
        val settingsDao = AppDatabase.getDatabase(applicationContext).settingsDao()
        val settings = settingsDao.getSettingsDirect()

        if (settings?.vbucksAlertsEnabled != true) {
            return Result.success()
        }

        // Tag the check in logs
        val checkId = UUID.randomUUID().toString().substring(0, 8)
        Log.i("VBucksCheckWorker", "[$checkId] Starting V-Bucks check...")

        val result = repository.checkForVBucksAlert()
        
        result.fold(
            onSuccess = { currentMissionId ->
                if (currentMissionId != null) {
                    val lastNotifiedId = settings.lastVBucksMissionId
                    
                    // We check if the current mission set is different from the last time we notified.
                    // If parsing failed but missions are there (fallback ID), we still notify.
                    if (currentMissionId != lastNotifiedId) {
                        Log.i("VBucksCheckWorker", "[$checkId] HAS_VBUCKS: New set detected ($currentMissionId). Notifying.")
                        sendNotification("V-Bucks missions are active! Open FortniteDB to see them.")
                        settingsDao.saveSettings(settings.copy(lastVBucksMissionId = currentMissionId))
                    } else {
                        Log.i("VBucksCheckWorker", "[$checkId] HAS_VBUCKS: Already notified for this set ($currentMissionId).")
                    }
                } else {
                    Log.i("VBucksCheckWorker", "[$checkId] NO_VBUCKS: Confirmed no missions today.")
                    if (settings.lastVBucksMissionId != null) {
                        settingsDao.saveSettings(settings.copy(lastVBucksMissionId = null))
                    }
                }
            },
            onFailure = { error ->
                Log.e("VBucksCheckWorker", "[$checkId] ERROR: Request failed: ${error.message}")
                // If the user wants extreme reliability, we could notify on error too, 
                // but that might be annoying. For now, just logging clearly.
            }
        )

        return Result.success()
    }

    private fun sendNotification(message: String) {
        val channelId = "vbucks_alerts"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "V-Bucks Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Fortnite V-Bucks Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2001, notification)
    }
}
