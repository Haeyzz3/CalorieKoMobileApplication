<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class SystemLog extends Model
{
    use HasFactory;

    protected $fillable = [
        'admin_email',
        'action',
        'target_resource',
        'status',
        'ip_address',
        'details',
    ];

    /**
     * Helper to quickly insert a system log.
     */
    public static function log($adminEmail, $action, $targetResource = null, $status = 'Success', $ipAddress = null, $details = null)
    {
        return self::create([
            'admin_email' => $adminEmail,
            'action' => $action,
            'target_resource' => $targetResource,
            'status' => $status,
            'ip_address' => $ipAddress,
            'details' => $details,
        ]);
    }
}
