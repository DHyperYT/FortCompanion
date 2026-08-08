package com.dhyper.fncompanion.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {
    @Query("SELECT * FROM cosmetic_wishlist ORDER BY addedAtMs DESC")
    fun getAllWishlistedItems(): Flow<List<WishlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWishlist(item: WishlistEntity)

    @Query("DELETE FROM cosmetic_wishlist WHERE id = :itemId")
    suspend fun removeFromWishlist(itemId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM cosmetic_wishlist WHERE id = :itemId)")
    suspend fun isWishlisted(itemId: String): Boolean
}
