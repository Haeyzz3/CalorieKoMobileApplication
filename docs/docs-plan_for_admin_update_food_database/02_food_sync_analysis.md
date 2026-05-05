# Food Database Sync Gap — Analysis & Proposed Solutions

**Date:** Monday, May 5, 2026

---

## 1. The Problem, Precisely Stated

The admin panel has a fully functional food database CRUD system (list, add, edit, delete, bulk import). But **nothing connects it to the mobile app**. The sync pipeline (`POST /api/sync/full`) handles profiles, meals, activities, and nutrition summaries — but completely skips food data.

The result: an admin can bulk-import 200 Filipino dishes with verified USDA nutrients, and **zero mobile users will ever see them**.

---

## 2. Why This Is Architecturally Tricky

This isn't a simple "add an endpoint" problem. The mobile app has **two parallel food data systems** that were never designed to receive server updates:

### System A — Legacy CSV (baked into APK)
```
dish_labels_and_values.csv → FoodItem (FOOD_TABLE)  → AI camera flow, ExploreScreen
dish_ingredients.csv       → DishIngredient          → Pantry matching
```
- Seeded at first launch via `FoodDatabaseCallback.onOpen()`
- `FoodItem.foodId` uses `autoGenerate = true` — IDs are **device-local**, not globally stable
- No `updated_at` column, no sync awareness

### System B — New JSON (baked into APK)
```
raw_ingredients.json    → RawIngredientEntity    → Nutrition calculation
dish_recipes.json       → DishRecipeEntity       → Manual logging, ExploreScreen
recipe_ingredients.json → RecipeIngredientEntity  → Ingredient gram weights
```
- Also seeded at first launch, re-seeded when asset count changes
- `ingredientId` is a `String` (e.g., `"pork_liempo"`) — globally stable
- No `updated_at` column, no sync awareness

### The Admin Panel's `food_table` (MySQL)

The admin panel manages a **single flat table** that maps closely to `FoodItem` (System A):
- Columns: `name_en`, `name_ph`, `category`, `ml_label`, 18 nutrient columns, `data_source`
- Has server-assigned integer `id` as PK

> [!WARNING]
> The admin's `food_table` schema matches mobile's `FOOD_TABLE` (System A), but the mobile's new nutrition engine runs on System B (the three-tier JSON data). **The admin panel has no awareness of System B at all** — no raw ingredients, no recipes, no gram weights.

---

## 3. The Core Architectural Decision You Need to Make

Before writing any code, you need to decide what "food sync" actually means:

### Option A: Sync System A Only (Quick Win)
Sync the admin's `food_table` → mobile's `FOOD_TABLE` (`FoodItem`). This means:
- ✅ Admin can add/edit dishes with per-100g macros
- ✅ New dishes appear in AI camera flow and Explore screen
- ✅ Straightforward — schema already matches
- ❌ Does NOT sync raw ingredients, recipes, or gram weights (System B)
- ❌ New dishes won't have per-ingredient nutrition breakdowns
- ❌ New dishes won't work in the Pantry matching/substitution flow

### Option B: Sync Both Systems (Full Solution)
The admin panel would need to also manage:
- Raw ingredients (per-100g USDA nutrients)
- Dish recipes (servings, cooked weight)
- Recipe-ingredient links (gram weights)

This means:
- ✅ Full feature parity — new dishes get per-ingredient nutrition, substitution, pantry matching
- ✅ Single source of truth for all food data
- ❌ Major admin panel expansion (3 new CRUD screens or a recipe builder UI)
- ❌ Complex sync — 4 tables with foreign key ordering
- ❌ Significantly more work

### Option C: Hybrid — Sync System A Now, Expand Later
- Sync `FoodItem` (System A) immediately
- New admin-added dishes work in camera flow and Explore, but show "flat" nutrition (no ingredient breakdown)
- Plan System B admin management as a future phase

> [!IMPORTANT]
> **My recommendation: Option C.** It solves the immediate "dead end" problem with minimal risk, and the System B data (82 ingredients, 24 recipes) is already USDA-verified and baked into the APK. You only need System B sync once the admin is actively managing recipes with gram weights — which is a much bigger product decision.

---

## 4. Proposed Implementation for Option C

### 4.1 Server Side (Laravel)

#### New Endpoint: `GET /api/sync/foods`

```
GET /api/sync/foods?since={timestamp}
Authorization: Bearer {firebase_id_token}

Response:
{
    "success": true,
    "foods": [ ...FoodItem objects... ],
    "server_timestamp": 1746403200000
}
```

- If `since=0` → return all foods (initial pull)
- If `since=1746400000` → return only foods where `updated_at > since` (delta pull)
- Protected by the same `firebase.auth` middleware

**Where to add it:** Inside `MobileSyncController.php` or a new `FoodSyncController.php`.

**Route:**
```php
Route::middleware('firebase.auth')->group(function () {
    Route::post('/sync/full', [MobileSyncController::class, 'syncFull']);
    Route::get('/sync/foods', [FoodSyncController::class, 'index']);  // NEW
});
```

#### Alternative: Embed in existing `syncFull` response

Instead of a separate endpoint, extend the `syncFull` response to include a `foods` key:

```json
{
    "success": true,
    "message": "...",
    "last_successful_sync": 1746403200000,
    "foods": [ ...delta food items... ],   // NEW
    "conflicts": []
}
```

> [!TIP]
> **I'd recommend the separate `GET /sync/foods` endpoint** rather than embedding in `syncFull`. Reasons:
> 1. Food catalog is **reference data** — it changes rarely and is the same for ALL users. User data (meals, activities) is per-user.
> 2. Separation lets you cache the food catalog response (all users get the same data).
> 3. The existing `syncFull` is a `POST` for push — mixing in a pull makes the semantics messy.

---

### 4.2 Mobile Side (Android/Kotlin)

#### Step 1: Add `updated_at` to `FoodItem`

The current `FoodItem` entity has no `updated_at` column. You'll need one to support delta sync:

```kotlin
@Entity(tableName = "FOOD_TABLE")
data class FoodItem(
    @PrimaryKey  // Remove autoGenerate — use server-assigned IDs
    @ColumnInfo(name = "food_id") val foodId: Int,
    // ... existing fields ...
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L  // NEW
)
```

> [!CAUTION]
> **Breaking change:** `foodId` currently uses `autoGenerate = true`, meaning each device generates its own IDs. Switching to server-assigned IDs requires a **Room migration** (version bump) and a one-time re-seed. Any existing `MealLogItemEntity` records that reference old `foodId` values would need remapping — or you accept that historical logs keep their old references.

#### Step 2: New Retrofit endpoint

```kotlin
interface CalorieKoApiService {
    @POST("api/sync/full")
    suspend fun syncFull(...): Response<SyncFullResponse>

    @GET("api/sync/foods")  // NEW
    suspend fun getFoodCatalog(
        @Header("Authorization") token: String,
        @Query("since") since: Long = 0
    ): Response<FoodCatalogResponse>
}
```

#### Step 3: FoodSyncManager

A new manager (or extension of `ApiSyncManager`) that:
1. Reads `last_food_sync_timestamp` from SharedPreferences
2. Calls `GET /api/sync/foods?since={timestamp}`
3. Upserts response into `FOOD_TABLE` via `FoodDao`
4. Persists new timestamp on success

#### Step 4: Trigger Points

When should the mobile app pull the food catalog?

| Trigger | Pros | Cons |
|---|---|---|
| At every app launch | Always fresh | Unnecessary traffic if catalog hasn't changed |
| During existing `syncFull` flow | Piggybacks on existing sync timing | Couples user sync with reference sync |
| Manual "Refresh Foods" button in Settings | User control, no waste | Users forget to press it |
| WorkManager periodic (e.g., daily) | Automatic, battery-friendly | 24h delay for new items |

**Recommended:** Pull food catalog **during the existing sync flow** (when user taps "Sync Data" in Settings or AutoSyncManager fires), plus a **WorkManager daily check**.

---

## 5. The `foodId` Identity Crisis

This is the single trickiest detail. Currently:

```
Mobile seeds FoodItem from CSV → autoGenerate = true → foodId = 1, 2, 3...
Admin panel has its own MySQL `id` column → id = 1, 2, 3...
```

These IDs are **coincidentally aligned** (both start at 1, same 24 dishes in same order), but this is fragile. If the admin adds dish #25, it gets `id=25` on the server. But a fresh mobile install seeds CSVs first (getting IDs 1–24), then syncs — dish #25 might get `foodId=25` or not, depending on Room's auto-increment state.

### Fix: Use `ml_label` as the stable identifier

Both systems already have `ml_label` (e.g., `"sinigang_pork"`). This is the TFLite output label and is globally unique. Use it as the natural key for upsert:

```kotlin
@Dao
interface FoodDao {
    @Query("SELECT * FROM FOOD_TABLE WHERE ml_label = :mlLabel LIMIT 1")
    suspend fun getFoodByMlLabel(mlLabel: String): FoodItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(food: FoodItem)
}
```

Server sync response should include `ml_label` so the mobile can match-and-upsert by label rather than by integer ID.

---

## 6. What About System B? (Future Phase)

If you eventually want the admin to manage full recipes (raw ingredients + gram weights), you'd need:

1. **Admin panel:** Three new CRUD screens or a unified "Recipe Builder" UI
2. **Server tables:** `raw_ingredients`, `dish_recipes`, `recipe_ingredients` in MySQL
3. **New sync endpoints:** `GET /api/sync/ingredients`, `GET /api/sync/recipes`
4. **Mobile:** Extend `FoodDatabaseCallback` to support server-seeded data alongside asset-seeded data

But this is a **product feature**, not a bug fix. The current System B data is already high-quality USDA-verified data. Until the admin actually needs to manage ingredients/recipes (not just flat dishes), this can wait.

---

## 7. Summary of Recommended Actions

| Priority | Action | Side | Effort |
|---|---|---|---|
| 🔴 P0 | Add `GET /api/sync/foods` endpoint | Server | Small |
| 🔴 P0 | Add food catalog pull to mobile sync flow | Mobile | Medium |
| 🟡 P1 | Migrate `FoodItem.foodId` from autoGenerate to server-assigned | Mobile | Medium (Room migration) |
| 🟡 P1 | Use `ml_label` as the upsert key for food sync | Both | Small |
| 🟢 P2 | Add `updated_at` to `FoodItem` for delta sync | Mobile | Small (Room migration) |
| 🟢 P2 | Daily WorkManager for background food catalog refresh | Mobile | Small |
| ⚪ Future | Admin panel recipe builder (System B management) | Server | Large |
| ⚪ Future | System B sync (ingredients, recipes, recipe-ingredients) | Both | Large |

---

## 8. Open Questions for You

1. **Do you want Option A, B, or C?** (I recommend C — sync `FoodItem` now, expand later)
2. **Historical meal logs:** Existing `MealLogItemEntity` records reference old auto-generated `foodId`s. Should we remap them during migration, or leave them as-is (they already store full nutrition data inline)?
3. **Do you want the admin-added foods to also appear in the Pantry matching flow?** If yes, we'd need to also sync `DishIngredient` entries alongside `FoodItem`.
4. **Separate endpoint vs. embedded in syncFull?** I recommend separate, but it's your call.
