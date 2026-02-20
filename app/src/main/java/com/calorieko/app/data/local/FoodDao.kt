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

    @Query("SELECT * FROM FOOD_TABLE")
    suspend fun getAllFoods(): List<FoodItem>
}
