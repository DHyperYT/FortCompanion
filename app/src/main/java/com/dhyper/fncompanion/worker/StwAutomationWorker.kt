package com.dhyper.fncompanion.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dhyper.fncompanion.data.db.AppDatabase
import com.dhyper.fncompanion.data.repository.AuthRepository
import com.dhyper.fncompanion.data.repository.StwAutomationRepository
import kotlinx.coroutines.flow.first

class StwAutomationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val authRepo = AuthRepository(db.authDao())
        val stwAutoRepo = StwAutomationRepository()

        val accounts = db.authDao().getAllAccounts().first()
        if (accounts.isEmpty()) return Result.success()

        accounts.forEachIndexed { index, account ->
            if (account.stwAutoRecycleJunk) {
                val sessionRes = authRepo.ensureActiveSession(account.accountId)
                sessionRes.getOrNull()?.let { session ->
                    val result = stwAutoRepo.runAutoRecycleJunk(applicationContext, session)
                    if (result.isSuccess) {
                        val msg = result.getOrNull() ?: "Completed"
                        if (msg.contains("Recycled") || msg.contains("Destroyed")) {
                            sendNotification(account.displayName, "Auto-Recycle: $msg", index + 3000)
                        }
                    }
                }
            }

            if (account.stwAutoClaimLlamas) {
                val sessionRes = authRepo.ensureActiveSession(account.accountId)
                sessionRes.getOrNull()?.let { session ->
                    val result = stwAutoRepo.runAutoClaimLlamas(session)
                    if (result.isSuccess) {
                        val msg = result.getOrNull() ?: "Completed"
                        if (msg.contains("claimed")) {
                            sendNotification(account.displayName, "Llama Claim: $msg", index + 4000)
                        }
                    }
                }
            }
        }

        return Result.success()
    }

    private fun sendNotification(accountName: String, message: String, notificationId: Int) {
        val channelId = "stw_automation"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "STW Automation", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("STW Automation: $accountName")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
