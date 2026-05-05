# Food Database Sync — Implementation Plan

**Objective:** *"Admin panel serves as central repository for food database, allowing researchers to update nutritional profiles which are asynchronously synchronized to the mobile app whenever internet is restored."*

**Strategy:** Full Replace sync for System A (`FOOD_TABLE`) + FoodItem fallback in ViewModels so new admin-added dishes are visible with nutrition.

---

## Scope — What Gets Synced vs. What Doesn't

| Data | Table | Synced? | Impact |
|---|---|---|---|
| Dish lookup + flat nutrition | `FOOD_TABLE` (FoodItem) | ✅ Yes | Admin can add/edit/delete dishes with per-100g nutrition |
| Pantry ingredient matching | `DISH_INGREDIENTS_TABLE` | ❌ No | New dishes won't appear in Pantry matching |
| USDA raw ingredients | `RAW_INGREDIENTS_TABLE` | ❌ No | Stays baked in APK via JSON |
| Dish recipes | `DISH_RECIPES_TABLE` | ❌ No | Stays baked in APK via JSON |
| Recipe-ingredient links | `RECIPE_INGREDIENTS_TABLE` | ❌ No | Stays baked in APK via JSON |

### Where Synced Data Shows Up

| Feature | Existing 24 dishes | New admin-added dishes |
|---|---|---|
| **AI Camera** (recognition) | ✅ TFLite recognizes + System B nutrition | ❌ TFLite can't recognize new dishes |
| **AI Camera** (nutrition update) | ✅ System B nutrition is unchanged but FoodItem metadata syncs | N/A |
| **Manual Log** (search + log) | ✅ System B nutrition (unchanged) | ✅ **NEW:** FoodItem fallback shows flat per-100g nutrition |
| **Explore** (browse dishes) | ✅ System B nutrition (unchanged) | ✅ **NEW:** FoodItem fallback shows flat per-100g nutrition |
| **Pantry matching** | ✅ Unchanged | ❌ No `DishIngredient` entries for new dishes |

---

## Proposed Changes

### Server Side (Laravel Admin Panel)

#### [MODIFY] [MobileSyncController.php](file:///var/www/calorieko-admin/app/Http/Controllers/Api/MobileSyncController.php)

Add a `getFoodCatalog()` method that returns all food items:

```php
/**
 * Returns the full food catalog for mobile sync.
 * The mobile app calls this to pull admin-managed food data.
 */
public function getFoodCatalog(Request $request)
{
    $foods = DB::table('FOOD_TABLE')
        ->select([
            'food_id', 'name_en', 'name_ph', 'category', 'ml_label',
            'calories_per_100g', 'protein_per_100g', 'carbs_per_100g',
            'fiber_per_100g', 'sugar_per_100g', 'fat_per_100g',
            'saturated_fat_per_100g', 'polyunsaturated_fat_per_100g',
            'monounsaturated_fat_per_100g', 'trans_fat_per_100g',
            'cholesterol_per_100g', 'sodium_per_100g', 'potassium_per_100g',
            'vitamin_a_per_100g', 'vitamin_c_per_100g',
            'calcium_per_100g', 'iron_per_100g', 'data_source'
        ])
        ->get();

    return response()->json([
        'success' => true,
        'foods'   => $foods,
        'count'   => $foods->count(),
        'server_timestamp' => (int)(microtime(true) * 1000),
    ]);
}
```

---

#### [MODIFY] [api.php](file:///var/www/calorieko-admin/routes/api.php)

Add the new route inside the existing `sync` middleware group (line ~61):

```php
Route::get('/foods/catalog', [MobileSyncController::class, 'getFoodCatalog']);
```

This goes inside the `firebase.auth` middleware group, so authentication works the same way as `POST /sync/full`.

---

### Mobile Side (Android/Kotlin)

#### [MODIFY] [CalorieKoApiService.kt](file:///home/raffyabydc/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/remote/api/CalorieKoApiService.kt)

Add the GET endpoint for pulling the food catalog:

```kotlin
@GET("api/sync/foods/catalog")
suspend fun getFoodCatalog(
    @retrofit2.http.Header("Authorization") token: String
): Response<FoodCatalogResponse>
```

---

#### [MODIFY] [SyncPayload.kt](file:///home/raffyabydc/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/remote/api/SyncPayload.kt)

Add response data classes at the bottom of the file:

```kotlin
// ── Food Catalog (Server → Mobile) ──

data class FoodCatalogResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("foods") val foods: List<SyncFoodItem>,
    @SerializedName("count") val count: Int,
    @SerializedName("server_timestamp") val serverTimestamp: Long
)

data class SyncFoodItem(
    @SerializedName("food_id") val foodId: Int,
    @SerializedName("name_en") val nameEn: String,
    @SerializedName("name_ph") val namePh: String,
    @SerializedName("category") val category: String,
    @SerializedName("ml_label") val mlLabel: String,
    @SerializedName("calories_per_100g") val caloriesPer100g: Float = 0f,
    @SerializedName("protein_per_100g") val proteinPer100g: Float = 0f,
    @SerializedName("carbs_per_100g") val carbsPer100g: Float = 0f,
    @SerializedName("fiber_per_100g") val fiberPer100g: Float = 0f,
    @SerializedName("sugar_per_100g") val sugarPer100g: Float = 0f,
    @SerializedName("fat_per_100g") val fatPer100g: Float = 0f,
    @SerializedName("saturated_fat_per_100g") val saturatedFatPer100g: Float = 0f,
    @SerializedName("polyunsaturated_fat_per_100g") val polyunsaturatedFatPer100g: Float = 0f,
    @SerializedName("monounsaturated_fat_per_100g") val monounsaturatedFatPer100g: Float = 0f,
    @SerializedName("trans_fat_per_100g") val transFatPer100g: Float = 0f,
    @SerializedName("cholesterol_per_100g") val cholesterolPer100g: Float = 0f,
    @SerializedName("sodium_per_100g") val sodiumPer100g: Float = 0f,
    @SerializedName("potassium_per_100g") val potassiumPer100g: Float = 0f,
    @SerializedName("vitamin_a_per_100g") val vitaminAPer100g: Float = 0f,
    @SerializedName("vitamin_c_per_100g") val vitaminCPer100g: Float = 0f,
    @SerializedName("calcium_per_100g") val calciumPer100g: Float = 0f,
    @SerializedName("iron_per_100g") val ironPer100g: Float = 0f,
    @SerializedName("data_source") val dataSource: String = "DOST_FNRI_MENU_GUIDE"
) {
    /** Maps server response to the Room FoodItem entity. */
    fun toFoodItem(): FoodItem = FoodItem(
        foodId = foodId, nameEn = nameEn, namePh = namePh,
        category = category, mlLabel = mlLabel,
        caloriesPer100g = caloriesPer100g, proteinPer100g = proteinPer100g,
        carbsPer100g = carbsPer100g, fiberPer100g = fiberPer100g,
        sugarPer100g = sugarPer100g, fatPer100g = fatPer100g,
        saturatedFatPer100g = saturatedFatPer100g,
        polyunsaturatedFatPer100g = polyunsaturatedFatPer100g,
        monounsaturatedFatPer100g = monounsaturatedFatPer100g,
        transFatPer100g = transFatPer100g, cholesterolPer100g = cholesterolPer100g,
        sodiumPer100g = sodiumPer100g, potassiumPer100g = potassiumPer100g,
        vitaminAPer100g = vitaminAPer100g, vitaminCPer100g = vitaminCPer100g,
        calciumPer100g = calciumPer100g, ironPer100g = ironPer100g,
        dataSource = dataSource
    )
}
```

---

#### [MODIFY] [SyncWorker.kt](file:///home/raffyabydc/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/remote/api/SyncWorker.kt)

Add a new step **after** the existing Laravel push (after Step 3, ~line 174). This is the **pull** direction — server → mobile:

```kotlin
// ── Step NEW: Pull food catalog from admin server ──
// This fulfills the research objective: admin-managed food data
// syncs to mobile whenever internet connection is restored.
try {
    val token = com.google.firebase.auth.FirebaseAuth.getInstance()
        .currentUser?.getIdToken(true)?.await()?.token
    if (token != null) {
        val foodApiService = RetrofitClient.getApiService(BuildConfig.API_BASE_URL)
        val foodResponse = foodApiService.getFoodCatalog("Bearer $token")
        if (foodResponse.isSuccessful && foodResponse.body()?.success == true) {
            val serverFoods = foodResponse.body()!!.foods
            val foodDao = db.foodDao()
            // Full replace: clear local → insert server catalog
            foodDao.deleteAllFoods()
            foodDao.insertAll(serverFoods.map { it.toFoodItem() })
            Log.d(TAG, "Food catalog synced: ${serverFoods.size} items from server.")
        }
    }
} catch (e: Exception) {
    Log.w(TAG, "Food catalog pull failed (non-fatal, CSV data remains): ${e.message}")
}
```

> [!NOTE]
> This is **non-fatal** — if the pull fails (server down, token expired, etc.), the mobile app continues using its existing CSV-seeded or previously-synced food data. The `SyncWorker` still returns `Result.success()`.

---

#### [MODIFY] [ExploreViewModel.kt](file:///home/raffyabydc/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/viewmodel/ExploreViewModel.kt)

**The fallback:** After loading System B dishes from `DishRecipeDao`, also load `FoodItem` entries from `FoodDao` that DON'T have a System B counterpart. This makes new admin-added dishes visible in Explore.

Changes to `loadAllDishes()` (line ~137):

```kotlin
private fun loadAllDishes() {
    viewModelScope.launch(Dispatchers.IO) {
        _isLoading.value = true

        // 1. Load System B dishes (existing behavior — full ingredient-level nutrition)
        val recipes = dishRecipeDao.getAllDishRecipes()
        val systemBLabels = recipes.map { it.dishLabel }.toSet()
        val systemBDishes = recipes.map { recipe ->
            // ... existing mapping code (unchanged) ...
        }

        // 2. Load System A dishes that DON'T exist in System B (admin-added via sync)
        val allFoodItems = foodDao.getAllFoods()
        val adminOnlyDishes = allFoodItems
            .filter { it.mlLabel !in systemBLabels && it.mlLabel != "negative" }
            .map { food ->
                ExploreDish(
                    dishLabel = food.mlLabel,
                    nameEn = food.nameEn,
                    namePh = food.namePh,
                    category = food.category,
                    calories = food.caloriesPer100g.toInt(),
                    protein = food.proteinPer100g.toInt(),
                    carbs = food.carbsPer100g.toInt(),
                    fats = food.fatPer100g.toInt(),
                    sodium = food.sodiumPer100g.toInt(),
                    dataSource = food.dataSource,
                    ingredientCount = 0,
                    ingredientNames = emptyList(),
                    servings = 1,
                    perServingWeightG = 100f  // Default per-100g
                )
            }

        _allDishes.value = systemBDishes + adminOnlyDishes
        _isLoading.value = false
    }
}
```

This requires adding `FoodDao` to the constructor — a minor wiring change in `ExploreViewModel` and `MainActivity.kt`.

---

#### [MODIFY] [ManualLogViewModel.kt](file:///home/raffyabydc/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/viewmodel/ManualLogViewModel.kt)

**The fallback:** The search currently only queries `DishRecipeDao`. For admin-added dishes, also query `FoodDao` and merge results.

Changes to `init` and `updateSearchQuery()`:

The current flow loads `dishRecipeDao.getAllDishRecipes()` and filters client-side. The fallback adds `FoodItem` entries that don't exist in System B, wrapping them into a `DishRecipeEntity`-compatible display object.

Two approaches:
- **Option A:** Introduce a sealed class `SearchableDish` that wraps either a `DishRecipeEntity` or a `FoodItem`
- **Option B:** Convert `FoodItem` → `DishRecipeEntity` with flat nutrition values filled in

**I'll go with Option B** (simpler, no UI changes needed):

```kotlin
/** Converts a FoodItem (System A) to a DishRecipeEntity-compatible object for display. */
private fun FoodItem.toDishRecipeEntity() = DishRecipeEntity(
    dishLabel = mlLabel,
    nameEn = nameEn,
    namePh = namePh,
    category = category,
    cookingMethod = "",
    servings = 1,
    totalRawWeightG = 100f,
    dishYieldFactor = 1.0f,
    cookedWeightG = 100f,
    perServingWeightG = 100f,
    ingredientCount = 0,
    calPerServing = caloriesPer100g,
    proteinPerServing = proteinPer100g,
    carbsPerServing = carbsPer100g,
    fatPerServing = fatPer100g,
    fiberPerServing = fiberPer100g,
    sugarPerServing = sugarPer100g,
    sodiumPerServing = sodiumPer100g,
    potassiumPerServing = potassiumPer100g,
    vitaminAPerServing = vitaminAPer100g,
    vitaminCPerServing = vitaminCPer100g,
    calciumPerServing = calciumPer100g,
    ironPerServing = ironPer100g
)
```

In `init`, after loading System B dishes, merge admin-only FoodItems:

```kotlin
init {
    viewModelScope.launch {
        val systemBDishes = withContext(Dispatchers.IO) { dishRecipeDao.getAllDishRecipes() }
        val systemBLabels = systemBDishes.map { it.dishLabel }.toSet()

        // Fallback: include admin-synced dishes not in System B
        val foodItems = withContext(Dispatchers.IO) { foodDao.getAllFoods() }
        val adminOnlyDishes = foodItems
            .filter { it.mlLabel !in systemBLabels && it.mlLabel != "negative" }
            .map { it.toDishRecipeEntity() }

        val allDishes = systemBDishes + adminOnlyDishes
        _allDishes.value = allDishes
        _filteredDishes.value = allDishes
    }
}
```

The `addDish()` function already calls `calculator.calculatePortionNutrition(dishLabel, weight)`. For admin-only dishes (no System B data), the calculator will return zeros. So we add a fallback in `addDish()`:

```kotlin
// If calculator returns zero (no System B data), use FoodItem's flat per-100g values
val nutrients = withContext(Dispatchers.IO) {
    val calculated = calculator.calculatePortionNutrition(recipe.dishLabel, weightGrams)
    if (calculated.calories == 0f) {
        // Fallback: scale FoodItem's per-100g values by weight
        val scale = weightGrams / 100f
        NutritionResult(
            calories = recipe.calPerServing * scale,
            protein = recipe.proteinPerServing * scale,
            carbs = recipe.carbsPerServing * scale,
            fat = recipe.fatPerServing * scale,
            fiber = recipe.fiberPerServing * scale,
            sugar = recipe.sugarPerServing * scale,
            sodium = recipe.sodiumPerServing * scale,
            potassium = recipe.potassiumPerServing * scale,
            vitaminA = recipe.vitaminAPerServing * scale,
            vitaminC = recipe.vitaminCPerServing * scale,
            calcium = recipe.calciumPerServing * scale,
            iron = recipe.ironPerServing * scale
        )
    } else calculated
}
```

This requires adding `FoodDao` to `ManualLogViewModel`'s constructor — minor wiring change in the factory and `MainActivity.kt`.

---

#### [MODIFY] [MainActivity.kt](file:///home/raffyabydc/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/MainActivity.kt)

Wire `FoodDao` into the `ExploreViewModel` and `ManualLogViewModel` factory calls. This is a one-line addition to each factory: `foodDao = database.foodDao()`.

---

## AI Camera Flow — What Syncs vs. What Doesn't

The AI camera log meal has **two stages**: (1) TFLite model recognizes the dish → outputs `ml_label`, (2) `FoodDao.getFoodByMlLabel()` looks up identification from `FOOD_TABLE`.

| Scenario | AI Camera | Manual Log | Explore |
|---|---|---|---|
| **Update** existing dish nutrients (e.g., Sinigang 86→92 kcal) | ✅ Works — TFLite still recognizes it, lookup returns synced nutrition | ✅ Works | ✅ Works |
| **Add** brand-new dish via admin | ❌ No — TFLite model was never trained on it | ✅ Works (text search + flat nutrition) | ✅ Works (flat nutrition) |

> [!NOTE]
> This aligns with the research objective: *"update nutritional profiles"*. Updating the existing 24 dishes' nutrients flows through **all** logging methods including AI camera. Adding entirely new dishes to the camera would require retraining the TFLite model (separate ML effort), but they still appear in Manual Log and Explore.

---

## Data Flow (End-to-End Demo)

```
Researcher opens Admin Panel
    ↓
Adds "Chicken Inasal" with 195 kcal/100g, 28g protein, etc.
    ↓
POST /api/admin/foods  →  MySQL FOOD_TABLE updated
    ↓
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    ↓
Mobile user opens app (or background sync fires)
    ↓
SyncWorker fires (NetworkType.CONNECTED constraint met)
    ↓
GET /api/sync/foods/catalog  →  returns all foods including Chicken Inasal
    ↓
FoodDao.deleteAllFoods() + FoodDao.insertAll(serverFoods)
    ↓
Room FOOD_TABLE now has Chicken Inasal
    ↓
User opens Manual Log → searches "Inasal" → dish appears → logs with nutrition
```

---

## What We're NOT Doing (and why)

| Skipped | Reason |
|---|---|
| Delta sync for food catalog | Catalog is tiny (~5KB). Full replace is simpler and equally fast. |
| Room schema migration | No schema change — `FoodItem` entity stays identical. |
| System B sync (ingredients/recipes) | Already USDA-verified and baked into APK. Admin doesn't manage these. |
| Pantry matching for new dishes | Would require syncing `DISH_INGREDIENTS_TABLE` — separate effort. |

> [!TIP]
> **The key insight:** The `FoodItem` entity schema is already identical between admin and mobile. We're just adding a pipe (sync) and a fallback (ViewModels) to make new dishes visible.

---

## Summary of File Changes

| File | Side | Change |
|---|---|---|
| `MobileSyncController.php` | Server | Add `getFoodCatalog()` method |
| `api.php` | Server | Add `GET /sync/foods/catalog` route |
| `CalorieKoApiService.kt` | Mobile | Add `getFoodCatalog()` Retrofit endpoint |
| `SyncPayload.kt` | Mobile | Add `FoodCatalogResponse` + `SyncFoodItem` data classes |
| `SyncWorker.kt` | Mobile | Add food catalog pull step (after push) |
| `ExploreViewModel.kt` | Mobile | Add `FoodDao` + merge admin-only dishes |
| `ManualLogViewModel.kt` | Mobile | Add `FoodDao` + merge admin-only dishes + nutrition fallback |
| `MainActivity.kt` | Mobile | Wire `foodDao` into ViewModel factories |

**Total: 2 server files + 6 mobile files. No Room migration. No schema changes.**

---

## Verification Plan

### Automated Tests
1. **Server:** `curl` the new endpoint with a valid Firebase token → verify JSON response with all food items
2. **Mobile:** Run the app, trigger sync, check logcat for `"Food catalog synced: N items from server."`

### Manual Verification
1. **Update existing dish:** In admin panel, edit "Sinigang na Baboy" calories from 86 → 99 kcal
2. Open mobile app → go to Settings → tap "Sync Data"
3. Point AI camera at Sinigang → verify it shows 99 kcal (proves sync works through AI flow)
4. **Add new dish:** In admin panel, add "Test Dish" with 999 kcal
5. Sync again → search "Test Dish" in **Manual Log** screen → verify it appears with 999 kcal
6. Confirm "Test Dish" does NOT appear in AI camera (expected — TFLite wasn't trained on it)

### Offline Scenario
1. Turn off mobile data/WiFi
2. Open app fresh (or after data clear) → verify CSV-seeded foods still load
3. Turn on WiFi → verify SyncWorker fires and replaces with server data
