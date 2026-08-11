package com.dhyper.fncompanion.data.models

import com.dhyper.fncompanion.data.db.AuthEntity
import com.dhyper.fncompanion.data.db.SettingsEntity
import com.dhyper.fncompanion.data.db.WishlistEntity
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppDataBackup(
    val accounts: List<AuthEntity> = emptyList(),
    val settings: SettingsEntity? = null,
    val wishlist: List<WishlistEntity> = emptyList()
)
