package com.calorieko.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Helper for requesting battery optimization exemptions on stock Android
 * and OEM-specific skins (OPPO/ColorOS, Xiaomi/MIUI, Vivo/FunTouchOS, etc.)
 *
 * ── Why is this needed? ──
 * Chinese OEM Android skins implement their own app-killing logic that sits
 * ABOVE the standard Android battery optimization framework. Even with a
 * foreground service + WakeLock + START_STICKY, these OEMs will kill apps
 * within 1–5 minutes of the screen turning off unless the app is explicitly
 * whitelisted in their proprietary "auto-start" / "battery saver" settings.
 *
 * The standard `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` intent handles stock
 * Android (and some Samsung devices), but OPPO/Xiaomi/Vivo require launching
 * their custom settings activities directly.
 *
 * Reference: https://dontkillmyapp.com
 */
object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptHelper"

    /**
     * Returns true if the app is already exempt from battery optimization.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Opens the system battery optimization dialog asking the user to whitelist
     * this app. On stock Android this is a one-tap dialog. On OEM skins it may
     * open the app-specific battery settings page.
     */
    fun requestBatteryOptimizationExemption(context: Context): Boolean {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open battery optimization dialog: ${e.message}")
            return false
        }
    }

    /**
     * Detect the device manufacturer and open the OEM-specific auto-start
     * or battery management settings. This is critical for OPPO, Xiaomi,
     * Vivo, Huawei, etc. which have proprietary app-killing mechanisms
     * that operate independently of Android's standard Doze/battery optimization.
     *
     * Returns true if an OEM-specific intent was launched successfully.
     */
    fun openOemBatterySettings(context: Context): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        Log.d(TAG, "Attempting OEM battery settings for manufacturer: $manufacturer")

        val oemIntents = when {
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> listOf(
                // ColorOS 14+ battery management
                Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
                Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
                // OPPO app battery management
                Intent().setComponent(ComponentName("com.oplus.battery", "com.oplus.battery.AppPowerManagerActivity")),
                Intent().setComponent(ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity")),
                // Fallback: app details
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )

            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> listOf(
                Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
                Intent().setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsContainerManagementActivity"))
            )

            manufacturer.contains("vivo") -> listOf(
                Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
                Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"))
            )

            manufacturer.contains("huawei") || manufacturer.contains("honor") -> listOf(
                Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
                Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"))
            )

            manufacturer.contains("samsung") -> listOf(
                Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")),
                Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.battery.ui.BatteryActivity"))
            )

            else -> emptyList()
        }

        for (intent in oemIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Log.d(TAG, "Opened OEM battery settings: ${intent.component}")
                    return true
                }
            } catch (e: Exception) {
                Log.d(TAG, "OEM intent failed: ${intent.component} - ${e.message}")
            }
        }

        Log.d(TAG, "No OEM-specific battery settings found for: $manufacturer")
        return false
    }

    /**
     * Detects if the current device is from an OEM known for aggressive
     * background app killing.
     */
    fun isAggressiveOem(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("oppo") ||
                manufacturer.contains("realme") ||
                manufacturer.contains("oneplus") ||
                manufacturer.contains("xiaomi") ||
                manufacturer.contains("redmi") ||
                manufacturer.contains("poco") ||
                manufacturer.contains("vivo") ||
                manufacturer.contains("huawei") ||
                manufacturer.contains("honor") ||
                manufacturer.contains("meizu") ||
                manufacturer.contains("asus") ||
                manufacturer.contains("nokia")
    }

    /**
     * Returns a user-friendly instruction string for the current OEM.
     */
    fun getOemInstructions(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                "Go to Settings → Apps → CalorieKo → Battery Usage → select \"Allow background activity\".\n\n" +
                "Also search for \"Auto-launch\" in Settings and enable it for CalorieKo.\n\n" +
                "In Recent Apps, swipe down on CalorieKo to lock it (🔒 icon appears)."
            }
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                "Go to Settings → Apps → Manage Apps → CalorieKo → Autostart → Enable.\n\n" +
                "Also go to Settings → Battery → App Battery Saver → CalorieKo → No restrictions.\n\n" +
                "In Recent Apps, long-press CalorieKo and tap the lock icon."
            }
            manufacturer.contains("vivo") -> {
                "Go to Settings → Battery → Background Power Consumption Management → CalorieKo → Allow.\n\n" +
                "Also go to i Manager → App Manager → Autostart Manager → enable CalorieKo."
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                "Go to Settings → Battery → App launch → CalorieKo → Manage manually → enable all toggles.\n\n" +
                "Also go to Settings → Apps → CalorieKo → Battery → Unrestricted."
            }
            else -> {
                "Go to Settings → Apps → CalorieKo → Battery → set to \"Unrestricted\".\n\n" +
                "This prevents the phone from stopping the workout tracker when the screen is off."
            }
        }
    }
}
