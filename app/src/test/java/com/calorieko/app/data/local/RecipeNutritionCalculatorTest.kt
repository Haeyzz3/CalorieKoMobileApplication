package com.calorieko.app.data.local

import com.calorieko.app.data.model.DishRecipeEntity
import com.calorieko.app.data.model.RawIngredientEntity
import com.calorieko.app.data.model.RecipeIngredientEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeNutritionCalculatorTest {

    @Test
    fun getIngredientBreakdown_returnsReplacementMetadataForSubstitution() = runBlocking {
        val calculator = testCalculator()

        val breakdown = calculator.getIngredientBreakdown(
            dishLabel = "test_dish",
            substitutions = mapOf("onion" to "garlic")
        )

        val onion = breakdown.getValue("onion")
        assertEquals("onion", onion.originalIngredientKey)
        assertEquals("Onion", onion.originalDisplayName)
        assertEquals("garlic", onion.replacementIngredientKey)
        assertEquals("Garlic", onion.replacementDisplayName)
        assertEquals("Garlic", onion.displayName)
        assertEquals("optional", onion.ingredientType)
        assertFalse(onion.isRemoved)
    }

    @Test
    fun getIngredientBreakdown_keepsRemovedIngredientAsZeroNutritionRow() = runBlocking {
        val calculator = testCalculator()

        val breakdown = calculator.getIngredientBreakdown(
            dishLabel = "test_dish",
            substitutions = mapOf("onion" to "__REMOVED__")
        )

        val onion = breakdown.getValue("onion")
        assertEquals("onion", onion.originalIngredientKey)
        assertEquals("Onion", onion.originalDisplayName)
        assertNull(onion.replacementIngredientKey)
        assertTrue(onion.isRemoved)
        assertEquals(0f, onion.rawWeightGrams, 0.001f)
        assertEquals(0f, onion.calories, 0.001f)
        assertEquals(0f, onion.protein, 0.001f)
        assertEquals(0f, onion.carbs, 0.001f)
        assertEquals(0f, onion.fat, 0.001f)
        assertEquals(0f, onion.sodium, 0.001f)
    }

    @Test
    fun getIngredientBreakdown_keepsUncustomizedIngredientsUnchanged() = runBlocking {
        val calculator = testCalculator()

        val breakdown = calculator.getIngredientBreakdown("test_dish")

        val chicken = breakdown.getValue("chicken")
        assertEquals("chicken", chicken.ingredientKey)
        assertEquals("Chicken", chicken.displayName)
        assertEquals("core", chicken.ingredientType)
        assertFalse(chicken.isRemoved)
        assertEquals(100f, chicken.rawWeightGrams, 0.001f)
        assertEquals(200f, chicken.calories, 0.001f)
        assertEquals(20f, chicken.protein, 0.001f)
    }

    @Test
    fun calculatePortionNutrition_scalesDishNutritionByCookedWeight() = runBlocking {
        val calculator = testCalculator()

        val nutrients = calculator.calculatePortionNutrition("test_dish", cookedWeightGrams = 75f)

        assertEquals(110f, nutrients.calories, 0.001f)
        assertEquals(10.5f, nutrients.protein, 0.001f)
        assertEquals(2f, nutrients.carbs, 0.001f)
        assertEquals(2.5f, nutrients.fat, 0.001f)
    }

    @Test
    fun calculatePortionNutrition_withSubstitutionsScalesSubstitutedRecipeNutrition() = runBlocking {
        val calculator = testCalculator()

        val nutrients = calculator.calculatePortionNutrition(
            dishLabel = "test_dish",
            cookedWeightGrams = 75f,
            substitutions = mapOf("onion" to "garlic")
        )

        assertEquals(125f, nutrients.calories, 0.001f)
        assertEquals(11.5f, nutrients.protein, 0.001f)
        assertEquals(5f, nutrients.carbs, 0.001f)
        assertEquals(2.5f, nutrients.fat, 0.001f)
    }

    @Test
    fun calculatePortionNutrition_withTweaksUsesAdjustedCookedYield() = runBlocking {
        val calculator = testCalculator()

        val nutrients = calculator.calculatePortionNutrition(
            dishLabel = "test_dish",
            cookedWeightGrams = 75f,
            substitutions = emptyMap(),
            tweaks = mapOf("onion" to 2f)
        )

        assertEquals(90f, nutrients.calories, 0.001f)
        assertEquals(8.25f, nutrients.protein, 0.001f)
        assertEquals(3f, nutrients.carbs, 0.001f)
        assertEquals(1.875f, nutrients.fat, 0.001f)
    }

    @Test
    fun calculatePortionNutrition_withSubstitutionAndTweakComposesBoth() = runBlocking {
        val calculator = testCalculator()

        val nutrients = calculator.calculatePortionNutrition(
            dishLabel = "test_dish",
            cookedWeightGrams = 75f,
            substitutions = mapOf("onion" to "garlic"),
            tweaks = mapOf("onion" to 2f)
        )

        assertEquals(112.5f, nutrients.calories, 0.001f)
        assertEquals(9.75f, nutrients.protein, 0.001f)
        assertEquals(7.5f, nutrients.carbs, 0.001f)
        assertEquals(1.875f, nutrients.fat, 0.001f)
    }

    @Test
    fun calculatePortionNutrition_removedIngredientIgnoresStaleTweak() = runBlocking {
        val calculator = testCalculator()

        val nutrients = calculator.calculatePortionNutrition(
            dishLabel = "test_dish",
            cookedWeightGrams = 75f,
            substitutions = mapOf("onion" to "__REMOVED__"),
            tweaks = mapOf("onion" to 4f)
        )

        assertEquals(150f, nutrients.calories, 0.001f)
        assertEquals(15f, nutrients.protein, 0.001f)
        assertEquals(0f, nutrients.carbs, 0.001f)
        assertEquals(3.75f, nutrients.fat, 0.001f)
    }

    @Test
    fun calculateWithSubstitution_ignoresProtectedCookingOilRemoval() = runBlocking {
        val calculator = testCalculator()

        val nutrients = calculator.calculateWithSubstitution(
            dishLabel = "protected_dish",
            substitutions = mapOf("cooking_oil" to "__REMOVED__")
        )

        assertEquals(310f, nutrients.calories, 0.001f)
        assertEquals(21f, nutrients.protein, 0.001f)
        assertEquals(4f, nutrients.carbs, 0.001f)
        assertEquals(15f, nutrients.fat, 0.001f)
    }

    @Test
    fun calculateWithSubstitution_usesReplacementForProtectedCookingOilSubstitution() = runBlocking {
        val calculator = testCalculator()

        val nutrients = calculator.calculateWithSubstitution(
            dishLabel = "protected_dish",
            substitutions = mapOf("cooking_oil" to "olive_oil")
        )

        assertEquals(300f, nutrients.calories, 0.001f)
        assertEquals(21f, nutrients.protein, 0.001f)
        assertEquals(4f, nutrients.carbs, 0.001f)
        assertEquals(13f, nutrients.fat, 0.001f)
    }

    @Test
    fun calculateWithTweaks_ignoresProtectedWaterRemovalForRawWeight() = runBlocking {
        val calculator = testCalculator()

        val (_, rawWeight) = calculator.calculateWithTweaks(
            dishLabel = "protected_dish",
            tweaks = emptyMap(),
            substitutions = mapOf("water" to "__REMOVED__")
        )

        assertEquals(185f, rawWeight, 0.001f)
    }

    @Test
    fun calculateWithSubstitution_stillRemovesNormalOptionalIngredient() = runBlocking {
        val calculator = testCalculator()

        val nutrients = calculator.calculateWithSubstitution(
            dishLabel = "protected_dish",
            substitutions = mapOf("onion" to "__REMOVED__")
        )

        assertEquals(290f, nutrients.calories, 0.001f)
        assertEquals(20f, nutrients.protein, 0.001f)
        assertEquals(0f, nutrients.carbs, 0.001f)
        assertEquals(15f, nutrients.fat, 0.001f)
    }

    private fun testCalculator(): RecipeNutritionCalculator =
        RecipeNutritionCalculator(
            dishRecipeDao = FakeDishRecipeDao,
            rawIngredientDao = FakeRawIngredientDao,
            recipeIngredientDao = FakeRecipeIngredientDao
        )

    private object FakeDishRecipeDao : DishRecipeDao {
        private val testDish = DishRecipeEntity(
            dishLabel = "test_dish",
            nameEn = "Test Dish",
            namePh = "Test Dish",
            category = "test",
            cookingMethod = "test",
            servings = 1,
            totalRawWeightG = 150f,
            dishYieldFactor = 1f,
            cookedWeightG = 150f,
            perServingWeightG = 150f,
            ingredientCount = 2,
            calPerServing = 220f,
            proteinPerServing = 21f,
            carbsPerServing = 4f,
            fatPerServing = 5f,
            fiberPerServing = 0f,
            sugarPerServing = 0f,
            sodiumPerServing = 55f,
            potassiumPerServing = 0f,
            vitaminAPerServing = 0f,
            vitaminCPerServing = 0f,
            calciumPerServing = 0f,
            ironPerServing = 0f
        )
        private val protectedDish = DishRecipeEntity(
            dishLabel = "protected_dish",
            nameEn = "Protected Dish",
            namePh = "Protected Dish",
            category = "test",
            cookingMethod = "test",
            servings = 1,
            totalRawWeightG = 185f,
            dishYieldFactor = 1f,
            cookedWeightG = 185f,
            perServingWeightG = 185f,
            ingredientCount = 4,
            calPerServing = 310f,
            proteinPerServing = 21f,
            carbsPerServing = 4f,
            fatPerServing = 15f,
            fiberPerServing = 0f,
            sugarPerServing = 0f,
            sodiumPerServing = 55f,
            potassiumPerServing = 0f,
            vitaminAPerServing = 0f,
            vitaminCPerServing = 0f,
            calciumPerServing = 0f,
            ironPerServing = 0f
        )
        private val dishes = listOf(testDish, protectedDish)

        override suspend fun insertAll(dishes: List<DishRecipeEntity>) = Unit
        override suspend fun getByDishLabel(dishLabel: String): DishRecipeEntity? =
            dishes.firstOrNull { dishLabel == it.dishLabel }

        override suspend fun getAllDishRecipes(): List<DishRecipeEntity> = dishes
        override suspend fun getByCategory(category: String): List<DishRecipeEntity> = dishes
        override suspend fun searchByName(query: String): List<DishRecipeEntity> = dishes
        override suspend fun getCount(): Int = dishes.size
        override suspend fun getAllDishLabels(): List<String> = dishes.map { it.dishLabel }
        override suspend fun getStoreBoughtDishes(): List<DishRecipeEntity> = emptyList()
        override suspend fun deleteAll() = Unit
    }

    private object FakeRawIngredientDao : RawIngredientDao {
        private val ingredients = listOf(
            RawIngredientEntity(
                ingredientKey = "chicken",
                displayName = "Chicken",
                category = "protein",
                subCategory = "meat",
                fdcId = 1,
                dataSource = "test",
                calories = 200f,
                protein = 20f,
                carbs = 0f,
                fat = 5f,
                sodium = 50f
            ),
            RawIngredientEntity(
                ingredientKey = "onion",
                displayName = "Onion",
                category = "produce",
                subCategory = "allium",
                fdcId = 2,
                dataSource = "test",
                calories = 40f,
                protein = 2f,
                carbs = 8f,
                fat = 0f,
                sodium = 10f
            ),
            RawIngredientEntity(
                ingredientKey = "garlic",
                displayName = "Garlic",
                category = "produce",
                subCategory = "allium",
                fdcId = 3,
                dataSource = "test",
                calories = 100f,
                protein = 6f,
                carbs = 20f,
                fat = 0f,
                sodium = 20f
            ),
            RawIngredientEntity(
                ingredientKey = "cooking_oil",
                displayName = "Soybean Oil",
                category = "pantry_staple",
                subCategory = "oil",
                fdcId = 4,
                dataSource = "test",
                calories = 900f,
                protein = 0f,
                carbs = 0f,
                fat = 100f,
                sodium = 0f
            ),
            RawIngredientEntity(
                ingredientKey = "olive_oil",
                displayName = "Olive Oil",
                category = "pantry_staple",
                subCategory = "oil",
                fdcId = 5,
                dataSource = "test",
                calories = 800f,
                protein = 0f,
                carbs = 0f,
                fat = 80f,
                sodium = 0f
            ),
            RawIngredientEntity(
                ingredientKey = "water",
                displayName = "Purified Water",
                category = "pantry_staple",
                subCategory = "water",
                fdcId = 6,
                dataSource = "test",
                calories = 0f,
                protein = 0f,
                carbs = 0f,
                fat = 0f,
                sodium = 0f
            ),
            RawIngredientEntity(
                ingredientKey = "mineral_water",
                displayName = "Mineral Water",
                category = "pantry_staple",
                subCategory = "water",
                fdcId = 7,
                dataSource = "test",
                calories = 0f,
                protein = 0f,
                carbs = 0f,
                fat = 0f,
                sodium = 0f
            )
        )

        override suspend fun insertAll(ingredients: List<RawIngredientEntity>) = Unit
        override suspend fun getByKey(key: String): RawIngredientEntity? =
            ingredients.firstOrNull { it.ingredientKey == key }

        override suspend fun getAllRawIngredients(): List<RawIngredientEntity> = ingredients
        override suspend fun getByCategory(category: String): List<RawIngredientEntity> =
            ingredients.filter { it.category == category }

        override suspend fun getCount(): Int = ingredients.size
        override suspend fun getSubstituteCandidates(subCategory: String, excludeKey: String): List<RawIngredientEntity> =
            ingredients.filter { it.subCategory == subCategory && it.ingredientKey != excludeKey && it.isSubstitutable }

        override suspend fun getAllBrowsable(): List<RawIngredientEntity> = ingredients
        override suspend fun getCategoriesForKeys(keys: List<String>): List<IngredientKeyCategory> =
            ingredients.filter { it.ingredientKey in keys }.map { IngredientKeyCategory(it.ingredientKey, it.category) }

        override suspend fun getDisplayNamesForKeys(keys: List<String>): List<IngredientKeyDisplayName> =
            ingredients.filter { it.ingredientKey in keys }.map { IngredientKeyDisplayName(it.ingredientKey, it.displayName) }

        override suspend fun deleteAll() = Unit
    }

    private object FakeRecipeIngredientDao : RecipeIngredientDao {
        private val recipeIngredients = listOf(
            RecipeIngredientEntity(
                dishLabel = "test_dish",
                ingredientKey = "chicken",
                ingredientType = "core",
                ingredientCategory = "protein",
                rawWeightGrams = 100f,
                portionOriginal = "100 g",
                preparationMethod = "sliced",
                step = 1
            ),
            RecipeIngredientEntity(
                dishLabel = "test_dish",
                ingredientKey = "onion",
                ingredientType = "optional",
                ingredientCategory = "produce",
                rawWeightGrams = 50f,
                portionOriginal = "1/2 cup",
                preparationMethod = "chopped",
                step = 2
            ),
            RecipeIngredientEntity(
                dishLabel = "protected_dish",
                ingredientKey = "chicken",
                ingredientType = "core",
                ingredientCategory = "protein",
                rawWeightGrams = 100f,
                portionOriginal = "100 g",
                preparationMethod = "sliced",
                step = 1
            ),
            RecipeIngredientEntity(
                dishLabel = "protected_dish",
                ingredientKey = "cooking_oil",
                ingredientType = "optional",
                ingredientCategory = "pantry_staple",
                rawWeightGrams = 10f,
                portionOriginal = "2 tsp",
                preparationMethod = "",
                step = 2
            ),
            RecipeIngredientEntity(
                dishLabel = "protected_dish",
                ingredientKey = "water",
                ingredientType = "optional",
                ingredientCategory = "pantry_staple",
                rawWeightGrams = 25f,
                portionOriginal = "25 ml",
                preparationMethod = "",
                step = 3
            ),
            RecipeIngredientEntity(
                dishLabel = "protected_dish",
                ingredientKey = "onion",
                ingredientType = "optional",
                ingredientCategory = "produce",
                rawWeightGrams = 50f,
                portionOriginal = "1/2 cup",
                preparationMethod = "chopped",
                step = 4
            )
        )

        override suspend fun insertAll(items: List<RecipeIngredientEntity>) = Unit
        override suspend fun getIngredientsForDish(dishLabel: String): List<RecipeIngredientEntity> =
            recipeIngredients.filter { it.dishLabel == dishLabel }

        override suspend fun getCoreIngredientsForDish(dishLabel: String): List<RecipeIngredientEntity> =
            recipeIngredients.filter { it.dishLabel == dishLabel && it.ingredientType == "core" }

        override suspend fun getCount(): Int = recipeIngredients.size
        override suspend fun deleteAll() = Unit
    }
}
