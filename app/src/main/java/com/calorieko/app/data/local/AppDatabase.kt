package com.calorieko.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.data.model.DishIngredient
import com.calorieko.app.data.model.FoodItem
import com.calorieko.app.data.model.MealLogEntity
import com.calorieko.app.data.model.MealLogItemEntity
import com.calorieko.app.data.model.PantryItem
import com.calorieko.app.data.model.PlannedMealEntity
import com.calorieko.app.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope

// INCREMENT version from 9 to 10 — adds updated_at column for delta sync
@Database(
    entities = [
        FoodItem::class,
        UserProfile::class,
        ActivityLogEntity::class,
        MealLogEntity::class,
        MealLogItemEntity::class,
        DailyNutritionSummaryEntity::class,
        DishIngredient::class,
        PantryItem::class,
        PlannedMealEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun userDao(): UserDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun mealLogItemDao(): MealLogItemDao
    abstract fun dailyNutritionSummaryDao(): DailyNutritionSummaryDao
    abstract fun pantryDao(): PantryDao
    abstract fun mealPlanDao(): MealPlanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration 9 → 10: Adds `updated_at` column to all sync-enabled tables.
         *
         * Uses ALTER TABLE to preserve existing user data. Default value is set to
         * the current epoch millis so that pre-existing rows participate in the
         * first delta sync (they will all be treated as "modified since epoch 0").
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()

                db.execSQL(
                    "ALTER TABLE activity_log_table ADD COLUMN updated_at INTEGER NOT NULL DEFAULT $now"
                )
                db.execSQL(
                    "ALTER TABLE meal_log_table ADD COLUMN updated_at INTEGER NOT NULL DEFAULT $now"
                )
                db.execSQL(
                    "ALTER TABLE meal_log_item_table ADD COLUMN updated_at INTEGER NOT NULL DEFAULT $now"
                )
                db.execSQL(
                    "ALTER TABLE daily_nutrition_summary_table ADD COLUMN updated_at INTEGER NOT NULL DEFAULT $now"
                )
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN updated_at INTEGER NOT NULL DEFAULT $now"
                )
            }
        }

        /**
         * Migration 10 → 11: Adds `sync_status` column to activity_log_table.
         *
         * Default is 0 (PENDING) so that all pre-existing rows will be picked up
         * by the offline-first SyncWorker on its next run.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE activity_log_table ADD COLUMN sync_status INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calorieko_database"
                )
                    // Pass a lambda providing the INSTANCE to the callback
                    .addCallback(FoodDatabaseCallback(context.applicationContext, scope) { INSTANCE!! })
                    // Register the migration so existing data is preserved
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11)
                    // Fallback only if no migration path exists (e.g. dev builds)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}