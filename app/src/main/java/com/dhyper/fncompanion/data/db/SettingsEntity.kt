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
    val notificationsEnabled: Boolean = true,
    val vbucksAlertsEnabled: Boolean = false,
    val shopRefreshTime: String = "00:00",
    val stwVBucksAlertTime: String = "00:00",
    val stwAutomationTime: String = "00:00",
    val lastVBucksMissionId: String? = null,
    val dataSaverMode: Boolean = false
)
