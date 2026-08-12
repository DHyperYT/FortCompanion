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
        val repository = FortniteRepository()
        val db = AppDatabase.getDatabase(applicationContext)
        val wishlistDao = db.wishlistDao()
        val authDao = db.authDao()
        val settingsDao = db.settingsDao()

        // 1. Get all logged-in accounts
        val accounts = authDao.getAllAccounts().first()
        if (accounts.isEmpty()) return Result.success()

        // 2. Check universal wishlist setting
        val settings = settingsDao.getSettingsDirect()
        val isUniversal = settings?.useUniversalWishlist == true

        // 3. Fetch the shop once
        val shopResult = repository.fetchItemShop()
        shopResult.fold(
            onSuccess = { shopData ->
                val allShopItemIds = shopData.entries?.flatMap { entry ->
                    val standardItems = (entry.items ?: emptyList()) + 
                                     (entry.brItems ?: emptyList()) + 
                                     (entry.cars ?: emptyList()) + 
                                     (entry.vehicles ?: emptyList()) + 
                                     (entry.instruments ?: emptyList())
                    
                    val standardIds = standardItems.map { it.id.lowercase() }
                    
                    val trackIds = entry.tracks?.flatMap { t ->
                        val trackMap = t.track as? Map<*, *>
                        val apiCosmeticId = trackMap?.get("id")?.toString()
                        val sidFromDevName = Regex("""sid_[a-zA-Z0-9_]+""", RegexOption.IGNORE_CASE).find(t.devName ?: "")?.value
                        val idField = t.id ?: ""
                        
                        val ids = mutableListOf<String>()
                        if (!apiCosmeticId.isNullOrBlank()) ids.add(apiCosmeticId.lowercase())
                        if (!sidFromDevName.isNullOrBlank()) ids.add(sidFromDevName.lowercase())
                        if (idField.isNotBlank()) ids.add(idField.lowercase())
                        
                        ids
                    } ?: emptyList()
                    
                    standardIds + trackIds
                }?.toSet() ?: emptySet()
                
                if (isUniversal) {
                    val universalWishlist = wishlistDao.getUniversalWishlist().first()
                    val foundItems = universalWishlist.filter { wishItem ->
                        val wishId = wishItem.id.lowercase()
                        val wishIdNoPrefix = wishId.substringAfter(":")
                        
                        allShopItemIds.contains(wishId) || 
                        allShopItemIds.contains(wishIdNoPrefix) ||
                        (wishId.startsWith("sid_") && allShopItemIds.any { it.contains(wishId) }) ||
                        allShopItemIds.any { it.contains(wishIdNoPrefix) && it.startsWith("sid_") }
                    }

                    if (foundItems.isNotEmpty()) {
                        sendNotification("Universal Wishlist: ${foundItems.joinToString { it.name }}", 999)
                    }
                } else {
                    // 4. Check wishlist for each account
                    accounts.forEachIndexed { index, account ->
                        val wishlist = wishlistDao.getAllWishlistedItems(account.accountId).first()
                        val foundItems = wishlist.filter { wishItem ->
                            val wishId = wishItem.id.lowercase()
                            val wishIdNoPrefix = wishId.substringAfter(":")
                            
                            allShopItemIds.contains(wishId) || 
                            allShopItemIds.contains(wishIdNoPrefix) ||
                            (wishId.startsWith("sid_") && allShopItemIds.any { it.contains(wishId) }) ||
                            allShopItemIds.any { it.contains(wishIdNoPrefix) && it.startsWith("sid_") }
                        }

                        if (foundItems.isNotEmpty()) {
                            sendNotification("${account.displayName}: ${foundItems.joinToString { it.name }}", index + 100)
                        }
                    }
                }
            },
            onFailure = {
            }
        )

        return Result.success()
    }

    private fun sendNotification(itemNames: String, notificationId: Int) {
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

        notificationManager.notify(notificationId, notification)
    }
}
