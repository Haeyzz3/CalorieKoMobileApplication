# CalorieKo — Comprehensive Before vs After Walkthrough

> This document explains every architectural change made during the refactoring, clarifies what's still active vs what could be retired, and confirms the app's offline capabilities.

---

## Table of Contents

1. [The Big Picture](#1-the-big-picture)
2. [Asset Files: What Feeds What](#2-asset-files-what-feeds-what)
3. [Database Tables: Old vs New](#3-database-tables-old-vs-new)
4. [Are the CSVs Obsolete?](#4-are-the-csvs-obsolete)
5. [Offline-First: No API at Runtime](#5-offline-first-no-api-at-runtime)
6. [Data Flow Diagrams](#6-data-flow-diagrams)
7. [Feature-by-Feature Changelog](#7-feature-by-feature-changelog)
8. [Complete File Inventory](#8-complete-file-inventory)

---

## 1. The Big Picture

### Before the Refactor
The app used a **single-tier CSV system**:
- `dish_labels_and_values.csv` → one row per dish, with basic nutrition (cal, protein, carbs, fat + a few more)
- `dish_ingredients.csv` → ingredient names mapped to dishes (text only, no gram weights)
- Nutrition was **pre-computed per dish** — you couldn't see individual ingredient contributions
- No concept of "raw ingredients" as independent entities

### After the Refactor
The app now uses a **three-tier JSON system** layered on top:
- **Tier 1: Raw Ingredients** (`raw_ingredients.json`) → 82 individual ingredients, each with USDA-verified per-100g nutrients
- **Tier 2: Dish Recipes** (`dish_recipes.json`) → 24 dish metadata entries (servings, cooked weight, source attribution)
- **Tier 3: Recipe Ingredients** (`recipe_ingredients.json`) → the bridge table linking dishes → ingredients with exact gram weights

Nutrition is now **dynamically calculated** by summing `(raw_weight_grams / 100) × nutrient_per_100g` for each ingredient.

> [!IMPORTANT]
> **Both systems currently coexist.** The old CSVs are NOT deleted — they still serve the AI camera recognition flow. The new JSONs power the Pantry nutrition, substitution, and manual logging features.

---

## 2. Asset Files: What Feeds What

| Asset File | Format | Role | Status |
|---|---|---|---|
| `labels.txt` | Text | TFLite ML model class labels (24 dishes) | **Active** — unchanged |
| `calorieko_model.tflite` | Binary | AI dish recognition model | **Active** — unchanged |
| `dish_labels_and_values.csv` | CSV | Legacy dish list with basic nutrients | **Still Active** — feeds `FoodItem` table |
| `dish_ingredients.csv` | CSV | Legacy dish → ingredient name mapping | **Still Active** — feeds `DishIngredient` table |
| `nutrient_daily_values.csv` | CSV | Daily recommended values per nutrient | **Active** — used by dashboard |
| `raw_ingredients.json` | JSON | **NEW** — 82 raw ingredients with full USDA nutrients | **Active** — feeds `raw_ingredients` table |
| `dish_recipes.json` | JSON | **NEW** — 24 dish recipes with metadata | **Active** — feeds `dish_recipes` table |
| `recipe_ingredients.json` | JSON | **NEW** — dish-to-ingredient links with gram weights | **Active** — feeds `recipe_ingredients` table |
| `sources/` | Directory | PDF proof documents for FNRI-sourced dishes | **Active** — used in source attribution |

---

## 3. Database Tables: Old vs New

The app has **12 Room tables** total. Here's how they group:

### Legacy Tables (fed by CSVs) — Still Active
| Table | Entity | Fed By | Used By |
|---|---|---|---|
| `food_table` | `FoodItem` | `dish_labels_and_values.csv` | AI camera flow (label → foodId lookup) |
| `dish_ingredients` | `DishIngredient` | `dish_ingredients.csv` | Pantry matching (which ingredients does a dish need?) |
| `pantry_items` | `PantryItem` | User input | User's ingredient inventory |

### New Tables (fed by JSONs) — Added in Refactor
| Table | Entity | Fed By | Used By |
|---|---|---|---|
| `raw_ingredients` | `RawIngredientEntity` | `raw_ingredients.json` | Nutrition calculation, substitution candidates |
| `dish_recipes` | `DishRecipeEntity` | `dish_recipes.json` | Per-serving nutrients, dish metadata, cooked weight |
| `recipe_ingredients` | `RecipeIngredientEntity` | `recipe_ingredients.json` | Ingredient gram weights per dish |

### User Data Tables — Unchanged
| Table | Entity | Purpose |
|---|---|---|
| `user_profiles` | `UserProfile` | Height, weight, TDEE targets |
| `activity_log` | `ActivityLogEntity` | Exercise tracking |
| `meal_logs` | `MealLogEntity` | Logged meal sessions |
| `meal_log_items` | `MealLogItemEntity` | Individual dishes in a meal |
| `daily_nutrition_summary` | `DailyNutritionSummaryEntity` | Daily totals cache |
| `planned_meals` | `PlannedMealEntity` | Meal plan (weekly) |

---

## 4. Are the CSVs Obsolete?

### Short answer: **Not yet, but they COULD be retired.**

Here's why they're still needed:

```
dish_labels_and_values.csv  →  FoodItem table  →  Used ONLY by LogMealViewModel
                                                    (AI camera flow needs foodId to
                                                     identify the recognized dish)
```

The `FoodItem` table has two fields that matter:
- `ml_label` — matches TFLite output (e.g., `"sinigang_pork"`)  
- `food_id` — unique identifier logged into `MealLogItemEntity`

The new `DishRecipeEntity` has the **exact same `dishLabel`** field that matches `ml_label`. So in theory, `LogMealViewModel` could be refactored to use `dishRecipeDao` instead of `foodDao`, which would make `dish_labels_and_values.csv` fully obsolete.

```
dish_ingredients.csv  →  DishIngredient table  →  Used by PantryViewModel
                                                    (ingredient matching, "what can I cook?")
```

The `DishIngredient` table stores ingredient names per dish. The new `RecipeIngredientEntity` table has the same data (and more — it adds gram weights). However, `PantryDao` queries use `DishIngredient` for the matching logic.

> [!NOTE]
> **Retiring the CSVs is possible but not urgent.** Both systems coexist without conflict. The CSVs are ~13KB total. A future cleanup would involve migrating `LogMealViewModel` to use `DishRecipeDao` and `PantryDao` matching queries to use `RecipeIngredientDao`.

---

## 5. Offline-First: No API at Runtime

> [!IMPORTANT]
> **The app is 100% offline-capable. Zero network calls are needed for any nutritional feature.**

Here's the key distinction:

### USDA API — Used Only During Development
The USDA FoodData Central API (`api.nal.usda.gov`) was used **only by Python scripts** running on your development machine:
- `tools/fetch_usda_data.py` — fetched per-100g nutrients for 69 original ingredients
- `tools/add_substitutes.py` — fetched nutrients for 13 substitute ingredients

These scripts wrote data to `raw_ingredients.json`, which is **bundled inside the APK** as a static asset. The API key is NOT in the app code.

### At Runtime — Everything is Local
```
App Launch
    ↓
FoodDatabaseCallback.onOpen()
    ↓
Reads raw_ingredients.json from APK assets (local file, no network)
    ↓
Inserts 82 RawIngredientEntity rows into Room SQLite DB
    ↓
All nutrition calculations use Room queries (local DB)
```

The data flow is:
```
[USDA API] → (dev machine) → [raw_ingredients.json] → (APK bundle) → [Room DB] → (app)
             ^^^^^^^^^^^^^^^^                          ^^^^^^^^^^^^^^
             ONE-TIME, offline                         BAKED INTO APP
             Python scripts                            No internet needed
```

### What DOES need internet?
Only these existing features (unchanged by our refactor):
- **Firebase Auth** — login/signup
- **Firestore Sync** — meal logs, pantry items, meal plans sync to cloud
- **Source Attribution links** — "Visit Database" button opens browser

All of these already had offline fallbacks (offline auth cache, Room-first sync pattern).

---

## 6. Data Flow Diagrams

### A. AI Camera Flow (Log Meal)
```
Camera → TFLite Model → "sinigang_pork" (label)
                              ↓
                    FoodDao.getFoodByMlLabel()     ← OLD CSV (FoodItem table)
                              ↓
                         FoodItem { foodId, dishName, mlLabel }
                              ↓
              User places dish on IoT scale → weight (150g)
                              ↓
              RecipeNutritionCalculator.calculatePortionNutrition()  ← NEW JSON
                              ↓
                    NutritionResult { cal, protein, carbs, fat, ... }
                              ↓
                    MealRepository.logMeal() → Room + Firestore
```

### B. Manual Log Flow
```
User searches "sinigang" → DishRecipeDao.searchByName()  ← NEW JSON
                              ↓
              DishRecipeEntity { dishLabel, dishName, servings }
                              ↓
              User enters weight → calculator.calculatePortionNutrition()  ← NEW JSON
                              ↓
                    MealRepository.logMeal() → Room + Firestore
```

### C. Pantry Matching Flow
```
User's pantry: ["pork", "kangkong", "tomato", ...]
                              ↓
         PantryDao.getDishMatchCounts(pantryItems)     ← OLD CSV (DishIngredient)
                              ↓
         List of dishes sorted by ingredient match %
                              ↓
         DishRecipeDao.getByLabel(dishLabel)            ← NEW JSON (for nutrition)
                              ↓
         RecipeDetailContent shows full nutrition breakdown
```

### D. Ingredient Substitution Flow
```
User taps "Pork Liempo" in recipe detail
         ↓
RawIngredientDao.getSubstituteCandidates("pork", "pork_liempo")  ← NEW JSON
         ↓
Shows: Pork Tenderloin, Pork Shoulder, Ground Pork
         ↓
User picks "Pork Tenderloin"
         ↓
RecipeNutritionCalculator.calculateWithSubstitution()  ← NEW JSON
         ↓
UI updates all nutrition values in real-time
```

---

## 7. Feature-by-Feature Changelog

### New Features Added
| Feature | Description | Files |
|---|---|---|
| **Per-ingredient nutrition** | Tap any ingredient → see its raw weight, kcal, protein, carbs, fat, sodium contribution | `RecipeNutritionCalculator.kt`, `PantryScreen.kt` |
| **Ingredient substitution** | Swap same-category ingredients (e.g., soybean oil → olive oil) with live nutrition recalculation | `PantryViewModel.kt`, `PantryScreen.kt` |
| **Substitution picker** | Bottom sheet showing alternatives with per-100g nutrition comparison | `PantryScreen.kt` |
| **Nutrition methodology disclaimer** | Info card: "Values computed from raw USDA-verified nutrients before cooking" | `PantryScreen.kt` |
| **Dynamic nutrition engine** | All 12 nutrients calculated from raw ingredients × gram weights | `RecipeNutritionCalculator.kt` |
| **Portion-based logging** | Log meals by cooked weight with proper yield-factor scaling | `LogMealViewModel.kt`, `ManualLogViewModel.kt` |
| **Source attribution** | Each dish shows its data source (FNRI/USDA) with proof documents | `PantryScreen.kt`, `ExploreScreen.kt` |
| **Auto-reseed** | Database automatically re-seeds when new ingredients are added to JSON | `FoodDatabaseCallback.kt` |

### Refactored Features
| Feature | Before | After |
|---|---|---|
| **Dish nutrition display** | Hardcoded per-dish values from CSV | Dynamically calculated from ingredient weights |
| **Manual meal logging** | Used `FoodItem` (CSV) for search | Uses `DishRecipeEntity` (JSON) for search |
| **Meal plan** | Same UI | Now shows full nutrition from new engine |
| **Explore screen** | Used CSV-based `FoodItem` nutrition | Uses `DishRecipeEntity` nutrition |

### Unchanged Features
| Feature | Notes |
|---|---|
| AI camera dish recognition | Still uses TFLite + `FoodItem` table |
| IoT scale BLE connection | No changes |
| Firebase Auth | No changes |
| Offline sync (Room → Firestore) | No changes |
| Dashboard/Diary | Reads from `MealLogEntity` — unchanged |
| Activity tracking | No changes |

---

## 8. Complete File Inventory

### Data Files (Assets)

**Still Active (Legacy CSV):**
- [dish_labels_and_values.csv](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/assets/dish_labels_and_values.csv) — 24 dishes with basic nutrients
- [dish_ingredients.csv](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/assets/dish_ingredients.csv) — ingredient names per dish

**New (JSON):**
- [raw_ingredients.json](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/assets/raw_ingredients.json) — **82 ingredients** with USDA per-100g nutrients
- [dish_recipes.json](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/assets/dish_recipes.json) — 24 dish recipes with metadata
- [recipe_ingredients.json](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/assets/recipe_ingredients.json) — dish → ingredient links with gram weights

### New/Modified Source Files

| File | Change Type |
|---|---|
| [RawIngredientEntity.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/model/RawIngredientEntity.kt) | **New** — data class for raw ingredients |
| [DishRecipeEntity.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/model/DishRecipeEntity.kt) | **New** — data class for dish recipes |
| [RecipeIngredientEntity.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/model/RecipeIngredientEntity.kt) | **New** — data class for recipe-ingredient links |
| [NutritionResult.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/model/NutritionResult.kt) | **New** — 12-nutrient container with math operators |
| [RawIngredientDao.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/local/RawIngredientDao.kt) | **New** — DAO for raw ingredients |
| [DishRecipeDao.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/local/DishRecipeDao.kt) | **New** — DAO for dish recipes |
| [RecipeIngredientDao.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/local/RecipeIngredientDao.kt) | **New** — DAO for recipe ingredients |
| [RecipeNutritionCalculator.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/local/RecipeNutritionCalculator.kt) | **New** — dynamic nutrition engine |
| [FoodJsonParser.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/local/FoodJsonParser.kt) | **New** — JSON parser for all 3 new asset files |
| [AppDatabase.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/local/AppDatabase.kt) | **Modified** — added 3 new entity classes, version 18→19 |
| [FoodDatabaseCallback.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/local/FoodDatabaseCallback.kt) | **Modified** — added JSON seeding + auto-reseed |
| [PantryViewModel.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/viewmodel/PantryViewModel.kt) | **Modified** — substitution state, breakdown method |
| [PantryScreen.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/ui/screens/PantryScreen.kt) | **Modified** — expandable nutrition, substitution UI, disclaimer |
| [LogMealViewModel.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/viewmodel/LogMealViewModel.kt) | **Modified** — uses calculator for portion nutrition |
| [ManualLogViewModel.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/viewmodel/ManualLogViewModel.kt) | **New** — manual entry ViewModel using new data model |
| [ExploreScreen.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/ui/screens/ExploreScreen.kt) | **Modified** — USDA_FDC source handling |
| [MainActivity.kt](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/MainActivity.kt) | **Modified** — calculator DI wiring |

### Dev/Build Tools
| File | Purpose |
|---|---|
| [tools/fetch_usda_data.py](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/tools/fetch_usda_data.py) | Fetches USDA nutrients for original 69 ingredients |
| [tools/add_substitutes.py](file:///c:/Users/dcjon/AndroidStudioProjects/CalorieKoMobileApplication/tools/add_substitutes.py) | Fetches USDA nutrients for 13 substitute ingredients |

> [!TIP]
> These Python scripts are **development tools only** — they run on your PC, write to JSON asset files, and are NOT included in the APK. The USDA API key is NOT in the app.
