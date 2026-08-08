package com.dhyper.fncompanion.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthDao {
    @Query("SELECT * FROM auth_session WHERE id = 1 LIMIT 1")
    fun getAuthSession(): Flow<AuthEntity?>

    @Query("SELECT * FROM auth_session WHERE id = 1 LIMIT 1")
    suspend fun getAuthSessionDirect(): AuthEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAuthSession(session: AuthEntity)

    @Query("DELETE FROM auth_session")
    suspend fun clearAuthSession()

    @Query("SELECT * FROM recent_player_search ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRecentSearch(search: RecentSearchEntity)

    @Query("DELETE FROM recent_player_search WHERE accountName = :name")
    suspend fun deleteRecentSearch(name: String)

    // Past Seasons
    @Query("SELECT * FROM past_seasons WHERE accountId = :accountId ORDER BY seasonNumber DESC")
    fun getPastSeasons(accountId: String): Flow<List<PastSeasonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePastSeasons(seasons: List<PastSeasonEntity>)

    @Query("DELETE FROM past_seasons WHERE accountId = :accountId")
    suspend fun clearPastSeasons(accountId: String)
}
