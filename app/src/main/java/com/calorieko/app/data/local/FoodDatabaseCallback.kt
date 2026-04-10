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

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        databaseProvider().let { database ->
            scope.launch(Dispatchers.IO) {
                populateDatabase(context, database.foodDao(), database.pantryDao())
            }
        }
    }

    /**
     * Runs on every database open. Checks if the dish ingredients table or
     * food table is empty (e.g. after a schema migration that drops and
     * recreates them). If either is empty, seeds both from the CSV assets.
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