<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Validator;

/**
 * MobileSyncController — Delta Sync with Last Write Wins Conflict Resolution
 *
 * ── Architecture ──
 * This controller acts as the System of Record (SoR) receiver for the CalorieKo
 * mobile application. It accepts delta payloads containing only records modified
 * since the client's last successful sync.
 *
 * ── Conflict Resolution Protocol ──
 * For every incoming entity:
 *   1. IF the record does NOT exist on the server → INSERT it.
 *   2. IF the record EXISTS and the incoming mobile `updated_at` is STRICTLY
 *      GREATER than the server's `mobile_updated_at` → UPDATE the record.
 *   3. IF the record EXISTS and the server's `mobile_updated_at` is greater than
 *      or equal to the mobile `updated_at` → REJECT the update (admin/server override).
 *
 * The response includes a structured `conflicts` array detailing all rejections.
 *
 * Endpoint: POST /api/sync/full
 * Auth: Firebase ID token (Bearer) — validated by middleware
 */
class MobileSyncController extends Controller
{
    /**
     * Process a delta sync payload from the mobile client.
     *
     * @param Request $request
     * @return \Illuminate\Http\JsonResponse
     */
    public function syncFull(Request $request)
    {
        $startTime = microtime(true);

        // ── 1. Validate the incoming payload ──
        $validator = Validator::make($request->all(), [
            'uid'                          => 'required|string',
            'last_sync_timestamp'          => 'required|integer',

            // Profile (optional — may be null if unchanged)
            'profile'                      => 'nullable|array',
            'profile.name'                 => 'sometimes|string|max:255',
            'profile.email'                => 'sometimes|email|max:255',
            'profile.age'                  => 'sometimes|integer|min:1|max:150',
            'profile.weight'               => 'sometimes|numeric|min:1',
            'profile.height'               => 'sometimes|numeric|min:1',
            'profile.sex'                  => 'sometimes|string',
            'profile.activityLevel'        => 'sometimes|string',
            'profile.goal'                 => 'sometimes|string',
            'profile.streak'               => 'sometimes|integer|min:0',
            'profile.level'                => 'sometimes|integer|min:1',
            'profile.updated_at'           => 'sometimes|integer',

            // Meals
            'meals'                        => 'nullable|array',
            'meals.*.uid'                  => 'required|string',
            'meals.*.meal_type'            => 'required|string',
            'meals.*.timestamp'            => 'required|integer',
            'meals.*.updated_at'           => 'required|integer',
            'meals.*.items'                => 'nullable|array',
            'meals.*.items.*.food_id'      => 'required|integer',
            'meals.*.items.*.dish_name'    => 'required|string',
            'meals.*.items.*.weight_grams' => 'required|numeric',
            'meals.*.items.*.updated_at'   => 'required|integer',

            // Activities
            'activities'                   => 'nullable|array',
            'activities.*.uid'             => 'required|string',
            'activities.*.type'            => 'required|string',
            'activities.*.name'            => 'required|string',
            'activities.*.timestamp'       => 'required|integer',
            'activities.*.updated_at'      => 'required|integer',

            // Nutrition Summaries
            'nutrition_summaries'                    => 'nullable|array',
            'nutrition_summaries.*.uid'              => 'required|string',
            'nutrition_summaries.*.date_epoch_day'   => 'required|integer',
            'nutrition_summaries.*.updated_at'       => 'required|integer',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Validation failed',
                'error'   => $validator->errors()->first(),
            ], 422);
        }

        $uid = $request->input('uid');
        $conflicts = [];
        $stats = [
            'profile_updated'       => false,
            'meals_inserted'        => 0,
            'meals_updated'         => 0,
            'meal_items_processed'  => 0,
            'activities_inserted'   => 0,
            'activities_updated'    => 0,
            'summaries_inserted'    => 0,
            'summaries_updated'     => 0,
        ];

        try {
            DB::beginTransaction();

            // ══════════════════════════════════════════════════
            // 2. PROFILE — Last Write Wins
            // ══════════════════════════════════════════════════
            $profileData = $request->input('profile');
            if ($profileData) {
                $mobileUpdatedAt = $profileData['updated_at'] ?? 0;

                $existingUser = DB::table('users')->where('firebase_uid', $uid)->first();

                if (!$existingUser) {
                    // New user — insert
                    DB::table('users')->insert([
                        'firebase_uid'      => $uid,
                        'name'              => $profileData['name'] ?? '',
                        'email'             => $profileData['email'] ?? '',
                        'age'               => $profileData['age'] ?? 0,
                        'weight'            => $profileData['weight'] ?? 0,
                        'height'            => $profileData['height'] ?? 0,
                        'sex'               => $profileData['sex'] ?? '',
                        'activity_level'    => $profileData['activityLevel'] ?? '',
                        'goal'              => $profileData['goal'] ?? '',
                        'streak'            => $profileData['streak'] ?? 0,
                        'level'             => $profileData['level'] ?? 1,
                        'mobile_updated_at' => $mobileUpdatedAt,
                        'created_at'        => now(),
                        'updated_at'        => now(),
                    ]);
                    $stats['profile_updated'] = true;
                } elseif ($mobileUpdatedAt > ($existingUser->mobile_updated_at ?? 0)) {
                    // Mobile is newer — update
                    DB::table('users')->where('firebase_uid', $uid)->update([
                        'name'              => $profileData['name'] ?? $existingUser->name,
                        'email'             => $profileData['email'] ?? $existingUser->email,
                        'age'               => $profileData['age'] ?? $existingUser->age,
                        'weight'            => $profileData['weight'] ?? $existingUser->weight,
                        'height'            => $profileData['height'] ?? $existingUser->height,
                        'sex'               => $profileData['sex'] ?? $existingUser->sex,
                        'activity_level'    => $profileData['activityLevel'] ?? $existingUser->activity_level,
                        'goal'              => $profileData['goal'] ?? $existingUser->goal,
                        'streak'            => $profileData['streak'] ?? $existingUser->streak,
                        'level'             => $profileData['level'] ?? $existingUser->level,
                        'mobile_updated_at' => $mobileUpdatedAt,
                        'updated_at'        => now(),
                    ]);
                    $stats['profile_updated'] = true;
                } else {
                    // Server is newer — reject mobile update
                    $conflicts[] = [
                        'entity_type' => 'profile',
                        'entity_id'   => $uid,
                        'reason'      => 'Server record is newer (admin override)',
                    ];
                }
            }

            // ══════════════════════════════════════════════════
            // 3. MEAL LOGS — Last Write Wins (no bulk delete!)
            // ══════════════════════════════════════════════════
            $meals = $request->input('meals', []);
            foreach ($meals as $meal) {
                $mobileUpdatedAt = $meal['updated_at'] ?? 0;

                // Use uid + timestamp + meal_type as the unique composite key
                $existing = DB::table('meal_logs')
                    ->where('uid', $uid)
                    ->where('timestamp', $meal['timestamp'])
                    ->where('meal_type', $meal['meal_type'])
                    ->first();

                $mealLogId = null;

                if (!$existing) {
                    // Insert new meal log
                    $mealLogId = DB::table('meal_logs')->insertGetId([
                        'uid'               => $uid,
                        'meal_type'         => $meal['meal_type'],
                        'timestamp'         => $meal['timestamp'],
                        'notes'             => $meal['notes'] ?? null,
                        'mobile_updated_at' => $mobileUpdatedAt,
                        'created_at'        => now(),
                        'updated_at'        => now(),
                    ]);
                    $stats['meals_inserted']++;
                } elseif ($mobileUpdatedAt > ($existing->mobile_updated_at ?? 0)) {
                    // Mobile is newer — update
                    $mealLogId = $existing->id;
                    DB::table('meal_logs')->where('id', $mealLogId)->update([
                        'notes'             => $meal['notes'] ?? $existing->notes,
                        'mobile_updated_at' => $mobileUpdatedAt,
                        'updated_at'        => now(),
                    ]);
                    $stats['meals_updated']++;
                } else {
                    // Server is newer — reject
                    $conflicts[] = [
                        'entity_type' => 'meal_log',
                        'entity_id'   => (string) ($existing->id ?? $meal['timestamp']),
                        'reason'      => 'Server record is newer (admin override)',
                    ];
                    continue; // Skip items processing for this rejected meal
                }

                // Process meal items (child entities of the accepted meal log)
                if ($mealLogId && isset($meal['items'])) {
                    foreach ($meal['items'] as $item) {
                        $itemMobileUpdatedAt = $item['updated_at'] ?? 0;

                        $existingItem = DB::table('meal_log_items')
                            ->where('meal_log_id', $mealLogId)
                            ->where('food_id', $item['food_id'])
                            ->first();

                        $itemData = [
                            'meal_log_id'         => $mealLogId,
                            'food_id'             => $item['food_id'],
                            'dish_name'           => $item['dish_name'],
                            'weight_grams'        => $item['weight_grams'],
                            'calories'            => $item['calories'] ?? 0,
                            'protein'             => $item['protein'] ?? 0,
                            'carbs'               => $item['carbs'] ?? 0,
                            'fat'                 => $item['fat'] ?? 0,
                            'fiber'               => $item['fiber'] ?? 0,
                            'sugar'               => $item['sugar'] ?? 0,
                            'saturated_fat'       => $item['saturated_fat'] ?? 0,
                            'polyunsaturated_fat' => $item['polyunsaturated_fat'] ?? 0,
                            'monounsaturated_fat' => $item['monounsaturated_fat'] ?? 0,
                            'trans_fat'           => $item['trans_fat'] ?? 0,
                            'cholesterol'         => $item['cholesterol'] ?? 0,
                            'sodium'              => $item['sodium'] ?? 0,
                            'potassium'           => $item['potassium'] ?? 0,
                            'vitamin_a'           => $item['vitamin_a'] ?? 0,
                            'vitamin_c'           => $item['vitamin_c'] ?? 0,
                            'calcium'             => $item['calcium'] ?? 0,
                            'iron'                => $item['iron'] ?? 0,
                            'mobile_updated_at'   => $itemMobileUpdatedAt,
                        ];

                        if (!$existingItem) {
                            $itemData['created_at'] = now();
                            $itemData['updated_at'] = now();
                            DB::table('meal_log_items')->insert($itemData);
                        } elseif ($itemMobileUpdatedAt > ($existingItem->mobile_updated_at ?? 0)) {
                            $itemData['updated_at'] = now();
                            DB::table('meal_log_items')->where('id', $existingItem->id)->update($itemData);
                        }
                        // If server item is newer, silently skip (no conflict reported for child items)

                        $stats['meal_items_processed']++;
                    }
                }
            }

            // ══════════════════════════════════════════════════
            // 4. ACTIVITY LOGS — Last Write Wins
            // ══════════════════════════════════════════════════
            $activities = $request->input('activities', []);
            foreach ($activities as $activity) {
                $mobileUpdatedAt = $activity['updated_at'] ?? 0;

                // Use uid + timestamp + name as composite key
                $existing = DB::table('activity_logs')
                    ->where('uid', $uid)
                    ->where('timestamp', $activity['timestamp'])
                    ->where('name', $activity['name'])
                    ->first();

                $activityData = [
                    'uid'                => $uid,
                    'type'               => $activity['type'],
                    'name'               => $activity['name'],
                    'time_string'        => $activity['timeString'] ?? '',
                    'weight_or_duration' => $activity['weightOrDuration'] ?? '',
                    'calories'           => $activity['calories'] ?? 0,
                    'protein'            => $activity['protein'] ?? 0,
                    'carbs'              => $activity['carbs'] ?? 0,
                    'fats'               => $activity['fats'] ?? 0,
                    'sodium'             => $activity['sodium'] ?? 0,
                    'timestamp'          => $activity['timestamp'],
                    'distance_km'        => $activity['distanceKm'] ?? null,
                    'pace'               => $activity['pace'] ?? null,
                    'moving_time_seconds'=> $activity['movingTimeSeconds'] ?? null,
                    'map_type'           => $activity['mapType'] ?? null,
                    'notes'              => $activity['notes'] ?? null,
                    'activity_tag'       => $activity['activityTag'] ?? null,
                    'mobile_updated_at'  => $mobileUpdatedAt,
                ];

                if (!$existing) {
                    $activityData['created_at'] = now();
                    $activityData['updated_at'] = now();
                    DB::table('activity_logs')->insert($activityData);
                    $stats['activities_inserted']++;
                } elseif ($mobileUpdatedAt > ($existing->mobile_updated_at ?? 0)) {
                    $activityData['updated_at'] = now();
                    DB::table('activity_logs')->where('id', $existing->id)->update($activityData);
                    $stats['activities_updated']++;
                } else {
                    $conflicts[] = [
                        'entity_type' => 'activity_log',
                        'entity_id'   => (string) ($existing->id ?? $activity['timestamp']),
                        'reason'      => 'Server record is newer (admin override)',
                    ];
                }
            }

            // ══════════════════════════════════════════════════
            // 5. DAILY NUTRITION SUMMARIES — Last Write Wins
            // ══════════════════════════════════════════════════
            $summaries = $request->input('nutrition_summaries', []);
            foreach ($summaries as $summary) {
                $mobileUpdatedAt = $summary['updated_at'] ?? 0;

                // Use uid + date_epoch_day as the unique composite key
                $existing = DB::table('daily_nutrition_summaries')
                    ->where('uid', $uid)
                    ->where('date_epoch_day', $summary['date_epoch_day'])
                    ->first();

                $summaryData = [
                    'uid'                      => $uid,
                    'date_epoch_day'           => $summary['date_epoch_day'],
                    'total_calories'           => $summary['total_calories'] ?? 0,
                    'total_protein'            => $summary['total_protein'] ?? 0,
                    'total_carbs'              => $summary['total_carbs'] ?? 0,
                    'total_fiber'              => $summary['total_fiber'] ?? 0,
                    'total_sugar'              => $summary['total_sugar'] ?? 0,
                    'total_fat'                => $summary['total_fat'] ?? 0,
                    'total_saturated_fat'      => $summary['total_saturated_fat'] ?? 0,
                    'total_polyunsaturated_fat'=> $summary['total_polyunsaturated_fat'] ?? 0,
                    'total_monounsaturated_fat'=> $summary['total_monounsaturated_fat'] ?? 0,
                    'total_trans_fat'          => $summary['total_trans_fat'] ?? 0,
                    'total_cholesterol'        => $summary['total_cholesterol'] ?? 0,
                    'total_sodium'             => $summary['total_sodium'] ?? 0,
                    'total_potassium'          => $summary['total_potassium'] ?? 0,
                    'total_vitamin_a'          => $summary['total_vitamin_a'] ?? 0,
                    'total_vitamin_c'          => $summary['total_vitamin_c'] ?? 0,
                    'total_calcium'            => $summary['total_calcium'] ?? 0,
                    'total_iron'               => $summary['total_iron'] ?? 0,
                    'breakfast_calories'       => $summary['breakfast_calories'] ?? 0,
                    'lunch_calories'           => $summary['lunch_calories'] ?? 0,
                    'dinner_calories'          => $summary['dinner_calories'] ?? 0,
                    'snacks_calories'          => $summary['snacks_calories'] ?? 0,
                    'mobile_updated_at'        => $mobileUpdatedAt,
                ];

                if (!$existing) {
                    $summaryData['created_at'] = now();
                    $summaryData['updated_at'] = now();
                    DB::table('daily_nutrition_summaries')->insert($summaryData);
                    $stats['summaries_inserted']++;
                } elseif ($mobileUpdatedAt > ($existing->mobile_updated_at ?? 0)) {
                    $summaryData['updated_at'] = now();
                    DB::table('daily_nutrition_summaries')->where('id', $existing->id)->update($summaryData);
                    $stats['summaries_updated']++;
                } else {
                    $conflicts[] = [
                        'entity_type' => 'nutrition_summary',
                        'entity_id'   => (string) $summary['date_epoch_day'],
                        'reason'      => 'Server record is newer (admin override)',
                    ];
                }
            }

            DB::commit();

            $elapsed = round((microtime(true) - $startTime) * 1000);
            $totalProcessed = $stats['meals_inserted'] + $stats['meals_updated']
                + $stats['activities_inserted'] + $stats['activities_updated']
                + $stats['summaries_inserted'] + $stats['summaries_updated']
                + ($stats['profile_updated'] ? 1 : 0);

            Log::info("Delta sync completed for UID: {$uid}", [
                'elapsed_ms' => $elapsed,
                'stats'      => $stats,
                'conflicts'  => count($conflicts),
            ]);

            return response()->json([
                'success'              => true,
                'message'              => "Delta sync complete: {$totalProcessed} records processed in {$elapsed}ms",
                'last_successful_sync' => (int) (microtime(true) * 1000),
                'stats'                => $stats,
                'conflicts'            => $conflicts,
                'error'                => null,
            ]);

        } catch (\Throwable $e) {
            DB::rollBack();

            Log::error("Delta sync FAILED for UID: {$uid}", [
                'error'   => $e->getMessage(),
                'trace'   => $e->getTraceAsString(),
            ]);

            return response()->json([
                'success' => false,
                'message' => 'Sync failed due to server error',
                'error'   => $e->getMessage(),
            ], 500);
        }
    }
}
