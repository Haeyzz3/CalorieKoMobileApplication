<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * Matches Room entity: ActivityLogEntity
 * Table name: activity_log_table
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('activity_log_table', function (Blueprint $table) {
            $table->increments('id');                       // PrimaryKey autoGenerate
            $table->string('uid');                           // Firebase UID
            $table->string('type');                          // "meal" or "workout"
            $table->string('name');                          // e.g., "Chicken Adobo"
            $table->string('timeString');                    // e.g., "8:30 AM"
            $table->string('weightOrDuration');              // e.g., "250g" or "30 min"

            // Nutrition / Burn Data
            $table->integer('calories');
            $table->integer('protein')->default(0);
            $table->integer('carbs')->default(0);
            $table->integer('fats')->default(0);
            $table->integer('sodium')->default(0);

            $table->bigInteger('timestamp');                 // Epoch millis — filter by "today"
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('activity_log_table');
    }
};
