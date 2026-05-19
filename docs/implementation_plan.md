# Implementation Plan: Fix Duplicate Lechon Manok Entry

## Problem

The Explore screen shows two "Lechon Manok - Pakpak" entries:
- ✅ **"Rotisserie Chicken - Wing"** — correct, from `DISH_RECIPES_TABLE` (key: `chicken_wing`)
- ❌ **"Lechon Manok - Wings"** — duplicate with wrong nutrition, from `FOOD_TABLE` (key: `chicken_wings`)

## Root Cause

A historical key rename from `chicken_wings` → `chicken_wing` was not propagated to all data sources. The Firestore admin server still has the old `chicken_wings` entry, which gets synced into `FOOD_TABLE`. The [ExploreViewModel](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/viewmodel/ExploreViewModel.kt#L145-L198) dedup filter (line 178) compares by `mlLabel`, so `chicken_wings ≠ chicken_wing` → duplicate leaks through.

## Affected Locations

| # | File | Line(s) | Issue |
|---|---|---|---|
| 1 | **Firestore server** | — | Stale `chicken_wings` document in food catalog |
| 2 | [assemble_dishes.py](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/tools/assemble_dishes.py#L175) | 175, 253 | Uses `chicken_wings` key |
| 3 | [convert_portions.py](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/tools/convert_portions.py#L186) | 186 | Uses `chicken_wings` key |
| 4 | [PantryScreen.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/ui/screens/PantryScreen.kt#L4157) | 4157 | USDA URL map uses `chicken_wings` |

---

## Proposed Changes

### Item 1: Firestore Cleanup + Force Local Re-seed

The stale `chicken_wings` entry lives in the server food catalog and gets synced into `FOOD_TABLE` via [FoodCatalogSyncManager.pullFoodCatalog()](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/remote/api/FoodCatalogSyncManager.kt#L27-L70).

> [!IMPORTANT]
> **Server-side action required (manual).** The `chicken_wings` document must be deleted from the Firestore `food_catalog` collection. This is an admin action outside the mobile codebase.

To ensure users who already synced the stale entry get cleaned up on next app launch:

#### [MODIFY] [FoodDatabaseCallback.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/local/FoodDatabaseCallback.kt#L85)

Bump `CURRENT_JSON_REFERENCE_VERSION` from `2` → `3`. This forces a full re-seed of all three JSON-backed tables (`RAW_INGREDIENTS_TABLE`, `DISH_RECIPES_TABLE`, `RECIPE_INGREDIENTS_TABLE`) on next app open.

```diff
-private const val CURRENT_JSON_REFERENCE_VERSION = 2
+private const val CURRENT_JSON_REFERENCE_VERSION = 3
```

**Why this works:** When the app opens and detects `seededVersion < CURRENT_JSON_REFERENCE_VERSION`, the `seedFromJson()` method at [line 119](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/local/FoodDatabaseCallback.kt#L119-L148) deletes all rows from the three tables and re-inserts from the JSON assets. This ensures the `DISH_RECIPES_TABLE` has the correct 29 dishes with `chicken_wing` (singular).

**However**, this alone doesn't clean `FOOD_TABLE` — that table is seeded separately from `dish_labels_and_values.csv`. Since `dish_labels_and_values.csv` already uses the correct `chicken_wing` key (line 21), users who haven't synced from the server will be fine. For users who *have* synced, the next `pullFoodCatalog()` will pick up the server-side deletion.

#### [MODIFY] [FoodCatalogSyncManager.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/remote/api/FoodCatalogSyncManager.kt#L38-L44)

Add a **one-time local cleanup** step to delete known stale labels from `FOOD_TABLE` before syncing. This handles the case where a user has the stale `chicken_wings` in their local DB but the server deletion hasn't propagated yet.

```diff
 // 1. Get USDA-protected labels (dishes with System B recipes)
 val protectedLabels = db.dishRecipeDao().getAllDishLabels().toSet()
 
+// 1a. Clean up known stale labels from FOOD_TABLE
+// These are historical key renames where the old key leaked into FOOD_TABLE
+// via a previous server sync.
+val staleLabels = listOf("chicken_wings")
+db.foodDao().deleteByMlLabels(staleLabels)
+
 // 2. Perform atomic sync in FoodDao
```

> [!NOTE]
> This is a defensive one-time cleanup. Once all users have rotated through a sync cycle, this code can be removed in a future release.

---

### Item 2: Fix `assemble_dishes.py`

#### [MODIFY] [assemble_dishes.py](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/tools/assemble_dishes.py)

This script is a secondary/legacy tool (not the canonical pipeline), but its stale keys could cause confusion if ever run. Two changes:

**Line 175** — Rename key in `DISH_METADATA`:
```diff
-    "chicken_wings": {
-        "name_en": "Lechon Manok - Wings",
+    "chicken_wing": {
+        "name_en": "Lechon Manok - Wing",
```

**Line 253** — Rename key in `YIELD_FACTORS`:
```diff
-    "chicken_wings":        1.00,
+    "chicken_wing":         1.00,
```

Also add a deprecation header similar to `convert_portions.py`, since `generate_dish_jsons.py` is the canonical pipeline.

---

### Item 3: Fix `convert_portions.py`

#### [MODIFY] [convert_portions.py](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/tools/convert_portions.py#L186)

**Line 186** — Rename key in `EMPTY_PORTION_DEFAULTS`:
```diff
-    ("chicken_wings", "store_bought_lechon_manok_wing"): 90.0,
+    ("chicken_wing", "store_bought_lechon_manok_wing"): 90.0,
```

---

### Item 4: Fix `PantryScreen.kt`

#### [MODIFY] [PantryScreen.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/ui/screens/PantryScreen.kt#L4157)

**Line 4157** — Rename key in the USDA proof URL map:
```diff
-        "chicken_wings" to "https://fdc.nal.usda.gov/food-details/172830/nutrients",
+        "chicken_wing" to "https://fdc.nal.usda.gov/food-details/172830/nutrients",
```

---

## Open Questions

### Q1: Firestore Admin Access
Do you have direct access to the Firestore console to delete the `chicken_wings` document from the food catalog collection? If not, who manages the admin panel, and should I create a migration script instead?

### Q2: Defensive Dedup in ExploreViewModel
As a safety net, should I also add a dedup filter in [ExploreViewModel.loadAllDishes()](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/viewmodel/ExploreViewModel.kt#L178) to filter admin dishes by `name_ph` in addition to `mlLabel`? This would prevent any future key rename mismatches from surfacing as duplicates. The same pattern exists in [ManualLogViewModel](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/viewmodel/ManualLogViewModel.kt#L182).

Example:
```kotlin
// Current: only filters by mlLabel
.filter { it.mlLabel !in systemBLabels && it.mlLabel != "negative" }

// Proposed: also filter by Filipino name to catch key-rename duplicates
val systemBNamesPh = recipes.map { it.namePh }.toSet()
.filter { it.mlLabel !in systemBLabels && it.mlLabel != "negative" }
.filter { it.namePh !in systemBNamesPh }  // prevent name-based duplicates
```

---

## Verification Plan

### Automated Checks
1. Search the entire codebase for `chicken_wings` — must return **zero results** after all fixes
2. Verify `dish_labels_and_values.csv` line 21 still uses `chicken_wing` (unchanged, already correct)
3. Verify `dish_recipes.json` has exactly **29 dishes** with `chicken_wing` (not `chicken_wings`)

### Manual Verification
1. Build and launch the app
2. Navigate to Explore screen → verify only **one** "Lechon Manok - Pakpak" entry appears
3. Tap it → verify nutrition matches the System B values (385.5 cal, not 257 cal)
4. Navigate to Pantry → "Store-Bought Items" → verify "Lechon Manok - Pakpak" still appears correctly
5. Tap the USDA proof link → verify it opens the correct FDC page
