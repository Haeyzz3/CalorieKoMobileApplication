package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calorieko.app.data.model.RawIngredientEntity

@Dao
interface RawIngredientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ingredients: List<RawIngredientEntity>)

    @Query("SELECT * FROM RAW_INGREDIENTS_TABLE WHERE ingredient_key = :key LIMIT 1")
    suspend fun getByKey(key: String): RawIngredientEntity?

    @Query("SELECT * FROM RAW_INGREDIENTS_TABLE ORDER BY display_name ASC")
    suspend fun getAllRawIngredients(): List<RawIngredientEntity>

    @Query("SELECT * FROM RAW_INGREDIENTS_TABLE WHERE category = :category ORDER BY display_name ASC")
    suspend fun getByCategory(category: String): List<RawIngredientEntity>

    @Query("SELECT COUNT(*) FROM RAW_INGREDIENTS_TABLE")
    suspend fun getCount(): Int

    /** Clears all rows. Used before re-seeding from JSON. */
    @Query("DELETE FROM RAW_INGREDIENTS_TABLE")
    suspend fun deleteAll()
}
