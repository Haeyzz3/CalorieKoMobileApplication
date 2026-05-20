package com.calorieko.app.util

object RecipeCustomizationRules {
    const val REMOVED_INGREDIENT = "__REMOVED__"

    private val protectedBaseIngredientKeys = setOf(
        "cooking_oil",
        "water"
    )

    fun isProtectedBaseIngredient(originalIngredientKey: String): Boolean =
        originalIngredientKey in protectedBaseIngredientKeys

    fun canRemoveIngredient(
        originalIngredientKey: String,
        ingredientType: String
    ): Boolean =
        ingredientType == "optional" && !isProtectedBaseIngredient(originalIngredientKey)

    fun sanitizeSubstitutions(
        substitutions: Map<String, String>
    ): Map<String, String> =
        substitutions.filterNot { (originalKey, mappedKey) ->
            isProtectedBaseIngredient(originalKey) && mappedKey == REMOVED_INGREDIENT
        }
}
