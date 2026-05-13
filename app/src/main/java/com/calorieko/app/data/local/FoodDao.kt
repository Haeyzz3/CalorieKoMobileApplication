package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    /** Deletes all foods whose ml_label is in the given list. */
    @Query("DELETE FROM FOOD_TABLE WHERE ml_label IN (:labels)")
    suspend fun deleteByMlLabels(labels: List<String>)

    /** Returns all ml_label values currently in FOOD_TABLE. */
    @Query("SELECT ml_label FROM FOOD_TABLE")
    suspend fun getAllMlLabels(): List<String>

    /**
     * Syncs food items from the admin server using ml_label as the identity key.
     *
     * Protection: USDA-verified dishes (those with a DishRecipeEntity in System B)
     * are NEVER overwritten. The caller must pass [protectedLabels] — the set of
     * ml_labels that exist in DISH_RECIPES_TABLE. Server foods matching these labels
     * are silently skipped.
     *
     * For admin-added dishes (not in System B): deletes existing rows matching
     * the server labels, then inserts the server data. This avoids the autoGenerate
     * ID collision issue.
     */
    @Transaction
    suspend fun syncFromServer(serverFoods: List<FoodItem>, protectedLabels: Set<String>) {
        // Filter out USDA-verified dishes — never overwrite them
        val adminOnlyFoods = serverFoods.filter { it.mlLabel !in protectedLabels }
        if (adminOnlyFoods.isEmpty()) return

        val adminLabels = adminOnlyFoods.map { it.mlLabel }
        deleteByMlLabels(adminLabels)
        insertAll(adminOnlyFoods)
    }
}