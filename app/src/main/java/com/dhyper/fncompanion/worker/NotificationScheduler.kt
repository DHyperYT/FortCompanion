package com.dhyper.fncompanion.worker

import android.content.Context
import androidx.work.*
import com.dhyper.fncompanion.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val WORK_NAME = "ShopCheckWork"

    fun schedule(context: Context) {
        val appContext = context.applicationContext
        val db = AppDatabase.getDatabase(appContext)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = db.settingsDao().getSettingsDirect() ?: return@launch
                
                if (!settings.notificationsEnabled) {
                    WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME)
                    return@launch
                }

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val now = Calendar.getInstance()
                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, settings.notificationHour)
                    set(Calendar.MINUTE, settings.notificationMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (target.before(now)) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }

                val initialDelay = target.timeInMillis - now.timeInMillis

                val request = PeriodicWorkRequestBuilder<ShopCheckWorker>(24, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .addTag(WORK_NAME)
                    .build()

                WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    request
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
