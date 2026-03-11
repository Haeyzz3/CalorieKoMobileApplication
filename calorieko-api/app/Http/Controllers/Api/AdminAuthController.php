<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use App\Models\SystemLog;

/**
 * Handles admin panel authentication.
 *
 * Credentials are stored in the .env file (ADMIN_EMAIL / ADMIN_PASSWORD).
 * On success, returns a simple token that the Vue admin stores in sessionStorage.
 */
class AdminAuthController extends Controller
{
    /**
     * POST /api/admin/login
     *
     * Validates the email/password against the .env credentials.
     * Returns a signed token on success.
     */
    public function login(Request $request)
    {
        $request->validate([
            'email'    => 'required|email',
            'password' => 'required|string',
        ]);

        $adminEmail    = config('app.admin_email');
        $adminPassword = config('app.admin_password');

        // Check if admin credentials are configured
        if (empty($adminEmail) || empty($adminPassword)) {
            return response()->json([
                'error' => 'Admin credentials not configured on the server.'
            ], 500);
        }

        // Validate credentials
        if (
            strtolower($request->email) === strtolower($adminEmail) &&
            $request->password === $adminPassword
        ) {
            // Generate a simple signed token (valid for this server session)
            $token = hash('sha256', $adminEmail . '|' . config('app.key') . '|' . now()->toDateString());

            SystemLog::log($adminEmail, 'Admin Login', null, 'Success', $request->ip(), 'Admin logged in successfully.');

            return response()->json([
                'message' => 'Login successful',
                'token'   => $token,
                'email'   => $adminEmail,
            ]);
        }

        SystemLog::log($request->email, 'Admin Login', null, 'Failed', $request->ip(), 'Invalid email or password.');

        return response()->json([
            'error' => 'Invalid email or password.'
        ], 401);
    }

    /**
     * POST /api/admin/verify
     *
     * Verifies if the stored token is still valid.
     */
    public function verify(Request $request)
    {
        $token = $request->bearerToken() ?? $request->input('token');

        if (empty($token)) {
            return response()->json(['valid' => false], 401);
        }

        $adminEmail = config('app.admin_email');
        $expectedToken = hash('sha256', $adminEmail . '|' . config('app.key') . '|' . now()->toDateString());

        if ($token === $expectedToken) {
            return response()->json(['valid' => true, 'email' => $adminEmail]);
        }

        return response()->json(['valid' => false], 401);
    }
}
