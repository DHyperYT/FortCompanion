package com.dhyper.fncompanion.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cosmetic_wishlist")
data class WishlistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val rarity: String,
    val iconUrl: String?,
    val addedAtMs: Long = System.currentTimeMillis()
)
