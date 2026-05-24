package com.calorieko.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.calorieko.app.CalorieKoApplication
import com.calorieko.app.MainActivity
import com.calorieko.app.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/**
 * MealPlanReminderWorker
 *
 * Runs periodically and checks the user's planned meals for the current day.
 * If there's an upcoming meal slot that hasn't passed yet, it posts a
 * local notification reminding the user.
 *
 * ## Notification timing
 *
 * Each meal slot maps to a target time:
 * - Breakfast → 7:00 AM
 * - Lunch → 11:30 AM
 * - Dinner → 5:30 PM
 * - Snack → 3:00 PM
 *
 * The worker fires every ~1 hour. When it runs, it finds the *next* upcoming
 * slot that is within the next 60 minutes and posts a reminder for it.
 * Already-passed slots are silently skipped.
 *
 * ## Preference gate
 *
 * Notifications are only posted when the user's SharedPreferences key
 * `meal_plan_reminders_enabled` is true (default: true).
 */
class MealPlanReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "calorieko_meal_plan_reminder"
        const val CHANNEL_NAME = "Meal Plan Reminders"
        const val NOTIFICATION_ID_BASE = 2000  // 2000–2003 for the 4 slots
        const val WORK_NAME = "meal_plan_daily_reminder"
        private const val PREFS_NAME = "calorieko_prefs"
        private const val KEY_ENABLED = "meal_plan_reminders_enabled"

        /** Default meal slot target hours. */
        private val SLOT_TIMES = mapOf(
            "Breakfast" to LocalTime.of(7, 0),
            "Lunch"     to LocalTime.of(11, 30),
            "Snack"     to LocalTime.of(15, 0),
            "Dinner"    to LocalTime.of(17, 30)
        )

        /** Slot-specific emoji for the notification title. */
        private val SLOT_EMOJI = mapOf(
            "Breakfast" to "☀️",
            "Lunch"     to "🍽️",
            "Snack"     to "🍎",
            "Dinner"    to "🌙"
        )

        /** Fixed notification ID offsets per slot. */
        private val SLOT_ID_OFFSET = mapOf(
            "Breakfast" to 0,
            "Lunch"     to 1,
            "Snack"     to 2,
            "Dinner"    to 3
        )

        /**
         * Schedules the periodic meal plan reminder worker.
         * Idempotent: safe to call on every app launch.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MealPlanReminderWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Cancels the meal plan reminder (call on logout). */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /** Checks whether meal plan reminders are enabled. */
        fun isEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, true)
        }

        /** Sets the meal plan reminder enabled state. */
        fun setEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, enabled).apply()
            if (enabled) {
                schedule(context)
            } else {
                cancel(context)
            }
        }
    }

    override suspend fun doWork(): Result {
        // ── Preference gate ──
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, true)) return Result.success()

        val db = (context.applicationContext as CalorieKoApplication).database

        // ── Determine today's day index and week start ──
        val today = LocalDate.now()
        val dayIndex = today.dayOfWeek.value - 1  // 0=Mon, 6=Sun
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .toString()  // "2026-04-20"

        // ── Fetch today's planned meals ──
        val todayMeals = db.mealPlanDao().getMealsForDayOneShot(dayIndex, weekStart)
        if (todayMeals.isEmpty()) return Result.success()

        // ── Filter out already-handled meals (logged/skipped) ──
        val actionableMeals = todayMeals.filter { it.status == "planned" }
        if (actionableMeals.isEmpty()) return Result.success()

        // ── Resolve dish labels → display names ──
        val dishNames = mutableMapOf<String, String>()
        for (meal in actionableMeals) {
            if (meal.dishLabel !in dishNames) {
                val recipe = db.dishRecipeDao().getByDishLabel(meal.dishLabel)
                dishNames[meal.dishLabel] = recipe?.nameEn
                    ?: meal.dishLabel.replace("_", " ").replaceFirstChar { it.uppercase() }
            }
        }

        // ── Group by slot and find the upcoming one ──
        val now = LocalTime.now()
        val mealsBySlot = actionableMeals.groupBy { it.mealSlot }

        for ((slot, targetTime) in SLOT_TIMES) {
            val meals = mealsBySlot[slot] ?: continue
            // Only notify if the target time is in the future but within 60 minutes
            val minutesUntil = java.time.Duration.between(now, targetTime).toMinutes()
            if (minutesUntil in 0..59) {
                val dishList = meals.map { dishNames[it.dishLabel] ?: it.dishLabel }
                postMealReminder(slot, dishList)
            }
        }

        return Result.success()
    }

    private fun postMealReminder(slot: String, dishes: List<String>) {
        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val slotOffset = SLOT_ID_OFFSET[slot] ?: 0
        val pendingIntent = PendingIntent.getActivity(
            context, NOTIFICATION_ID_BASE + slotOffset, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val emoji = SLOT_EMOJI[slot] ?: "🍽️"
        val dishSummary = dishes.joinToString(", ")
        val title = "$emoji Time for $slot!"
        val body = "Your planned meal: $dishSummary"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = NOTIFICATION_ID_BASE + slotOffset
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted on Android 13+ — gracefully ignore
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for your planned meals throughout the day"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
