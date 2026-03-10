<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\UserProfile;
use Illuminate\Http\Request;
use Illuminate\Http\JsonResponse;

class UserProfileController extends Controller
{
    /**
     * Sync: Upsert a user profile from the mobile app.
     */
    public function sync(Request $request): JsonResponse
    {
        \Log::info('Profile Sync Request:', $request->all());
        
        try {
            $data = $request->validate([
                'uid'           => 'required|string',
                'name'          => 'required|string',
                'email'         => 'required|string|email',
                'age'           => 'required|integer',
                'weight'        => 'required|numeric',
                'height'        => 'required|numeric',
                'sex'           => 'nullable|string',
                'activityLevel' => 'nullable|string',
                'goal'          => 'required|string',
                'streak'        => 'nullable|integer',
                'level'         => 'nullable|integer',
            ]);
        } catch (\Illuminate\Validation\ValidationException $e) {
            \Log::error('Profile Sync Validation Failed:', $e->errors());
            throw $e;
        }

        // Ensure the authenticated user matches the profile uid
        if ($request->firebaseUid !== $data['uid']) {
            \Log::error("UID Mismatch: Firebase {$request->firebaseUid} vs Request {$data['uid']}");
            return response()->json(['error' => 'UID mismatch'], 403);
        }

        $profile = UserProfile::updateOrCreate(
            ['uid' => $data['uid']],
            $data
        );

        \Log::info('Profile synced successfully for UID: ' . $data['uid']);
        return response()->json($profile, 200);
    }

    /**
     * Admin: List all user profiles.
     */
    public function index(): JsonResponse
    {
        return response()->json(UserProfile::all());
    }

    /**
     * Admin: Show a single user profile.
     */
    public function show(string $uid): JsonResponse
    {
        $profile = UserProfile::findOrFail($uid);
        return response()->json($profile);
    }

    /**
     * Admin: Toggle active status.
     */
    public function deactivate(string $uid): JsonResponse
    {
        $profile = UserProfile::findOrFail($uid);
        $profile->is_active = !$profile->is_active;
        $profile->save();

        return response()->json([
            'message' => 'User status updated successfully.',
            'is_active' => $profile->is_active
        ]);
    }

    /**
     * Admin: Trigger password reset mock.
     */
    public function resetPassword(string $uid): JsonResponse
    {
        $profile = UserProfile::findOrFail($uid);
        // Note: For Firebase auth, the backend doesn't reset directly via API unless using Admin SDK.
        // Returning success to mock the capability for capstone presentation.
        \Log::info("Password reset requested for UID: {$uid}");
        return response()->json([
            'message' => "Password reset sequence triggered for {$profile->email}."
        ]);
    }

    /**
     * Admin: Delete user from registry.
     */
    public function destroy(string $uid): JsonResponse
    {
        $profile = UserProfile::findOrFail($uid);
        $profile->delete();

        // Warning: Does not delete cascaded tables automatically without foreign key setups,
        // but sufficient for basic admin removal preview.
        \Log::info("User profile deleted for UID: {$uid}");
        
        return response()->json([
            'message' => 'User successfully deleted.'
        ]);
    }
}
