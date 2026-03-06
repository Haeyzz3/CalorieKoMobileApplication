<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * Matches Room entity: FoodItem
 * Table name: FOOD_TABLE
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('FOOD_TABLE', function (Blueprint $table) {
            $table->increments('food_id');                  // PrimaryKey autoGenerate

            $table->string('name_en');
            $table->string('name_ph');
            $table->string('category');

            // Core Energy
            $table->float('calories_per_100g')->default(0);

            // Macros
            $table->float('protein_per_100g')->default(0);
            $table->float('carbs_per_100g')->default(0);
            $table->float('fiber_per_100g')->default(0);
            $table->float('sugar_per_100g')->default(0);
            $table->float('fat_per_100g')->default(0);

            // Fat Details
            $table->float('saturated_fat_per_100g')->default(0);
            $table->float('polyunsaturated_fat_per_100g')->default(0);
            $table->float('monounsaturated_fat_per_100g')->default(0);
            $table->float('trans_fat_per_100g')->default(0);
            $table->float('cholesterol_per_100g')->default(0);

            // Minerals & Vitamins
            $table->float('sodium_per_100g')->default(0);
            $table->float('potassium_per_100g')->default(0);
            $table->float('vitamin_a_per_100g')->default(0);
            $table->float('vitamin_c_per_100g')->default(0);
            $table->float('calcium_per_100g')->default(0);
            $table->float('iron_per_100g')->default(0);

            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('FOOD_TABLE');
    }
};
