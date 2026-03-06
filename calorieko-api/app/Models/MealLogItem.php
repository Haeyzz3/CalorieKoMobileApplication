<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class MealLogItem extends Model
{
    protected $table = 'meal_log_item_table';
    protected $primaryKey = 'meal_log_item_id';

    protected $fillable = [
        'meal_log_id',
        'food_id',
        'dish_name',
        'weight_grams',
        'calories',
        'protein',
        'carbs',
        'fiber',
        'sugar',
        'fat',
        'saturated_fat',
        'polyunsaturated_fat',
        'monounsaturated_fat',
        'trans_fat',
        'cholesterol',
        'sodium',
        'potassium',
        'vitamin_a',
        'vitamin_c',
        'calcium',
        'iron',
    ];

    protected $casts = [
        'meal_log_id'          => 'integer',
        'food_id'              => 'integer',
        'weight_grams'         => 'float',
        'calories'             => 'float',
        'protein'              => 'float',
        'carbs'                => 'float',
        'fiber'                => 'float',
        'sugar'                => 'float',
        'fat'                  => 'float',
        'saturated_fat'        => 'float',
        'polyunsaturated_fat'  => 'float',
        'monounsaturated_fat'  => 'float',
        'trans_fat'            => 'float',
        'cholesterol'          => 'float',
        'sodium'               => 'float',
        'potassium'            => 'float',
        'vitamin_a'            => 'float',
        'vitamin_c'            => 'float',
        'calcium'              => 'float',
        'iron'                 => 'float',
    ];

    /**
     * This item belongs to a meal log.
     */
    public function mealLog(): BelongsTo
    {
        return $this->belongsTo(MealLog::class, 'meal_log_id', 'meal_log_id');
    }
}
