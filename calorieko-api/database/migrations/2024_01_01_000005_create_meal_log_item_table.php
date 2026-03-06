<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * Matches Room entity: MealLogItemEntity
 * Table name: meal_log_item_table
 *
 * Foreign-keyed to meal_log_table with CASCADE delete.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('meal_log_item_table', function (Blueprint $table) {
            $table->bigIncrements('meal_log_item_id');       // PrimaryKey autoGenerate (Long)

            // FK → meal_log_table.meal_log_id  (CASCADE delete)
            $table->unsignedBigInteger('meal_log_id');
            $table->foreign('meal_log_id')
                  ->references('meal_log_id')
                  ->on('meal_log_table')
                  ->onDelete('cascade');

            $table->integer('food_id');                       // Reference to FOOD_TABLE.food_id
            $table->string('dish_name');                      // Snapshot of dish name at log time
            $table->float('weight_grams');                    // Actual weight from IoT scale

            // Core Energy
            $table->float('calories')->default(0);

            // Macros
            $table->float('protein')->default(0);
            $table->float('carbs')->default(0);
            $table->float('fiber')->default(0);
            $table->float('sugar')->default(0);
            $table->float('fat')->default(0);

            // Fat Details
            $table->float('saturated_fat')->default(0);
            $table->float('polyunsaturated_fat')->default(0);
            $table->float('monounsaturated_fat')->default(0);
            $table->float('trans_fat')->default(0);
            $table->float('cholesterol')->default(0);

            // Minerals & Vitamins
            $table->float('sodium')->default(0);
            $table->float('potassium')->default(0);
            $table->float('vitamin_a')->default(0);
            $table->float('vitamin_c')->default(0);
            $table->float('calcium')->default(0);
            $table->float('iron')->default(0);

            $table->timestamps();

            // Index on meal_log_id for fast lookups (matches Room @Index)
            $table->index('meal_log_id', 'idx_meal_log_item_meal_log_id');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('meal_log_item_table');
    }
};
