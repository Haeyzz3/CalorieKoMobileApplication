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