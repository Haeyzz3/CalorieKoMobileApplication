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
import com.calorieko.app.data.model.DishRecipeEntity
import com.calorieko.app.data.model.FoodItem
import com.calorieko.app.data.model.MealLogEntity
import com.calorieko.app.data.model.MealLogItemEntity
import com.calorieko.app.data.model.PantryItem
import com.calorieko.app.data.model.PlannedMealEntity
import com.calorieko.app.data.model.RawIngredientEntity
import com.calorieko.app.data.model.RecipeIngredientEntity
import com.calorieko.app.data.model.UserProfile
import com.calorieko.app.data.model.WeightLogEntity
import kotlinx.coroutines.CoroutineScope

// INCREMENT version from 18 to 19 — adds raw ingredient, dish recipe, and recipe ingredient tables
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
        PlannedMealEntity::class,
        RawIngredientEntity::class,
        DishRecipeEntity::class,
        RecipeIngredientEntity::class,
        WeightLogEntity::class
    ],
    version = 27,
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
    abstract fun rawIngredientDao(): RawIngredientDao
    abstract fun dishRecipeDao(): DishRecipeDao
    abstract fun recipeIngredientDao(): RecipeIngredientDao
    abstract fun weightLogDao(): WeightLogDao

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

        /**
         * Migration 13 → 14: Expands PLANNED_MEALS_TABLE composite PK
         * from (day_index, week_start_date, meal_slot) to
         * (day_index, week_start_date, meal_slot, dish_label).
         *
         * This enables multiple dishes per meal slot per day.
         * SQLite requires table recreation to change primary keys.
         * Existing data is preserved as-is (each old row becomes one dish in its slot).
         */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS PLANNED_MEALS_TABLE_new (
                        day_index INTEGER NOT NULL,
                        dish_label TEXT NOT NULL,
                        week_start_date TEXT NOT NULL,
                        meal_slot TEXT NOT NULL DEFAULT 'Lunch',
                        PRIMARY KEY(day_index, week_start_date, meal_slot, dish_label)
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO PLANNED_MEALS_TABLE_new (day_index, dish_label, week_start_date, meal_slot)
                    SELECT day_index, dish_label, week_start_date, meal_slot
                    FROM PLANNED_MEALS_TABLE
                """.trimIndent())
                db.execSQL("DROP TABLE PLANNED_MEALS_TABLE")
                db.execSQL("ALTER TABLE PLANNED_MEALS_TABLE_new RENAME TO PLANNED_MEALS_TABLE")
            }
        }

        /**
         * Migration 14 → 15: Adds globalXp and milestonesTier columns to user_profile.
         *
         * These support the gamified badge leveling system (Epic 1).
         * Existing users start at tier 1 and XP 0.
         */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN globalXp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN milestonesTier INTEGER NOT NULL DEFAULT 1")
            }
        }

        /**
         * Migration 15 → 16: Drops and recreates DISH_INGREDIENTS_TABLE with
         * three new columns: `portion_quantity`, `preparation_method`, and `step`.
         * The PK is expanded to (dish_label, ingredient_name, step) to allow
         * the same ingredient to appear multiple times in a single dish.
         *
         * FoodDatabaseCallback.onOpen() detects count == 0 and re-seeds
         * automatically from the updated dish_ingredients.csv.
         */
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS DISH_INGREDIENTS_TABLE")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS DISH_INGREDIENTS_TABLE (
                        dish_label TEXT NOT NULL,
                        ingredient_name TEXT NOT NULL,
                        ingredient_type TEXT NOT NULL DEFAULT 'core',
                        ingredient_category TEXT NOT NULL DEFAULT 'pantry_staple',
                        portion_quantity TEXT NOT NULL DEFAULT '',
                        preparation_method TEXT NOT NULL DEFAULT '',
                        step INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(dish_label, ingredient_name, step)
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration 16 → 17: Adds `data_source` column to FOOD_TABLE for
         * nutritional data source attribution (DOST FNRI / USDA).
         *
         * Drops and recreates both FOOD_TABLE and DISH_INGREDIENTS_TABLE so
         * that FoodDatabaseCallback.onOpen() re-seeds from the updated CSVs.
         */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop both tables so the callback re-seeds with fresh CSV data
                db.execSQL("DROP TABLE IF EXISTS FOOD_TABLE")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS FOOD_TABLE (
                        food_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name_en TEXT NOT NULL,
                        name_ph TEXT NOT NULL,
                        category TEXT NOT NULL,
                        ml_label TEXT NOT NULL,
                        calories_per_100g REAL NOT NULL DEFAULT 0,
                        protein_per_100g REAL NOT NULL DEFAULT 0,
                        carbs_per_100g REAL NOT NULL DEFAULT 0,
                        fiber_per_100g REAL NOT NULL DEFAULT 0,
                        sugar_per_100g REAL NOT NULL DEFAULT 0,
                        fat_per_100g REAL NOT NULL DEFAULT 0,
                        saturated_fat_per_100g REAL NOT NULL DEFAULT 0,
                        polyunsaturated_fat_per_100g REAL NOT NULL DEFAULT 0,
                        monounsaturated_fat_per_100g REAL NOT NULL DEFAULT 0,
                        trans_fat_per_100g REAL NOT NULL DEFAULT 0,
                        cholesterol_per_100g REAL NOT NULL DEFAULT 0,
                        sodium_per_100g REAL NOT NULL DEFAULT 0,
                        potassium_per_100g REAL NOT NULL DEFAULT 0,
                        vitamin_a_per_100g REAL NOT NULL DEFAULT 0,
                        vitamin_c_per_100g REAL NOT NULL DEFAULT 0,
                        calcium_per_100g REAL NOT NULL DEFAULT 0,
                        iron_per_100g REAL NOT NULL DEFAULT 0,
                        data_source TEXT NOT NULL DEFAULT 'DOST_FNRI_MENU_GUIDE'
                    )
                """.trimIndent())

                // Also reset dish ingredients so both tables re-seed together
                db.execSQL("DROP TABLE IF EXISTS DISH_INGREDIENTS_TABLE")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS DISH_INGREDIENTS_TABLE (
                        dish_label TEXT NOT NULL,
                        ingredient_name TEXT NOT NULL,
                        ingredient_type TEXT NOT NULL DEFAULT 'core',
                        ingredient_category TEXT NOT NULL DEFAULT 'pantry_staple',
                        portion_quantity TEXT NOT NULL DEFAULT '',
                        preparation_method TEXT NOT NULL DEFAULT '',
                        step INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(dish_label, ingredient_name, step)
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration 17 → 18: Adds `sync_status` column to meal_log_table.
         *
         * Mirrors the existing sync_status on activity_log_table (added in Migration 10→11).
         * Default is 0 (PENDING) so that all pre-existing meal logs will be picked up
         * by the SyncWorker on its next run. Firestore uses set() which is idempotent,
         * so re-pushing already-synced meals is safe (just overwrites with same data).
         */
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE meal_log_table ADD COLUMN sync_status INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Migration 18 → 19: Creates 3 new tables for the raw-ingredient
         * nutritional engine (Phase 2 of the refactor).
         *
         * This is purely additive — no existing tables are modified or dropped.
         * The new tables are seeded from JSON assets by FoodDatabaseCallback.onOpen().
         */
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Raw ingredients table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS RAW_INGREDIENTS_TABLE (
                        ingredient_key TEXT NOT NULL PRIMARY KEY,
                        display_name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        sub_category TEXT NOT NULL,
                        fdc_id INTEGER NOT NULL,
                        data_source TEXT NOT NULL,
                        calories REAL NOT NULL DEFAULT 0,
                        protein REAL NOT NULL DEFAULT 0,
                        carbs REAL NOT NULL DEFAULT 0,
                        fat REAL NOT NULL DEFAULT 0,
                        fiber REAL NOT NULL DEFAULT 0,
                        sugar REAL NOT NULL DEFAULT 0,
                        sodium REAL NOT NULL DEFAULT 0,
                        potassium REAL NOT NULL DEFAULT 0,
                        vitamin_a REAL NOT NULL DEFAULT 0,
                        vitamin_c REAL NOT NULL DEFAULT 0,
                        calcium REAL NOT NULL DEFAULT 0,
                        iron REAL NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // 2. Dish recipes table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS DISH_RECIPES_TABLE (
                        dish_label TEXT NOT NULL PRIMARY KEY,
                        name_en TEXT NOT NULL,
                        name_ph TEXT NOT NULL,
                        category TEXT NOT NULL,
                        cooking_method TEXT NOT NULL,
                        servings INTEGER NOT NULL,
                        total_raw_weight_g REAL NOT NULL,
                        dish_yield_factor REAL NOT NULL,
                        cooked_weight_g REAL NOT NULL,
                        per_serving_weight_g REAL NOT NULL,
                        ingredient_count INTEGER NOT NULL,
                        cal_per_serving REAL NOT NULL DEFAULT 0,
                        protein_per_serving REAL NOT NULL DEFAULT 0,
                        carbs_per_serving REAL NOT NULL DEFAULT 0,
                        fat_per_serving REAL NOT NULL DEFAULT 0,
                        fiber_per_serving REAL NOT NULL DEFAULT 0,
                        sugar_per_serving REAL NOT NULL DEFAULT 0,
                        sodium_per_serving REAL NOT NULL DEFAULT 0,
                        potassium_per_serving REAL NOT NULL DEFAULT 0,
                        vitamin_a_per_serving REAL NOT NULL DEFAULT 0,
                        vitamin_c_per_serving REAL NOT NULL DEFAULT 0,
                        calcium_per_serving REAL NOT NULL DEFAULT 0,
                        iron_per_serving REAL NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // 3. Recipe ingredients table (with FKs and indices)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS RECIPE_INGREDIENTS_TABLE (
                        dish_label TEXT NOT NULL,
                        ingredient_key TEXT NOT NULL,
                        ingredient_type TEXT NOT NULL,
                        ingredient_category TEXT NOT NULL,
                        raw_weight_grams REAL NOT NULL,
                        portion_original TEXT NOT NULL,
                        preparation_method TEXT NOT NULL,
                        step INTEGER NOT NULL,
                        PRIMARY KEY(dish_label, ingredient_key, step),
                        FOREIGN KEY(dish_label) REFERENCES DISH_RECIPES_TABLE(dish_label) ON DELETE CASCADE,
                        FOREIGN KEY(ingredient_key) REFERENCES RAW_INGREDIENTS_TABLE(ingredient_key) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_RECIPE_INGREDIENTS_TABLE_dish_label ON RECIPE_INGREDIENTS_TABLE(dish_label)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_RECIPE_INGREDIENTS_TABLE_ingredient_key ON RECIPE_INGREDIENTS_TABLE(ingredient_key)")
            }
        }

        /**
         * Migration 19 → 20: Adds `steps` column to activity_log_table.
         */
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE activity_log_table ADD COLUMN steps INTEGER DEFAULT NULL"
                )
            }
        }

        /**
         * Migration 20 → 21: Adds `substitutions_json` column to PLANNED_MEALS_TABLE.
         *
         * Persists ingredient substitutions as a JSON string alongside the planned meal.
         * Existing rows get an empty string default (no substitutions).
         */
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE PLANNED_MEALS_TABLE ADD COLUMN substitutions_json TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        /**
         * Migration 21 → 22: Adds `is_substitutable` column to RAW_INGREDIENTS_TABLE.
         *
         * Controls whether an ingredient appears as a substitution candidate.
         * Default is 1 (true / substitutable). Non-substitutable ingredients
         * (water, food coloring, store-bought items) are updated via JSON re-seed.
         */
        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE RAW_INGREDIENTS_TABLE ADD COLUMN is_substitutable INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        /**
         * Migration 22 → 23: Adds `onboarding_completed` column to user_profile.
         *
         * Tracks if the user has completed the initial onboarding (Target Summary, BLE).
         * Existing users are assumed to have completed onboarding.
         */
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN onboarding_completed INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        /**
         * Migration 23 -> 24: Adds dated weight logs.
         *
         * A profile row only stores the current weight, which made charts rewrite
         * every past day with today's value. This table stores actual measurement
         * dates so progress charts can render real history.
         */
        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                val todayEpochDay = java.time.LocalDate.now().toEpochDay()

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS weight_log_table (
                        uid TEXT NOT NULL,
                        date_epoch_day INTEGER NOT NULL,
                        weight_kg REAL NOT NULL,
                        timestamp INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        sync_status INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(uid, date_epoch_day)
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT OR IGNORE INTO weight_log_table (
                        uid,
                        date_epoch_day,
                        weight_kg,
                        timestamp,
                        updated_at,
                        sync_status
                    )
                    SELECT
                        uid,
                        $todayEpochDay,
                        weight,
                        $now,
                        updated_at,
                        0
                    FROM user_profile
                    WHERE weight > 0
                """.trimIndent())
            }
        }

        /**
         * Migration 24 → 25: Adds `serving_size_description` column to DISH_RECIPES_TABLE.
         *
         * Stores the FNRI serving size text (e.g., "1 1/2 cups") for UI display.
         * Actual nutrition calculations use only the servings count, not this field.
         */
        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE DISH_RECIPES_TABLE ADD COLUMN serving_size_description TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        /**
         * Migration 25 → 26: Adds `nutrient_proxy_note` column to RAW_INGREDIENTS_TABLE.
         *
         * Stores a human-readable transparency note for ingredients whose
         * nutritional data comes from a proxy source (e.g., "Nutritional
         * values approximated from Watercress"). Empty for direct USDA matches.
         */
        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE RAW_INGREDIENTS_TABLE ADD COLUMN nutrient_proxy_note TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        /**
         * Migration 26 -> 27: Make weight logs append-only.
         *
         * The old primary key was (uid, date_epoch_day), so saving a new weight on
         * an existing date replaced the previous measurement. Timestamp is now part
         * of the identity so the progress screen can show real history.
         */
        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS weight_log_table_new (
                        uid TEXT NOT NULL,
                        date_epoch_day INTEGER NOT NULL,
                        weight_kg REAL NOT NULL,
                        timestamp INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        sync_status INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(uid, timestamp)
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT OR IGNORE INTO weight_log_table_new (
                        uid,
                        date_epoch_day,
                        weight_kg,
                        timestamp,
                        updated_at,
                        sync_status
                    )
                    SELECT
                        uid,
                        date_epoch_day,
                        weight_kg,
                        timestamp,
                        updated_at,
                        sync_status
                    FROM weight_log_table
                """.trimIndent())

                db.execSQL("DROP TABLE weight_log_table")
                db.execSQL("ALTER TABLE weight_log_table_new RENAME TO weight_log_table")
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
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27)
                    // Fallback only if no migration path exists (e.g. dev builds)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
