<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class UserProfile extends Model
{
    protected $table = 'user_profile';
    protected $primaryKey = 'uid';
    public $incrementing = false;        // uid is a string, not auto-increment
    protected $keyType = 'string';

    protected $fillable = [
        'uid',
        'name',
        'email',
        'age',
        'weight',
        'height',
        'sex',
        'activityLevel',
        'goal',
        'streak',
        'level',
        'is_active',
    ];

    protected $casts = [
        'age'       => 'integer',
        'weight'    => 'double',
        'height'    => 'double',
        'streak'    => 'integer',
        'level'     => 'integer',
        'is_active' => 'boolean',
    ];
}
