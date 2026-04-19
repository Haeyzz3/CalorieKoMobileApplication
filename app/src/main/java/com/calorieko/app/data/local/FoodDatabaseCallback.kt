package com.calorieko.app.data.local

import android.content.Context
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

    // NOTE: We intentionally do NOT override onCreate() for seeding.
    // Room always calls onOpen() after onCreate(), so the onOpen() empty-check
    // handles fresh installs, migrations, and normal opens — all from one path.
    // Having both onCreate and onOpen seed caused a race condition where two
    // async coroutines would interleave, doubling the dish count.

    /**
     * Runs on every database open. Checks if the dish ingredients table or
     * food table is empty (e.g. fresh install or schema migration that drops
     * and recreates them). If either is empty, seeds both from the CSV assets.
     */
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        databaseProvider().let { database ->
            scope.launch(Dispatchers.IO) {
                val dishCount = database.pantryDao().getDishIngredientCount()
                val foodCount = database.foodDao().getAllFoods().size
                if (dishCount == 0 || foodCount == 0) {
                    populateDatabase(context, database.foodDao(), database.pantryDao())
                }
            }
        }
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
    val dishCount = pantryDao.getDishIngredientCount()
    val foodCount = foodDao.getAllFoods().size
    if (dishCount == 0 || foodCount == 0) {
        populateDatabase(context, foodDao, pantryDao)
    }
}