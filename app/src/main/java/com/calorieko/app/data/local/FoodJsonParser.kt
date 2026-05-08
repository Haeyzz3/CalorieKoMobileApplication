package com.calorieko.app.data.local

import com.calorieko.app.data.model.DishRecipeEntity
import com.calorieko.app.data.model.RawIngredientEntity
import com.calorieko.app.data.model.RecipeIngredientEntity
import org.json.JSONArray
import java.io.InputStream

/**
 * Parses the Phase 1 JSON asset files into Room entity lists.
 *
 * Uses Android's built-in `org.json` (no extra dependencies).
 * Each method reads a JSON array from an InputStream and maps it
 * to the corresponding Room entity.
 */
object FoodJsonParser {

    /**
     * Parses `raw_ingredients.json` → List<RawIngredientEntity>.
     *
     * Expected JSON structure per item:
     * ```json
     * {
     *   "ingredient_key": "chicken_egg",
     *   "display_name": "Chicken Egg",
     *   "category": "protein",
     *   "sub_category": "egg",
     *   "fdc_id": 171287,
     *   "data_source": "USDA_SR_LEGACY",
     *   "nutrients_per_100g": { "calories": 143.0, ... },
     *   "portions": [...]
     * }
     * ```
     */
    fun parseRawIngredients(inputStream: InputStream): List<RawIngredientEntity> {
        val jsonText = inputStream.bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonText)
        val results = mutableListOf<RawIngredientEntity>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val nutrients = obj.getJSONObject("nutrients_per_100g")

            results.add(
                RawIngredientEntity(
                    ingredientKey = obj.getString("ingredient_key"),
                    displayName = obj.getString("display_name"),
                    category = obj.getString("category"),
                    subCategory = obj.getString("sub_category"),
                    fdcId = obj.optInt("fdc_id", 0),
                    dataSource = obj.getString("data_source"),
                    calories = nutrients.optDouble("calories", 0.0).toFloat(),
                    protein = nutrients.optDouble("protein", 0.0).toFloat(),
                    carbs = nutrients.optDouble("carbs", 0.0).toFloat(),
                    fat = nutrients.optDouble("fat", 0.0).toFloat(),
                    fiber = nutrients.optDouble("fiber", 0.0).toFloat(),
                    sugar = nutrients.optDouble("sugar", 0.0).toFloat(),
                    sodium = nutrients.optDouble("sodium", 0.0).toFloat(),
                    potassium = nutrients.optDouble("potassium", 0.0).toFloat(),
                    vitaminA = nutrients.optDouble("vitamin_a", 0.0).toFloat(),
                    vitaminC = nutrients.optDouble("vitamin_c", 0.0).toFloat(),
                    calcium = nutrients.optDouble("calcium", 0.0).toFloat(),
                    iron = nutrients.optDouble("iron", 0.0).toFloat(),
                    isSubstitutable = obj.optBoolean("is_substitutable", true),
                    nutrientProxyNote = obj.optString("nutrient_proxy_note", ""),
                )
            )
        }
        return results
    }

    /**
     * Parses `dish_recipes.json` → List<DishRecipeEntity>.
     *
     * Expected JSON structure per item:
     * ```json
     * {
     *   "dish_label": "sinigang_pork",
     *   "name_en": "Pork Sinigang",
     *   "name_ph": "Sinigang na Baboy",
     *   "category": "Soup",
     *   "cooking_method": "simmered",
     *   "servings": 10,
     *   "total_raw_weight_g": 4121.6,
     *   "dish_yield_factor": 0.85,
     *   "cooked_weight_g": 3503.4,
     *   "per_serving_weight_g": 350.3,
     *   "per_serving_nutrients": { "calories": 417.6, ... },
     *   "ingredient_count": 11
     * }
     * ```
     */
    fun parseDishRecipes(inputStream: InputStream): List<DishRecipeEntity> {
        val jsonText = inputStream.bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonText)
        val results = mutableListOf<DishRecipeEntity>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val perServing = obj.getJSONObject("per_serving_nutrients")

            results.add(
                DishRecipeEntity(
                    dishLabel = obj.getString("dish_label"),
                    nameEn = obj.getString("name_en"),
                    namePh = obj.getString("name_ph"),
                    category = obj.getString("category"),
                    cookingMethod = obj.getString("cooking_method"),
                    servings = obj.getInt("servings"),
                    totalRawWeightG = obj.getDouble("total_raw_weight_g").toFloat(),
                    dishYieldFactor = obj.getDouble("dish_yield_factor").toFloat(),
                    cookedWeightG = obj.getDouble("cooked_weight_g").toFloat(),
                    perServingWeightG = obj.getDouble("per_serving_weight_g").toFloat(),
                    ingredientCount = obj.getInt("ingredient_count"),
                    servingSizeDescription = obj.optString("serving_size_description", ""),
                    calPerServing = perServing.optDouble("calories", 0.0).toFloat(),
                    proteinPerServing = perServing.optDouble("protein", 0.0).toFloat(),
                    carbsPerServing = perServing.optDouble("carbs", 0.0).toFloat(),
                    fatPerServing = perServing.optDouble("fat", 0.0).toFloat(),
                    fiberPerServing = perServing.optDouble("fiber", 0.0).toFloat(),
                    sugarPerServing = perServing.optDouble("sugar", 0.0).toFloat(),
                    sodiumPerServing = perServing.optDouble("sodium", 0.0).toFloat(),
                    potassiumPerServing = perServing.optDouble("potassium", 0.0).toFloat(),
                    vitaminAPerServing = perServing.optDouble("vitamin_a", 0.0).toFloat(),
                    vitaminCPerServing = perServing.optDouble("vitamin_c", 0.0).toFloat(),
                    calciumPerServing = perServing.optDouble("calcium", 0.0).toFloat(),
                    ironPerServing = perServing.optDouble("iron", 0.0).toFloat(),
                )
            )
        }
        return results
    }

    /**
     * Parses `recipe_ingredients.json` → List<RecipeIngredientEntity>.
     *
     * Expected JSON structure per item:
     * ```json
     * {
     *   "dish_label": "tokneneng_salad",
     *   "ingredient_key": "chicken_egg",
     *   "ingredient_type": "core",
     *   "ingredient_category": "protein",
     *   "raw_weight_grams": 250.0,
     *   "portion_original": "5 pcs",
     *   "preparation_method": "boiled & peeled",
     *   "step": 1
     * }
     * ```
     */
    fun parseRecipeIngredients(inputStream: InputStream): List<RecipeIngredientEntity> {
        val jsonText = inputStream.bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonText)
        val results = mutableListOf<RecipeIngredientEntity>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            results.add(
                RecipeIngredientEntity(
                    dishLabel = obj.getString("dish_label"),
                    ingredientKey = obj.getString("ingredient_key"),
                    ingredientType = obj.getString("ingredient_type"),
                    ingredientCategory = obj.getString("ingredient_category"),
                    rawWeightGrams = obj.getDouble("raw_weight_grams").toFloat(),
                    portionOriginal = obj.optString("portion_original", ""),
                    preparationMethod = obj.optString("preparation_method", ""),
                    step = obj.optInt("step", 1),
                )
            )
        }
        return results
    }
}
