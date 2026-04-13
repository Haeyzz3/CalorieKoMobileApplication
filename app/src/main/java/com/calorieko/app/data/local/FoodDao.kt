package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calorieko.app.data.model.FoodItem

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<FoodItem>): List<Long>

    @Query("SELECT * FROM FOOD_TABLE WHERE name_en = :name OR name_ph = :name LIMIT 1")
    suspend fun getFoodByName(name: String): FoodItem?

    @Query("SELECT * FROM FOOD_TABLE WHERE ml_label = :mlLabel LIMIT 1")
    suspend fun getFoodByMlLabel(mlLabel: String): FoodItem?

    @Query("SELECT * FROM FOOD_TABLE WHERE food_id = :foodId LIMIT 1")
    suspend fun getFoodById(foodId: Int): FoodItem?

    @Query("SELECT * FROM FOOD_TABLE")
    suspend fun getAllFoods(): List<FoodItem>

    @Query("SELECT * FROM FOOD_TABLE WHERE name_en LIKE '%' || :query || '%' OR name_ph LIKE '%' || :query || '%' ORDER BY name_en ASC")
    suspend fun searchFoodsByName(query: String): List<FoodItem>

    /** Clears all rows from FOOD_TABLE. Used before re-seeding from CSV. */
    @Query("DELETE FROM FOOD_TABLE")
    suspend fun deleteAllFoods()
}