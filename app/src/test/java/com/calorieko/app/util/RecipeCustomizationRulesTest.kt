package com.calorieko.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeCustomizationRulesTest {

    @Test
    fun canRemoveIngredient_blocksProtectedOptionalIngredients() {
        assertFalse(RecipeCustomizationRules.canRemoveIngredient("cooking_oil", "optional"))
        assertFalse(RecipeCustomizationRules.canRemoveIngredient("water", "optional"))
    }

    @Test
    fun canRemoveIngredient_allowsNormalOptionalIngredients() {
        assertTrue(RecipeCustomizationRules.canRemoveIngredient("black_pepper", "optional"))
    }

    @Test
    fun canRemoveIngredient_blocksCoreIngredients() {
        assertFalse(RecipeCustomizationRules.canRemoveIngredient("cooking_oil", "core"))
        assertFalse(RecipeCustomizationRules.canRemoveIngredient("black_pepper", "core"))
    }

    @Test
    fun sanitizeSubstitutions_removesProtectedRemovalSentinels() {
        val sanitized = RecipeCustomizationRules.sanitizeSubstitutions(
            mapOf(
                "cooking_oil" to RecipeCustomizationRules.REMOVED_INGREDIENT,
                "water" to RecipeCustomizationRules.REMOVED_INGREDIENT
            )
        )

        assertTrue(sanitized.isEmpty())
    }

    @Test
    fun sanitizeSubstitutions_preservesProtectedSubstitutions() {
        val substitutions = mapOf(
            "cooking_oil" to "olive_oil",
            "water" to "mineral_water"
        )

        assertEquals(substitutions, RecipeCustomizationRules.sanitizeSubstitutions(substitutions))
    }

    @Test
    fun sanitizeSubstitutions_preservesNormalRemovalSentinels() {
        val substitutions = mapOf("black_pepper" to RecipeCustomizationRules.REMOVED_INGREDIENT)

        assertEquals(substitutions, RecipeCustomizationRules.sanitizeSubstitutions(substitutions))
    }
}
