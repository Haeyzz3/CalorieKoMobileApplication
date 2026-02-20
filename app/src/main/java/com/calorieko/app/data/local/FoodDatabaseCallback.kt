package com.calorieko.app.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.calorieko.app.data.model.FoodItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FoodDatabaseCallback(
    private val scope: CoroutineScope
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // We can't access AppDatabase.INSTANCE safely here if it's a top-level class.
        // Usually you'd use a Provider or handle this in the Application class.
    }
}

suspend fun populateDatabase(foodDao: FoodDao) {
    val testDishes = listOf(
        FoodItem(nameEn = "Fried Egg", namePh = "Pritong Itlog", caloriesPer100g = 196f, sodiumPer100g = 124f, category = "Protein"),
        FoodItem(nameEn = "Boiled White Rice", namePh = "Kanin", caloriesPer100g = 130f, sodiumPer100g = 1f, category = "Carbohydrates"),
        FoodItem(nameEn = "Chicken Adobo", namePh = "Adobong Manok", caloriesPer100g = 250f, sodiumPer100g = 600f, category = "Main Dish"),
        FoodItem(nameEn = "Pork Sinigang", namePh = "Sinigang na Baboy", caloriesPer100g = 120f, sodiumPer100g = 400f, category = "Soup"),
        FoodItem(nameEn = "Pandesal", namePh = "Pandesal", caloriesPer100g = 300f, sodiumPer100g = 450f, category = "Bread")
    )
    foodDao.insertAll(testDishes)
}
