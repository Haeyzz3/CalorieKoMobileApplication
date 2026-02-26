package com.calorieko.app.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.calorieko.app.data.model.FoodItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FoodDatabaseCallback(
    private val scope: CoroutineScope,
    private val databaseProvider: () -> AppDatabase // Allows safe access to the database instance
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        databaseProvider().let { database ->
            scope.launch(Dispatchers.IO) {
                populateDatabase(database.foodDao())
            }
        }
    }
}

suspend fun populateDatabase(foodDao: FoodDao) {
    // You will manually encode your 20-25 PhilFCT dishes here.
    // I've expanded Chicken Adobo as an example to show how you'll assign the 16 nutrients.
    val dishes = listOf(
        FoodItem(
            nameEn = "Chicken Adobo",
            namePh = "Adobong Manok",
            category = "Main Dish",
            caloriesPer100g = 250f,
            proteinPer100g = 15f,
            carbsPer100g = 10f,
            fatPer100g = 14f,
            sodiumPer100g = 600f,
            // ... add the rest of the PhilFCT values here
        ),
        FoodItem(
            nameEn = "Pork Sinigang",
            namePh = "Sinigang na Baboy",
            category = "Soup",
            caloriesPer100g = 120f,
            sodiumPer100g = 400f
            // Due to default parameters (=0f), any omitted nutrient automatically registers as 0.
            // You can fill them in as you read the PDF.
        )
        // Add the rest of the 20-25 dishes here...
    )

    foodDao.insertAll(dishes)
}