package com.dhyper.fncompanion.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dhyper.fncompanion.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class VBucksAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "com.dhyper.fncompanion.ACTION_VBUCKS_CHECK") {
            val request = OneTimeWorkRequestBuilder<VBucksCheckWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
            
            scheduleNextAlarm(context)
        }
    }

    companion object {
        fun scheduleNextAlarm(context: Context) {
            val db = AppDatabase.getDatabase(context)
            val scope = CoroutineScope(Dispatchers.IO)
            
            scope.launch {
                val settings = db.settingsDao().getSettingsDirect() ?: return@launch
                if (!settings.vbucksAlertsEnabled) return@launch

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, VBucksAlertReceiver::class.java).apply {
                    action = "com.dhyper.fncompanion.ACTION_VBUCKS_CHECK"
                }
                
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    2001,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val timeParts = settings.vbucksAlertTime.split(":")
                val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
                val min = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

                val now = System.currentTimeMillis()
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = now
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, min)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= now) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            }
        }
    }
}
