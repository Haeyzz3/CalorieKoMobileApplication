<?php

/**
 * Hashing Configuration — CalorieKo API
 *
 * Overrides Laravel's default bcrypt driver with argon2id to comply with
 * the established security objectives. Argon2id is the recommended
 * password hashing algorithm per OWASP and is resistant to both
 * side-channel and GPU-based attacks.
 *
 * These settings apply to all Hash::make() calls, including the default
 * User model password hashing, admin portal credentials, and any future
 * credential storage.
 */

return [

    /*
    |--------------------------------------------------------------------------
    | Default Hash Driver
    |--------------------------------------------------------------------------
    |
    | Supported: "bcrypt", "argon", "argon2id"
    |
    | Overridden from 'bcrypt' to 'argon2id' per security compliance.
    |
    */

    'driver' => env('HASH_DRIVER', 'argon2id'),

    /*
    |--------------------------------------------------------------------------
    | Bcrypt Options (retained for reference / fallback)
    |--------------------------------------------------------------------------
    */

    'bcrypt' => [
        'rounds' => env('BCRYPT_ROUNDS', 12),
        'verify'  => env('HASH_VERIFY', true),
    ],

    /*
    |--------------------------------------------------------------------------
    | Argon2id Options
    |--------------------------------------------------------------------------
    |
    | memory  — peak memory cost in KiB (default 65536 = 64 MB)
    | time    — number of iterations (default 4)
    | threads — degree of parallelism (default 1)
    |
    | These values follow OWASP 2024 recommendations for password storage.
    |
    */

    'argon' => [
        'memory'  => env('ARGON_MEMORY', 65536),
        'threads' => env('ARGON_THREADS', 1),
        'time'    => env('ARGON_TIME', 4),
        'verify'  => env('HASH_VERIFY', true),
    ],

];
