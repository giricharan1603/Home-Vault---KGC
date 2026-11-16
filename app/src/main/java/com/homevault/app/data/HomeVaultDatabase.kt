package com.homevault.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AssetEntity::class, VaultDocumentEntity::class],
    version = 3,
    exportSchema = false
)
abstract class HomeVaultDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao

    companion object {
        @Volatile
        private var INSTANCE: HomeVaultDatabase? = null

        fun getInstance(context: Context): HomeVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HomeVaultDatabase::class.java,
                    "homevault.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}