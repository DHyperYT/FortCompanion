package com.dhyper.fncompanion.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auth_session")
data class AuthEntity(
    @PrimaryKey val accountId: String,
    val displayName: String,
    val accessToken: String,
    val refreshToken: String?,
    val expiresAtMs: Long,
    val deviceId: String? = null,
    val deviceSecret: String? = null,
    val loginTimeMs: Long = System.currentTimeMillis(),
    val lastRefreshTimeMs: Long = System.currentTimeMillis(),
    val isActive: Boolean = false,
    val equippedSkinIcon: String? = null,
    val deviceAuthStatus: String = "VALID" // VALID, REVOKED, UNKNOWN
)

@Entity(tableName = "recent_player_search")
data class RecentSearchEntity(
    @PrimaryKey val accountName: String,
    val timestamp: Long = System.currentTimeMillis()
)
