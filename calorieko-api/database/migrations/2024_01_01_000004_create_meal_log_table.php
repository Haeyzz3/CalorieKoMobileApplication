<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * Matches Room entity: MealLogEntity
 * Table name: meal_log_table
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('meal_log_table', function (Blueprint $table) {
            $table->bigIncrements('meal_log_id');            // PrimaryKey autoGenerate (Long)
            $table->string('uid');                            // Firebase UID
            $table->string('meal_type');                      // "Breakfast", "Lunch", "Dinner", "Snacks"
            $table->bigInteger('timestamp');                   // Epoch millis
            $table->string('notes')->nullable();              // Optional user notes
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('meal_log_table');
    }
};
