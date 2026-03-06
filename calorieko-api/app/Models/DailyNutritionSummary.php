<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class DailyNutritionSummary extends Model
{
    protected $table = 'daily_nutrition_summary_table';
    protected $primaryKey = 'id';

    protected $fillable = [
        'uid',
        'date_epoch_day',
        'total_calories',
        'total_protein',
        'total_carbs',
        'total_fiber',
        'total_sugar',
        'total_fat',
        'total_saturated_fat',
        'total_polyunsaturated_fat',
        'total_monounsaturated_fat',
        'total_trans_fat',
        'total_cholesterol',
        'total_sodium',
        'total_potassium',
        'total_vitamin_a',
        'total_vitamin_c',
        'total_calcium',
        'total_iron',
        'breakfast_calories',
        'lunch_calories',
        'dinner_calories',
        'snacks_calories',
    ];

    protected $casts = [
        'date_epoch_day'              => 'integer',
        'total_calories'              => 'float',
        'total_protein'               => 'float',
        'total_carbs'                 => 'float',
        'total_fiber'                 => 'float',
        'total_sugar'                 => 'float',
        'total_fat'                   => 'float',
        'total_saturated_fat'         => 'float',
        'total_polyunsaturated_fat'   => 'float',
        'total_monounsaturated_fat'   => 'float',
        'total_trans_fat'             => 'float',
        'total_cholesterol'           => 'float',
        'total_sodium'                => 'float',
        'total_potassium'             => 'float',
        'total_vitamin_a'             => 'float',
        'total_vitamin_c'             => 'float',
        'total_calcium'               => 'float',
        'total_iron'                  => 'float',
        'breakfast_calories'          => 'float',
        'lunch_calories'              => 'float',
        'dinner_calories'             => 'float',
        'snacks_calories'             => 'float',
    ];
}
