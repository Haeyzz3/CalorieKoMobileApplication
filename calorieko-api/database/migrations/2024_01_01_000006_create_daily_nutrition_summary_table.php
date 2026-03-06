<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * Matches Room entity: DailyNutritionSummaryEntity
 * Table name: daily_nutrition_summary_table
 *
 * Unique index on (uid, date_epoch_day) — one row per user per day.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('daily_nutrition_summary_table', function (Blueprint $table) {
            $table->bigIncrements('id');                             // PrimaryKey autoGenerate (Long)
            $table->string('uid');                                    // Firebase UID
            $table->bigInteger('date_epoch_day');                     // LocalDate.toEpochDay()

            // ── Totals (all meals combined) ──
            $table->float('total_calories')->default(0);
            $table->float('total_protein')->default(0);
            $table->float('total_carbs')->default(0);
            $table->float('total_fiber')->default(0);
            $table->float('total_sugar')->default(0);
            $table->float('total_fat')->default(0);
            $table->float('total_saturated_fat')->default(0);
            $table->float('total_polyunsaturated_fat')->default(0);
            $table->float('total_monounsaturated_fat')->default(0);
            $table->float('total_trans_fat')->default(0);
            $table->float('total_cholesterol')->default(0);
            $table->float('total_sodium')->default(0);
            $table->float('total_potassium')->default(0);
            $table->float('total_vitamin_a')->default(0);
            $table->float('total_vitamin_c')->default(0);
            $table->float('total_calcium')->default(0);
            $table->float('total_iron')->default(0);

            // ── Per-meal-type calorie breakdowns (for CaloriesTab donut chart) ──
            $table->float('breakfast_calories')->default(0);
            $table->float('lunch_calories')->default(0);
            $table->float('dinner_calories')->default(0);
            $table->float('snacks_calories')->default(0);

            $table->timestamps();

            // Unique index on (uid, date_epoch_day) — matches Room @Index(unique = true)
            $table->unique(['uid', 'date_epoch_day'], 'uq_uid_date_epoch_day');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('daily_nutrition_summary_table');
    }
};
