package com.calorieko.app.data.local

import com.calorieko.app.data.model.DishRecipeEntity
import com.calorieko.app.data.model.NutritionResult
import com.calorieko.app.data.model.RawIngredientEntity
import com.calorieko.app.data.model.RecipeIngredientEntity

/**
 * Calculates dish nutrition using the raw-ingredient summation model.
 */
class RecipeNutritionCalculator(
    private val dishRecipeDao: DishRecipeDao,
    private val rawIngredientDao: RawIngredientDao,
    private val recipeIngredientDao: RecipeIngredientDao
) {

    /**
     * Calculates the nutrients for a specific cooked weight of a dish.
     */
    suspend fun calculatePortionNutrition(
        dishLabel: String,
        cookedWeightGrams: Float
    ): NutritionResult {
        val dish = dishRecipeDao.getByDishLabel(dishLabel)
            ?: return NutritionResult.ZERO

        if (dish.cookedWeightG <= 0f) return NutritionResult.ZERO

        val portionFraction = cookedWeightGrams / dish.cookedWeightG
        val totalNutrients = perServingToTotal(dish)

        return totalNutrients * portionFraction
    }

    /**
     * Calculates portion nutrition with ingredient substitutions.
     */
    suspend fun calculatePortionNutrition(
        dishLabel: String,
        cookedWeightGrams: Float,
        substitutions: Map<String, String>
    ): NutritionResult {
        if (substitutions.isEmpty()) {
            return calculatePortionNutrition(dishLabel, cookedWeightGrams)
        }

        val dish = dishRecipeDao.getByDishLabel(dishLabel)
            ?: return NutritionResult.ZERO

        if (dish.cookedWeightG <= 0f) return NutritionResult.ZERO

        val totalBatchPerServing = calculateWithSubstitution(dishLabel, substitutions)
        val totalBatch = totalBatchPerServing * dish.servings.coerceAtLeast(1).toFloat()
        val portionFraction = cookedWeightGrams / dish.cookedWeightG

        return totalBatch * portionFraction
    }

    /**
     * Returns the total nutrients for the entire dish batch.
     */
    suspend fun calculateDishNutrition(dishLabel: String): NutritionResult {
        val dish = dishRecipeDao.getByDishLabel(dishLabel)
            ?: return NutritionResult.ZERO

        return perServingToTotal(dish)
    }

    /**
     * Returns the per-serving nutrients for a dish.
     */
    suspend fun calculatePerServingNutrition(dishLabel: String): NutritionResult {
        val dish = dishRecipeDao.getByDishLabel(dishLabel)
            ?: return NutritionResult.ZERO

        return dishToPerServing(dish)
    }

    /**
     * Recalculates dish nutrition with ingredient substitutions.
     *
     * Removed ingredients are intentionally skipped here so they no longer
     * contribute to the recalculated meal nutrition.
     */
    suspend fun calculateWithSubstitution(
        dishLabel: String,
        substitutions: Map<String, String>
    ): NutritionResult {
        val dish = dishRecipeDao.getByDishLabel(dishLabel)
            ?: return NutritionResult.ZERO

        val recipeIngredients = recipeIngredientDao.getIngredientsForDish(dishLabel)
        if (recipeIngredients.isEmpty()) return NutritionResult.ZERO

        var totalRaw = NutritionResult.ZERO

        for (ri in recipeIngredients) {
            val effectiveKey = substitutions[ri.ingredientKey] ?: ri.ingredientKey
            if (effectiveKey == REMOVED_INGREDIENT) continue
            val ingredient = rawIngredientDao.getByKey(effectiveKey) ?: continue

            val factor = ri.rawWeightGrams / 100f
            totalRaw = totalRaw + (rawIngredientToNutrition(ingredient) * factor)
        }

        val servings = dish.servings.coerceAtLeast(1)
        return totalRaw * (1f / servings)
    }

    /**
     * Recalculates dish nutrition with per-ingredient tweaks AND substitutions.
     *
     * Tweaks are multipliers on each ingredient's `raw_weight_grams` (e.g., 2.0 = double).
     * Substitutions swap one ingredient key for another (existing Phase 1 behaviour).
     *
     * @return Pair of (per-serving NutritionResult, new total raw weight in grams).
     *         The caller can use the raw weight with [DishRecipeEntity.dishYieldFactor]
     *         to estimate the new cooked/per-serving weight.
     */
    suspend fun calculateWithTweaks(
        dishLabel: String,
        tweaks: Map<String, Float>,
        substitutions: Map<String, String> = emptyMap()
    ): Pair<NutritionResult, Float> {
        val dish = dishRecipeDao.getByDishLabel(dishLabel)
            ?: return Pair(NutritionResult.ZERO, 0f)

        val recipeIngredients = recipeIngredientDao.getIngredientsForDish(dishLabel)
        if (recipeIngredients.isEmpty()) return Pair(NutritionResult.ZERO, 0f)

        var totalRaw = NutritionResult.ZERO
        var totalRawWeightG = 0f

        for (ri in recipeIngredients) {
            val effectiveKey = substitutions[ri.ingredientKey] ?: ri.ingredientKey
            if (effectiveKey == REMOVED_INGREDIENT) continue
            val ingredient = rawIngredientDao.getByKey(effectiveKey) ?: continue

            val tweakMultiplier = tweaks[ri.ingredientKey] ?: 1f
            val adjustedWeight = ri.rawWeightGrams * tweakMultiplier
            val factor = adjustedWeight / 100f

            totalRaw = totalRaw + (rawIngredientToNutrition(ingredient) * factor)
            totalRawWeightG += adjustedWeight
        }

        val servings = dish.servings.coerceAtLeast(1)
        val perServing = totalRaw * (1f / servings)
        return Pair(perServing, totalRawWeightG)
    }

    /**
     * Calculates per-cooked-100g nutrient values for a dish.
     */
    suspend fun calculatePer100gCooked(dishLabel: String): NutritionResult {
        val dish = dishRecipeDao.getByDishLabel(dishLabel)
            ?: return NutritionResult.ZERO

        if (dish.cookedWeightG <= 0f) return NutritionResult.ZERO

        val totalNutrients = perServingToTotal(dish)
        return totalNutrients * (100f / dish.cookedWeightG)
    }

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

    private fun perServingToTotal(dish: DishRecipeEntity): NutritionResult =
        dishToPerServing(dish) * dish.servings.toFloat()

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

    /**
     * Returns per-ingredient nutrition contributions for a dish.
     *
     * Results are keyed by original recipe ingredient key. A removed ingredient
     * is still returned as a zero-nutrition row so the UI can show the removal.
     */
    suspend fun getIngredientBreakdown(
        dishLabel: String,
        substitutions: Map<String, String> = emptyMap()
    ): Map<String, IngredientNutritionBreakdown> {
        val recipeIngredients = recipeIngredientDao.getIngredientsForDish(dishLabel)
        if (recipeIngredients.isEmpty()) return emptyMap()

        val allZeroWeight = recipeIngredients.all { it.rawWeightGrams <= 0f }

        if (allZeroWeight) {
            val dish = dishRecipeDao.getByDishLabel(dishLabel)
                ?: return emptyMap()
            val perServing = dishToPerServing(dish)
            val primary = recipeIngredients.firstOrNull { it.ingredientType == "core" }
                ?: recipeIngredients.first()
            val result = mutableMapOf<String, IngredientNutritionBreakdown>()

            for (ri in recipeIngredients) {
                val isRemoved = substitutions[ri.ingredientKey] == REMOVED_INGREDIENT
                val getsDishNutrition = ri.ingredientKey == primary.ingredientKey && !isRemoved
                result[ri.ingredientKey] = buildIngredientBreakdown(
                    recipeIngredient = ri,
                    substitutions = substitutions,
                    rawWeightGrams = if (getsDishNutrition) dish.perServingWeightG else 0f,
                    calories = if (getsDishNutrition) perServing.calories else 0f,
                    protein = if (getsDishNutrition) perServing.protein else 0f,
                    carbs = if (getsDishNutrition) perServing.carbs else 0f,
                    fat = if (getsDishNutrition) perServing.fat else 0f,
                    sodium = if (getsDishNutrition) perServing.sodium else 0f
                )
            }

            return result
        }

        val result = mutableMapOf<String, IngredientNutritionBreakdown>()

        for (ri in recipeIngredients) {
            val effectiveKey = substitutions[ri.ingredientKey] ?: ri.ingredientKey
            val isRemoved = effectiveKey == REMOVED_INGREDIENT
            val ingredient = if (isRemoved) null else (rawIngredientDao.getByKey(effectiveKey) ?: continue)
            val factor = if (isRemoved) 0f else ri.rawWeightGrams / 100f

            val existing = result[ri.ingredientKey]
            if (existing != null) {
                result[ri.ingredientKey] = existing.copy(
                    rawWeightGrams = existing.rawWeightGrams + if (isRemoved) 0f else ri.rawWeightGrams,
                    calories = existing.calories + ((ingredient?.calories ?: 0f) * factor),
                    protein = existing.protein + ((ingredient?.protein ?: 0f) * factor),
                    carbs = existing.carbs + ((ingredient?.carbs ?: 0f) * factor),
                    fat = existing.fat + ((ingredient?.fat ?: 0f) * factor),
                    sodium = existing.sodium + ((ingredient?.sodium ?: 0f) * factor)
                )
            } else {
                result[ri.ingredientKey] = buildIngredientBreakdown(
                    recipeIngredient = ri,
                    substitutions = substitutions,
                    rawWeightGrams = if (isRemoved) 0f else ri.rawWeightGrams,
                    calories = (ingredient?.calories ?: 0f) * factor,
                    protein = (ingredient?.protein ?: 0f) * factor,
                    carbs = (ingredient?.carbs ?: 0f) * factor,
                    fat = (ingredient?.fat ?: 0f) * factor,
                    sodium = (ingredient?.sodium ?: 0f) * factor
                )
            }
        }

        return result
    }

    /**
     * Returns same-subcategory substitution candidates for a given ingredient.
     */
    suspend fun getSubstitutesForIngredient(ingredientKey: String): List<RawIngredientEntity> {
        val ingredient = rawIngredientDao.getByKey(ingredientKey) ?: return emptyList()
        return rawIngredientDao.getSubstituteCandidates(ingredient.subCategory, ingredientKey)
    }

    private suspend fun buildIngredientBreakdown(
        recipeIngredient: RecipeIngredientEntity,
        substitutions: Map<String, String>,
        rawWeightGrams: Float,
        calories: Float,
        protein: Float,
        carbs: Float,
        fat: Float,
        sodium: Float
    ): IngredientNutritionBreakdown {
        val originalKey = recipeIngredient.ingredientKey
        val mappedKey = substitutions[originalKey]
        val isRemoved = mappedKey == REMOVED_INGREDIENT
        val replacementKey = mappedKey.takeUnless { isRemoved || it == originalKey }
        val effectiveKey = replacementKey ?: originalKey

        val originalIngredient = rawIngredientDao.getByKey(originalKey)
        val effectiveIngredient = if (isRemoved) originalIngredient else rawIngredientDao.getByKey(effectiveKey)
        val originalDisplayName = originalIngredient?.displayName ?: formatIngredientKey(originalKey)
        val effectiveDisplayName = if (isRemoved) {
            originalDisplayName
        } else {
            effectiveIngredient?.displayName ?: formatIngredientKey(effectiveKey)
        }

        return IngredientNutritionBreakdown(
            ingredientKey = if (isRemoved) originalKey else effectiveKey,
            displayName = effectiveDisplayName,
            rawWeightGrams = rawWeightGrams,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            sodium = sodium,
            originalIngredientKey = originalKey,
            originalDisplayName = originalDisplayName,
            replacementIngredientKey = replacementKey,
            replacementDisplayName = replacementKey?.let { effectiveDisplayName },
            ingredientType = recipeIngredient.ingredientType,
            ingredientCategory = recipeIngredient.ingredientCategory,
            portionQuantity = recipeIngredient.portionOriginal,
            preparationMethod = recipeIngredient.preparationMethod,
            step = recipeIngredient.step,
            isRemoved = isRemoved
        )
    }

    private fun formatIngredientKey(key: String): String =
        key.replace("_", " ").replaceFirstChar { it.uppercase() }

    private companion object {
        const val REMOVED_INGREDIENT = "__REMOVED__"
    }
}

/**
 * Per-ingredient nutrition contribution to a dish recipe.
 * All nutrient values represent the actual amounts contributed by this ingredient.
 */
data class IngredientNutritionBreakdown(
    val ingredientKey: String,
    val displayName: String,
    val rawWeightGrams: Float,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val sodium: Float,
    val originalIngredientKey: String = ingredientKey,
    val originalDisplayName: String = displayName,
    val replacementIngredientKey: String? = null,
    val replacementDisplayName: String? = null,
    val ingredientType: String = "",
    val ingredientCategory: String = "",
    val portionQuantity: String = "",
    val preparationMethod: String = "",
    val step: Int = 0,
    val isRemoved: Boolean = false
)
