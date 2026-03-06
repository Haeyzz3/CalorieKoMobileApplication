<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\DailyNutritionSummary;
use Illuminate\Http\Request;
use Illuminate\Http\JsonResponse;

class DailyNutritionSummaryController extends Controller
{
    /**
     * Sync: Upsert a daily nutrition summary from the mobile app.
     * Uses (uid, date_epoch_day) as the unique key for upserting.
     */
    public function sync(Request $request): JsonResponse
    {
        $data = $request->validate([
            'uid'                         => 'required|string',
            'date_epoch_day'              => 'required|integer',
            'total_calories'              => 'nullable|numeric',
            'total_protein'               => 'nullable|numeric',
            'total_carbs'                 => 'nullable|numeric',
            'total_fiber'                 => 'nullable|numeric',
            'total_sugar'                 => 'nullable|numeric',
            'total_fat'                   => 'nullable|numeric',
            'total_saturated_fat'         => 'nullable|numeric',
            'total_polyunsaturated_fat'   => 'nullable|numeric',
            'total_monounsaturated_fat'   => 'nullable|numeric',
            'total_trans_fat'             => 'nullable|numeric',
            'total_cholesterol'           => 'nullable|numeric',
            'total_sodium'                => 'nullable|numeric',
            'total_potassium'             => 'nullable|numeric',
            'total_vitamin_a'             => 'nullable|numeric',
            'total_vitamin_c'             => 'nullable|numeric',
            'total_calcium'               => 'nullable|numeric',
            'total_iron'                  => 'nullable|numeric',
            'breakfast_calories'          => 'nullable|numeric',
            'lunch_calories'              => 'nullable|numeric',
            'dinner_calories'             => 'nullable|numeric',
            'snacks_calories'             => 'nullable|numeric',
        ]);

        // Ensure the authenticated user matches the data uid
        if ($request->firebaseUid !== $data['uid']) {
            return response()->json(['error' => 'UID mismatch'], 403);
        }

        $summary = DailyNutritionSummary::updateOrCreate(
            ['uid' => $data['uid'], 'date_epoch_day' => $data['date_epoch_day']],
            $data
        );

        return response()->json($summary, 200);
    }

    /**
     * Admin: List daily nutrition summaries with optional filtering.
     */
    public function index(Request $request): JsonResponse
    {
        $query = DailyNutritionSummary::query();

        if ($request->has('uid')) {
            $query->where('uid', $request->input('uid'));
        }

        // Filter by date range (epoch days)
        if ($request->has('from')) {
            $query->where('date_epoch_day', '>=', $request->input('from'));
        }
        if ($request->has('to')) {
            $query->where('date_epoch_day', '<=', $request->input('to'));
        }

        return response()->json(
            $query->orderBy('date_epoch_day', 'desc')->get()
        );
    }

    /**
     * Admin: Show a single daily nutrition summary.
     */
    public function show(int $id): JsonResponse
    {
        $summary = DailyNutritionSummary::findOrFail($id);
        return response()->json($summary);
    }
}
