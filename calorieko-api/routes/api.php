<?php

use Illuminate\Support\Facades\Route;
use App\Http\Controllers\Api\UserProfileController;
use App\Http\Controllers\Api\FoodItemController;
use App\Http\Controllers\Api\ActivityLogController;
use App\Http\Controllers\Api\MealLogController;
use App\Http\Controllers\Api\DailyNutritionSummaryController;
use App\Http\Controllers\Api\AdminAuthController;
use App\Http\Controllers\Api\SystemLogController;

/*
|--------------------------------------------------------------------------
| CalorieKo API Routes
|--------------------------------------------------------------------------
|
| Mobile sync endpoints (require Firebase auth):
|   POST /api/sync/profile
|   POST /api/sync/food
|   POST /api/sync/activity-log
|   POST /api/sync/activity-log/batch
|   POST /api/sync/meal-log
|   POST /api/sync/nutrition-summary
|
| Admin read endpoints (no auth for now — add admin auth later):
|   GET /api/admin/profiles
|   GET /api/admin/profiles/{uid}
|   GET /api/admin/foods
|   GET /api/admin/foods/{id}
|   GET /api/admin/activity-logs
|   GET /api/admin/activity-logs/{id}
|   GET /api/admin/meal-logs
|   GET /api/admin/meal-logs/{id}
|   GET /api/admin/nutrition-summaries
|   GET /api/admin/nutrition-summaries/{id}
|
*/

// ── Mobile Sync Endpoints (Firebase Auth required) ──
Route::prefix('sync')
    ->middleware('firebase.auth')
    ->group(function () {
        Route::post('/profile',           [UserProfileController::class, 'sync']);
        Route::post('/food',              [FoodItemController::class, 'sync']);
        Route::post('/activity-log',      [ActivityLogController::class, 'sync']);
        Route::post('/activity-log/batch',[ActivityLogController::class, 'syncBatch']);
        Route::post('/meal-log',          [MealLogController::class, 'sync']);
        Route::post('/nutrition-summary', [DailyNutritionSummaryController::class, 'sync']);
    });

// ── Admin Auth Endpoints ──
Route::post('/admin/login',  [AdminAuthController::class, 'login']);
Route::post('/admin/verify', [AdminAuthController::class, 'verify']);

// ── Admin Read Endpoints ──
Route::prefix('admin')->group(function () {
    // User Profiles
    Route::get('/profiles',                     [UserProfileController::class, 'index']);
    Route::get('/profiles/{uid}',               [UserProfileController::class, 'show']);
    Route::put('/profiles/{uid}/deactivate',    [UserProfileController::class, 'deactivate']);
    Route::post('/profiles/{uid}/reset-password',[UserProfileController::class, 'resetPassword']);
    Route::delete('/profiles/{uid}',            [UserProfileController::class, 'destroy']);

    // Food Items
    Route::get('/foods',              [FoodItemController::class, 'index']);
    Route::get('/foods/{id}',         [FoodItemController::class, 'show']);
    Route::post('/foods',             [FoodItemController::class, 'store']);
    Route::put('/foods/{id}',         [FoodItemController::class, 'update']);
    Route::delete('/foods/{id}',      [FoodItemController::class, 'destroy']);

    // Activity Logs
    Route::get('/activity-logs',      [ActivityLogController::class, 'index']);
    Route::get('/activity-logs/{id}', [ActivityLogController::class, 'show']);

    // Meal Logs (with items)
    Route::get('/meal-logs',          [MealLogController::class, 'index']);
    Route::get('/meal-logs/{id}',     [MealLogController::class, 'show']);

    // Daily Nutrition Summaries
    Route::get('/nutrition-summaries',      [DailyNutritionSummaryController::class, 'index']);
    Route::get('/nutrition-summaries/{id}', [DailyNutritionSummaryController::class, 'show']);

    // System Logs
    Route::get('/system-logs',              [SystemLogController::class, 'index']);
});

// Health check
Route::get('/health', function () {
    return response()->json(['status' => 'ok', 'app' => 'CalorieKo API']);
});
