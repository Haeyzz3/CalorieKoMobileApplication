<?php

/**
 * CalorieKo API Routes — Mobile Sync
 *
 * Add this route registration to your existing routes/api.php file.
 * The route must be protected by Firebase authentication middleware.
 *
 * ── Usage ──
 * Add the following lines to routes/api.php:
 *
 *   use App\Http\Controllers\Api\MobileSyncController;
 *
 *   Route::middleware('firebase.auth')->group(function () {
 *       Route::post('/sync/full', [MobileSyncController::class, 'syncFull']);
 *   });
 *
 * ── Middleware ──
 * Ensure your `firebase.auth` middleware validates the Bearer token
 * from the Authorization header against Firebase Admin SDK.
 */

use App\Http\Controllers\Api\MobileSyncController;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| Mobile Sync Routes (Delta Sync with Last Write Wins)
|--------------------------------------------------------------------------
|
| POST /api/sync/full
|   - Accepts a delta JSON payload containing only modified entities
|   - Implements Last Write Wins conflict resolution per-entity
|   - Returns structured response with sync stats and conflict details
|
*/

Route::middleware('firebase.auth')->group(function () {
    Route::post('/sync/full', [MobileSyncController::class, 'syncFull'])
        ->name('mobile.sync.full');
});
