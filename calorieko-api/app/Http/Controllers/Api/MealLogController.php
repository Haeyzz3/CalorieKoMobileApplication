<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\MealLog;
use App\Models\MealLogItem;
use Illuminate\Http\Request;
use Illuminate\Http\JsonResponse;

class MealLogController extends Controller
{
    /**
     * Sync: Store a meal log with its items from the mobile app.
     * Expects a JSON body with meal log data and an "items" array.
     */
    public function sync(Request $request): JsonResponse
    {
        $data = $request->validate([
            'uid'       => 'required|string',
            'meal_type' => 'required|string|in:Breakfast,Lunch,Dinner,Snacks',
            'timestamp' => 'required|integer',
            'notes'     => 'nullable|string',
            'items'     => 'required|array|min:1',
            'items.*.food_id'      => 'required|integer',
            'items.*.dish_name'    => 'required|string',
            'items.*.weight_grams' => 'required|numeric',
            'items.*.calories'     => 'nullable|numeric',
            'items.*.protein'      => 'nullable|numeric',
            'items.*.carbs'        => 'nullable|numeric',
            'items.*.fiber'        => 'nullable|numeric',
            'items.*.sugar'        => 'nullable|numeric',
            'items.*.fat'          => 'nullable|numeric',
            'items.*.saturated_fat'        => 'nullable|numeric',
            'items.*.polyunsaturated_fat'  => 'nullable|numeric',
            'items.*.monounsaturated_fat'  => 'nullable|numeric',
            'items.*.trans_fat'            => 'nullable|numeric',
            'items.*.cholesterol'          => 'nullable|numeric',
            'items.*.sodium'               => 'nullable|numeric',
            'items.*.potassium'            => 'nullable|numeric',
            'items.*.vitamin_a'            => 'nullable|numeric',
            'items.*.vitamin_c'            => 'nullable|numeric',
            'items.*.calcium'              => 'nullable|numeric',
            'items.*.iron'                 => 'nullable|numeric',
        ]);

        // Ensure the authenticated user matches the data uid
        if ($request->firebaseUid !== $data['uid']) {
            return response()->json(['error' => 'UID mismatch'], 403);
        }

        // Create the parent meal log
        $mealLog = MealLog::create([
            'uid'       => $data['uid'],
            'meal_type' => $data['meal_type'],
            'timestamp' => $data['timestamp'],
            'notes'     => $data['notes'] ?? null,
        ]);

        // Create each child item
        foreach ($data['items'] as $itemData) {
            $itemData['meal_log_id'] = $mealLog->meal_log_id;
            MealLogItem::create($itemData);
        }

        // Return the meal log with its items
        $mealLog->load('items');

        return response()->json($mealLog, 201);
    }

    /**
     * Admin: List all meal logs with optional filtering.
     */
    public function index(Request $request): JsonResponse
    {
        $query = MealLog::with('items');

        if ($request->has('uid')) {
            $query->where('uid', $request->input('uid'));
        }

        if ($request->has('meal_type')) {
            $query->where('meal_type', $request->input('meal_type'));
        }

        // Filter by date range (epoch millis)
        if ($request->has('from')) {
            $query->where('timestamp', '>=', $request->input('from'));
        }
        if ($request->has('to')) {
            $query->where('timestamp', '<=', $request->input('to'));
        }

        return response()->json(
            $query->orderBy('timestamp', 'desc')->get()
        );
    }

    /**
     * Admin: Show a single meal log with its items.
     */
    public function show(int $id): JsonResponse
    {
        $mealLog = MealLog::with('items')->findOrFail($id);
        return response()->json($mealLog);
    }
}
