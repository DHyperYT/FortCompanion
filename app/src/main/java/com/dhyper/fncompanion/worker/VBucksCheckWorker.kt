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

class VBucksCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = FortniteRepository()
        val settingsDao = AppDatabase.getDatabase(applicationContext).settingsDao()
        val settings = settingsDao.getSettingsDirect()

        if (settings?.vbucksAlertsEnabled != true) {
            Log.d("VBucksCheckWorker", "V-Bucks alerts are disabled in settings.")
            return Result.success()
        }

        val result = repository.checkForVBucksAlert()
        
        result.fold(
            onSuccess = { currentMissionId ->
                if (currentMissionId != null) {
                    val lastNotifiedId = settings.lastVBucksMissionId
                    if (currentMissionId != lastNotifiedId) {
                        Log.i("VBucksCheckWorker", "HAS_VBUCKS: New missions detected ($currentMissionId). Notifying user.")
                        sendNotification("V-Bucks missions are active! Check FortniteDB for details.")
                        settingsDao.saveSettings(settings.copy(lastVBucksMissionId = currentMissionId))
                    } else {
                        Log.d("VBucksCheckWorker", "HAS_VBUCKS: Missions are still the same as last notification. Skipping.")
                    }
                } else {
                    Log.d("VBucksCheckWorker", "NO_VBUCKS: No V-Bucks missions found today.")
                    // Optionally clear the last notified ID if no missions are found, 
                    // or keep it to avoid re-notifying if missions reappear then disappear? 
                    // Usually, missions change daily, so clearing it when "No Missions" is found is safe.
                    if (settings.lastVBucksMissionId != null) {
                        settingsDao.saveSettings(settings.copy(lastVBucksMissionId = null))
                    }
                }
            },
            onFailure = { error ->
                Log.e("VBucksCheckWorker", "ERROR: Failed to check for V-Bucks alerts: ${error.message}", error)
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
