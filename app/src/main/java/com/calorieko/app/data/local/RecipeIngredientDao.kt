package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calorieko.app.data.model.RecipeIngredientEntity

@Dao
interface RecipeIngredientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RecipeIngredientEntity>)

    /**
     * Returns all recipe ingredients for a given dish, ordered by step.
     */
    @Query("""
        SELECT * FROM RECIPE_INGREDIENTS_TABLE 
        WHERE dish_label = :dishLabel 
        ORDER BY step ASC
    """)
    suspend fun getIngredientsForDish(dishLabel: String): List<RecipeIngredientEntity>

    /**
     * Returns only the core ingredients for a given dish.
     */
    @Query("""
        SELECT * FROM RECIPE_INGREDIENTS_TABLE 
        WHERE dish_label = :dishLabel AND ingredient_type = 'core'
        ORDER BY step ASC
    """)
    suspend fun getCoreIngredientsForDish(dishLabel: String): List<RecipeIngredientEntity>

    @Query("SELECT COUNT(*) FROM RECIPE_INGREDIENTS_TABLE")
    suspend fun getCount(): Int

    /** Clears all rows. Used before re-seeding from JSON. */
    @Query("DELETE FROM RECIPE_INGREDIENTS_TABLE")
    suspend fun deleteAll()
}
