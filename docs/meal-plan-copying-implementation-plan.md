# Meal Plan Copying Implementation Plan

## Summary

Improve Meal Plan Calendar copying so users can safely reuse planned meals and customized dishes without accidental appends, silent actions, or past-date writes.

The refactor covers three copy paths:

1. Copy the current week to the next week.
2. Copy one entire meal slot, such as a Breakfast with multiple dishes, to a future day and slot.
3. Copy one planned dish, including saved customizations, to a future day and slot.

Core behavior:

- Week copy replaces the entire target week.
- Meal copy replaces the entire target meal slot.
- Dish copy inserts into the target slot, but replaces the same dish if that dish already exists there.
- Copy sources may be past, present, or future.
- Copy targets must be today or future.
- Copied meals preserve `substitutionsJson`, `scaledServings`, and `tweaksJson`.

## Current State

Relevant files:

- `app/src/main/java/com/calorieko/app/ui/screens/PantryScreen.kt`
- `app/src/main/java/com/calorieko/app/viewmodel/PantryViewModel.kt`
- `app/src/main/java/com/calorieko/app/data/local/MealPlanDao.kt`
- `app/src/main/java/com/calorieko/app/data/model/PlannedMealEntity.kt`
- `app/src/main/java/com/calorieko/app/data/remote/FirestoreSyncRepository.kt`

Current week copy flow:

- `PantryScreen.kt` renders a `Copy to Next Week` surface in `MealPlanCalendarSection`.
- The button directly calls `viewModel.copyWeekToNext()`.
- `PantryViewModel.copyWeekToNext()` fetches current-week meals, changes `weekStartDate` to next week, and calls `mealPlanDao.insertMeals(copiedMeals)`.
- `MealPlanDao.insertMeals()` uses `OnConflictStrategy.REPLACE`.

Current issues:

- No confirmation before replacing or copying.
- No snackbar or visible success/error feedback.
- The current behavior appends non-conflicting dishes because the primary key includes `dish_label`. `REPLACE` only replaces exact same dish keys, not the whole target week.
- Remote Firestore state can keep stale target-week documents unless the target scope is explicitly cleared before copied meals are synced.

## UX Design

### Week Copy

Keep the existing `Copy to Next Week` action in the calendar action row, but change it from immediate execution to a confirmation dialog.

Dialog content:

- Title: `Copy to Next Week?`
- Body:
  - Source week label.
  - Target week label.
  - Number of dishes being copied.
  - Warning: `Existing meals in the target week will be replaced.`
- Actions:
  - `Cancel`
  - `Copy & Replace`

Feedback:

- Success snackbar: `Copied week to May 25-31.`
- Empty source snackbar: `No meals to copy this week.`
- Horizon failure snackbar: `Next week is outside your planning range.`
- Unexpected failure snackbar: `Could not copy meal plan. Please try again.`

### Meal Copy

Add a `Copy Meal` action inside the meal-detail dialog for any populated meal slot.

Placement:

- In the sticky bottom action area of the meal-detail dialog.
- Use a neutral copy-style action near `Add Dish`.
- Show it even when the source meal is in the past, because reading a past source is allowed.

Flow:

1. User opens a meal slot detail, for example `May 18 - Breakfast`.
2. User taps `Copy Meal`.
3. A target picker opens with source summary: `May 18 Breakfast - 4 dishes`.
4. User selects target week, target day, and target meal slot.
5. If target slot has dishes, the confirmation copy says the target meal will be replaced.
6. User confirms.

Feedback:

- Success snackbar: `Copied Breakfast to May 25 Dinner.`
- Invalid target snackbar: `Choose today or a future date.`
- Empty source snackbar: `No dishes found in this meal.`

### Single Dish Copy

Add `Copy Dish` at the dish-card level in the meal-detail dialog.

Recommended UI:

- Add a compact overflow menu or action row on each planned dish card.
- Keep visible clutter low:
  - `View Recipe` remains available.
  - `Copy Dish` is available for every dish.
  - `Remove Dish` remains gated by editability.

Flow:

1. User opens a meal slot detail.
2. User chooses `Copy Dish` for one planned dish.
3. Target picker opens with source summary: dish display name and customized badge if applicable.
4. User chooses target week, day, and meal slot.
5. If the same dish already exists in the target slot, confirmation text says the existing dish customization will be replaced.
6. User confirms.

Dish copy conflict behavior:

- If target slot does not contain the dish, insert it.
- If target slot already contains the same `dishLabel`, replace that one dish row.
- Do not remove other dishes from the target slot.

## Architecture

### UI State

Add copy-related UI state inside `MealPlanCalendarSection`:

```kotlin
private sealed interface MealPlanCopySource {
    data class Week(val sourceWeekStart: String) : MealPlanCopySource
    data class MealSlot(
        val sourceWeekStart: String,
        val dayIndex: Int,
        val mealSlot: String
    ) : MealPlanCopySource
    data class Dish(
        val meal: PlannedMealEntity
    ) : MealPlanCopySource
}
```

Because `MealPlanCalendarSection` is in a Kotlin file with many composables, this sealed interface can be placed near the meal plan composables as a private top-level type.

For target selection:

```kotlin
var copySource by remember { mutableStateOf<MealPlanCopySource?>(null) }
var copyTargetWeekStart by remember { mutableStateOf(currentWeekStart) }
var copyTargetDayIndex by remember { mutableIntStateOf(-1) }
var copyTargetSlot by remember { mutableStateOf<String?>(null) }
var showCopyTargetPicker by remember { mutableStateOf(false) }
var showCopyConfirmDialog by remember { mutableStateOf(false) }
```

Reset target state whenever a new copy source is selected.

### ViewModel Events

Add a one-shot event flow to `PantryViewModel`:

```kotlin
sealed interface PantryUiEvent {
    data class Snackbar(val message: String) : PantryUiEvent
}
```

Expose it as:

```kotlin
private val _uiEvents = MutableSharedFlow<PantryUiEvent>()
val uiEvents: SharedFlow<PantryUiEvent> = _uiEvents.asSharedFlow()
```

`PantryScreen` should collect this flow and display snackbar messages through `SnackbarHostState`.

### ViewModel Copy API

Add explicit copy functions:

```kotlin
fun copyCurrentWeekToNextReplacing()

fun copyMealSlot(
    sourceWeekStart: String,
    sourceDayIndex: Int,
    sourceMealSlot: String,
    targetWeekStart: String,
    targetDayIndex: Int,
    targetMealSlot: String
)

fun copySingleDish(
    sourceMeal: PlannedMealEntity,
    targetWeekStart: String,
    targetDayIndex: Int,
    targetMealSlot: String
)
```

Rules enforced in ViewModel:

- Reject target dates before `LocalDate.now()`.
- Reject target weeks beyond `getMaxPlanningWeekStart()`.
- Reject empty source copy operations.
- Preserve customization fields.
- Emit snackbar events for success and user-recoverable failures.

### DAO API

Add transaction helpers to `MealPlanDao`:

```kotlin
@Transaction
suspend fun replaceWeek(weekStartDate: String, meals: List<PlannedMealEntity>) {
    clearWeek(weekStartDate)
    insertMeals(meals)
}

@Transaction
suspend fun replaceSlot(
    dayIndex: Int,
    weekStartDate: String,
    mealSlot: String,
    meals: List<PlannedMealEntity>
) {
    clearSlot(dayIndex, weekStartDate, mealSlot)
    insertMeals(meals)
}
```

Single dish copy can continue using `insertMeal()` because the existing primary key replaces the same dish in the same target slot without affecting other dishes.

### Firestore Sync

Mirror overwrite semantics remotely:

- Week copy:
  - Local: `replaceWeek(targetWeek, copiedMeals)`.
  - Remote: `clearWeekPlannedMeals(uid, targetWeek)`, then `syncPlannedMealsBatch(uid, copiedMeals)`.

- Meal copy:
  - Local: `replaceSlot(targetDayIndex, targetWeekStart, targetMealSlot, copiedMeals)`.
  - Remote: `deletePlannedMealSlot(uid, targetDayIndex, targetWeekStart, targetMealSlot)`, then `syncPlannedMealsBatch(uid, copiedMeals)`.

- Dish copy:
  - Local: `insertMeal(copiedMeal)`.
  - Remote: `syncPlannedMeal(uid, copiedMeal)`.

Keep existing `AutoSyncManager.triggerSync(appContext, uid)` calls after local changes so any failed best-effort remote writes are retried by the broader sync path.

## Detailed Implementation Steps

### Step 1: Add Snackbar Plumbing

1. Add `PantryUiEvent` and `uiEvents` to `PantryViewModel`.
2. In `PantryScreen`, create:

```kotlin
val snackbarHostState = remember { SnackbarHostState() }
```

3. Add `snackbarHost = { SnackbarHost(snackbarHostState) }` to the existing `Scaffold`.
4. Collect events:

```kotlin
LaunchedEffect(Unit) {
    viewModel.uiEvents.collect { event ->
        when (event) {
            is PantryUiEvent.Snackbar -> snackbarHostState.showSnackbar(event.message)
        }
    }
}
```

### Step 2: Fix Week Copy Overwrite

1. Rename or replace `copyWeekToNext()` with `copyCurrentWeekToNextReplacing()`.
2. Compute:
   - `sourceWeek = _currentWeekStart.value`
   - `targetWeek = LocalDate.parse(sourceWeek).plusWeeks(1)`
3. Reject if `targetWeek > getMaxPlanningWeekStart()`.
4. Load source meals.
5. If source is empty, emit snackbar and return.
6. Copy each meal with `weekStartDate = targetWeek`.
7. Call `mealPlanDao.replaceWeek(targetWeek, copiedMeals)`.
8. Clear and resync Firestore target week.
9. Recompute week scrubber data.
10. Emit success snackbar.

### Step 3: Add Week Copy Confirmation UI

1. Add `showCopyWeekDialog` state in `MealPlanCalendarSection`.
2. Change `Copy to Next Week` click handler to set `showCopyWeekDialog.value = true`.
3. Add an `AlertDialog`:
   - Shows source/target labels and dish count.
   - Explicitly says existing target-week meals will be replaced.
   - Confirm calls `viewModel.copyCurrentWeekToNextReplacing()`.

### Step 4: Add Meal and Dish Copy Entry Points

1. In meal-detail dialog:
   - Show `Copy Meal` when `slotMeals.isNotEmpty()`.
   - It should not require `isEditable`.
2. On click:
   - Set `copySource = MealPlanCopySource.MealSlot(currentWeekStart, dayIdx, slot)`.
   - Initialize target week to the current real-world week or displayed week if it is not past.
   - Show target picker.
3. In each dish card:
   - Add `Copy Dish`.
   - On click, set `copySource = MealPlanCopySource.Dish(meal)`.

### Step 5: Build Reusable Target Picker

Create a private composable such as:

```kotlin
@Composable
private fun MealPlanCopyTargetDialog(
    source: MealPlanCopySource,
    targetWeekStart: String,
    targetDayIndex: Int,
    targetSlot: String?,
    plannedMealsForTargetWeek: List<PlannedMealEntity>,
    onTargetWeekChange: (String) -> Unit,
    onTargetDayChange: (Int) -> Unit,
    onTargetSlotChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: PantryViewModel
)
```

Target picker behavior:

- Week navigator follows existing recipe plan dialog behavior.
- Previous week is disabled if it would go before current real-world week.
- Next week is disabled if it would go beyond planning horizon.
- Day chips use `viewModel.isDayEditableForWeek(dayIndex, targetWeekStart)`.
- Past target days are visually disabled and not clickable.
- Meal slot buttons show whether the target has existing dishes.
- Confirm button disabled until a valid day and slot are selected.

Target conflict text:

- Meal copy: if target slot has dishes, show `This will replace N existing dish(es).`
- Dish copy: if same dish exists, show `This will replace the existing copy of this dish.`
- Dish copy into a slot with other dishes: show `This will add this dish to the selected meal.`

### Step 6: Add Meal Copy ViewModel Logic

Implementation logic:

1. Validate target date is not past.
2. Fetch source meals from `sourceWeekStart`.
3. Filter by `sourceDayIndex` and `sourceMealSlot`.
4. If empty, emit snackbar and return.
5. Map meals to:

```kotlin
it.copy(
    weekStartDate = targetWeekStart,
    dayIndex = targetDayIndex,
    mealSlot = targetMealSlot
)
```

6. Call `mealPlanDao.replaceSlot(targetDayIndex, targetWeekStart, targetMealSlot, copiedMeals)`.
7. Clear and resync remote target slot.
8. Recompute scrubber data.
9. Emit success snackbar.

### Step 7: Add Dish Copy ViewModel Logic

Implementation logic:

1. Validate target date is not past.
2. Build copied meal:

```kotlin
sourceMeal.copy(
    weekStartDate = targetWeekStart,
    dayIndex = targetDayIndex,
    mealSlot = targetMealSlot
)
```

3. Call `mealPlanDao.insertMeal(copiedMeal)`.
4. Sync copied meal remotely.
5. Recompute scrubber data.
6. Emit success snackbar.

This preserves customization fields automatically because `copy()` keeps all unchanged fields.

### Step 8: Refresh Target Week Conflict Data

The target picker needs accurate conflict information for weeks other than the displayed week.

Use the existing `getPlannedMealsForWeekSnapshot(weekStartDate)` helper:

- Launch a `LaunchedEffect(copyTargetWeekStart)` to fetch target-week planned meals.
- Store in local picker state.
- Re-fetch after copy operations only if the picker remains open; otherwise dismiss.

### Step 9: Remove or Deprecate Old Immediate Copy

After the new week-copy function is wired:

- Remove direct calls to `copyWeekToNext()`.
- Either delete old `copyWeekToNext()` or keep it as a wrapper around `copyCurrentWeekToNextReplacing()` if backward compatibility is needed.
- Update comments to avoid saying `REPLACE` means whole-week overwrite.

## Edge Cases

- Source week has no meals: no copy, snackbar only.
- Target week is beyond planning horizon: no copy, snackbar only.
- Source is past but target is today/future: allowed.
- Target is yesterday or earlier: blocked in both UI and ViewModel.
- Copy meal to same exact source slot:
  - Allowed but effectively rewrites the same slot with the same dishes.
  - Optional UX improvement: disable confirm and show `Source and target are the same.`
- Copy single dish to same exact source slot:
  - Allowed but no visible change.
  - Optional UX improvement: disable confirm.
- Remote sync failure:
  - Local operation succeeds.
  - Trigger `AutoSyncManager`.
  - Snackbar should not claim cloud sync success; use local user-facing language like `Copied meal.`
- Large meal slots:
  - Existing scrollable meal detail dialog handles many dishes.
  - Target picker should use bounded scroll if target summaries grow.

## Acceptance Criteria

Week copy:

- Tapping `Copy to Next Week` opens confirmation.
- Confirming replaces the entire target week, removing old target-only dishes.
- Copied dishes preserve customization data.
- Success snackbar appears.
- No Gradle or build behavior changes are required by the feature itself.

Meal copy:

- A past meal can be copied to today or a future day.
- Past target days are disabled.
- Copying a meal to a populated target slot replaces that slot only.
- Other slots on the target day remain unchanged.

Dish copy:

- A single planned dish can be copied to a future slot.
- If the target slot already has other dishes, they remain.
- If the target slot has the same dish, that dish row is replaced.
- Customizations are preserved.

Data consistency:

- Local Room state and Firestore overwrite semantics match.
- Week scrubber meal counts update after copy.
- Duplicate dish rows in the same target slot are still prevented by the existing composite primary key.

## Manual Review Checklist

Before completing implementation:

- Confirm all new imports are used.
- Confirm no old immediate `copyWeekToNext()` button path remains.
- Confirm all copy writes validate the target date in the ViewModel.
- Confirm transaction helpers are annotated with `@Transaction`.
- Confirm Firestore target-clearing matches local overwrite scope.
- Confirm snackbar collection is scoped once in `PantryScreen`.
- Confirm no copy operation writes to a past date.
- Confirm customization fields are not dropped during `copy()`.
