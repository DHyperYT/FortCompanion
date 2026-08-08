package com.dhyper.fncompanion.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dhyper.fncompanion.data.db.AppDatabase
import com.dhyper.fncompanion.data.repository.FortniteRepository
import kotlinx.coroutines.flow.first

class ShopCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val isManualTest = inputData.getBoolean("is_test", false)
        val repository = FortniteRepository()
        val db = AppDatabase.getDatabase(applicationContext)
        val wishlistDao = db.wishlistDao()

        val wishlist = wishlistDao.getAllWishlistedItems().first()
        if (wishlist.isEmpty()) {
            if (isManualTest) sendNotification("Wishlist is empty. Add items to test!")
            return Result.success()
        }

        val shopResult = repository.fetchItemShop()
        shopResult.fold(
            onSuccess = { shopData ->
                val allShopItems = shopData.entries?.flatMap { entry ->
                    (entry.items ?: emptyList()) + 
                    (entry.brItems ?: emptyList()) + 
                    (entry.cars ?: emptyList()) + 
                    (entry.vehicles ?: emptyList()) + 
                    (entry.instruments ?: emptyList())
                } ?: emptyList()
                
                val foundItems = wishlist.filter { wishItem ->
                    allShopItems.any { shopItem -> shopItem.id.equals(wishItem.id, ignoreCase = true) }
                }

                if (foundItems.isNotEmpty()) {
                    sendNotification(foundItems.joinToString { it.name })
                } else if (isManualTest) {
                    sendNotification("Shop check complete: None of your wishlisted items are currently in the shop.")
                }
            },
            onFailure = {
                if (isManualTest) sendNotification("Failed to check shop: ${it.localizedMessage}")
            }
        )

        return Result.success()
    }

    private fun sendNotification(itemNames: String) {
        val channelId = "shop_alerts"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Check for POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Shop Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Wishlist Item in Shop!")
            .setContentText("The following items are now available: $itemNames")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
