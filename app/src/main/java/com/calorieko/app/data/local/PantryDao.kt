package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calorieko.app.data.model.DishIngredient
import com.calorieko.app.data.model.PantryItem
import kotlinx.coroutines.flow.Flow

/**
 * Data class for the raw SQL result used by the Cosine Similarity engine.
 * Each row = one dish with its total ingredient count and how many match the pantry.
 */
data class DishMatchInfo(
    val dish_label: String,
    val total_ingredients: Int,
    val matched_count: Int
)

@Dao
interface PantryDao {

    // --- Pantry CRUD ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: PantryItem)

    @Query("DELETE FROM PANTRY_TABLE WHERE ingredient_name = :name")
    suspend fun deleteItem(name: String)

    @Query("SELECT ingredient_name FROM PANTRY_TABLE ORDER BY ingredient_name ASC")
    fun getAllItems(): Flow<List<String>>

    // --- Dish Ingredients Seeding ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDishIngredients(items: List<DishIngredient>)

    // --- Autocomplete ---

    @Query("SELECT DISTINCT ingredient_name FROM DISH_INGREDIENTS_TABLE ORDER BY ingredient_name ASC")
    suspend fun getAllUniqueIngredients(): List<String>

    // --- Cosine Similarity Support ---

    /**
     * Returns the total ingredient count and matched count for every dish.
     * The matched_count uses a conditional SUM: counts ingredients that appear in the user's pantry list.
     *
     * Room's @Query with IN (:list) generates the proper SQL for variable-length lists.
     */
    @Query("""
        SELECT 
            d.dish_label,
            COUNT(*) AS total_ingredients,
            SUM(CASE WHEN d.ingredient_name IN (:pantryItems) THEN 1 ELSE 0 END) AS matched_count
        FROM DISH_INGREDIENTS_TABLE d
        GROUP BY d.dish_label
        HAVING matched_count > 0
    """)
    suspend fun getDishMatchCounts(pantryItems: List<String>): List<DishMatchInfo>

    /**
     * Returns the ingredient names for a dish that are NOT in the user's pantry.
     */
    @Query("""
        SELECT ingredient_name 
        FROM DISH_INGREDIENTS_TABLE 
        WHERE dish_label = :dishLabel AND ingredient_name NOT IN (:pantryItems)
    """)
    suspend fun getMissingIngredients(dishLabel: String, pantryItems: List<String>): List<String>

    /**
     * Returns all ingredient names for a given dish.
     */
    @Query("SELECT ingredient_name FROM DISH_INGREDIENTS_TABLE WHERE dish_label = :dishLabel")
    suspend fun getIngredientsForDish(dishLabel: String): List<String>
}
