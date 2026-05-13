# Food Database Sync — Walkthrough

## Summary
Implemented **Alternative A (Improved Workaround)** for syncing admin-managed food data from the Laravel backend to the mobile app. The sync uses `ml_label` as the identity key (not `foodId`), protects USDA-verified dishes from overwrite on both sides (admin panel blocks edits, mobile sync skips them), and adds Community vs. USDA badge distinction in the UI.

---

## Changes Made (12 files)

### API Layer

#### [CalorieKoApiService.kt](file:///home/raffyabydc/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/remote/api/CalorieKoApiService.kt)
- Added `getFoodCatalog()` GET endpoint for `api/sync/foods/catalog`

#### [SyncPayload.kt](file:///home/raffyabydc/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/remote/api/SyncPayload.kt)
- Added `FoodCatalogResponse` and `SyncFoodItem` data classes
- `SyncFoodItem.toFoodItem()` sets `foodId = 0` (Room auto-generates)

---

### Data Layer

#### [FoodDao.kt](file:///home/raffyabydc/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/local/FoodDao.kt)
- Added `deleteByMlLabels()` — batch delete by ml_label
- Added `getAllMlLabels()` — lightweight label query
- Added `syncFromServer(serverFoods, protectedLabels)` — **USDA-safe upsert**: filters out any server food whose `ml_label` matches a System B dish, then deletes+inserts only admin-added dishes

#### [DishRecipeDao.kt](file:///home/raffyabydc/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/local/DishRecipeDao.kt)
- Added `getAllDishLabels()` — returns dish_label strings for USDA protection

#### [FoodDatabaseCallback.kt](file:///home/raffyabydc/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/local/FoodDatabaseCallback.kt)
- Added `food_catalog_synced` SharedPreferences flag check
- CSV re-seeding for FOOD_TABLE is now skipped once the server has synced
- DISH_INGREDIENTS_TABLE still seeds from CSV independently

---

### Sync Pipeline

#### [SyncWorker.kt](file:///home/raffyabydc/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/data/remote/api/SyncWorker.kt)
- Added **Step 5: Pull food catalog** after successful Laravel push
- Gets Firebase token → calls `getFoodCatalog()` → resolves USDA-protected labels → calls `foodDao.syncFromServer()` with protection
- Sets `food_catalog_synced` flag and `last_food_catalog_sync_ms` timestamp
- Entire step is **non-fatal** — failure doesn't affect sync result

---

### ViewModel Layer

#### [ExploreViewModel.kt](file:///home/raffyabydc/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/viewmodel/ExploreViewModel.kt)
- Added `FoodDao` dependency
- `loadAllDishes()` now merges admin-only dishes (from FOOD_TABLE where ml_label ∉ System B) with `dataSource = "COMMUNITY"`
- `getSourceDisplayLabel()` returns "CalorieKo Community Database" for Community dishes
- `getSourceBadgeLabel()` returns "Community" for Community dishes
- `getSourceUrl()` returns empty string for Community dishes (no USDA link)

#### [ManualLogViewModel.kt](file:///home/raffyabydc/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/viewmodel/ManualLogViewModel.kt)
- Added `FoodDao` dependency
- `init` merges admin-only dishes as synthetic `DishRecipeEntity` objects
- `addDish()` now checks `dishRecipeDao.getByDishLabel()` existence (not `calories == 0f`) to decide path:
  - **System B (USDA)**: Full `RecipeNutritionCalculator`
  - **System A (Community)**: Flat per-100g scaling

---

### UI Layer

#### [ExploreScreen.kt](file:///home/raffyabydc/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/ui/screens/ExploreScreen.kt)
- **Dish cards**: Added USDA (green) / Community (purple) source badge in top-right corner
- **Detail sheet**: Source-aware colors and icon (VerifiedUser for USDA, Description for Community)
- Community dishes show "Per 100g flat nutrition — no ingredient breakdown available" notice
- USDA proof buttons and FNRI recipe section are hidden for Community dishes

#### [SettingsViewModel.kt](file:///home/raffyabydc/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/viewmodel/SettingsViewModel.kt)
- Added `lastFoodCatalogSyncedAt` StateFlow reading from `sync_prefs → last_food_catalog_sync_ms`
- Refreshed after successful sync

#### [SettingsScreen.kt](file:///home/raffyabydc/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/ui/screens/SettingsScreen.kt)
- Sync Data row subtitle now shows two lines:
  ```
  Last synced: Today, 9:46 AM
  Food catalog: Never synced
  ```

---

### Factory Wiring

#### [MainActivity.kt](file:///home/raffyabydc/AndroidStudioProjects/CalorieKoMobileApplication/app/src/main/java/com/calorieko/app/MainActivity.kt)
- Added `foodDao = db.foodDao()` to 1 `ExploreViewModel` factory and 3 `ManualLogViewModel` factory call sites

---

## Verification

### 1. Mobile Sync Architecture
*   **Centralized Manager:** Created `FoodCatalogSyncManager.kt` to share pull/sync logic between background workers and manual UI triggers.
*   **Manual Sync Fix:** Updated `SettingsViewModel.kt` to trigger a food catalog pull immediately after a successful activity log sync.
*   **Background Sync:** Refactored `SyncWorker.kt` to use the new manager, ensuring background integrity.
*   **Timestamp UI:** Fixed the issue where "Food catalog" would stay at "never synced" after a manual update.

### 2. Admin Protection & Data Integrity
*   **Narrowed Guard:** Updated `FoodItemController.php` to block edits/deletes for any item with a `USDA` data source or any of the **29 core System B dishes**.
*   **Editable FNRI:** Restored edit access for `DOST_FNRI` items (unless they are part of the core 29 recipes), allowing admins to manage local database entries.
*   **Data Restoration:** Inserted 10 missing USDA dishes (Humba, Kwek-kwek, etc.) to bring the server count to parity with mobile requirements.
*   **Recovered Values:** Corrected the Sinigang na Baboy values (159.51 kcal) after an accidental edit.

### 3. Admin UI Improvements
*   **Clean View:** Hiding all protected/read-only items by default to reduce management clutter.
*   **Dynamic Toggle:** Added a "Show [N] USDA-protected" toggle that accurately reflects the count of hidden items from the database.
*   **Search Logic:** Wrapped frontend search in a nested `where()` closure to prevent `OR` conditions from accidentally bypassing the USDA/Category filters.

### Step 1: Verify Manual Sync (Mobile)
1.  Open CalorieKo app.
2.  Go to **Settings** -> Tap **"Sync Data"**.
3.  **Expect:** Both "Last synced" and "Food catalog" timestamps should update to "Just now".
4.  Navigate to **Explore**.
5.  **Expect:** Admin-added dishes (if any) should appear with a purple **"Community"** badge.

### Step 2: Verify Protection (Admin)
1.  Open Admin Panel -> **Food Database**.
2.  **Expect:** All USDA dishes are hidden by default. The toggle should show the total count of hidden items (e.g., "Show 35 USDA-protected").
3.  Check the toggle.
4.  **Search:** Search for "Sinigang na Baboy" (FNRI) and "egg_fried" (USDA).
5.  **Expect:** "Sinigang na Baboy" should be editable. "egg_fried" should have a "Verified" badge and its Edit/Delete buttons should be hidden.
6.  **Try Edit:** Attempt to edit a USDA item via the API directly.
7.  **Expect:** `403 Forbidden` response.

- ✅ Mobile build passes (`BUILD SUCCESSFUL in 1m 3s`)
- ✅ Admin Vite build passes (`built in 5.68s`)
- ✅ Laravel caches cleared
- No Room migration needed
- No schema changes
- Server side fully deployed
