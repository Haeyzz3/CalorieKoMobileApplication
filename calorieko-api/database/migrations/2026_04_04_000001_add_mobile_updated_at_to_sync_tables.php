<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * Adds `mobile_updated_at` column to all sync-enabled tables
 * for Last Write Wins conflict resolution.
 *
 * Table names match Room entity definitions:
 *   user_profile, meal_log_table, meal_log_item_table,
 *   activity_log_table, daily_nutrition_summary_table
 */
return new class extends Migration
{
    public function up(): void
    {
        $tables = [
            'user_profile',
            'meal_log_table',
            'meal_log_item_table',
            'activity_log_table',
            'daily_nutrition_summary_table',
        ];

        foreach ($tables as $tableName) {
            if (Schema::hasTable($tableName) && !Schema::hasColumn($tableName, 'mobile_updated_at')) {
                Schema::table($tableName, function (Blueprint $table) {
                    $table->bigInteger('mobile_updated_at')->nullable()->after('updated_at')
                        ->comment('Epoch millis from mobile client for LWW conflict resolution');
                });
            }
        }
    }

    public function down(): void
    {
        $tables = [
            'user_profile',
            'meal_log_table',
            'meal_log_item_table',
            'activity_log_table',
            'daily_nutrition_summary_table',
        ];

        foreach ($tables as $tableName) {
            if (Schema::hasTable($tableName) && Schema::hasColumn($tableName, 'mobile_updated_at')) {
                Schema::table($tableName, function (Blueprint $table) {
                    $table->dropColumn('mobile_updated_at');
                });
            }
        }
    }
};
