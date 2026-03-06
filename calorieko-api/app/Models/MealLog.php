<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class MealLog extends Model
{
    protected $table = 'meal_log_table';
    protected $primaryKey = 'meal_log_id';

    protected $fillable = [
        'uid',
        'meal_type',
        'timestamp',
        'notes',
    ];

    protected $casts = [
        'timestamp' => 'integer',
    ];

    /**
     * A meal log has many meal log items.
     */
    public function items(): HasMany
    {
        return $this->hasMany(MealLogItem::class, 'meal_log_id', 'meal_log_id');
    }
}
