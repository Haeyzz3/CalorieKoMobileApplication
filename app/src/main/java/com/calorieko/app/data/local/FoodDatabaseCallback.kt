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
     * Runs on every database open. Checks if the dish ingredients table
     * was created by a schema migration but never seeded (the onCreate
     * callback only fires on first creation). If empty, seeds both the
     * food table and dish ingredients table from the CSV asset files.
     */
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        databaseProvider().let { database ->
            scope.launch(Dispatchers.IO) {
                val count = database.pantryDao().getDishIngredientCount()
                if (count == 0) {
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