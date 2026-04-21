package com.calorieko.app.data.local

import com.calorieko.app.data.model.DishRecipeEntity
import com.calorieko.app.data.model.NutritionResult
import com.calorieko.app.data.model.RawIngredientEntity
import com.calorieko.app.data.model.RecipeIngredientEntity

/**
 * Calculates dish nutrition using the raw-ingredient summation model.
 *
 * ## Two computation paths
 *
 * **Fast path (default):** Uses pre-computed per-serving nutrients stored in
 * [DishRecipeEntity]. This is what the IoT scale and manual entry flows use.
 *
 * **Dynamic path (substitution):** Sums raw ingredient nutrients from
 * [RawIngredientEntity] × [RecipeIngredientEntity], applying the dish yield
 * factor. Used only when the user substitutes an ingredient.
 *
 * ## Usage
 *
 * ```kotlin
 * val calculator = RecipeNutritionCalculator(
 *     dishRecipeDao = database.dishRecipeDao(),
 *     rawIngredientDao = database.rawIngredientDao(),
 *     recipeIngredientDao = database.recipeIngredientDao()
 * )
 *
 * // IoT scale flow: user weighed 150g of cooked sinigang
 * val portion = calculator.calculatePortionNutrition("sinigang_pork", 150f)
 *
 * // Full batch totals
 * val total = calculator.calculateDishNutrition("sinigang_pork")
 * ```
 */
class RecipeNutritionCalculator(
    private val dishRecipeDao: DishRecipeDao,
    private val rawIngredientDao: RawIngredientDao,
    private val recipeIngredientDao: RecipeIngredientDao
) {

    /**
     * Calculates the nutrients for a specific cooked weight of a dish.
     *
     * This is the **primary method** that replaces the old
     * `food.caloriesPer100g * weight / 100f` formula.
     *
     * Formula:
     * ```
     * portionFraction = cookedWeightGrams / dish.cookedWeightG
     * nutrient = dish.totalNutrientRaw × portionFraction
     * ```
     *
     * In other words: "If the full cooked batch weighs X grams and contains Y kcal,
     * then a portion weighing Z grams contains Y × (Z / X) kcal."
     *
     * @param dishLabel The ML label / dish key (e.g., "sinigang_pork")
     * @param cookedWeightGrams The actual cooked weight from the IoT scale or manual entry
     * @return [NutritionResult] with nutrients for the weighed portion, or [NutritionResult.ZERO] if dish not found
     */
    suspend fun calculatePortionNutrition(
        dishLabel: String,
        cookedWeightGrams: Float
    ): NutritionResult {
        val dish = dishRecipeDao.getByDishLabel(dishLabel)
            ?: return NutritionResult.ZERO

        if (dish.cookedWeightG <= 0f) return NutritionResult.ZERO

        // What fraction of the total cooked batch is this portion?
        val portionFraction = cookedWeightGrams / dish.cookedWeightG

        // Total nutrients for the entire batch = per-serving × servings
        val totalNutrients = perServingToTotal(dish)

        return totalNutrients * portionFraction
    }

    /**
     * Returns the total nutrients for the entire dish batch (all servings).
     *
     * Uses pre-computed per-serving values from [DishRecipeEntity], multiplied
     * by the number of servings.
     *
     * @param dishLabel The ML label / dish key
     * @return [NutritionResult] with total batch nutrients, or [NutritionResult.ZERO] if not found
     */
    suspend fun calculateDishNutrition(dishLabel: String): NutritionResult {
        val dish = dishRecipeDao.getByDishLabel(dishLabel)
            ?: return NutritionResult.ZERO

        return perServingToTotal(dish)
    }

    /**
     * Returns the per-serving nutrients for a dish (1 standard serving).
     *
     * @param dishLabel The ML label / dish key
     * @return [NutritionResult] with per-serving nutrients, or [NutritionResult.ZERO] if not found
     */
    suspend fun calculatePerServingNutrition(dishLabel: String): NutritionResult {
        val dish = dishRecipeDao.getByDishLabel(dishLabel)
            ?: return NutritionResult.ZERO

        return dishToPerServing(dish)
    }

    /**
     * Recalculates dish nutrition with ingredient substitutions.
     *
     * This uses the **dynamic path**: reads all recipe ingredients from Room,
     * swaps any substituted ingredient keys, sums the raw nutrient values,
     * and returns the per-serving result.
     *
     * @param dishLabel The ML label / dish key
     * @param substitutions Map of originalIngredientKey → replacementIngredientKey
     * @return [NutritionResult] with recalculated per-serving nutrients
     */
    suspend fun calculateWithSubstitution(
        dishLabel: String,
        substitutions: Map<String, String>
    ): NutritionResult {
        val dish = dishRecipeDao.getByDishLabel(dishLabel)
            ?: return NutritionResult.ZERO

        val recipeIngredients = recipeIngredientDao.getIngredientsForDish(dishLabel)
        if (recipeIngredients.isEmpty()) return NutritionResult.ZERO

        // Sum raw nutrients, substituting ingredient keys where specified
        var totalRaw = NutritionResult.ZERO

        for (ri in recipeIngredients) {
            val effectiveKey = substitutions[ri.ingredientKey] ?: ri.ingredientKey
            val ingredient = rawIngredientDao.getByKey(effectiveKey) ?: continue

            // nutrient_amount = (raw_weight_grams / 100) × nutrients_per_100g
            val factor = ri.rawWeightGrams / 100f
            val ingredientNutrients = rawIngredientToNutrition(ingredient) * factor
            totalRaw = totalRaw + ingredientNutrients
        }

        // Divide by servings for per-serving result
        val servings = dish.servings.coerceAtLeast(1)
        return totalRaw * (1f / servings)
    }

    /**
     * Calculates per-cooked-100g nutrient values for a dish.
     *
     * This is useful for UI displays that show "per 100g" values
     * (matching the format of the old FoodItem entity).
     *
     * Formula: totalBatchNutrients × (100 / cookedWeightG)
     *
     * @param dishLabel The ML label / dish key
     * @return [NutritionResult] with nutrients per 100g cooked weight
     */
    suspend fun calculatePer100gCooked(dishLabel: String): NutritionResult {
        val dish = dishRecipeDao.getByDishLabel(dishLabel)
            ?: return NutritionResult.ZERO

        if (dish.cookedWeightG <= 0f) return NutritionResult.ZERO

        val totalNutrients = perServingToTotal(dish)
        return totalNutrients * (100f / dish.cookedWeightG)
    }

    // ── Private helpers ──

    /** Converts a [DishRecipeEntity]'s per-serving fields to a [NutritionResult]. */
    private fun dishToPerServing(dish: DishRecipeEntity): NutritionResult =
        NutritionResult(
            calories = dish.calPerServing,
            protein = dish.proteinPerServing,
            carbs = dish.carbsPerServing,
            fat = dish.fatPerServing,
            fiber = dish.fiberPerServing,
            sugar = dish.sugarPerServing,
            sodium = dish.sodiumPerServing,
            potassium = dish.potassiumPerServing,
            vitaminA = dish.vitaminAPerServing,
            vitaminC = dish.vitaminCPerServing,
            calcium = dish.calciumPerServing,
            iron = dish.ironPerServing,
        )

    /** Scales per-serving nutrients to total batch (per-serving × servings). */
    private fun perServingToTotal(dish: DishRecipeEntity): NutritionResult =
        dishToPerServing(dish) * dish.servings.toFloat()

    /** Extracts per-100g nutrients from a [RawIngredientEntity] as a [NutritionResult]. */
    private fun rawIngredientToNutrition(ingredient: RawIngredientEntity): NutritionResult =
        NutritionResult(
            calories = ingredient.calories,
            protein = ingredient.protein,
            carbs = ingredient.carbs,
            fat = ingredient.fat,
            fiber = ingredient.fiber,
            sugar = ingredient.sugar,
            sodium = ingredient.sodium,
            potassium = ingredient.potassium,
            vitaminA = ingredient.vitaminA,
            vitaminC = ingredient.vitaminC,
            calcium = ingredient.calcium,
            iron = ingredient.iron,
        )
}
