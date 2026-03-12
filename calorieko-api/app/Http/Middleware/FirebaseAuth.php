<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Kreait\Laravel\Firebase\Facades\Firebase;
use Symfony\Component\HttpFoundation\Response;

/**
 * Authenticates incoming API requests using a Firebase ID token.
 *
 * The mobile app sends the token in the Authorization header:
 *   Authorization: Bearer <firebase_id_token>
 *
 * On success, the verified Firebase UID is stored on the request
 * so controllers can access it via $request->firebaseUid.
 */
class FirebaseAuth
{
    public function handle(Request $request, Closure $next): Response
    {
        $token = $request->bearerToken();

        if (!$token) {
            \Log::error('FirebaseAuth: No token provided');
            return response()->json(['error' => 'No token provided'], 401);
        }

        try {
            $auth = Firebase::auth();
            $verifiedIdToken = $auth->verifyIdToken($token);
            $uid = $verifiedIdToken->claims()->get('sub');

            // Check if user is locally deactivated
            $profile = \App\Models\UserProfile::find($uid);
            if ($profile && !$profile->is_active) {
                \Log::warning("FirebaseAuth: Blocked deactivated user UID: {$uid}");
                return response()->json([
                    'error' => 'Account deactivated',
                    'message' => 'Your account has been deactivated by the administrator.'
                ], 403);
            }

            // Merge the UID into the request so controllers can use it
            $request->merge(['firebaseUid' => $uid]);
            \Log::info("FirebaseAuth: Token verified for UID " . $uid);

            return $next($request);
        } catch (\Exception $e) {
            \Log::error('FirebaseAuth Exception: ' . $e->getMessage());
            return response()->json([
                'error'   => 'Invalid or expired token',
                'message' => $e->getMessage(),
            ], 401);
        }
    }
}
