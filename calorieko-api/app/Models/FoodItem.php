<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class FoodItem extends Model
{
    protected $table = 'FOOD_TABLE';
    protected $primaryKey = 'food_id';

    protected $fillable = [
        'name_en',
        'name_ph',
        'category',
        'calories_per_100g',
        'protein_per_100g',
        'carbs_per_100g',
        'fiber_per_100g',
        'sugar_per_100g',
        'fat_per_100g',
        'saturated_fat_per_100g',
        'polyunsaturated_fat_per_100g',
        'monounsaturated_fat_per_100g',
        'trans_fat_per_100g',
        'cholesterol_per_100g',
        'sodium_per_100g',
        'potassium_per_100g',
        'vitamin_a_per_100g',
        'vitamin_c_per_100g',
        'calcium_per_100g',
        'iron_per_100g',
    ];

    protected $casts = [
        'calories_per_100g'           => 'float',
        'protein_per_100g'            => 'float',
        'carbs_per_100g'              => 'float',
        'fiber_per_100g'              => 'float',
        'sugar_per_100g'              => 'float',
        'fat_per_100g'                => 'float',
        'saturated_fat_per_100g'      => 'float',
        'polyunsaturated_fat_per_100g'=> 'float',
        'monounsaturated_fat_per_100g'=> 'float',
        'trans_fat_per_100g'          => 'float',
        'cholesterol_per_100g'        => 'float',
        'sodium_per_100g'             => 'float',
        'potassium_per_100g'          => 'float',
        'vitamin_a_per_100g'          => 'float',
        'vitamin_c_per_100g'          => 'float',
        'calcium_per_100g'            => 'float',
        'iron_per_100g'               => 'float',
    ];
}
