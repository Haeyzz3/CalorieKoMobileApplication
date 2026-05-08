package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calorieko.app.data.model.DishRecipeEntity

@Dao
interface DishRecipeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dishes: List<DishRecipeEntity>)

    @Query("SELECT * FROM DISH_RECIPES_TABLE WHERE dish_label = :dishLabel LIMIT 1")
    suspend fun getByDishLabel(dishLabel: String): DishRecipeEntity?

    @Query("SELECT * FROM DISH_RECIPES_TABLE ORDER BY name_en ASC")
    suspend fun getAllDishRecipes(): List<DishRecipeEntity>

    @Query("SELECT * FROM DISH_RECIPES_TABLE WHERE category = :category ORDER BY name_en ASC")
    suspend fun getByCategory(category: String): List<DishRecipeEntity>

    @Query("""
        SELECT * FROM DISH_RECIPES_TABLE 
        WHERE name_en LIKE '%' || :query || '%' 
           OR name_ph LIKE '%' || :query || '%'
        ORDER BY name_en ASC
    """)
    suspend fun searchByName(query: String): List<DishRecipeEntity>

    @Query("SELECT COUNT(*) FROM DISH_RECIPES_TABLE")
    suspend fun getCount(): Int

    /** Returns dishes with no ingredients (store-bought items like Lechon Manok). */
    @Query("SELECT * FROM DISH_RECIPES_TABLE WHERE ingredient_count = 0 ORDER BY name_en ASC")
    suspend fun getStoreBoughtDishes(): List<DishRecipeEntity>

    /** Clears all rows. Used before re-seeding from JSON. */
    @Query("DELETE FROM DISH_RECIPES_TABLE")
    suspend fun deleteAll()
}
