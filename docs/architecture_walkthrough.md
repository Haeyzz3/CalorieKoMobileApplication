# CalorieKo Current Architecture Walkthrough

Updated: 2026-05-31  
Scope: current codebase state, with the previous CSV-to-JSON refactor treated only as historical context.

## Executive Summary

CalorieKo is an offline-first Android app built around local reference data, local user data, and optional cloud synchronization. The core nutrition features do not call USDA FoodData Central at runtime. USDA data is collected by Python tooling during development, serialized into static JSON files under `app/src/main/assets`, bundled into the APK, and loaded into the local Room database.

Current reference-data state:

| Asset | Current count | Runtime status | Primary role |
|---|---:|---|---|
| `raw_ingredients.json` | 87 ingredients | Active | Authoritative per-100g ingredient nutrient data |
| `dish_recipes.json` | 29 dishes | Active | Dish metadata, cooked weights, yield factors, per-serving nutrients |
| `recipe_ingredients.json` | 196 rows | Active | Dish-to-ingredient bridge with raw gram weights |
| `dish_labels_and_values.csv` | 29 rows | Active, but legacy/bootstrap | Seeds `FOOD_TABLE` unless server catalog sync has taken over |
| `dish_ingredients.csv` | 196 rows | Active | Seeds pantry matching and ingredient-detail table |
| `nutrient_daily_values.csv` | Reference CSV | Active | Daily nutrient target/reference values |
| `labels.txt` and `calorieko_model.tflite` | ML assets | Active | On-device image classification |

The most important current-state finding is that the legacy CSV files are not fully obsolete. They are still opened and parsed by runtime code. However, they are no longer the authority for USDA-derived nutrient calculations. The JSON assets and Room tables behind `RecipeNutritionCalculator` are the authority for ingredient-level nutrition.

## Technology Stack

| Layer | Current implementation |
|---|---|
| App platform | Android, Kotlin, Jetpack Compose |
| Presentation pattern | Screen composables backed by ViewModels and StateFlow |
| Local database | Room, currently schema version 30 |
| Database encryption | SQLCipher via `SupportFactory` |
| Local reference data | APK assets parsed into Room on database open |
| ML | TensorFlow Lite model loaded from assets |
| Scale integration | BLE scale manager and log-meal ViewModels |
| Auth | Firebase Auth |
| Cloud user-data sync | Firestore and Laravel API sync endpoints |
| Food catalog sync | Laravel admin catalog pulled through Retrofit, with USDA/System-B protection |
| Development data pipeline | Python scripts under `tools/` and `scripts/` using USDA FoodData Central |

## Architectural Model

The codebase currently has two food-data systems that coexist.

### System B: Ingredient-Level JSON Nutrition

System B is the current authoritative nutrition model for built-in USDA-backed dishes.

```text
USDA FoodData Central API
        |
        | development-time Python scripts
        v
raw_ingredients.json
        |
        | generate dish recipe assets
        v
recipe_ingredients.json + dish_recipes.json
        |
        | APK asset bundle
        v
FoodDatabaseCallback.onOpen()
        |
        | FoodJsonParser
        v
Room tables:
  RAW_INGREDIENTS_TABLE
  DISH_RECIPES_TABLE
  RECIPE_INGREDIENTS_TABLE
        |
        v
RecipeNutritionCalculator
```

System B tracks 12 nutrient fields:

```text
calories, protein, carbs, fat, fiber, sugar,
sodium, potassium, vitamin_a, vitamin_c, calcium, iron
```

All ingredient nutrient values are per 100 grams. Vitamin A is stored as micrograms RAE, not obsolete international-unit values.

### System A: Legacy/Admin Food Catalog

System A is the older flat dish catalog. It is still present because it is also the structure used for admin-managed food catalog sync.

```text
dish_labels_and_values.csv
        |
        | FoodCsvParser
        v
FOOD_TABLE / FoodItem
        |
        +-- fallback/admin-only dishes in Explore and Manual Log
        +-- protected from overwriting System B dishes during server catalog sync
```

The current app filters out `FOOD_TABLE` dishes whose `ml_label` already exists in `DISH_RECIPES_TABLE` when building user-facing food lists. That means built-in System B dishes are shown from JSON-derived Room tables, while server/admin-only foods can still be shown from `FOOD_TABLE`.

### Pantry Ingredient Matching Table

`dish_ingredients.csv` is not obsolete. It still seeds `DISH_INGREDIENTS_TABLE`, and the pantry matching engine queries that table.

```text
dish_ingredients.csv
        |
        | FoodCsvParser.parseDishIngredients()
        v
DISH_INGREDIENTS_TABLE
        |
        v
PantryDao.getDishMatchCounts()
PantryDao.getMissingIngredients()
PantryDao.getIngredientDetailsForDish()
```

This data overlaps heavily with `recipe_ingredients.json`, but the runtime pantry SQL currently depends on the CSV-seeded table.

## Runtime Database Seeding

`FoodDatabaseCallback.onOpen()` is the central reference-data bootstrap path. It runs whenever the Room database opens.

### Legacy CSV Seeding

The callback checks:

```text
hasSyncedFromServer = reference_data_seed.food_catalog_synced
dishCount = DISH_INGREDIENTS_TABLE row count
foodCount = FOOD_TABLE row count
```

If the app has not pulled the admin catalog and either the food catalog or dish ingredients are empty, it calls `populateDatabase()`, which:

1. Deletes all rows from `FOOD_TABLE`.
2. Deletes all rows from `DISH_INGREDIENTS_TABLE`.
3. Reads `dish_labels_and_values.csv`.
4. Parses rows into `FoodItem`.
5. Reads `dish_ingredients.csv`.
6. Parses rows into `DishIngredient`.

If the admin catalog has already synced, CSV reseeding of `FOOD_TABLE` is skipped because the server catalog becomes authoritative for admin-added foods. `DISH_INGREDIENTS_TABLE` can still be reseeded from `dish_ingredients.csv` because that table is not synced from the server.

### JSON Seeding

The same callback also checks:

```text
raw ingredient count
dish recipe count
recipe ingredient count
stored JSON reference version
raw_ingredients.json asset count
```

It reseeds JSON-backed tables when any table is empty, a previous seed was partial, the asset has more ingredients than the database, or the stored JSON reference version is behind `CURRENT_JSON_REFERENCE_VERSION`.

`seedFromJson()` clears and reloads tables in foreign-key-safe order:

1. Delete `RECIPE_INGREDIENTS_TABLE`.
2. Delete `DISH_RECIPES_TABLE`.
3. Delete `RAW_INGREDIENTS_TABLE`.
4. Insert parsed `raw_ingredients.json`.
5. Insert parsed `dish_recipes.json`.
6. Insert parsed `recipe_ingredients.json`.

## Main Room Tables

### Reference Tables

| Table | Entity | Seed source | Current use |
|---|---|---|---|
| `FOOD_TABLE` | `FoodItem` | `dish_labels_and_values.csv` and admin catalog sync | Admin-only food catalog, legacy fallback, server sync target |
| `DISH_INGREDIENTS_TABLE` | `DishIngredient` | `dish_ingredients.csv` | Pantry matching, ingredient details, autocomplete |
| `RAW_INGREDIENTS_TABLE` | `RawIngredientEntity` | `raw_ingredients.json` | Ingredient nutrient lookup and substitution candidates |
| `DISH_RECIPES_TABLE` | `DishRecipeEntity` | `dish_recipes.json` | Built-in dish catalog and per-serving nutrition |
| `RECIPE_INGREDIENTS_TABLE` | `RecipeIngredientEntity` | `recipe_ingredients.json` | Ingredient gram weights for dynamic nutrition |

### User Data Tables

| Table | Entity | Purpose |
|---|---|---|
| `user_profile` | `UserProfile` | User profile, goals, onboarding state |
| `activity_log_table` | `ActivityLogEntity` | Workout/activity records |
| `meal_log_table` | `MealLogEntity` | Meal log headers |
| `meal_log_item_table` | `MealLogItemEntity` | Logged dishes and nutrient values |
| `daily_nutrition_summary_table` | `DailyNutritionSummaryEntity` | Daily nutrient totals cache |
| `PANTRY_TABLE` | `PantryItem` | User pantry inventory |
| `PLANNED_MEALS_TABLE` | `PlannedMealEntity` | Weekly meal plan, substitutions, scaling, tweaks, status |
| `weight_log_table` | `WeightLogEntity` | Append-only weight history |

## Nutrition Calculation

`RecipeNutritionCalculator` is the core runtime nutrition engine.

For a normal dish portion:

```text
portionFraction = cookedWeightGrams / dish.cookedWeightG
totalDishNutrients = dish.perServingNutrients * dish.servings
portionNutrients = totalDishNutrients * portionFraction
```

For substitutions and tweaks, it recalculates from recipe ingredient rows:

```text
for each recipe ingredient:
    effectiveKey = substitution[ingredientKey] ?: ingredientKey
    adjustedWeight = raw_weight_grams * tweakMultiplier
    contribution = rawIngredientNutrientsPer100g * (adjustedWeight / 100)

perServing = sum(contributions) / dish.servings
```

Special cases:

- Removed ingredients are represented by the sentinel `__REMOVED__`.
- Non-substitutable ingredients are excluded from substitute candidate queries.
- Store-bought dishes with no recipe rows preserve precomputed dish-level values.
- Per-ingredient breakdowns are returned for UI display and include original/replacement metadata.

## Runtime Feature Flows

### AI Camera Meal Logging

```text
Camera frame
  -> CalorieKoClassifier loads calorieko_model.tflite and labels.txt
  -> top ML label, e.g. "sinigang_pork"
  -> LogMealViewModel queries DishRecipeDao.getByDishLabel(label)
  -> BLE scale supplies cooked weight
  -> RecipeNutritionCalculator calculates portion nutrients
  -> MealRepository writes MealLogEntity and MealLogItemEntity locally
  -> optional cloud sync runs later/alongside local persistence
```

The current camera path no longer needs `FOOD_TABLE` to compute nutrition for built-in dishes. It resolves recognized labels directly against `DISH_RECIPES_TABLE`.

### Manual Meal Logging

Manual logging loads:

1. All System B dishes from `DishRecipeDao.getAllDishRecipes()`.
2. Admin-only `FoodItem` rows from `FOOD_TABLE` whose `ml_label` is not in System B.

Admin-only foods are adapted into `DishRecipeEntity`-shaped objects so the UI can search and select them through the same flow. Built-in dishes use `RecipeNutritionCalculator`; admin-only dishes use flat per-100g values.

### Pantry Matching

```text
User pantry items
  -> RawIngredientDao expands substitutable items by sub_category
  -> PantryDao.getDishMatchCounts(expandedPantry)
  -> weighted readiness score:
       core ingredient = 3 points
       optional ingredient = 1 point
  -> DishRecipeDao supplies names, serving weights, and nutrition
  -> RecipeNutritionCalculator supplies substitution-aware recalculation
```

`dish_ingredients.csv` remains active here because the SQL matching engine reads `DISH_INGREDIENTS_TABLE`.

### Explore Screen

`ExploreViewModel` builds the list in two parts:

1. System B dishes from `DISH_RECIPES_TABLE`, displayed as USDA/FDC-backed.
2. Admin-only foods from `FOOD_TABLE`, excluding any label already present in System B.

Ingredient display names are resolved through `RAW_INGREDIENTS_TABLE` where possible.

### Daily Values

`NutritionalValuesRepository` reads `nutrient_daily_values.csv` from assets. This CSV is separate from the legacy dish CSVs and remains active for dashboard/reference calculations.

## CSV Obsolescence and Utility Check

### `dish_labels_and_values.csv`

Verdict: active, but no longer authoritative for built-in dish nutrition.

Evidence in current code:

- `FoodDatabaseCallback.populateDatabase()` opens `dish_labels_and_values.csv`.
- `FoodCsvParser.parse()` maps rows into `FoodItem`.
- `FOOD_TABLE` is still queried by `ManualLogViewModel` and `ExploreViewModel` for admin-only foods.
- `FoodCatalogSyncManager` syncs server foods into `FOOD_TABLE` while protecting labels that exist in `DISH_RECIPES_TABLE`.

Current role:

- Bootstrap/fallback seed for `FOOD_TABLE` before server food catalog sync.
- Compatibility structure for admin-managed foods.
- Not the source of truth for System B nutrient math.

Retirement path:

- Keep admin-added foods entirely server-driven or move them to a JSON/Room structure with explicit provenance.
- Remove any dependency on CSV-seeded `FOOD_TABLE` for built-in labels.
- Ensure manual/explore/search flows still have a source for admin-only foods.

### `dish_ingredients.csv`

Verdict: actively used at runtime.

Evidence in current code:

- `FoodDatabaseCallback` opens `dish_ingredients.csv` even after server catalog sync if `DISH_INGREDIENTS_TABLE` is empty.
- `FoodCsvParser.parseDishIngredients()` maps it into `DishIngredient`.
- `PantryDao` queries `DISH_INGREDIENTS_TABLE` for matching, missing ingredients, autocomplete, and ingredient-detail display.

Current role:

- Pantry recipe matching.
- Ingredient details in recipe cards.
- Ingredient autocomplete and grouping.

Retirement path:

- Rewrite pantry matching queries to use `RECIPE_INGREDIENTS_TABLE`.
- Resolve display names and categories from `RAW_INGREDIENTS_TABLE`.
- Generate any needed detail view from the JSON-backed recipe rows.

### Are the CSVs useful as static LLM reference files?

Yes. Even though they should not be treated as the nutrient authority, they are valuable for LLM-assisted database management because they are compact, tabular, and easy to diff.

Useful properties:

- `dish_labels_and_values.csv` gives a quick label/name/category index for all 29 known dishes.
- `dish_ingredients.csv` gives a readable recipe matrix with `ml_label`, `ingredient_name`, core/optional type, category, original portion text, preparation method, and step.
- The CSVs are easier for humans and LLMs to audit than deeply nested JSON.
- They are useful for coverage checks, label consistency, ingredient naming consistency, and recipe audit prompts.

Important limitations:

- The CSVs duplicate information now present in JSON and Room.
- Duplicated data can drift if there is no generator or validation gate.
- Flat nutrition values in `dish_labels_and_values.csv` should not be used to audit USDA-derived nutrient calculations.
- For future database management, treat CSVs as review and authoring aids, then validate against JSON outputs before shipping.

Recommended policy:

```text
Author/review in CSV if it improves readability.
Generate or validate JSON from the CSV.
Treat raw_ingredients.json, recipe_ingredients.json, and dish_recipes.json as the shipping reference assets.
Run validation scripts before release.
```

## Development-Time USDA Pipeline

The USDA FoodData Central API is used by Python tooling, not by the Android runtime nutrition engine.

### Canonical Nutrient Schema

`tools/usda_nutrient_schema.py` centralizes nutrient extraction.

Key responsibilities:

- Defines the 12 tracked nutrient keys.
- Defines nutrient units.
- Maps CalorieKo nutrient keys to USDA nutrient IDs.
- Extracts nutrients from both detail API responses and search API results.
- Prevents obsolete Vitamin A mapping by using USDA nutrient `1106` for RAE micrograms and disallowing nutrient `1104` international units.

This file is the common dependency for the important USDA scripts.

### `tools/fetch_usda_data.py`

Purpose: initial bulk generation of `raw_ingredients.json`.

Inputs:

- Hardcoded `INGREDIENT_MAP` of ingredient keys, display names, categories, subcategories, and FDC IDs.
- Hardcoded manual overrides for ingredients without suitable FDC entries.
- `--api-key` argument.
- Optional `--output` argument.
- Optional `--dry-run`.

API interaction:

```text
GET https://api.nal.usda.gov/fdc/v1/food/{fdc_id}?api_key={api_key}
```

Behavior:

1. Iterates every ingredient in `INGREDIENT_MAP`.
2. Uses manual override data for items such as water, food coloring, sinigang mix, and pansit-pansitan.
3. Caches duplicate FDC IDs so repeated ingredients do not trigger repeated API calls.
4. Fetches USDA detail records for FDC-backed ingredients.
5. Retries rate-limit and transient failures.
6. Extracts the canonical 12 nutrients via `extract_nutrients_from_detail()`.
7. Extracts USDA portion metadata from `foodPortions`.
8. Infers `data_source` from the USDA `dataType`.
9. Writes the final list to `app/src/main/assets/raw_ingredients.json`.

Failure behavior:

- Missing or failed fetches become `FETCH_FAILED` entries with zeroed nutrients.
- The script still writes a complete JSON array so failures can be audited.

### `tools/add_substitutes.py`

Purpose: append additional substitution candidates to `raw_ingredients.json`.

Inputs:

- Hardcoded `NEW_INGREDIENTS` list.
- Existing `raw_ingredients.json`.
- `--api-key` argument.
- Optional `--dry-run`.

API interaction:

```text
GET https://api.nal.usda.gov/fdc/v1/food/{fdc_id}?api_key={api_key}
```

Behavior:

1. Loads existing `raw_ingredients.json`.
2. Builds a set of existing `ingredient_key` values.
3. Skips any substitute already present.
4. Fetches each new ingredient from FoodData Central.
5. Extracts the canonical 12 nutrients.
6. Extracts up to three portion descriptions.
7. Appends each new ingredient with `data_source = "USDA_FDC"`.
8. Writes the updated array back to `raw_ingredients.json`.

This script expanded substitute coverage across oils, poultry, pork, noodles, grains, citrus juices, and smoked fish candidates.

### `scripts/generate_corrected_json.py`

Purpose: verify, correct, remove, or preserve raw ingredient entries using an explicit verified FDC map.

Inputs:

- Existing `raw_ingredients.json`.
- Hardcoded `VERIFIED_FDC` map:
  - `keep`: keep the FDC ID but re-fetch and normalize nutrients.
  - `update`: replace with a corrected FDC ID and re-fetch nutrients.
  - `remove`: remove the ingredient from the JSON.
  - `manual`: keep current values without an API call.

API interaction:

```text
GET https://api.nal.usda.gov/fdc/v1/food/{fdc_id}?api_key={API_KEY}
```

Behavior:

1. Loads the existing raw ingredient list.
2. Iterates each ingredient by `ingredient_key`.
3. Passes through keys not present in `VERIFIED_FDC`.
4. Removes entries marked `remove`.
5. Preserves entries marked `manual`.
6. For `keep` and `update`, fetches the target FDC detail record.
7. Extracts canonical nutrient values and portions.
8. Builds corrected entries with updated FDC IDs and nutrient values.
9. Runs sanity checks:
   - produce calorie values should not be unusually high,
   - protein calorie values should be within broad expected bounds,
   - salt should have zero calories,
   - macro-derived calories should be roughly consistent with total calories,
   - salt sodium should be in the expected range.
10. Writes a backup to `raw_ingredients.json.backup`.
11. Overwrites `raw_ingredients.json`.

The script is a correction/audit tool, not an Android runtime dependency.

### Supporting Scripts and Tools

The repository has many one-off and validation scripts. The important current categories are:

| Category | Representative files | Purpose |
|---|---|---|
| USDA search and candidate selection | `tools/search_usda.py`, `scripts/search_correct_fdc.py`, `scripts/search_new_ingredients.py` | Find candidate FDC IDs before committing them to a map |
| Raw ingredient verification | `scripts/verify_ingredients.py`, `tools/verify_fdc_ids.py`, `tools/validate_json.py` | Compare JSON values to USDA, check required fields, guard Vitamin A mapping |
| Raw ingredient correction | `scripts/generate_corrected_json.py`, `tools/fix_fdc_data.py`, `tools/fix_ingredient_fdc.py`, `tools/execute_taxonomy_overhaul.py` | Correct FDC IDs, categories, taxonomy, and nutrient values |
| Dish JSON generation | `scripts/generate_dish_jsons.py` | Generates `recipe_ingredients.json` and `dish_recipes.json` from CSV recipe inputs and `raw_ingredients.json` |
| Dish nutrient refresh | `tools/refresh_dish_nutrients.py` | Recomputes dish nutrient totals after raw ingredient corrections |
| Dish validation/audit | `tools/validate_dish_recipes.py`, `tools/audit_dish_weights.py`, `tools/audit_nutrition.py`, `tools/check_zero_weights.py` | Check recipe weights, nutrient totals, missing references, and edge cases |

`tools/assemble_dishes.py` is explicitly marked deprecated and superseded by `scripts/generate_dish_jsons.py`.

## How JSON Assets Are Produced

Current intended pipeline:

```text
1. Search USDA candidates
   tools/search_usda.py or scripts/search_correct_fdc.py

2. Generate or update raw ingredients
   tools/fetch_usda_data.py
   tools/add_substitutes.py
   scripts/generate_corrected_json.py

3. Validate raw ingredients
   tools/validate_json.py
   scripts/verify_ingredients.py when API verification is needed

4. Generate recipe bridge and dish summaries
   scripts/generate_dish_jsons.py

5. Refresh dish nutrients after raw ingredient corrections, when needed
   tools/refresh_dish_nutrients.py

6. Validate dish outputs
   tools/validate_dish_recipes.py
   tools/validate_calculator.py
```

The result is a static asset bundle:

```text
app/src/main/assets/raw_ingredients.json
app/src/main/assets/recipe_ingredients.json
app/src/main/assets/dish_recipes.json
```

These files are what the Android app reads at runtime.

## Offline-First Defense for Presentation

Use this wording:

> CalorieKo is offline-first for its core nutrition, pantry, meal logging, meal planning, and on-device recognition workflows. The app does not call the USDA FoodData Central API while users are using those features. Instead, USDA data is collected during development by Python scripts, normalized into static JSON assets, bundled into the APK, and loaded into the encrypted local Room database. At runtime, the app reads local assets and local SQLite tables. Network access is only needed for account/authentication flows, optional cloud synchronization, server-managed catalog sync, user-initiated source links, and non-core remote media/profile image loading.

The key architectural distinction:

| Concern | Development-time/build-time | Runtime |
|---|---|---|
| USDA API usage | Python scripts query FoodData Central | No USDA API calls |
| API key | Used by local scripts | Not needed by nutrition runtime |
| Ingredient nutrients | Downloaded, normalized, audited | Read from local `RAW_INGREDIENTS_TABLE` |
| Dish nutrients | Generated from static ingredient JSON and recipe weights | Calculated from Room rows and cached dish metadata |
| Network dependency | Needed only to refresh the shipped reference dataset | Not needed for core food features |

Presentation diagram:

```text
Development machine:
  USDA FoodData Central API
        -> Python tooling
        -> raw_ingredients.json
        -> recipe_ingredients.json
        -> dish_recipes.json

Android runtime:
  APK assets
        -> Room seed on database open
        -> local nutrition calculation
        -> local meal/pantry/planning workflows
```

This means the app can rely heavily on USDA as a data source without being online-dependent at runtime. USDA is an upstream data provider for the shipped reference database, not a runtime service dependency.

### Runtime Network Surfaces

Observed runtime network-related surfaces:

- Firebase Auth for login, signup, password reset, and current-user identity.
- Firestore sync for user profile, activity logs, meal logs, daily summaries, pantry items, planned meals, and weight logs.
- Laravel API sync for full sync and admin food catalog pull.
- External browser intents and Compose URI handlers for USDA/FNRI proof links.
- Decorative or user-media loading through Coil, including the remote Unsplash image in `IntroScreen` and possible profile photo URLs.

Core nutrition, pantry matching, meal logging, meal planning, TFLite inference, and BLE scale workflows remain local-first. The only caveat for a strict "network only for Auth/Sync/Links" claim is the current decorative remote image and profile-image loading path. If the presentation requires that exact claim for the whole app, bundle the intro image locally or remove that remote URL.

## Current Validation Results

The following local validation commands were run during this walkthrough update:

```text
python tools/validate_json.py
python tools/validate_dish_recipes.py
python tools/validate_calculator.py
```

Results:

- `raw_ingredients.json`: all 87 ingredients valid.
- `dish_recipes.json`: 29 dishes valid.
- Recomputed dishes with recipe rows: 25.
- Preserved dishes without recipe rows: 4 (`chicken_wing`, `chicken_thigh`, `chicken_drumstick`, `chicken_breast`).
- Calculator comparison produced expected warnings for methodology differences between older cooked-dish CSV values and raw-ingredient summation.

## Current Risks and Cleanup Opportunities

| Area | Risk | Suggested cleanup |
|---|---|---|
| CSV/JSON duplication | `dish_ingredients.csv` and `recipe_ingredients.json` can drift | Make one generated from the other or add a parity validation script |
| Legacy flat nutrition | `dish_labels_and_values.csv` can be mistaken as authoritative | Document it as bootstrap/admin catalog only |
| Pantry SQL dependency | Pantry still depends on CSV-seeded `DISH_INGREDIENTS_TABLE` | Migrate pantry matching to `RECIPE_INGREDIENTS_TABLE` |
| Remote decorative image | Weakens a strict whole-app offline claim | Bundle the image locally |
| Many one-off scripts | Hard to know canonical pipeline | Add a short `scripts/README.md` or `docs/data_pipeline.md` |
| Deprecated tool retained | `tools/assemble_dishes.py` can be run accidentally | Keep deprecated banner or move to an archive directory |

## Source File Map

### Runtime data loading

- `app/src/main/java/com/calorieko/app/data/local/FoodDatabaseCallback.kt`
- `app/src/main/java/com/calorieko/app/data/local/FoodCsvParser.kt`
- `app/src/main/java/com/calorieko/app/data/local/FoodJsonParser.kt`
- `app/src/main/java/com/calorieko/app/data/local/AppDatabase.kt`

### Runtime nutrition model

- `app/src/main/java/com/calorieko/app/data/model/RawIngredientEntity.kt`
- `app/src/main/java/com/calorieko/app/data/model/DishRecipeEntity.kt`
- `app/src/main/java/com/calorieko/app/data/model/RecipeIngredientEntity.kt`
- `app/src/main/java/com/calorieko/app/data/model/NutritionResult.kt`
- `app/src/main/java/com/calorieko/app/data/local/RecipeNutritionCalculator.kt`

### Runtime DAOs

- `app/src/main/java/com/calorieko/app/data/local/FoodDao.kt`
- `app/src/main/java/com/calorieko/app/data/local/PantryDao.kt`
- `app/src/main/java/com/calorieko/app/data/local/RawIngredientDao.kt`
- `app/src/main/java/com/calorieko/app/data/local/DishRecipeDao.kt`
- `app/src/main/java/com/calorieko/app/data/local/RecipeIngredientDao.kt`

### Feature ViewModels

- `app/src/main/java/com/calorieko/app/viewmodel/LogMealViewModel.kt`
- `app/src/main/java/com/calorieko/app/viewmodel/ManualLogViewModel.kt`
- `app/src/main/java/com/calorieko/app/viewmodel/PantryViewModel.kt`
- `app/src/main/java/com/calorieko/app/viewmodel/ExploreViewModel.kt`
- `app/src/main/java/com/calorieko/app/viewmodel/DashboardViewModel.kt`

### Remote sync

- `app/src/main/java/com/calorieko/app/data/remote/FirestoreSyncRepository.kt`
- `app/src/main/java/com/calorieko/app/data/remote/api/ApiSyncManager.kt`
- `app/src/main/java/com/calorieko/app/data/remote/api/SyncWorker.kt`
- `app/src/main/java/com/calorieko/app/data/remote/api/FoodCatalogSyncManager.kt`
- `app/src/main/java/com/calorieko/app/data/remote/api/CalorieKoApiService.kt`
- `app/src/main/java/com/calorieko/app/data/remote/api/RetrofitClient.kt`

### Development data pipeline

- `tools/usda_nutrient_schema.py`
- `tools/fetch_usda_data.py`
- `tools/add_substitutes.py`
- `scripts/generate_corrected_json.py`
- `scripts/generate_dish_jsons.py`
- `tools/refresh_dish_nutrients.py`
- `tools/validate_json.py`
- `tools/validate_dish_recipes.py`
- `scripts/verify_ingredients.py`
