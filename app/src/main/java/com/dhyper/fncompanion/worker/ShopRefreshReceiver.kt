package com.dhyper.fncompanion.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ShopRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "com.dhyper.fncompanion.ACTION_SHOP_REFRESH") {
            // 1. Trigger the actual check
            val request = OneTimeWorkRequestBuilder<ShopCheckWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
            
            // 2. Schedule the next alarm
            scheduleNextAlarm(context)
        }
    }

    companion object {
        fun scheduleNextAlarm(context: Context, forceEnabled: Boolean? = null) {
            val db = com.dhyper.fncompanion.data.db.AppDatabase.getDatabase(context)
            val scope = CoroutineScope(Dispatchers.IO)

            scope.launch {
                val settings = db.settingsDao().getSettingsDirect() ?: return@launch
                val isEnabled = forceEnabled ?: settings.notificationsEnabled

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, ShopRefreshReceiver::class.java).apply {
                    action = "com.dhyper.fncompanion.ACTION_SHOP_REFRESH"
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    1001,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (!isEnabled) {
                    alarmManager.cancel(pendingIntent)
                    return@launch
                }

                val timeParts = settings.shopRefreshTime.split(":")
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
                
                val scheduledTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(calendar.time)
                android.util.Log.i("ShopRefreshReceiver", "Next shop alert scheduled for: $scheduledTime")

                // use exact alarm to ensure it fires even if app is swiped away
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        } else {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } catch (e: Exception) {
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
