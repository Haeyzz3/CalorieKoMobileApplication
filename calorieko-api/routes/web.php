<?php

use Illuminate\Support\Facades\Route;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Http;

Route::get('/', function () {
    return view('welcome');
});

// Custom Firebase Auth Action Handler
Route::match(['get', 'post'], '/__/auth/action', function (Request $request) {
    $mode = $request->query('mode');
    $oobCode = $request->query('oobCode');
    $apiKey = config('services.firebase.web_api_key', 'AIzaSyCCHAzAg5VBKJR1SK3aCG2n-rpFhhEBTRc');

    if (!$mode || !$oobCode) {
        return response('Invalid or missing authentication codes.', 400);
    }

    if ($mode === 'resetPassword') {
        if ($request->isMethod('post')) {
            $request->validate([
                'password' => 'required|min:6'
            ]);

            // Submit the new password to Firebase REST API
            $response = Http::post("https://identitytoolkit.googleapis.com/v1/accounts:resetPassword?key={$apiKey}", [
                'oobCode' => $oobCode,
                'newPassword' => $request->password
            ]);

            if ($response->successful()) {
                return back()->with('success', 'Your password has been successfully reset! You can now log into the mobile app.');
            }

            $errorMessage = $response->json('error.message', 'Failed to securely reset password. The link may have expired.');
            return back()->withErrors(['firebase' => $errorMessage]);
        }

        // Display the stunning custom reset form on GET request
        return view('auth.reset-password', ['oobCode' => $oobCode]);
    }

    return response('Unsupported authentication mode.', 400);
});
