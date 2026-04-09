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
import com.calorieko.app.MainActivity
import com.calorieko.app.R
import com.calorieko.app.data.local.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * StreakReminderWorker (Epic 5)
 *
 * Scheduled once daily via WorkManager.
 * At runtime, it checks if the current user has logged any meal or workout today.
 * If NOT, it posts a local push notification reminding the user to keep their streak alive.
 *
 * The notification is suppressed if:
 * - No user is logged in
 * - The user already has at least one activity today
 */
class StreakReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "calorieko_streak_reminder"
        const val CHANNEL_NAME = "Streak Reminders"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "streak_daily_reminder"

        /**
         * Schedules (or reschedules) daily streak notifications.
         * This is idempotent: safe to call multiple times.
         * WorkManager will replace an existing enqueued job with KEEP policy.
         */
        fun schedule(context: Context) {
            // Compute initial delay so the first run lands at 8:00 PM local time
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 20)   // 8 PM
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // If 8 PM has already passed today, schedule for tomorrow
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            val initialDelay = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<StreakReminderWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,   // Don't reset timer if already scheduled
                request
            )
        }

        /**
         * Cancels the streak reminder (call on logout).
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        // Get start of today in epoch milliseconds
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val db = (context.applicationContext as com.calorieko.app.CalorieKoApplication).database

        // Check if the user has logged any activity (meal or workout) today
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000L
        val activityLogs = db.activityLogDao().getLogsForToday(uid, startOfDay)
        val mealLogs = db.mealLogDao().getMealLogsByDate(uid, startOfDay, endOfDay)

        val hasLoggedToday = activityLogs.isNotEmpty() || mealLogs.isNotEmpty()

        if (!hasLoggedToday) {
            postStreakNotification()
        }

        return Result.success()
    }

    private fun postStreakNotification() {
        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔥 Keep your streak alive!")
            .setContentText("Log a meal or workout today to maintain your streak.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Keep your streak alive! Log a meal or workout today and stay on track toward your health goals. Every day counts! 💪")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
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
                description = "Daily reminders to keep your CalorieKo streak alive"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
