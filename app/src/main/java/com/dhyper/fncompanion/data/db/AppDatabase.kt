package com.dhyper.fncompanion.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AuthEntity::class, RecentSearchEntity::class, WishlistEntity::class, SettingsEntity::class],
    version = 13,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun authDao(): AuthDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN lastVBucksMissionId TEXT")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Remove past_seasons table
                db.execSQL("DROP TABLE IF EXISTS past_seasons")
                
                // Recreate auth_session without the career columns to stop caching live data
                db.execSQL("""
                    CREATE TABLE auth_session_new (
                        accountId TEXT NOT NULL PRIMARY KEY,
                        displayName TEXT NOT NULL,
                        accessToken TEXT NOT NULL,
                        refreshToken TEXT,
                        expiresAtMs INTEGER NOT NULL,
                        deviceId TEXT,
                        deviceSecret TEXT,
                        loginTimeMs INTEGER NOT NULL,
                        lastRefreshTimeMs INTEGER NOT NULL,
                        isActive INTEGER NOT NULL,
                        equippedSkinIcon TEXT,
                        deviceAuthStatus TEXT NOT NULL DEFAULT 'VALID'
                    )
                """.trimIndent())
                
                db.execSQL("""
                    INSERT INTO auth_session_new (
                        accountId, displayName, accessToken, refreshToken, expiresAtMs,
                        deviceId, deviceSecret, loginTimeMs, lastRefreshTimeMs, isActive,
                        equippedSkinIcon, deviceAuthStatus
                    )
                    SELECT 
                        accountId, displayName, accessToken, refreshToken, expiresAtMs,
                        deviceId, deviceSecret, loginTimeMs, lastRefreshTimeMs, isActive,
                        equippedSkinIcon, deviceAuthStatus
                    FROM auth_session
                """.trimIndent())
                
                db.execSQL("DROP TABLE auth_session")
                db.execSQL("ALTER TABLE auth_session_new RENAME TO auth_session")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN dataSaverMode INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Use a more robust approach to add columns if they don't exist
                val cursor = db.query("PRAGMA table_info(auth_session)")
                var hasRecycle = false
                var hasClaim = false
                while (cursor.moveToNext()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    if (name == "stwAutoRecycleJunk") hasRecycle = true
                    if (name == "stwAutoClaimLlamas") hasClaim = true
                }
                cursor.close()

                if (!hasRecycle) {
                    db.execSQL("ALTER TABLE auth_session ADD COLUMN stwAutoRecycleJunk INTEGER NOT NULL DEFAULT 0")
                }
                if (!hasClaim) {
                    db.execSQL("ALTER TABLE auth_session ADD COLUMN stwAutoClaimLlamas INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rename vbucksAlertTime to shopRefreshTime and add new STW time columns
                db.execSQL("ALTER TABLE app_settings RENAME COLUMN vbucksAlertTime TO shopRefreshTime")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN stwVBucksAlertTime TEXT NOT NULL DEFAULT '00:00'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN stwAutomationTime TEXT NOT NULL DEFAULT '00:00'")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE auth_session ADD COLUMN isFounder INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fortnite_companion.db"
                )
                    .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    // Prevents crashes on downgrade by dropping and recreating tables.
                    // For upgrades, we no longer use fallbackToDestructiveMigration(), 
                    // ensuring migrations MUST be provided to preserve user data.
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
