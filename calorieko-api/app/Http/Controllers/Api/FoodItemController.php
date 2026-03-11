<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\FoodItem;
use Illuminate\Http\Request;
use Illuminate\Http\JsonResponse;

class FoodItemController extends Controller
{
    /**
     * Sync: Upsert a food item from the mobile app.
     */
    public function sync(Request $request): JsonResponse
    {
        $data = $request->validate([
            'food_id'                     => 'nullable|integer',
            'name_en'                     => 'required|string',
            'name_ph'                     => 'required|string',
            'category'                    => 'required|string',
            'calories_per_100g'           => 'nullable|numeric',
            'protein_per_100g'            => 'nullable|numeric',
            'carbs_per_100g'              => 'nullable|numeric',
            'fiber_per_100g'              => 'nullable|numeric',
            'sugar_per_100g'              => 'nullable|numeric',
            'fat_per_100g'                => 'nullable|numeric',
            'saturated_fat_per_100g'      => 'nullable|numeric',
            'polyunsaturated_fat_per_100g'=> 'nullable|numeric',
            'monounsaturated_fat_per_100g'=> 'nullable|numeric',
            'trans_fat_per_100g'          => 'nullable|numeric',
            'cholesterol_per_100g'        => 'nullable|numeric',
            'sodium_per_100g'             => 'nullable|numeric',
            'potassium_per_100g'          => 'nullable|numeric',
            'vitamin_a_per_100g'          => 'nullable|numeric',
            'vitamin_c_per_100g'          => 'nullable|numeric',
            'calcium_per_100g'            => 'nullable|numeric',
            'iron_per_100g'               => 'nullable|numeric',
        ]);

        $food = FoodItem::updateOrCreate(
            ['name_en' => $data['name_en'], 'name_ph' => $data['name_ph']],
            $data
        );

        return response()->json($food, 200);
    }

    /**
     * Admin: List all food items with optional search.
     */
    public function index(Request $request): JsonResponse
    {
        $query = FoodItem::query();

        if ($request->has('search')) {
            $search = $request->input('search');
            $query->where('name_en', 'like', "%{$search}%")
                  ->orWhere('name_ph', 'like', "%{$search}%");
        }

        if ($request->has('category')) {
            $query->where('category', $request->input('category'));
        }

        return response()->json($query->get());
    }

    /**
     * Admin: Show a single food item.
     */
    public function show(int $id): JsonResponse
    {
        $food = FoodItem::findOrFail($id);
        return response()->json($food);
    }

    /**
     * Admin: Create a new food item.
     */
    public function store(Request $request): JsonResponse
    {
        $data = $request->validate([
            'name_en'                     => 'required|string',
            'name_ph'                     => 'required|string',
            'category'                    => 'required|string',
            'calories_per_100g'           => 'nullable|numeric',
            'protein_per_100g'            => 'nullable|numeric',
            'carbs_per_100g'              => 'nullable|numeric',
            'fat_per_100g'                => 'nullable|numeric',
        ]);

        $food = FoodItem::create($data);

        $adminEmail = config('app.admin_email') ?? 'admin@calorieko.com';
        \App\Models\SystemLog::log($adminEmail, "Added Food Item", "Food ID: {$food->food_id}", 'Success', request()->ip(), "Admin added food: {$food->name_en}");

        return response()->json($food, 201);
    }

    /**
     * Admin: Update an existing food item.
     */
    public function update(Request $request, int $id): JsonResponse
    {
        $food = FoodItem::findOrFail($id);
        
        $data = $request->validate([
            'name_en'                     => 'required|string',
            'name_ph'                     => 'required|string',
            'category'                    => 'required|string',
            'calories_per_100g'           => 'nullable|numeric',
            'protein_per_100g'            => 'nullable|numeric',
            'carbs_per_100g'              => 'nullable|numeric',
            'fat_per_100g'                => 'nullable|numeric',
        ]);

        $food->update($data);

        $adminEmail = config('app.admin_email') ?? 'admin@calorieko.com';
        \App\Models\SystemLog::log($adminEmail, "Updated Food Item", "Food ID: {$food->food_id}", 'Success', request()->ip(), "Admin updated food: {$food->name_en}");

        return response()->json($food);
    }

    /**
     * Admin: Delete a food item.
     */
    public function destroy(int $id): JsonResponse
    {
        $food = FoodItem::findOrFail($id);
        $name = $food->name_en;
        $food->delete();

        $adminEmail = config('app.admin_email') ?? 'admin@calorieko.com';
        \App\Models\SystemLog::log($adminEmail, "Deleted Food Item", "Food ID: {$id}", 'Success', request()->ip(), "Admin deleted food: {$name}");

        return response()->json(['message' => 'Food item successfully deleted.']);
    }
}
