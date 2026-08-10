package com.dhyper.fncompanion.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val fortniteApiKey: String? = null,
    val firstLaunchHandled: Boolean = false,
    val useUniversalWishlist: Boolean = false,
    val accentColor: String = "Cyan", // "Cyan", "Primary", "Emerald", "Gold"
    val notificationsEnabled: Boolean = true
)
