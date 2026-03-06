<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\ActivityLog;
use Illuminate\Http\Request;
use Illuminate\Http\JsonResponse;

class ActivityLogController extends Controller
{
    /**
     * Sync: Store a new activity log entry from the mobile app.
     */
    public function sync(Request $request): JsonResponse
    {
        $data = $request->validate([
            'uid'              => 'required|string',
            'type'             => 'required|string|in:meal,workout',
            'name'             => 'required|string',
            'timeString'       => 'required|string',
            'weightOrDuration' => 'required|string',
            'calories'         => 'required|integer',
            'protein'          => 'nullable|integer',
            'carbs'            => 'nullable|integer',
            'fats'             => 'nullable|integer',
            'sodium'           => 'nullable|integer',
            'timestamp'        => 'required|integer',
        ]);

        // Ensure the authenticated user matches the data uid
        if ($request->firebaseUid !== $data['uid']) {
            return response()->json(['error' => 'UID mismatch'], 403);
        }

        $log = ActivityLog::create($data);

        return response()->json($log, 201);
    }

    /**
     * Sync: Batch sync multiple activity log entries.
     */
    public function syncBatch(Request $request): JsonResponse
    {
        $request->validate([
            'entries'              => 'required|array',
            'entries.*.uid'        => 'required|string',
            'entries.*.type'       => 'required|string|in:meal,workout',
            'entries.*.name'       => 'required|string',
            'entries.*.timeString' => 'required|string',
            'entries.*.weightOrDuration' => 'required|string',
            'entries.*.calories'   => 'required|integer',
            'entries.*.timestamp'  => 'required|integer',
        ]);

        $entries = $request->input('entries');
        $created = [];

        foreach ($entries as $entry) {
            if ($request->firebaseUid !== $entry['uid']) {
                continue; // Skip entries that don't match the authenticated user
            }
            $created[] = ActivityLog::create($entry);
        }

        return response()->json($created, 201);
    }

    /**
     * Admin: List activity logs with optional filtering by uid and date.
     */
    public function index(Request $request): JsonResponse
    {
        $query = ActivityLog::query();

        if ($request->has('uid')) {
            $query->where('uid', $request->input('uid'));
        }

        if ($request->has('type')) {
            $query->where('type', $request->input('type'));
        }

        // Filter by date range (epoch millis)
        if ($request->has('from')) {
            $query->where('timestamp', '>=', $request->input('from'));
        }
        if ($request->has('to')) {
            $query->where('timestamp', '<=', $request->input('to'));
        }

        return response()->json($query->orderBy('timestamp', 'desc')->get());
    }

    /**
     * Admin: Show a single activity log entry.
     */
    public function show(int $id): JsonResponse
    {
        $log = ActivityLog::findOrFail($id);
        return response()->json($log);
    }
}
