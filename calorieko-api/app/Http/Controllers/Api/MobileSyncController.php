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
 * ── Table Name Mapping (Room Entity → MySQL) ──
 *   UserProfile           → user_profile              (PK: uid)
 *   MealLogEntity         → meal_log_table             (PK: meal_log_id)
 *   MealLogItemEntity     → meal_log_item_table        (PK: meal_log_item_id)
 *   ActivityLogEntity     → activity_log_table          (PK: id)
 *   DailyNutritionSummary → daily_nutrition_summary_table (PK: id)
 *
 * ── Column Name Convention ──
 *   Columns use the SAME names as Room entities (camelCase for activity_log_table):
 *   timeString, weightOrDuration, distanceKm, movingTimeSeconds, mapType, activityTag
 *
 * Endpoint: POST /api/sync/full
 * Auth: Firebase ID token (Bearer) — validated by middleware
 */
class MobileSyncController extends Controller
{
    public function syncFull(Request $request)
    {
        $startTime = microtime(true);

        // ── 1. Validate ──
        $validator = Validator::make($request->all(), [
            'uid'                          => 'required|string',
            'last_sync_timestamp'          => 'required|integer',

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

            'activities'                   => 'nullable|array',
            'activities.*.uid'             => 'required|string',
            'activities.*.type'            => 'required|string',
            'activities.*.name'            => 'required|string',
            'activities.*.timestamp'       => 'required|integer',
            'activities.*.updated_at'      => 'required|integer',

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

        // UID mismatch guard
        if ($request->firebaseUid && $request->firebaseUid !== $uid) {
            return response()->json([
                'success' => false,
                'message' => 'UID mismatch',
                'error'   => 'uid_mismatch',
            ], 403);
        }

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
            // 2. PROFILE — user_profile (PK: uid)
            // ══════════════════════════════════════════════════
            $profileData = $request->input('profile');
            if ($profileData) {
                $mobileUpdatedAt = $profileData['updated_at'] ?? 0;
                $existing = DB::table('user_profile')->where('uid', $uid)->first();

                if (!$existing) {
                    DB::table('user_profile')->insert([
                        'uid'              => $uid,
                        'name'             => $profileData['name'] ?? '',
                        'email'            => $profileData['email'] ?? '',
                        'age'              => $profileData['age'] ?? 0,
                        'weight'           => $profileData['weight'] ?? 0,
                        'height'           => $profileData['height'] ?? 0,
                        'sex'              => $profileData['sex'] ?? '',
                        'activityLevel'    => $profileData['activityLevel'] ?? '',
                        'goal'             => $profileData['goal'] ?? '',
                        'streak'           => $profileData['streak'] ?? 0,
                        'level'            => $profileData['level'] ?? 1,
                        'is_active'        => true,
                        'mobile_updated_at'=> $mobileUpdatedAt,
                        'created_at'       => now(),
                        'updated_at'       => now(),
                    ]);
                    $stats['profile_updated'] = true;
                } elseif ($mobileUpdatedAt > ($existing->mobile_updated_at ?? 0)) {
                    DB::table('user_profile')->where('uid', $uid)->update([
                        'name'             => $profileData['name'] ?? $existing->name,
                        'email'            => $profileData['email'] ?? $existing->email,
                        'age'              => $profileData['age'] ?? $existing->age,
                        'weight'           => $profileData['weight'] ?? $existing->weight,
                        'height'           => $profileData['height'] ?? $existing->height,
                        'sex'              => $profileData['sex'] ?? $existing->sex,
                        'activityLevel'    => $profileData['activityLevel'] ?? $existing->activityLevel,
                        'goal'             => $profileData['goal'] ?? $existing->goal,
                        'streak'           => $profileData['streak'] ?? $existing->streak,
                        'level'            => $profileData['level'] ?? $existing->level,
                        'mobile_updated_at'=> $mobileUpdatedAt,
                        'updated_at'       => now(),
                    ]);
                    $stats['profile_updated'] = true;
                } else {
                    $conflicts[] = [
                        'entity_type' => 'profile',
                        'entity_id'   => $uid,
                        'reason'      => 'Server record is newer (admin override)',
                    ];
                }
            }

            // ══════════════════════════════════════════════════
            // 3. MEAL LOGS — meal_log_table (PK: meal_log_id)
            //    Items   — meal_log_item_table (PK: meal_log_item_id)
            // ══════════════════════════════════════════════════
            $meals = $request->input('meals', []);
            foreach ($meals ?? [] as $meal) {
                $mobileUpdatedAt = $meal['updated_at'] ?? 0;

                $existing = DB::table('meal_log_table')
                    ->where('uid', $uid)
                    ->where('timestamp', $meal['timestamp'])
                    ->where('meal_type', $meal['meal_type'])
                    ->first();

                $mealLogId = null;

                if (!$existing) {
                    $mealLogId = DB::table('meal_log_table')->insertGetId([
                        'uid'               => $uid,
                        'meal_type'         => $meal['meal_type'],
                        'timestamp'         => $meal['timestamp'],
                        'notes'             => $meal['notes'] ?? null,
                        'mobile_updated_at' => $mobileUpdatedAt,
                        'created_at'        => now(),
                        'updated_at'        => now(),
                    ], 'meal_log_id');
                    $stats['meals_inserted']++;
                } elseif ($mobileUpdatedAt > ($existing->mobile_updated_at ?? 0)) {
                    $mealLogId = $existing->meal_log_id;
                    DB::table('meal_log_table')->where('meal_log_id', $mealLogId)->update([
                        'notes'             => $meal['notes'] ?? $existing->notes,
                        'mobile_updated_at' => $mobileUpdatedAt,
                        'updated_at'        => now(),
                    ]);
                    $stats['meals_updated']++;
                } else {
                    $conflicts[] = [
                        'entity_type' => 'meal_log',
                        'entity_id'   => (string) ($existing->meal_log_id ?? $meal['timestamp']),
                        'reason'      => 'Server record is newer (admin override)',
                    ];
                    continue;
                }

                // Process child meal items
                if ($mealLogId && isset($meal['items'])) {
                    foreach ($meal['items'] as $item) {
                        $itemMobileUpdatedAt = $item['updated_at'] ?? 0;

                        $existingItem = DB::table('meal_log_item_table')
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
                            DB::table('meal_log_item_table')->insert($itemData);
                        } elseif ($itemMobileUpdatedAt > ($existingItem->mobile_updated_at ?? 0)) {
                            $itemData['updated_at'] = now();
                            DB::table('meal_log_item_table')
                                ->where('meal_log_item_id', $existingItem->meal_log_item_id)
                                ->update($itemData);
                        }
                        $stats['meal_items_processed']++;
                    }
                }
            }

            // ══════════════════════════════════════════════════
            // 4. ACTIVITY LOGS — activity_log_table (PK: id)
            //    Columns use camelCase matching Room entity:
            //    timeString, weightOrDuration, distanceKm, etc.
            // ══════════════════════════════════════════════════
            $activities = $request->input('activities', []);
            foreach ($activities ?? [] as $activity) {
                $mobileUpdatedAt = $activity['updated_at'] ?? 0;

                $existing = DB::table('activity_log_table')
                    ->where('uid', $uid)
                    ->where('timestamp', $activity['timestamp'])
                    ->where('name', $activity['name'])
                    ->first();

                $activityData = [
                    'uid'              => $uid,
                    'type'             => $activity['type'],
                    'name'             => $activity['name'],
                    'timeString'       => $activity['timeString'] ?? '',
                    'weightOrDuration' => $activity['weightOrDuration'] ?? '',
                    'calories'         => $activity['calories'] ?? 0,
                    'protein'          => $activity['protein'] ?? 0,
                    'carbs'            => $activity['carbs'] ?? 0,
                    'fats'             => $activity['fats'] ?? 0,
                    'sodium'           => $activity['sodium'] ?? 0,
                    'timestamp'        => $activity['timestamp'],
                    'distanceKm'       => $activity['distanceKm'] ?? null,
                    'pace'             => $activity['pace'] ?? null,
                    'movingTimeSeconds'=> $activity['movingTimeSeconds'] ?? null,
                    'mapType'          => $activity['mapType'] ?? null,
                    'notes'            => $activity['notes'] ?? null,
                    'activityTag'      => $activity['activityTag'] ?? null,
                    'mobile_updated_at'=> $mobileUpdatedAt,
                ];

                if (!$existing) {
                    $activityData['created_at'] = now();
                    $activityData['updated_at'] = now();
                    DB::table('activity_log_table')->insert($activityData);
                    $stats['activities_inserted']++;
                } elseif ($mobileUpdatedAt > ($existing->mobile_updated_at ?? 0)) {
                    $activityData['updated_at'] = now();
                    DB::table('activity_log_table')->where('id', $existing->id)->update($activityData);
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
            // 5. NUTRITION SUMMARIES — daily_nutrition_summary_table (PK: id)
            // ══════════════════════════════════════════════════
            $summaries = $request->input('nutrition_summaries', []);
            foreach ($summaries ?? [] as $summary) {
                $mobileUpdatedAt = $summary['updated_at'] ?? 0;

                $existing = DB::table('daily_nutrition_summary_table')
                    ->where('uid', $uid)
                    ->where('date_epoch_day', $summary['date_epoch_day'])
                    ->first();

                $summaryData = [
                    'uid'                       => $uid,
                    'date_epoch_day'            => $summary['date_epoch_day'],
                    'total_calories'            => $summary['total_calories'] ?? 0,
                    'total_protein'             => $summary['total_protein'] ?? 0,
                    'total_carbs'               => $summary['total_carbs'] ?? 0,
                    'total_fiber'               => $summary['total_fiber'] ?? 0,
                    'total_sugar'               => $summary['total_sugar'] ?? 0,
                    'total_fat'                 => $summary['total_fat'] ?? 0,
                    'total_saturated_fat'       => $summary['total_saturated_fat'] ?? 0,
                    'total_polyunsaturated_fat' => $summary['total_polyunsaturated_fat'] ?? 0,
                    'total_monounsaturated_fat' => $summary['total_monounsaturated_fat'] ?? 0,
                    'total_trans_fat'           => $summary['total_trans_fat'] ?? 0,
                    'total_cholesterol'         => $summary['total_cholesterol'] ?? 0,
                    'total_sodium'              => $summary['total_sodium'] ?? 0,
                    'total_potassium'           => $summary['total_potassium'] ?? 0,
                    'total_vitamin_a'           => $summary['total_vitamin_a'] ?? 0,
                    'total_vitamin_c'           => $summary['total_vitamin_c'] ?? 0,
                    'total_calcium'             => $summary['total_calcium'] ?? 0,
                    'total_iron'                => $summary['total_iron'] ?? 0,
                    'breakfast_calories'        => $summary['breakfast_calories'] ?? 0,
                    'lunch_calories'            => $summary['lunch_calories'] ?? 0,
                    'dinner_calories'           => $summary['dinner_calories'] ?? 0,
                    'snacks_calories'           => $summary['snacks_calories'] ?? 0,
                    'mobile_updated_at'         => $mobileUpdatedAt,
                ];

                if (!$existing) {
                    $summaryData['created_at'] = now();
                    $summaryData['updated_at'] = now();
                    DB::table('daily_nutrition_summary_table')->insert($summaryData);
                    $stats['summaries_inserted']++;
                } elseif ($mobileUpdatedAt > ($existing->mobile_updated_at ?? 0)) {
                    $summaryData['updated_at'] = now();
                    DB::table('daily_nutrition_summary_table')->where('id', $existing->id)->update($summaryData);
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
                'error' => $e->getMessage(),
                'file'  => $e->getFile(),
                'line'  => $e->getLine(),
            ]);

            return response()->json([
                'success' => false,
                'message' => 'Sync failed due to server error',
                'error'   => $e->getMessage(),
            ], 500);
        }
    }
}
