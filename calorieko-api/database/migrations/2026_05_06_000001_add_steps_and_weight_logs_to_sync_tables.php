<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (Schema::hasTable('activity_log_table') && !Schema::hasColumn('activity_log_table', 'steps')) {
            Schema::table('activity_log_table', function (Blueprint $table) {
                $table->integer('steps')->nullable()->after('movingTimeSeconds');
            });
        }

        if (!Schema::hasTable('weight_log_table')) {
            Schema::create('weight_log_table', function (Blueprint $table) {
                $table->id();
                $table->string('uid');
                $table->bigInteger('date_epoch_day');
                $table->double('weight_kg');
                $table->bigInteger('timestamp');
                $table->bigInteger('mobile_updated_at')->nullable();
                $table->timestamps();

                $table->unique(['uid', 'date_epoch_day'], 'weight_log_uid_date_unique');
                $table->index('uid');
                $table->index('date_epoch_day');
            });
        }
    }

    public function down(): void
    {
        if (Schema::hasTable('weight_log_table')) {
            Schema::dropIfExists('weight_log_table');
        }

        if (Schema::hasTable('activity_log_table') && Schema::hasColumn('activity_log_table', 'steps')) {
            Schema::table('activity_log_table', function (Blueprint $table) {
                $table->dropColumn('steps');
            });
        }
    }
};
