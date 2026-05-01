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

    /**
     * Returns all raw ingredients in the same [subCategory], excluding
     * the ingredient with [excludeKey] and any non-substitutable ingredients.
     * Used for the substitution picker.
     */
    @Query("""
        SELECT * FROM RAW_INGREDIENTS_TABLE 
        WHERE sub_category = :subCategory 
          AND ingredient_key != :excludeKey 
          AND is_substitutable = 1
        ORDER BY display_name ASC
    """)
    suspend fun getSubstituteCandidates(
        subCategory: String,
        excludeKey: String
    ): List<RawIngredientEntity>

    /**
     * Returns all ingredients except store_bought (synthetic entries).
     * Used by the Ingredient Browser to show only user-addable items.
     */
    @Query("SELECT * FROM RAW_INGREDIENTS_TABLE WHERE category != 'store_bought' ORDER BY display_name ASC")
    suspend fun getAllBrowsable(): List<RawIngredientEntity>

    /**
     * Returns ingredient_key and category for a list of keys.
     * Used by the pantry to group items by their authoritative category.
     */
    @Query("SELECT ingredient_key, category FROM RAW_INGREDIENTS_TABLE WHERE ingredient_key IN (:keys)")
    suspend fun getCategoriesForKeys(keys: List<String>): List<IngredientKeyCategory>

    /** Clears all rows. Used before re-seeding from JSON. */
    @Query("DELETE FROM RAW_INGREDIENTS_TABLE")
    suspend fun deleteAll()
}

/**
 * Lightweight pair of ingredient_key + category, used for pantry UI grouping.
 */
data class IngredientKeyCategory(
    val ingredient_key: String,
    val category: String
)
