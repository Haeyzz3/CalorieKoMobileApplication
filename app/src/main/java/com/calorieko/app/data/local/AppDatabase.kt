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
    version = 13,
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

        /**
         * Migration 11 → 12: Adds `meal_slot` column to PLANNED_MEALS_TABLE
         * and expands the composite PK to (day_index, week_start_date, meal_slot).
         *
         * SQLite does not support ALTER TABLE ADD PRIMARY KEY, so we recreate
         * the table. Existing meals default to "Lunch".
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS PLANNED_MEALS_TABLE_new (
                        day_index INTEGER NOT NULL,
                        dish_label TEXT NOT NULL,
                        week_start_date TEXT NOT NULL,
                        meal_slot TEXT NOT NULL DEFAULT 'Lunch',
                        PRIMARY KEY(day_index, week_start_date, meal_slot)
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO PLANNED_MEALS_TABLE_new (day_index, dish_label, week_start_date, meal_slot)
                    SELECT day_index, dish_label, week_start_date, 'Lunch'
                    FROM PLANNED_MEALS_TABLE
                """.trimIndent())
                db.execSQL("DROP TABLE PLANNED_MEALS_TABLE")
                db.execSQL("ALTER TABLE PLANNED_MEALS_TABLE_new RENAME TO PLANNED_MEALS_TABLE")
            }
        }

        /**
         * Migration 12 → 13: Drops and recreates DISH_INGREDIENTS_TABLE with
         * two new columns: `ingredient_type` and `ingredient_category`.
         *
         * We drop-and-recreate instead of ALTER TABLE because every existing
         * row needs the classification data from the updated CSV.
         * FoodDatabaseCallback.onOpen() detects count == 0 and re-seeds
         * automatically from dish_ingredients.csv.
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS DISH_INGREDIENTS_TABLE")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS DISH_INGREDIENTS_TABLE (
                        dish_label TEXT NOT NULL,
                        ingredient_name TEXT NOT NULL,
                        ingredient_type TEXT NOT NULL DEFAULT 'core',
                        ingredient_category TEXT NOT NULL DEFAULT 'pantry_staple',
                        PRIMARY KEY(dish_label, ingredient_name)
                    )
                """.trimIndent())
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
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    // Fallback only if no migration path exists (e.g. dev builds)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}