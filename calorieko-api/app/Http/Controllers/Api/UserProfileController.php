<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\UserProfile;
use App\Models\SystemLog;
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

        // Sync status to Firebase Authentication
        try {
            if ($profile->is_active) {
                app('firebase.auth')->enableUser($uid);
            } else {
                app('firebase.auth')->disableUser($uid);
            }
        } catch (\Exception $e) {
            \Log::error("Failed to sync Firebase Auth status for {$uid}: " . $e->getMessage());
        }

        $adminEmail = config('app.admin_email') ?? 'admin@calorieko.com';
        $statusStr = $profile->is_active ? 'Reactivated' : 'Deactivated';
        SystemLog::log($adminEmail, "User {$statusStr}", "User UID: {$uid}", 'Success', request()->ip(), "Admin {$statusStr} user {$profile->email}.");

        return response()->json([
            'message' => 'User status updated successfully.',
            'is_active' => $profile->is_active
        ]);
    }

    /**
     * Admin: Trigger password reset.
     */
    public function resetPassword(string $uid): JsonResponse
    {
        $profile = UserProfile::findOrFail($uid);
        
        try {
            // Using Firebase Identity Toolkit REST API directly to bypass OAuth signature issues
            $apiKey = config('services.firebase.web_api_key', 'AIzaSyCCHAzAg5VBKJR1SK3aCG2n-rpFhhEBTRc');
            $response = \Illuminate\Support\Facades\Http::post("https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key={$apiKey}", [
                'requestType' => 'PASSWORD_RESET',
                'email' => $profile->email
            ]);

            if (!$response->successful()) {
                throw new \Exception($response->json('error.message', 'Unknown Firebase API Error'));
            }

            $adminEmail = config('app.admin_email') ?? 'admin@calorieko.com';
            SystemLog::log($adminEmail, 'Password Reset', "User UID: {$uid}", 'Success', request()->ip(), "Admin sent password reset link to {$profile->email}.");
            
            \Log::info("Password reset sent for UID: {$uid}");
            return response()->json([
                'message' => "Password reset sequence triggered for {$profile->email}."
            ]);
        } catch (\Exception $e) {
            \Log::error("Failed to send password reset via REST: " . $e->getMessage());
            return response()->json(['error' => 'Failed to send password reset link: ' . $e->getMessage()], 500);
        }
    }

    /**
     * Admin: Delete user from registry.
     */
    public function destroy(string $uid): JsonResponse
    {
        $profile = UserProfile::findOrFail($uid);
        $email = $profile->email;
        $profile->delete();

        // Delete from Firebase DB as well
        try {
            app('firebase.auth')->deleteUser($uid);
        } catch (\Exception $e) {
            \Log::error("Failed to delete user from Firebase Auth {$uid}: " . $e->getMessage());
        }

        $adminEmail = config('app.admin_email') ?? 'admin@calorieko.com';
        SystemLog::log($adminEmail, 'Deleted User', "User UID: {$uid}", 'Success', request()->ip(), "Admin deleted user {$email}.");

        \Log::info("User profile deleted for UID: {$uid}");
        
        return response()->json([
            'message' => 'User successfully deleted.'
        ]);
    }
}
