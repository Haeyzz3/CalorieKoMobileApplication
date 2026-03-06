<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * Matches Room entity: UserProfile
 * Table name: user_profile
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('user_profile', function (Blueprint $table) {
            $table->string('uid')->primary();        // Firebase UID — PK, not auto-increment
            $table->string('name');
            $table->string('email');
            $table->integer('age');
            $table->double('weight');
            $table->double('height');
            $table->string('sex')->default('');
            $table->string('activityLevel')->default(''); // "not_very_active", "lightly_active", "active", "very_active"
            $table->string('goal');
            $table->integer('streak')->default(0);
            $table->integer('level')->default(1);
            $table->timestamps();                     // Laravel bonus: created_at, updated_at
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('user_profile');
    }
};
