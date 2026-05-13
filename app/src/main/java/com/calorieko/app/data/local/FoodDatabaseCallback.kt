package com.calorieko.app.data.local

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FoodDatabaseCallback(
    private val context: Context,
    private val scope: CoroutineScope,
    private val databaseProvider: () -> AppDatabase // Allows safe access to the database instance
) : RoomDatabase.Callback() {

    private val seedPrefs by lazy {
        context.getSharedPreferences(REFERENCE_DATA_PREFS, Context.MODE_PRIVATE)
    }

    // NOTE: We intentionally do NOT override onCreate() for seeding.
    // Room always calls onOpen() after onCreate(), so the onOpen() empty-check
    // handles fresh installs, migrations, and normal opens — all from one path.
    // Having both onCreate and onOpen seed caused a race condition where two
    // async coroutines would interleave, doubling the dish count.

    /**
     * Runs on every database open. Checks if tables are empty (e.g. fresh install
     * or schema migration) and seeds from assets if needed.
     */
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        databaseProvider().let { database ->
            scope.launch(Dispatchers.IO) {
                // Legacy CSV seeding (kept for backward compatibility)
                // Guard: skip FOOD_TABLE CSV re-seeding if server sync has already run.
                // Once the admin server has pushed food data, it becomes the authority.
                val hasSyncedFromServer = seedPrefs.getBoolean(KEY_FOOD_CATALOG_SYNCED, false)
                val dishCount = database.pantryDao().getDishIngredientCount()
                val foodCount = database.foodDao().getAllFoods().size
                if (!hasSyncedFromServer && (dishCount == 0 || foodCount == 0)) {
                    populateDatabase(context, database.foodDao(), database.pantryDao())
                } else if (dishCount == 0) {
                    // Even after server sync, DISH_INGREDIENTS_TABLE still needs CSV seeding
                    // (it's not synced from server)
                    context.assets.open("dish_ingredients.csv").use { inputStream ->
                        val dishIngredients = FoodCsvParser.parseDishIngredients(inputStream)
                        database.pantryDao().insertAllDishIngredients(dishIngredients)
                    }
                }

                // New JSON seeding for raw ingredient tables (Phase 2)
                // Re-seed if DB is empty, has new ingredients, OR if a previous
                // seed was interrupted (recipe_ingredients is empty while raw isn't)
                val rawCount = database.rawIngredientDao().getCount()
                val recipeCount = database.dishRecipeDao().getCount()
                val recipeIngCount = database.recipeIngredientDao().getCount()
                val seededVersion = seedPrefs.getInt(KEY_JSON_REFERENCE_VERSION, 0)
                val assetIngredients = context.assets.open("raw_ingredients.json").use { stream ->
                    FoodJsonParser.parseRawIngredients(stream)
                }
                val needsReseed = rawCount == 0
                        || recipeCount == 0
                        || rawCount < assetIngredients.size
                        || seededVersion < CURRENT_JSON_REFERENCE_VERSION
                        || recipeIngCount == 0  // Partial seed recovery
                if (needsReseed) {
                    try {
                        seedFromJson(context, database)
                        seedPrefs.edit()
                            .putInt(KEY_JSON_REFERENCE_VERSION, CURRENT_JSON_REFERENCE_VERSION)
                            .apply()
                    } catch (e: Exception) {
                        Log.e("FoodDatabaseCallback", "JSON seeding failed", e)
                    }
                }
            }
        }
    }

    companion object {
        private const val REFERENCE_DATA_PREFS = "reference_data_seed"
        private const val KEY_JSON_REFERENCE_VERSION = "json_reference_version"
        const val KEY_FOOD_CATALOG_SYNCED = "food_catalog_synced"
        private const val CURRENT_JSON_REFERENCE_VERSION = 2
    }
}

suspend fun populateDatabase(context: Context, foodDao: FoodDao, pantryDao: PantryDao) {
    // Clear existing rows first to prevent duplicates.
    // This is necessary because FoodItem uses autoGenerate = true for food_id,
    // so OnConflictStrategy.REPLACE never triggers (each insert gets a new ID).
    // Without this, a race between onCreate() and onOpen() callbacks can
    // cause the CSV data to be inserted twice, doubling the dish count.
    foodDao.deleteAllFoods()
    pantryDao.deleteAllDishIngredients()

    // Seed the food table
    context.assets.open("dish_labels_and_values.csv").use { inputStream ->
        val dishes = FoodCsvParser.parse(inputStream)
        foodDao.insertAll(dishes)
    }

    // Seed the dish ingredients table
    context.assets.open("dish_ingredients.csv").use { inputStream ->
        val dishIngredients = FoodCsvParser.parseDishIngredients(inputStream)
        pantryDao.insertAllDishIngredients(dishIngredients)
    }
}

/**
 * Seeds the 3 new raw-ingredient tables from JSON asset files.
 *
 * Insertion order matters due to foreign key constraints:
 * 1. RAW_INGREDIENTS_TABLE first (parent of RECIPE_INGREDIENTS_TABLE)
 * 2. DISH_RECIPES_TABLE second (parent of RECIPE_INGREDIENTS_TABLE)
 * 3. RECIPE_INGREDIENTS_TABLE last (child with FKs to both parents)
 */
suspend fun seedFromJson(context: Context, database: AppDatabase) {
    Log.d("FoodDatabaseCallback", "Seeding raw ingredient tables from JSON assets...")

    // Clear in reverse FK order (child first, then parents)
    database.recipeIngredientDao().deleteAll()
    database.dishRecipeDao().deleteAll()
    database.rawIngredientDao().deleteAll()

    // 1. Seed raw ingredients
    context.assets.open("raw_ingredients.json").use { inputStream ->
        val ingredients = FoodJsonParser.parseRawIngredients(inputStream)
        database.rawIngredientDao().insertAll(ingredients)
        Log.d("FoodDatabaseCallback", "Seeded ${ingredients.size} raw ingredients")
    }

    // 2. Seed dish recipes
    context.assets.open("dish_recipes.json").use { inputStream ->
        val dishes = FoodJsonParser.parseDishRecipes(inputStream)
        database.dishRecipeDao().insertAll(dishes)
        Log.d("FoodDatabaseCallback", "Seeded ${dishes.size} dish recipes")
    }

    // 3. Seed recipe ingredients (must be after parents are inserted)
    context.assets.open("recipe_ingredients.json").use { inputStream ->
        val recipeIngredients = FoodJsonParser.parseRecipeIngredients(inputStream)
        database.recipeIngredientDao().insertAll(recipeIngredients)
        Log.d("FoodDatabaseCallback", "Seeded ${recipeIngredients.size} recipe ingredients")
    }

    Log.d("FoodDatabaseCallback", "JSON seeding complete.")
}

/**
 * Ensures that FOOD_TABLE and DISH_INGREDIENTS_TABLE are populated.
 *
 * This is a **defense-in-depth** measure that ViewModels can call before
 * querying reference data. It covers edge cases where the async
 * FoodDatabaseCallback.onOpen() re-seed hasn't completed yet (e.g., after
 * wipeAllData() or db.clearAllTables() is called on some code path).
 *
 * Safe to call multiple times — checks row counts before re-seeding.
 */
suspend fun ensureReferenceDataSeeded(context: Context, foodDao: FoodDao, pantryDao: PantryDao) {
    val seedPrefs = context.getSharedPreferences("reference_data_seed", Context.MODE_PRIVATE)
    val hasSyncedFromServer = seedPrefs.getBoolean(FoodDatabaseCallback.KEY_FOOD_CATALOG_SYNCED, false)
    val dishCount = pantryDao.getDishIngredientCount()
    val foodCount = foodDao.getAllFoods().size
    if (!hasSyncedFromServer && (dishCount == 0 || foodCount == 0)) {
        populateDatabase(context, foodDao, pantryDao)
    } else if (dishCount == 0) {
        // DISH_INGREDIENTS_TABLE still needs CSV seeding (not synced from server)
        context.assets.open("dish_ingredients.csv").use { inputStream ->
            val dishIngredients = FoodCsvParser.parseDishIngredients(inputStream)
            pantryDao.insertAllDishIngredients(dishIngredients)
        }
    }
}
