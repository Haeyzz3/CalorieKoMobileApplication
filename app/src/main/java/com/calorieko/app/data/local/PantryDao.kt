package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calorieko.app.data.model.DishIngredient
import com.calorieko.app.data.model.PantryItem
import kotlinx.coroutines.flow.Flow

/**
 * Data class for the raw SQL result used by the recipe matching engine.
 * Each row = one dish with its total/core ingredient counts and how many match the pantry.
 */
data class DishMatchInfo(
    val dish_label: String,
    val total_ingredients: Int,
    val matched_count: Int,
    val core_total: Int,
    val core_matched: Int
)

/**
 * Lightweight pair of ingredient name + type, used when listing missing ingredients.
 */
data class IngredientWithType(
    val ingredient_name: String,
    val ingredient_type: String
)

/**
 * Lightweight pair of ingredient name + category, used for pantry UI grouping.
 */
data class IngredientWithCategory(
    val ingredient_name: String,
    val ingredient_category: String
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

    /** One-shot fetch of all pantry item names (for cloud sync). */
    @Query("SELECT ingredient_name FROM PANTRY_TABLE ORDER BY ingredient_name ASC")
    suspend fun getAllItemsList(): List<String>

    // --- Dish Ingredients Seeding ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDishIngredients(items: List<DishIngredient>)

    // --- Autocomplete ---

    @Query("SELECT DISTINCT ingredient_name FROM DISH_INGREDIENTS_TABLE ORDER BY ingredient_name ASC")
    suspend fun getAllUniqueIngredients(): List<String>

    // --- Core-Aware Recipe Matching ---

    /**
     * Returns the total/core ingredient counts and matched counts for every dish.
     * A dish is included if it has at least 1 core ingredient matched.
     *
     * - total_ingredients: all ingredients for the dish
     * - matched_count: how many of the dish's ingredients are in the user's pantry
     * - core_total: how many core ingredients the dish has
     * - core_matched: how many core ingredients match the user's pantry
     */
    @Query("""
        SELECT 
            d.dish_label,
            COUNT(*) AS total_ingredients,
            SUM(CASE WHEN d.ingredient_name IN (:pantryItems) THEN 1 ELSE 0 END) AS matched_count,
            SUM(CASE WHEN d.ingredient_type = 'core' THEN 1 ELSE 0 END) AS core_total,
            SUM(CASE WHEN d.ingredient_type = 'core' AND d.ingredient_name IN (:pantryItems) THEN 1 ELSE 0 END) AS core_matched
        FROM DISH_INGREDIENTS_TABLE d
        GROUP BY d.dish_label
        HAVING core_matched > 0
    """)
    suspend fun getDishMatchCounts(pantryItems: List<String>): List<DishMatchInfo>

    /**
     * Returns the ingredient names and types for a dish that are NOT in the user's pantry.
     */
    @Query("""
        SELECT ingredient_name, ingredient_type 
        FROM DISH_INGREDIENTS_TABLE 
        WHERE dish_label = :dishLabel AND ingredient_name NOT IN (:pantryItems)
    """)
    suspend fun getMissingIngredients(dishLabel: String, pantryItems: List<String>): List<IngredientWithType>

    /**
     * Returns all ingredient names for a given dish.
     */
    @Query("SELECT ingredient_name FROM DISH_INGREDIENTS_TABLE WHERE dish_label = :dishLabel")
    suspend fun getIngredientsForDish(dishLabel: String): List<String>

    /**
     * Returns the ingredient type (core/optional) for a specific ingredient in a specific dish.
     */
    @Query("SELECT ingredient_type FROM DISH_INGREDIENTS_TABLE WHERE dish_label = :dishLabel AND ingredient_name = :ingredientName LIMIT 1")
    suspend fun getIngredientType(dishLabel: String, ingredientName: String): String?

    /**
     * Returns categories for a list of ingredient names.
     * Used by the ViewModel to group pantry items by category in the UI.
     * Returns DISTINCT rows since the same ingredient may appear in multiple dishes.
     */
    @Query("""
        SELECT DISTINCT ingredient_name, ingredient_category 
        FROM DISH_INGREDIENTS_TABLE 
        WHERE ingredient_name IN (:ingredientNames)
    """)
    suspend fun getCategoriesForIngredients(ingredientNames: List<String>): List<IngredientWithCategory>

    /**
     * Returns the total row count of the dish ingredients table.
     * Used by FoodDatabaseCallback.onOpen() to check if seeding is needed.
     */
    @Query("SELECT COUNT(*) FROM DISH_INGREDIENTS_TABLE")
    suspend fun getDishIngredientCount(): Int
}
