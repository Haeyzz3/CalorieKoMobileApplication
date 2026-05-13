# Food Database Sync — Manual Verification Steps

> [!IMPORTANT]
> These steps verify the synchronization between the Laravel Admin Panel and the Android App, as well as the data integrity guards.

---

## Part 1: Admin Panel Verification

### 1.1 Clean Management View
1. Open Admin Panel -> **Food Database**.
2. **Expect:** The list is clean. All USDA-sourced dishes (and the 29 recipes) are hidden by default.
3. **Check Toggle:** Look at the toggle button. It should show a dynamic count (e.g., "Show 35 USDA-protected").
4. **Verify Toggle:** Turn the toggle ON. The 35 items should appear. Turn it OFF. They should disappear.

### 1.2 Data Integrity Guards
1. Turn the toggle ON.
2. Find an item with source `USDA_FNDDS` (e.g., **egg_fried**).
3. **Expect:** It shows a teal **"Verified"** badge. Edit and Delete buttons are HIDDEN.
4. Find an item with source `DOST_FNRI` (e.g., **Sinigang na Baboy**).
5. **Expect:** It is **Editable**. You should see the Edit ✏️ and Delete 🗑️ buttons.
   - *Note: Only the 29 dishes that use System B on mobile are protected if they have FNRI source.*

### 1.3 Search & Filter Logic
1. Type "Sinigang" in the search box.
2. **Expect:** Sinigang na Baboy (FNRI) appears.
3. Turn the toggle ON.
4. **Expect:** Both the editable Sinigang and any protected ones (if any) appear.
5. This verifies the nested search closure fix.

---

## Part 2: Mobile App Verification

### 2.1 Manual Sync Button
1. Open CalorieKo App -> **Settings**.
2. Tap **"Sync Data"**.
3. **Expect:** After a few seconds, the "Food catalog" status changes from "never synced" to **"Just now"**.
4. This verifies `FoodCatalogSyncManager` is working in the foreground.

### 2.2 Community Badge
1. Add a test food in the Admin Panel (e.g., "Test Adobo", source: Community).
2. Sync on mobile.
3. Go to **Explore**.
4. **Expect:** "Test Adobo" appears with a **purple "Community"** badge.
5. Tap it. The detail sheet should attribute it to "CalorieKo Community Database".

### 2.3 USDA Protection (End-to-End)
1. In Admin Panel, try to edit "Sinigang na Baboy" (if it's protected) or "Chicken Breast" (USDA).
2. If it's a locked USDA item, you shouldn't be able to edit it.
3. Verify on mobile that "Chicken Breast" still shows its original USDA nutritional values, not any accidental server overrides.

---

## Part 3: Data Restoration Check
1. Search for **Sinigang na Baboy** on Admin.
2. **Expect:** Nutritional values are **159.51 kcal** per 100g (original values restored).
3. **Missing Dishes:** Verify that **Humba**, **Kwek-kwek**, and **Lechon Manok Pakpak** are now present in the database (they were previously missing from the server).
