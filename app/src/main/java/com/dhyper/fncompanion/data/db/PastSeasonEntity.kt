package com.dhyper.fncompanion.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "past_seasons")
data class PastSeasonEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val accountId: String,
    val seasonNumber: Int,
    val seasonName: String,
    val seasonLevel: Int,
    val battlePassTier: Int,
    val seasonWins: Int,
    val hasBattlePass: Boolean
)
