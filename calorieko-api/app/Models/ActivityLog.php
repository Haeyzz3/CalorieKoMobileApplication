<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class ActivityLog extends Model
{
    protected $table = 'activity_log_table';
    protected $primaryKey = 'id';

    protected $fillable = [
        'uid',
        'type',
        'name',
        'timeString',
        'weightOrDuration',
        'calories',
        'protein',
        'carbs',
        'fats',
        'sodium',
        'timestamp',
    ];

    protected $casts = [
        'calories'  => 'integer',
        'protein'   => 'integer',
        'carbs'     => 'integer',
        'fats'      => 'integer',
        'sodium'    => 'integer',
        'timestamp' => 'integer',
    ];
}
