# Food Database Management — Audit Summary

**Date:** Monday, May 5, 2026  
**Scope:** CalorieKo Admin Panel → Food Database feature (`FoodDatabaseView.vue`, `FoodItemController.php`, `FoodItem.php`, sync pipeline)

---

## What's Fully Functional (Admin Panel Side)

The admin panel's food database CRUD is **fully wired end-to-end** — frontend, API routes, controller, and model all exist and are connected.

| Feature | Details |
|---|---|
| **List all food items** | Fetches from `GET /api/admin/foods`, renders in a data table with columns for name (EN/PH), category, ML label, macros (cal/protein/carbs/fat), source, and actions |
| **Search & filter** | Client-side search by English or local name + category dropdown filter. Backend also supports `?search=` and `?category=` query params (not used by frontend) |
| **Add new food item** | Modal form with 21 nutrient fields → `POST /api/admin/foods` with full validation |
| **Edit food item** | Same modal pre-populated → `PUT /api/admin/foods/{id}` |
| **Delete food item** | Confirmation modal → `DELETE /api/admin/foods/{id}` |
| **Bulk import (CSV/XLSX)** | Drag-and-drop file upload → `POST /api/admin/foods/bulk-import` — parses CSV or XLSX (via PhpSpreadsheet), upserts by `name_en` + `name_ph`, reports imported/skipped/errors |
| **Stats summary** | Shows total items, FNRI count, USDA count, ML-labeled count |
| **Toast notifications** | Success/error feedback with auto-dismiss |
| **System logging** | All create/update/delete/import actions are logged to `SystemLog` |

### Key Files

- **Frontend:** [FoodDatabaseView.vue](file:///var/www/calorieko-admin/resources/js/views/FoodDatabaseView.vue)
- **API service:** [api.js](file:///var/www/calorieko-admin/resources/js/services/api.js) (lines 184–229)
- **Controller:** [FoodItemController.php](file:///var/www/calorieko-admin/app/Http/Controllers/Api/FoodItemController.php)
- **Model:** [FoodItem.php](file:///var/www/calorieko-admin/app/Models/FoodItem.php)
- **Routes:** [api.php](file:///var/www/calorieko-admin/routes/api.php) (lines 101–107)
- **Migration:** [create_food_table.php](file:///var/www/calorieko-admin/database/migrations/2024_01_01_000002_create_food_table.php), [add_ml_label_and_source.php](file:///var/www/calorieko-admin/database/migrations/2026_04_21_000001_add_ml_label_and_source_to_food_table.php)

---

## Admin Panel Limitations (Minor)

| Gap | Details |
|---|---|
| No pagination | `index()` does `$query->get()` — loads all items at once |
| No server-side search | Frontend fetches everything and filters locally |
| No sorting | Table has no sortable column headers |
| No export | Can import CSV/XLSX but no "Export" button |
| No duplicate preview | Bulk import upserts silently — no diff/preview before overwriting |
| No batch delete | One item at a time only |
| No food photos | No image field in schema or UI |
| No serving sizes | All values are per 100g only |

---

## Critical Finding: Food Data Does NOT Sync to Mobile

> [!CAUTION]
> The food database is a **dead end** — admin edits never reach mobile users.

### The Sync Gap

| Direction | Route | Status |
|---|---|---|
| **Mobile → Server** (push 1 item) | `POST /api/sync/food` → `FoodItemController::sync()` | ✅ Works |
| **Admin → Server** (CRUD) | `GET/POST/PUT/DELETE /api/admin/foods/*` | ✅ Works |
| **Server → Mobile** (pull catalog) | ❌ **No endpoint exists** | ❌ **Missing** |

### Why It's Broken

The master sync controller ([MobileSyncController.php](file:///var/www/calorieko-admin/app/Http/Controllers/Api/MobileSyncController.php)) handles **profiles, meals, activities, and nutrition summaries** — but **completely skips `FOOD_TABLE`**. There is:

- No `foods` key in the sync payload
- No logic to return food items in the sync response
- No standalone `GET /api/sync/foods` endpoint for the mobile app to pull the catalog

### Consequences

1. Adding/editing food in the admin panel → stays only on the server
2. Bulk-importing 200 foods via CSV → invisible to mobile users
3. The mobile app's local Room `FOOD_TABLE` is completely independent and never reconciles with the server
4. `POST /api/sync/food` only pushes **one** food from mobile → server (no reverse)

### What's Needed to Fix

1. A **`GET /api/sync/foods`** endpoint (behind `firebase.auth`) that returns the full food catalog (or a delta based on `last_sync_timestamp`)
2. The **mobile app's sync worker** must call this endpoint and upsert results into its local Room `FOOD_TABLE`
3. Optionally, include a `foods` section in the `syncFull` response to push updated items during master sync
