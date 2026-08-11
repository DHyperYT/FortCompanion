package com.dhyper.fncompanion.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthDao {
    @Query("SELECT * FROM auth_session WHERE isActive = 1 LIMIT 1")
    fun getAuthSession(): Flow<AuthEntity?>

    @Query("SELECT * FROM auth_session WHERE isActive = 1 LIMIT 1")
    suspend fun getAuthSessionDirect(): AuthEntity?

    @Query("SELECT * FROM auth_session WHERE accountId = :accountId LIMIT 1")
    suspend fun getAccountByIdDirect(accountId: String): AuthEntity?

    @Transaction
    suspend fun saveAuthSession(session: AuthEntity) {
        clearActiveStatus()
        upsertAuthSession(session.copy(isActive = true))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAuthSession(session: AuthEntity)

    @Query("UPDATE auth_session SET isActive = 0")
    suspend fun clearActiveStatus()

    @Query("SELECT * FROM auth_session ORDER BY loginTimeMs DESC")
    fun getAllAccounts(): Flow<List<AuthEntity>>

    @Query("DELETE FROM auth_session WHERE accountId = :accountId")
    suspend fun deleteAccountById(accountId: String)

    @Query("DELETE FROM auth_session")
    suspend fun clearAllSessions()

    @Query("SELECT * FROM recent_player_search ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRecentSearch(search: RecentSearchEntity)

    @Query("DELETE FROM recent_player_search WHERE accountName = :name")
    suspend fun deleteRecentSearch(name: String)
}
