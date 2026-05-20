# LogMeal Ingredient Tweaks Implementation Plan

Generated: 2026-05-19

## Objective

Bring the Pantry recipe customization capability for individual ingredient tweaking into the LogMeal Summary Phase, while intentionally excluding recipe serving scaling from meal logging.

The target behavior is:

- Users can adjust individual ingredient amounts from the LogMeal Summary ingredient sheet.
- Ingredient substitutions and removals continue to work.
- Planned meal quick-log preserves Pantry customizations when moving from Pantry or Dashboard into LogMeal.
- Recipe scaling remains a planning and cooking feature, not a logging feature.
- No-ingredient dishes, such as the store-bought lechon manok entries, continue to log from direct nutrition data and show no ingredient-adjustment UI.

## High-Level Decisions

### Decision 1: Do not implement recipe scaling in LogMeal

Recipe scaling changes the planned batch size, not the actual consumed portion. LogMeal is weight-based. The user logs an actual cooked portion weight, and nutrient values should follow that weight.

Current runtime math supports this:

```text
base portion fraction = logged_cooked_weight_g / original_cooked_batch_weight_g
logged nutrients = total_batch_nutrients * base portion fraction
```

If a recipe is scaled from 5 servings to 10 servings with the same ingredient ratios, nutrition density does not change. The logged cooked weight is sufficient.

Important nuance: LogMeal is not strictly "single serving." It is "actual consumed cooked weight." A user can log half a serving, one serving, or multiple servings if the entered or measured weight reflects that.

### Decision 2: Implement individual ingredient tweaking in LogMeal

Ingredient tweaking changes the recipe composition and therefore changes nutrition density. It should be available in LogMeal when the user wants to reflect the actual plate they ate.

Example:

- The base recipe has 10 g oil.
- The user ate a portion from a version cooked with half the oil.
- Logged cooked weight alone cannot represent that change.
- A per-ingredient tweak can represent it and recalculate nutrition correctly.

### Decision 3: Preserve planned meal tweak metadata into the Summary Phase

The Dashboard planned meal quick-log path currently passes `tweaksJson` into `ManualLogViewModel`, and the initial nutrient calculation applies it. However, `LoggedDish` only stores `substitutionsJson`, so tweak metadata is lost after the logged dish is created.

This creates two problems:

- The Summary Phase cannot display active Pantry ingredient tweaks.
- Any later substitution or removal recalculation can accidentally drop previously applied tweaks.

The implementation should add `tweaksJson` to `LoggedDish` and keep all recalculation paths substitution-aware and tweak-aware.

## Current Architecture Summary

### Pantry customization flow

Relevant files:

- `app/src/main/java/com/calorieko/app/ui/screens/PantryScreen.kt`
- `app/src/main/java/com/calorieko/app/viewmodel/PantryViewModel.kt`
- `app/src/main/java/com/calorieko/app/data/model/PlannedMealEntity.kt`
- `app/src/main/java/com/calorieko/app/data/local/RecipeNutritionCalculator.kt`

Pantry currently supports:

- Ingredient substitution via `activeSubstitutions`
- Ingredient removal via a sentinel substitution value
- Recipe scaling via `scaledServings`
- Ingredient amount tweaks via `ingredientTweaks`

When a recipe is added to the meal plan, Pantry persists:

```kotlin
PlannedMealEntity(
    substitutionsJson = ...,
    scaledServings = ...,
    tweaksJson = ...
)
```

This happens in `PantryViewModel.addMealToPlan`.

### Dashboard planned meal quick-log flow

Relevant files:

- `app/src/main/java/com/calorieko/app/ui/screens/DashboardScreen.kt`
- `app/src/main/java/com/calorieko/app/MainActivity.kt`
- `app/src/main/java/com/calorieko/app/viewmodel/ManualLogViewModel.kt`

Dashboard passes planned meals into `QuickLogBridge`:

```kotlin
QuickLogDishEntry(
    dishLabel = it.dishLabel,
    substitutionsJson = it.substitutionsJson,
    scaledServings = it.scaledServings,
    tweaksJson = it.tweaksJson
)
```

`MainActivity` reads `QuickLogBridge` for the `logMeal/quickSlot` route and initializes `ManualLogViewModel`.

`ManualLogViewModel.buildLoggedDishFromPlannedEntry` already parses:

```kotlin
val substitutions = parseSubstitutionsJson(entry.substitutionsJson)
val tweaks = parseTweaksJson(entry.tweaksJson)
```

It then calls:

```kotlin
calculator.calculatePortionNutrition(
    entry.dishLabel,
    cookedWeightGrams,
    substitutions,
    tweaks
)
```

So the initial planned meal nutrition is already tweak-aware.

### Current metadata gap

`LoggedDish` currently has:

```kotlin
val substitutionsJson: String = ""
```

It does not have:

```kotlin
val tweaksJson: String = ""
```

Because of this, Summary UI and later recalculation paths can only reconstruct substitutions and removals, not ingredient tweaks.

### LogMeal Summary UI

Relevant file:

- `app/src/main/java/com/calorieko/app/ui/screens/LogMealScreen.kt`

There are two summary implementations:

- `ManualMealSummaryOverlay`
- The AI and smart scale summary overlay later in the file

Both render:

- Dish cards
- Nutrient totals
- A "View Ingredients" bottom sheet
- Ingredient substitution and removal controls

Both currently need to be updated or refactored so ingredient tweaking works consistently across manual, planned quick-log, and AI plus smart scale flows.

## Nutrition Math Review

### Base recipe logging

`RecipeNutritionCalculator.calculatePortionNutrition(dishLabel, cookedWeightGrams)`:

```text
portion_fraction = cookedWeightGrams / dish.cookedWeightG
total_batch_nutrients = per_serving_nutrients * servings
logged_nutrients = total_batch_nutrients * portion_fraction
```

This uses cooked weight, not serving size description text.

### Substitution logging

For substitutions and removals:

```text
recalculate per-serving nutrients from recipe ingredients
total_batch = recalculated_per_serving * servings
portion_fraction = logged_cooked_weight_g / original_cooked_batch_weight_g
logged_nutrients = total_batch * portion_fraction
```

Removed ingredients are skipped.

### Ingredient tweak logging

For tweaks:

```text
adjusted_raw_weight = original_raw_weight * ingredient_tweak_multiplier
recalculate total raw nutrients from adjusted weights
estimate adjusted cooked batch weight = adjusted_total_raw_weight * dish_yield_factor
portion_fraction = logged_cooked_weight_g / adjusted_cooked_batch_weight
logged_nutrients = adjusted_total_batch_nutrients * portion_fraction
```

This is the correct place to apply individual ingredient changes because tweaks affect nutrient density and the estimated cooked yield.

### Recipe scaling in logging

`scaledServings` should not participate in logged nutrient calculation. Scaling the batch by the same ingredient ratios changes total batch output but does not change nutrition density for a measured cooked portion.

The only possible use for `scaledServings` in LogMeal is display-only, for example a read-only badge saying the planned recipe was scaled in Pantry. It should not affect logged nutrients.

## No-Ingredient Dishes

Some dishes have `ingredient_count = 0`, such as the store-bought lechon manok entries.

Expected behavior:

- LogMeal still calculates portion nutrition from `DishRecipeEntity` per-serving data.
- Ingredient breakdown returns empty.
- Summary bottom sheet shows "No ingredient breakdown available."
- No tweak, substitution, or removal controls are shown.

This behavior should be preserved.

## Implementation Plan

### Step 1: Extend `LoggedDish` with tweak metadata

File:

- `app/src/main/java/com/calorieko/app/data/model/LoggedDish.kt`

Add:

```kotlin
val tweaksJson: String = ""
```

Keep the default value so existing call sites continue compiling with minimal changes.

Do not add `scaledServings` unless a display-only planned-meal badge is explicitly needed. It is not required for nutrition.

### Step 2: Preserve `tweaksJson` when building planned quick-log dishes

File:

- `app/src/main/java/com/calorieko/app/viewmodel/ManualLogViewModel.kt`

In `buildLoggedDishFromPlannedEntry`, populate:

```kotlin
tweaksJson = entry.tweaksJson
```

This keeps Pantry tweaks visible and editable in Summary after quick-log creates a `LoggedDish`.

### Step 3: Add tweak JSON helpers to ViewModels

Files:

- `app/src/main/java/com/calorieko/app/viewmodel/ManualLogViewModel.kt`
- `app/src/main/java/com/calorieko/app/viewmodel/LogMealViewModel.kt`

Manual already has `parseTweaksJson`; add or expose an equivalent in `LogMealViewModel`.

Add:

```kotlin
private fun tweaksToJson(tweaks: Map<String, Float>): String
```

Normalize identity values:

- Empty map returns `""`
- `1f` should be removed from the map before serialization
- Values should be stored as JSON doubles to match Pantry behavior

### Step 4: Make substitution recalculation preserve tweaks

Files:

- `ManualLogViewModel.kt`
- `LogMealViewModel.kt`

Current substitution functions recalculate using substitutions only:

```kotlin
calculator.calculatePortionNutrition(dish.dishLabel, dish.weightGrams, substitutions)
```

Change the recalculation path to:

```kotlin
val currentTweaks = parseTweaksJson(dish.tweaksJson)
val nutrients = if (currentTweaks.isNotEmpty()) {
    calculator.calculatePortionNutrition(
        dish.dishLabel,
        dish.weightGrams,
        substitutions,
        currentTweaks
    )
} else {
    calculator.calculatePortionNutrition(
        dish.dishLabel,
        dish.weightGrams,
        substitutions
    )
}
```

This prevents substitutions and removals from wiping existing ingredient tweaks.

### Step 5: Add ViewModel functions for ingredient tweaking

Files:

- `ManualLogViewModel.kt`
- `LogMealViewModel.kt`

Add functions similar to:

```kotlin
fun applyIngredientTweakToDish(
    dishIndex: Int,
    ingredientKey: String,
    multiplier: Float
)
```

Behavior:

1. Load the current `LoggedDish`.
2. Parse `dish.substitutionsJson`.
3. Parse `dish.tweaksJson`.
4. Update or remove the ingredient tweak.
5. Recalculate nutrients using substitutions and tweaks together.
6. Update the dish nutrients and `tweaksJson`.

Also add:

```kotlin
fun resetIngredientTweakFromDish(dishIndex: Int, ingredientKey: String)
fun clearIngredientTweaksFromDish(dishIndex: Int)
```

The clear function is useful for a banner or reset button.

### Step 6: Make ingredient breakdown display tweak-aware

Current `getIngredientBreakdown` accepts substitutions only:

```kotlin
calculator.getIngredientBreakdown(dishLabel, substitutions)
```

There are two possible approaches.

Preferred minimal approach:

- Keep `RecipeNutritionCalculator.getIngredientBreakdown` unchanged.
- In the UI, parse `dish.tweaksJson`.
- Multiply display values by the tweak multiplier when rendering each ingredient row.

This matches Pantry's current rendering pattern.

Important detail:

- `getIngredientBreakdown` already reflects substituted ingredients.
- The UI can multiply raw weight and nutrient contribution by `dishTweaks[originalIngredientKey] ?: 1f`.
- Removed ingredients should remain zero or hidden from nutrient display.

Potential future refactor:

- Add a calculator overload that returns tweak-adjusted ingredient breakdown directly.
- This would reduce duplicated UI math but is not necessary for the first implementation.

### Step 7: Add tweak controls to Summary ingredient sheets

File:

- `app/src/main/java/com/calorieko/app/ui/screens/LogMealScreen.kt`

Both Summary ingredient sheets should gain:

- Active tweak map:

```kotlin
var activeTweaks by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
```

- Parse tweaks when opening the sheet:

```kotlin
activeTweaks = parseTweaksJson(dish.tweaksJson)
```

- A row-level stepper for non-removed ingredients:

```text
0.25x  0.5x  1x  1.5x  2x  3x  4x
```

Use Pantry's existing behavior:

- `1x` resets/removes the tweak.
- Controls are hidden for removed ingredients.
- Controls are hidden for qualitative portion strings via `PortionScaler.isQualitative`.
- Display values use `PortionScaler.scale(portionQuantity, tweakMultiplier)` when portion text exists.

### Step 8: Update planned meal Summary restrictions

Current `ManualMealSummaryOverlay` displays a planned-meal restriction:

```text
Further adjustments cannot be made to planned meals here.
Please modify your planned meals in the Pantry Screen prior to logging.
```

This is no longer accurate if ingredient tweaking is allowed during logging.

Replace it with a narrower message:

```text
Pantry recipe substitutions are preserved from the plan.
Ingredient amounts can be adjusted here for this logged meal only.
```

If the product decision is to keep planned substitutions/removals locked, keep those actions disabled for planned meals. Tweak controls can still be enabled because they are log-specific and do not mutate the meal plan.

### Step 9: Add active customization indicators

Recommended UI indicators:

- Dish card badge when `substitutionsJson` or `tweaksJson` is non-blank.
- Ingredient sheet banner when tweaks are active.
- Row-level highlight color for tweaked ingredients.

Suggested copy:

```text
2 ingredient amounts adjusted
Nutrition updated for this logged portion
```

For planned meals, avoid implying the plan itself is changed:

```text
Adjusted for this log only
```

### Step 10: Consider extracting duplicated Summary ingredient sheet UI

`LogMealScreen.kt` has duplicated ingredient sheet logic for manual and AI flows.

Recommended refactor:

Create a reusable private composable such as:

```kotlin
private fun LoggedDishIngredientSheet(
    dish: LoggedDish,
    dishIndex: Int,
    isPlannedMeal: Boolean,
    ingredientBreakdown: Map<String, IngredientNutritionBreakdown>?,
    activeSubstitutions: Map<String, String>,
    activeTweaks: Map<String, Float>,
    onApplySubstitution: (Map<String, String>) -> Unit,
    onApplyTweak: (String, Float) -> Unit,
    onReloadBreakdown: suspend (Map<String, String>) -> Map<String, IngredientNutritionBreakdown>,
    getSubstitutesForIngredient: suspend (String) -> List<RawIngredientEntity>,
    formatIngredientName: (String) -> String
)
```

This is optional but recommended because the current duplication increases the chance that manual, AI, and quick-log behavior diverge.

Pragmatic first pass:

- Add helpers and update both existing blocks directly.
- Refactor after behavior is verified.

## Data Flow After Implementation

### Manual entry

```text
User selects dish and weight
ManualLogViewModel.addDish
LoggedDish(tweaksJson = "")
Summary ingredient sheet
User tweaks ingredient amount
ManualLogViewModel.applyIngredientTweakToDish
LoggedDish nutrients and tweaksJson updated
Confirm meal saves final nutrients
```

### AI plus smart scale

```text
AI identifies dish
Scale supplies weight
LogMealViewModel.logCurrentDish
LoggedDish(tweaksJson = "")
Summary ingredient sheet
User tweaks ingredient amount
LogMealViewModel.applyIngredientTweakToDish
LoggedDish nutrients and tweaksJson updated
Confirm meal saves final nutrients
```

### Dashboard planned meal quick-log

```text
Pantry saves PlannedMealEntity(substitutionsJson, scaledServings, tweaksJson)
Dashboard copies planned customizations into QuickLogBridge
ManualLogViewModel.quickLogSlotFromPlan
User supplies actual cooked weight
buildLoggedDishFromPlannedEntry applies substitutions + tweaks
LoggedDish(substitutionsJson, tweaksJson)
Summary shows preserved customizations
User can optionally adjust ingredient amounts for this log
Confirm meal saves final nutrients
```

## Edge Cases

### No-ingredient dishes

Expected:

- No ingredient controls.
- No crash.
- Portion nutrition still calculated from direct dish data.

### Removed ingredient with an existing tweak

Recommended behavior:

- If an ingredient is removed, ignore its tweak.
- Optionally remove that ingredient key from `tweaksJson` during removal.
- If removal is undone, the previous tweak does not need to be restored unless a product requirement says otherwise.

### Substituted ingredient with a tweak

Recommended behavior:

- Tweaks remain keyed by original ingredient key.
- This matches Pantry and `RecipeNutritionCalculator.calculateWithTweaks`.
- The multiplier applies to the substituted ingredient's raw weight contribution.

### Tweak reset to 1x

Expected:

- Remove the key from the tweak map.
- If the tweak map becomes empty, serialize to `""`.
- Recalculate using substitutions only if substitutions remain.

### Malformed JSON

Expected:

- Existing parse behavior returns empty map.
- No crash.
- Summary displays base or substitution-only values.

### Qualitative portions

Expected:

- Hide amount stepper when `PortionScaler.isQualitative(portionQuantity)` is true.
- Still show substitution or removal actions when appropriate.

## Verification Plan

### Unit-level or ViewModel tests

Add tests where feasible for:

- Planned quick-log carries `tweaksJson` into `LoggedDish`.
- Applying a tweak updates nutrients and `tweaksJson`.
- Applying a substitution after a tweak preserves the tweak.
- Removing an ingredient after a tweak does not crash and recalculates correctly.
- Resetting a tweak removes the key and recalculates.
- Empty or malformed `tweaksJson` behaves like no tweaks.

### Manual QA: Manual entry

1. Log a normal recipe manually.
2. Open Summary.
3. Open ingredients.
4. Apply a 2x tweak to one ingredient.
5. Confirm dish card calories and macro totals update.
6. Reset to 1x and confirm values return to baseline.

### Manual QA: AI plus smart scale

1. Recognize a dish.
2. Log with scale weight.
3. Open Summary.
4. Apply a tweak.
5. Confirm dish card, meal totals, and full nutrient totals update.

### Manual QA: Dashboard planned meal

1. In Pantry, customize a recipe with:
   - A substitution
   - An optional ingredient removal
   - An ingredient tweak
   - A scaled serving count
2. Add it to today's calendar.
3. Go to Dashboard.
4. Quick-log the meal.
5. Enter or scale-capture actual cooked weight.
6. In Summary, confirm:
   - Substitutions/removals are visible.
   - Ingredient tweaks are visible.
   - Scaled servings do not affect logged nutrients.
   - Further log-only ingredient tweaking works.

### Manual QA: Store-bought dishes

1. Quick-log or manually log a lechon manok dish.
2. Open ingredients.
3. Confirm no ingredient breakdown appears.
4. Confirm no tweak controls appear.
5. Confirm meal can still be saved.

## Implementation Risks

### Risk 1: Duplicate Summary UI divergence

There are two similar ingredient sheet implementations in `LogMealScreen.kt`. Updating only one will create inconsistent behavior.

Mitigation:

- Update both flows in the first pass.
- Consider extracting a shared composable after behavior is stable.

### Risk 2: Recalculation drops existing customization state

Substitution and tweak functions can overwrite each other if each function only serializes its own state.

Mitigation:

- Every recalculation should parse both `substitutionsJson` and `tweaksJson`.
- Every update should write back only the changed JSON while preserving the other.

### Risk 3: Planned meal wording implies plan mutation

If users tweak a planned meal during logging, the planned meal should not be edited.

Mitigation:

- Use copy like "for this log only."
- Keep persistence scoped to the final logged meal nutrients.

### Risk 4: Cooked weight estimate for tweaks may surprise users

Tweaks estimate a new cooked batch weight using `dishYieldFactor`. This is mathematically consistent with current Pantry behavior but still an estimate.

Mitigation:

- Avoid overpromising precision in UI copy.
- Continue using "estimated" language around nutrition.

## Recommended Code Change Order

1. Add `tweaksJson` to `LoggedDish`.
2. Preserve `entry.tweaksJson` in planned quick-log `LoggedDish`.
3. Add tweak parse and serialization helpers in both ViewModels.
4. Add a shared private function in each ViewModel to recalculate a `LoggedDish` from substitutions and tweaks.
5. Update existing substitution/removal functions to use the shared recalculation path.
6. Add ingredient tweak update functions.
7. Update Manual Summary ingredient sheet UI.
8. Update AI Summary ingredient sheet UI.
9. Update planned meal restriction copy.
10. Run compile checks and targeted manual QA.

## Definition of Done

- Manual, AI plus smart scale, and planned quick-log Summary phases all support individual ingredient tweaking.
- Planned `tweaksJson` survives into `LoggedDish`.
- Substitution, removal, and tweak operations compose correctly.
- Recipe scaling is not used in LogMeal nutrition calculation.
- Store-bought/no-ingredient dishes remain stable.
- Meal confirmation saves the final recalculated nutrients.
- The implementation compiles and passes available tests.

