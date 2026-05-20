# Implementation Plan: Prevent Removal of Protected Base Ingredients

## Objective

Prevent users from removing recipe base ingredients that are required for nutrition and yield math:

- `cooking_oil` displayed as "Soybean Oil"
- `water` displayed as "Purified Water"

This restriction must apply even when the base ingredient is classified as `optional` in the recipe data. If the base ingredient is substituted, for example `cooking_oil -> olive_oil` or `water -> mineral_water`, the substituted ingredient must inherit the same unremovable behavior because it still occupies the protected original recipe slot.

The implementation should cover:

- Pantry recipe customization in `PantryScreen.kt`
- AI + Smart Scale meal summary customization in `LogMealScreen.kt` via `LogMealViewModel`
- Manual Entry meal summary customization in `LogMealScreen.kt` via `ManualLogViewModel`

The Dashboard Planned Meal entry point can remain view-only and does not need new customization behavior.

## Current Architecture Summary

Ingredient removal is currently modeled as a substitution sentinel:

```kotlin
originalIngredientKey -> "__REMOVED__"
```

This is used consistently across the app:

- `PantryViewModel.REMOVED_INGREDIENT`
- `LogMealViewModel.REMOVED_INGREDIENT`
- `ManualLogViewModel.REMOVED_INGREDIENT`
- `RecipeNutritionCalculator.REMOVED_INGREDIENT`

Substitution is also keyed by the original recipe ingredient key:

```kotlin
originalIngredientKey -> replacementIngredientKey
```

This is important because the "substitution inherits restriction" requirement can be satisfied by checking the original key. A substituted oil row still has original key `cooking_oil`, and a substituted water row still has original key `water`.

## Design Decision

Use a protected-original-slot rule:

```kotlin
originalIngredientKey in setOf("cooking_oil", "water")
```

Do not globally block every oil or water ingredient from removal. The rule is about base recipe slots, not every possible replacement ingredient. For example, `olive_oil` should become unremovable only when it is replacing original `cooking_oil`; it does not need a global special case.

## Recommended Implementation Shape

Add a shared rules utility, then consume it from all three state mutation paths and all three UI surfaces.

Recommended new file:

```text
app/src/main/java/com/calorieko/app/util/RecipeCustomizationRules.kt
```

Suggested contents:

```kotlin
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
```

Notes:

- Keeping `REMOVED_INGREDIENT` in this utility would reduce duplicated sentinel constants.
- To minimize blast radius, the first implementation can keep the existing public constants in each ViewModel and calculator, but route comparisons through the utility.
- If constants are consolidated, verify all call sites still compare against the same string.

## State-Layer Enforcement

UI hiding is not sufficient. The ViewModels and calculator paths should also reject or sanitize invalid removal entries so stale state, future callers, and persisted planned meals cannot reintroduce the bug.

### PantryViewModel

Relevant current methods:

- `applySubstitution(dishLabel, originalKey, newKey)`
- `removeIngredient(dishLabel, ingredientKey)`
- `getPlannedDishDetail(...)`
- `getCompactDishNutrition(...)`
- `addMealToPlan(...)`

Implementation details:

1. In `removeIngredient(dishLabel, ingredientKey)`, return early if `ingredientKey` is protected.

2. In `applySubstitution(...)`, if `newKey == REMOVED_INGREDIENT` and `originalKey` is protected, ignore the mutation and optionally emit a snackbar.

3. Before storing `_activeSubstitutions`, sanitize the map:

   ```kotlin
   val sanitizedDishSubs = RecipeCustomizationRules.sanitizeSubstitutions(dishSubs)
   ```

4. Before recalculating nutrition with `calculator.calculateWithSubstitution(...)`, use the sanitized map.

5. When planning a meal via `addMealToPlan(...)`, sanitize the incoming `substitutions` before converting to JSON. This prevents persisted planned meals from containing protected removals.

6. In planned-meal detail readers like `getPlannedDishDetail(...)` and compact nutrition helpers, sanitize parsed `substitutionsJson` before applying it. This handles legacy/stale planned meals that may already have invalid protected removals.

### LogMealViewModel

Relevant current methods:

- `applySubstitutionToDish(dishIndex, substitutions)`
- `removeIngredientFromDish(dishIndex, ingredientKey)`
- `updateDishCustomizations(...)`
- `applyCustomizationsToDish(...)`
- `computePantryOverlap()`

Implementation details:

1. In `removeIngredientFromDish(...)`, return early for protected keys.

2. In `applySubstitutionToDish(...)`, sanitize `substitutions` before filtering tweaks and before calling `applyCustomizationsToDish(...)`.

3. In `updateDishCustomizations(...)`, sanitize after `transform(...)` and before removing stale tweaks.

4. In `applyCustomizationsToDish(...)`, sanitize again defensively before calculating nutrition and serializing `substitutionsJson`.

5. In `computePantryOverlap()`, sanitize parsed substitutions before calling `calculator.getIngredientBreakdown(...)`.

This protects AI + Smart Scale logging, including the `LogMealPhase.MEAL_SUMMARY` flow.

### ManualLogViewModel

Relevant current methods mirror `LogMealViewModel`:

- `applySubstitutionToDish(dishIndex, substitutions)`
- `removeIngredientFromDish(dishIndex, ingredientKey)`
- `updateDishCustomizations(...)`
- `applyCustomizationsToDish(...)`
- `computePantryOverlap()`

Implementation details:

1. Apply the same sanitization and early-return rules as `LogMealViewModel`.

2. Ensure manual entry flow and planned quick-log flow both use sanitized substitutions before recalculation and persistence.

This covers `isManualMode` in `LogMealScreenWithManual`, `ManualMealContent`, and `ManualMealSummaryOverlay`.

### RecipeNutritionCalculator

Relevant current methods:

- `calculateWithSubstitution(...)`
- `calculateWithTweaks(...)`
- `getIngredientBreakdown(...)`

Recommended defensive change:

At the start of each method that consumes substitutions, sanitize the substitutions map:

```kotlin
val effectiveSubstitutions = RecipeCustomizationRules.sanitizeSubstitutions(substitutions)
```

Then use `effectiveSubstitutions` instead of the raw input.

Reason:

- This catches stale persisted data.
- This prevents any future caller from accidentally removing protected base ingredients.
- This keeps the nutrition layer mathematically consistent even if a UI path is missed.

Potential tradeoff:

- Existing stale planned meals/logged dishes with protected removals will silently calculate as if the base ingredient was not removed.
- This is the desired data-recovery behavior, but the UI may still show a customization count until that dish is saved again unless the ViewModel also serializes the sanitized map back.

## UI Updates

### PantryScreen.kt

Current behavior:

- `isOptional = detail.type == "optional"`
- Remove button is shown when `isOptional`
- Button calls `viewModel.removeIngredient(recipe.dishLabel, detail.ingredientKey)`

Change:

```kotlin
val canRemove = RecipeCustomizationRules.canRemoveIngredient(
    originalIngredientKey = detail.ingredientKey,
    ingredientType = detail.type
)
```

Then use `canRemove` instead of `isOptional` for the remove action.

Expected behavior:

- `cooking_oil` optional rows do not show "Remove ingredient".
- `water` optional rows do not show "Remove ingredient".
- Optional non-base ingredients remain removable.
- Core oil rows remain unremovable because `ingredientType != "optional"`.
- Swapping `cooking_oil` to `olive_oil` keeps the row unremovable because the original key remains `cooking_oil`.
- Undo for substitutions should remain available. Undo means "restore original ingredient", not "remove ingredient".

### LogMealScreen.kt: ManualMealSummaryOverlay

Current behavior:

- `isOptional = ing.ingredientType == "optional"`
- Delete icon is shown when `isOptional && !isPlannedMeal`
- Deletion writes `newSubs[originalIngredientKey] = ManualLogViewModel.REMOVED_INGREDIENT`

Change:

Use:

```kotlin
val canRemove = RecipeCustomizationRules.canRemoveIngredient(
    originalIngredientKey = originalIngredientKey,
    ingredientType = ing.ingredientType
)
```

Then show the delete icon only when:

```kotlin
canRemove && !isPlannedMeal
```

Before applying a manually built `newSubs`, sanitize it.

### LogMealScreen.kt: MealSummaryOverlay

Current behavior mirrors manual summary:

- `isOptional = breakdown.ingredientType == "optional"`
- Delete icon is shown when `isOptional`
- Deletion writes `newSubs[originalIngredientKey] = LogMealViewModel.REMOVED_INGREDIENT`

Change:

Use `canRemove` from `RecipeCustomizationRules`, same as manual.

Before applying a manually built `newSubs`, sanitize it.

## Handling Existing Invalid Data

There are two realistic stale-data sources:

1. Planned meals saved before this fix with:

   ```json
   {"cooking_oil":"__REMOVED__"}
   ```

   or:

   ```json
   {"water":"__REMOVED__"}
   ```

2. In-progress or restored logged dishes with the same invalid entries in `substitutionsJson`.

Recommended handling:

- Sanitize on read before nutrition calculations.
- Sanitize on mutation before writing new `substitutionsJson`.
- Do not add a Room migration solely for this unless product requires cleaning persisted rows immediately.

Optional cleanup enhancement:

- Add a repository or DAO-level cleanup later that scans planned meals and logged meal items for protected removals and rewrites the JSON. This is not required for the behavioral fix if all readers sanitize before calculation.

## Hidden Issue to Address Separately

`ManualMealSummaryOverlay` computes:

```kotlin
val isFromMealPlan = isPlannedMeal || dish.substitutionsJson.isNotBlank()
```

This causes a normal manually customized dish to display a "From Meal Plan" badge whenever it has substitutions. Action gating still uses `isPlannedMeal`, so it is mostly a labeling bug, not a permissions bug.

Recommended separate cleanup:

```kotlin
val isFromMealPlan = isPlannedMeal
```

If a separate "Customized" badge is desired, derive it from `substitutionsJson` and `tweaksJson` with different copy.

## Test Plan

### Unit Tests

Add tests for the shared rules utility:

- `cooking_oil` optional returns `canRemove == false`
- `water` optional returns `canRemove == false`
- `cooking_oil` core returns `canRemove == false`
- normal optional ingredient returns `canRemove == true`
- normal core ingredient returns `canRemove == false`
- `sanitizeSubstitutions(mapOf("cooking_oil" to "__REMOVED__"))` removes that entry
- `sanitizeSubstitutions(mapOf("water" to "__REMOVED__"))` removes that entry
- `sanitizeSubstitutions(mapOf("cooking_oil" to "olive_oil"))` preserves substitution
- `sanitizeSubstitutions(mapOf("water" to "mineral_water"))` preserves substitution
- `sanitizeSubstitutions(mapOf("black_pepper" to "__REMOVED__"))` preserves removable optional deletion

Add calculator tests:

- Protected removal does not zero out `cooking_oil` nutrition.
- Protected substituted oil remains calculated with replacement nutrition.
- Protected removal does not reduce total raw weight/yield in tweak path.
- Non-protected optional removal still works as before.

Existing test file to extend:

```text
app/src/test/java/com/calorieko/app/data/local/RecipeNutritionCalculatorTest.kt
```

The existing fake recipe can be extended with protected `cooking_oil` or a new fake recipe can be added.

### ViewModel-Level Tests

If the project has existing ViewModel tests or test scaffolding, add coverage for:

- `PantryViewModel.removeIngredient(dishLabel, "cooking_oil")` does not write `__REMOVED__`.
- `LogMealViewModel.applySubstitutionToDish(index, mapOf("water" to "__REMOVED__"))` serializes without protected removal.
- `ManualLogViewModel.applySubstitutionToDish(index, mapOf("cooking_oil" to "__REMOVED__"))` serializes without protected removal.

If ViewModel tests are heavy to set up, prioritize utility and calculator tests plus manual QA.

### Manual QA

Pantry:

1. Open a dish with optional `cooking_oil`.
2. Expand Soybean Oil.
3. Verify "Remove ingredient" is absent.
4. Swap Soybean Oil to Olive Oil.
5. Verify Undo is available.
6. Verify Remove is still absent for the substituted row.
7. Verify optional non-base ingredients still show Remove.
8. Add the customized dish to meal plan and verify substitutions persist.

Manual Entry:

1. Enter Manual mode.
2. Add a dish with optional `water`.
3. Open View Ingredients in meal summary.
4. Verify Purified Water cannot be removed.
5. Swap Purified Water to Mineral Water.
6. Verify the row remains unremovable.
7. Verify optional non-base removals still work and nutrition recalculates.

AI + Smart Scale:

1. Reach `LogMealPhase.MEAL_SUMMARY`.
2. Open View Ingredients.
3. Verify the same oil/water behavior as Manual Entry.
4. Confirm meal and verify saved totals are reasonable.

Stale data:

1. Simulate or load a planned meal with `{"cooking_oil":"__REMOVED__"}`.
2. Open detail or summary.
3. Verify nutrition includes oil, not zeroed oil.
4. Save or update customization and verify protected removal is no longer serialized.

## Acceptance Criteria

- Users cannot remove original `cooking_oil` from Pantry customization when it is optional.
- Users cannot remove original `water` from Pantry customization when it is optional.
- Users cannot remove original `cooking_oil` or `water` from Manual Entry meal summary customization.
- Users cannot remove original `cooking_oil` or `water` from AI + Smart Scale meal summary customization.
- Substitutions for these base slots remain allowed.
- Substituted replacements inherit the unremovable behavior because the original key remains protected.
- Core ingredients remain unremovable as before.
- Optional non-base ingredients remain removable.
- Existing stale protected removals do not break nutrition calculations.
- Planned meal and logged meal serialization does not newly persist protected removals.
- Tests cover protected removal sanitization and unchanged behavior for ordinary optional removals.

## Implementation Order

1. Add `RecipeCustomizationRules`.
2. Add utility tests for protected base ingredient rules.
3. Sanitize substitutions in `RecipeNutritionCalculator`.
4. Extend calculator tests for protected removals and substituted protected rows.
5. Guard `PantryViewModel` mutation and planned-meal serialization/read paths.
6. Guard `LogMealViewModel` mutation and calculation paths.
7. Guard `ManualLogViewModel` mutation and calculation paths.
8. Update `PantryScreen.kt` to use `canRemove`.
9. Update both ingredient-sheet implementations in `LogMealScreen.kt` to use `canRemove`.
10. Optionally fix the misleading `isFromMealPlan` badge logic in `ManualMealSummaryOverlay`.
11. Run unit tests.
12. Run manual QA for Pantry, Manual Entry, and AI + Smart Scale.

## Risk Assessment

Low-to-medium risk.

The behavior is localized, but there is duplicated customization logic across screens and view models. The main risk is missing one path and allowing invalid `__REMOVED__` entries to leak through. Defensive sanitization in the calculator and ViewModels reduces that risk substantially.

The most important implementation detail is to key the rule by original ingredient key, not display name and not replacement key.

