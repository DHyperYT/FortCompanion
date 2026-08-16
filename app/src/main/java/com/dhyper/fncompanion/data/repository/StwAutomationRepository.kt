package com.dhyper.fncompanion.data.repository

import android.content.Context
import android.util.Log
import com.dhyper.fncompanion.data.db.AuthEntity

class StwAutomationRepository(
    private val epicAccountRepo: EpicAccountRepository = EpicAccountRepository()
) {
    suspend fun runAutoRecycleJunk(context: Context, session: AuthEntity): Result<String> {
        Log.i("StwAutomation", "Running Auto-Recycle Junk for ${session.displayName}")
        
        val homebaseResult = epicAccountRepo.fetchStwHomebaseData(context, session.accessToken, session.accountId)
        val homebase = homebaseResult.getOrNull() ?: return Result.failure(Exception("Failed to fetch homebase data"))
        
        val backpack = homebase.inventory.backpack
        val itemsToRecycle = backpack.filter { 
            (it.type == "Ranged" || it.type == "Melee" || it.type == "Trap") && 
            (it.rarity.contains("Common", true) || it.rarity.contains("Uncommon", true)) 
        }

        val ingredientsToDestroy = backpack.filter { 
            it.type == "Material" && it.templateId.contains("_t01", true)
        }.map { it.id }

        if (itemsToRecycle.isEmpty() && ingredientsToDestroy.isEmpty()) {
            return Result.success("No junk items found.")
        }

        val profileRevisions = homebase.profileRevisions
        val theaterRvn = profileRevisions["theater0"] ?: -1
        
        var summary = ""
        if (itemsToRecycle.isNotEmpty()) {
            val pairs = itemsToRecycle.map { mapOf("itemId" to it.id, "quantity" to 1) }
            val res = epicAccountRepo.disassembleItems(session.accessToken, session.accountId, pairs, theaterRvn)
            if (res.isSuccess) summary += "Recycled ${itemsToRecycle.size} junk items. "
        }

        if (ingredientsToDestroy.isNotEmpty()) {
            val res = epicAccountRepo.destroyItems(session.accessToken, session.accountId, ingredientsToDestroy, theaterRvn)
            if (res.isSuccess) summary += "Destroyed ${ingredientsToDestroy.size} T1 mats."
        }

        return Result.success(summary.ifEmpty { "Actions performed but no changes reported." })
    }

    suspend fun runAutoClaimLlamas(session: AuthEntity): Result<String> {
        Log.i("StwAutomation", "Running Auto-Claim Llamas for ${session.displayName}")
        
        val storefrontRes = epicAccountRepo.fetchStorefront(session.accessToken)
        val storefront = storefrontRes.getOrNull() ?: return Result.failure(Exception("Failed to fetch storefront"))
        
        val storefronts = storefront["storefronts"] as? List<*> ?: emptyList<Any>()
        var purchasedCount = 0
        
        storefronts.filterIsInstance<Map<String, Any>>().forEach { sf ->
            if (sf["name"] == "CardPackStorePreroll") {
                val entries = sf["catalogEntries"] as? List<*> ?: emptyList<Any>()
                entries.filterIsInstance<Map<String, Any>>().forEach { entry ->
                    val prices = entry["prices"] as? List<*> ?: emptyList<Any>()
                    val price = (prices.firstOrNull() as? Map<*, *>)?.get("finalPrice") as? Number
                    
                    if (price?.toInt() == 0) {
                        val offerId = entry["offerId"]?.toString() ?: ""
                        // For auto-claim, we assume we want to claim them.
                        // We need common_core rvn. 
                        // Fetching it dynamically or using -1 if supported.
                        // Better to fetch it to be safe.
                        val res = epicAccountRepo.purchaseCatalogEntry(
                            session.accessToken, session.accountId, offerId, 
                            1, "GameItem", "AccountResource:currency_xrayllama", 0, -1
                        )
                        if (res.isSuccess) purchasedCount++
                    }
                }
            }
        }

        return if (purchasedCount > 0) {
            Result.success("Automatically claimed $purchasedCount free items.")
        } else {
            Result.success("No free items found in shop.")
        }
    }
}
