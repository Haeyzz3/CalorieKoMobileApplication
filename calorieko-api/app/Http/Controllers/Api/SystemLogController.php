<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\SystemLog;
use Illuminate\Http\Request;
use Illuminate\Http\JsonResponse;

class SystemLogController extends Controller
{
    /**
     * Admin: List all system logs.
     */
    public function index(): JsonResponse
    {
        return response()->json(SystemLog::orderBy('created_at', 'desc')->get());
    }
}
