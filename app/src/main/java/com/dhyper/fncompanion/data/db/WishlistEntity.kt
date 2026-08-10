package com.dhyper.fncompanion.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cosmetic_wishlist", primaryKeys = ["id", "accountId"])
data class WishlistEntity(
    val id: String,
    val accountId: String,
    val name: String,
    val type: String,
    val rarity: String,
    val iconUrl: String?,
    val addedAtMs: Long = System.currentTimeMillis()
)
