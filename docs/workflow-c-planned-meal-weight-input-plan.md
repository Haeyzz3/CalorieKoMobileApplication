# Workflow C Planned Meal Weight Input Plan

Date: 2026-05-12

## Scope

Refactor Dashboard planned-meal logging so users must enter an actual consumed weight for every planned dish before confirming the meal. This applies to the planned meal quick-log routes:

- `logMeal/quick/{dishLabel}/{mealSlot}` for the legacy single-dish route.
- `logMeal/quickSlot` for the current Dashboard multi-dish slot route.

No application code has been changed for this plan.

## Verification Summary

The assumption about the base nutrition model is correct at the recipe-data level:

- `RecipeIngredientEntity.rawWeightGrams` stores each recipe ingredient weight.
- `RecipeNutritionCalculator.calculateWithSubstitution()` sums each ingredient as `(rawWeightGrams / 100) * rawIngredient per-100g nutrient`.
- It divides the raw batch total by `dish.servings` to return per-serving nutrition.
- `DishRecipeEntity` stores precomputed per-serving nutrients from `dish_recipes.json`.

Current Workflow C quick-log behavior is slightly different from "unknown/default assumed weight":

- `ManualLogViewModel.quickLogFromPlan()` and `quickLogSlotFromPlan()` immediately create `LoggedDish` entries.
- They set `weightGrams` to one cooked serving, using `recipe.cookedWeightG / recipe.servings` when servings are positive.
- They set nutrients to `calculatePerServingNutrition()` for normal planned dishes, or `calculateWithSubstitution()` for planned dishes with substitutions.
- The UI immediately sets `_showSummary = true`, so `QuickLogScreen` opens `ManualMealSummaryOverlay` with confirm enabled.

For actual weighed portions, the correct existing calculator entry point is:

- `RecipeNutritionCalculator.calculatePortionNutrition(dishLabel, cookedWeightGrams)`
- or `calculatePortionNutrition(dishLabel, cookedWeightGrams, substitutions)` when the planned dish has saved substitutions.

## Current Flow

1. Dashboard groups today's planned meals by slot.
2. Tapping `Log Meal` writes planned dish labels and substitutions into `QuickLogBridge`.
3. `MainActivity` navigates to `logMeal/quickSlot`.
4. `ManualLogViewModel.quickLogSlotFromPlan()` creates one `LoggedDish` per planned dish at one serving each.
5. `QuickLogScreen` renders `ManualMealSummaryOverlay`.
6. `ManualMealSummaryOverlay` enables `Confirm Meal` whenever `dishes.isNotEmpty()` and confirmation is not in progress.

## Proposed Flow

1. Dashboard navigation remains the same.
2. Quick-log loads the planned dishes as pending planned dishes, not confirmed/loggable dishes.
3. `QuickLogScreen` first presents a planned-meal weight method selection screen:
   - Smart Scale
   - Manual Entry
4. The selected method applies to the entire planned meal slot; users do not switch methods per dish.
5. After method selection, the user enters weight for each planned dish in the slot.
6. Each planned dish is mandatory once the user starts logging that slot.
7. Each completed dish is converted to a `LoggedDish` using the best available nutrition path for that dish and the actual input weight.
8. Dishes without recipe ingredients or recipe details still require weight input and may be logged with the available dish-level nutrition or a weight-only/zero-nutrition fallback if no nutrition source exists.
9. Only after every planned dish has a valid positive weight does the screen show the summary as confirmable.
10. `ManualMealSummaryOverlay` additionally receives a confirm gate so the button cannot confirm planned meals with missing weights, even if a future route accidentally opens the summary early.

## Product Decisions

- One weight method is selected for the entire planned meal slot.
- Every planned dish is mandatory once the user starts logging that slot.
- Planned dishes without recipe ingredients, such as lechon manok cuts, must still be weighed and logged.
- Smart Scale capture requires an explicit `Use This Weight` tap for each dish after the weight is stable.

## Implementation Execution Constraint

Do not execute any Gradle commands (`./gradlew`, `gradle`, etc.). Under no circumstances should compilation be verified via terminal.

The final implementation step is strictly limited to a manual code review of changed files for syntax, imports, and reference integrity. Once the code is written and manually reviewed, the task is considered 100% complete.

## Implementation Plan

### 1. Add planned quick-log state to `ManualLogViewModel`

Introduce state that represents planned dishes before they become logged dishes:

- `plannedQuickLogEntries: StateFlow<List<QuickLogDishEntry>>`
- `plannedWeightMethod: StateFlow<PlannedWeightMethod?>`
- `plannedWeightIndex: StateFlow<Int>`
- `isPlannedQuickLog: StateFlow<Boolean>`

Add a small enum:

```kotlin
enum class PlannedWeightMethod {
    SMART_SCALE,
    MANUAL
}
```

Replace the current eager behavior in `quickLogFromPlan()` and `quickLogSlotFromPlan()`:

- Set meal type from the slot.
- Store planned entries.
- Store a single selected weight method for the entire slot once the user chooses one.
- Clear `loggedDishes`.
- Set `showSummary` to false.
- Do not create one-serving `LoggedDish` rows yet.

Defer fallback dish handling until the user reaches that dish in the weight step.

### 2. Add ViewModel operations for planned dish weight capture

Add methods to `ManualLogViewModel`:

- `selectPlannedWeightMethod(method: PlannedWeightMethod)`
- `setCurrentPlannedManualWeight(text: String)`
- `logCurrentPlannedDishWithManualWeight()`
- `logCurrentPlannedDishWithScaleWeight(weightGrams: Float)`
- `skip/clear/reset` behavior only if product wants cancellation; otherwise Back exits the flow.

Both manual and scale paths should call a shared private helper:

```kotlin
private suspend fun buildLoggedDishFromPlannedEntry(
    entry: QuickLogDishEntry,
    cookedWeightGrams: Float
): LoggedDish
```

That helper should:

- Load `DishRecipeEntity` by dish label when available.
- Parse substitutions.
- Use `calculatePortionNutrition(dishLabel, cookedWeightGrams, substitutions)` when substitutions exist.
- Use `calculatePortionNutrition(dishLabel, cookedWeightGrams)` otherwise.
- If dish-level nutrition exists but ingredient recipe details do not, still create the `LoggedDish` using the available dish-level nutrition scaled by actual weight.
- If no nutrition source exists for the dish, still create a `LoggedDish` with the entered weight, display name fallback, and zeroed nutrients so mandatory weight capture is preserved.
- Preserve `substitutionsJson` on the resulting `LoggedDish`.
- Set `confidence = 1.0f` for planned identified dishes.

### 3. Reuse manual weight UI for planned dishes

Refactor `WeightInputContent` so it can be used for a preselected planned dish without showing manual search:

- Keep the existing `DishRecipeEntity`, weight text, and add action contract.
- Add optional button text such as `Add Weight` or `Next Dish`.
- Add optional progress text such as `Dish 1 of 3`.

In `QuickLogScreen`, when method is `MANUAL`:

- Resolve the current planned dish to `DishRecipeEntity`.
- Render the reused `WeightInputContent`.
- On valid weight, call the planned quick-log ViewModel method.
- When all planned dishes are weighed, show `ManualMealSummaryOverlay`.

### 4. Add Smart Scale-only planned dish weight UI

Create a new composable that reuses the smart-scale concepts from `AiScaleMealContent` without camera or classifier:

- Observe `bleScaleManager.connectionState`.
- Observe `bleScaleManager.liveWeight`.
- Apply the same stabilization behavior currently in `LogMealViewModel`, either by moving stabilization into a shared helper/ViewModel method or by adding planned-scale state to `ManualLogViewModel`.
- Show the current planned dish name and progress.
- Show connected/disconnected/stabilizing/stable states.
- Provide a `Zero Scale` action using `bleScaleManager.sendTareCommand()`.
- Enable `Use This Weight` only when the scale is connected, stable, and `weight > 0`.
- On use, call `logCurrentPlannedDishWithScaleWeight(weight)`.
- Require the explicit `Use This Weight` tap for each planned dish; do not auto-advance when the scale stabilizes.

Recommended implementation detail:

- Move weight stabilization into `ManualLogViewModel` for planned quick-log instead of trying to drive planned flow through `LogMealViewModel`, because planned quick-log is already owned by `ManualLogViewModel` and uses its pantry deduction and summary path.

### 5. Update `QuickLogScreen` routing

Change `QuickLogScreen` signature to accept:

- `bleScaleManager: BleScaleManager`
- `onNavigateToPairing: () -> Unit`

Render states in this order:

1. Pantry deduction screen if active.
2. Error/loading state if planned entries or recipes cannot load.
3. Method selection if planned quick-log is active and method is null.
4. Current planned-dish weight screen based on selected method.
5. Summary after all planned entries have positive weights.

Update both quick-log routes in `MainActivity` to pass `bleScaleManager` and the pairing navigation callback.

### 6. Strengthen `ManualMealSummaryOverlay` confirmation gating

Add parameters:

```kotlin
canConfirmMeal: Boolean = dishes.isNotEmpty()
confirmDisabledReason: String? = null
```

Update the button:

- `enabled = canConfirmMeal && !isConfirming`
- Show a concise message when `confirmDisabledReason != null`.

For planned quick-log, compute:

- `requiredCount = plannedQuickLogEntries.size`
- `completedCount = loggedDishes.count { it.weightGrams > 0f }`
- `canConfirmMeal = completedCount == requiredCount && dishes.all { it.weightGrams > 0f }`

This protects the summary even if an incomplete planned meal reaches it.

For planned quick-log, hide or disable dish removal in the summary. Removing a planned dish is not supported because every planned dish in the slot is mandatory once logging starts.

### 7. Preserve ingredient bottom sheet behavior

Keep `substitutionsJson` on every `LoggedDish` created from a planned entry.

This preserves:

- View Ingredients button.
- From Meal Plan badge.
- Substitution-aware ingredient breakdown.
- View-only planned meal substitution behavior.

### 8. Edge cases

Handle these explicitly:

- Missing recipe ingredients or dish recipe details: do not bypass weight capture. Log the dish with the actual input weight and the best available nutrition fallback.
- Zero or negative weight: do not create `LoggedDish`; keep the user on the weight step.
- Multi-dish planned slot: progress dish by dish until all are weighed.
- Method selection: one method applies to the whole slot.
- Scale disconnected: show pairing action and keep `Use This Weight` disabled.
- Removing a dish from summary: disallow for planned meals.

### 9. Tests and validation

Recommended validation:

- Unit test the planned ViewModel helper to verify positive weight creates a `LoggedDish` with portion-scaled nutrition.
- Unit test substitution path to ensure planned substitutions use `calculatePortionNutrition(dishLabel, weight, substitutions)`.
- Unit test that confirm is not allowed while planned entries remain unweighed.
- Manual UI test:
  - Dashboard planned meal slot with one dish, Manual Entry.
  - Dashboard planned meal slot with multiple dishes, Manual Entry.
  - Dashboard planned meal slot with Smart Scale disconnected.
  - Dashboard planned meal slot with Smart Scale connected and stable weight.
  - Dashboard planned meal slot with a no-recipe planned dish, confirming weight is still required.
  - Ingredient breakdown still opens for planned dishes.
  - Pantry deduction still appears after confirming.

Do not run Gradle, Android build, or terminal compilation checks. The final verification step is a manual code review of changed files for:

- Syntax consistency.
- Imports.
- Function and property references.
- Compose parameter updates at all call sites.
- Planned quick-log state transitions.
