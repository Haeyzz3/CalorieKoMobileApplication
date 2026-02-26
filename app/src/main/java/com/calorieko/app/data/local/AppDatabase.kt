package com.calorieko.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.FoodItem
import com.calorieko.app.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope

// INCREMENT version from 3 to 4
@Database(entities = [FoodItem::class, UserProfile::class, ActivityLogEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun userDao(): UserDao
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calorieko_database"
                )
                    // Pass a lambda providing the INSTANCE to the callback
                    .addCallback(FoodDatabaseCallback(scope) { INSTANCE!! })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}