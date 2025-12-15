package com.rank2gaming.aura.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.rank2gaming.aura.R
import com.rank2gaming.aura.utils.SettingsManager

class WaterMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var isWet = false
    private val NOTIF_INTERVAL = 5 * 60 * 1000L // 5 Minutes

    companion object {
        const val CHANNEL_ID_SILENT = "water_monitor_silent"
        // Versioned ID to ensure Android registers the new Sound settings if you previously ran the app
        const val CHANNEL_ID_ALERT = "water_monitor_alert_v2"
        const val NOTIF_ID_MONITOR = 888
        const val NOTIF_ID_ALERT = 999

        fun startMonitoring(context: Context) {
            val intent = Intent(context, WaterMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopMonitoring(context: Context) {
            context.stopService(Intent(context, WaterMonitorService::class.java))
        }
    }

    private val notificationRunnable = object : Runnable {
        override fun run() {
            if (isWet && SettingsManager.isWaterNotifEnabled(this@WaterMonitorService)) {
                sendRecurringSilentAlert()
                handler.postDelayed(this, NOTIF_INTERVAL)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        // 1. Start Foreground immediately (Required by Android 8+)
        // This notification is SILENT (Low Importance) to not annoy the user while monitoring
        val monitorNotification = NotificationCompat.Builder(this, CHANNEL_ID_SILENT)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Water Detector Active")
            .setContentText("Monitoring charging port...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIF_ID_MONITOR, monitorNotification)

        // 2. Start Detection Logic
        simulateWaterDetection()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            // Channel 1: Silent (For Hardware Monitoring & 5-min reminders)
            val silentChannel = NotificationChannel(
                CHANNEL_ID_SILENT,
                "Water Monitor (Silent)",
                NotificationManager.IMPORTANCE_LOW
            )
            silentChannel.description = "Background monitoring and reminders"

            // Channel 2: Alert (Custom Sound) - Used ONLY when water is detected
            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERT,
                "Water Alerts (Sound)",
                NotificationManager.IMPORTANCE_HIGH
            )
            alertChannel.description = "Immediate water detection alerts"

            // --- CUSTOM SOUND LOGIC (res/raw/water_alert.mp3) ---
            val soundUri = Uri.parse("android.resource://$packageName/${R.raw.water_alert}")
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .build()

            alertChannel.setSound(soundUri, audioAttributes)
            alertChannel.enableVibration(true)

            nm.createNotificationChannels(listOf(silentChannel, alertChannel))
        }
    }

    private fun simulateWaterDetection() {
        // Simulating detection event after 5 seconds
        handler.postDelayed({
            isWet = true
            if (SettingsManager.isWaterNotifEnabled(this)) {
                sendInitialLoudAlert()
                // Schedule the recurring silent reminders
                handler.postDelayed(notificationRunnable, NOTIF_INTERVAL)
            }
        }, 5000)
    }

    // TRIGGER 1: The "Water Detector" functionality (SOUND ON)
    private fun sendInitialLoudAlert() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = Uri.parse("android.resource://$packageName/${R.raw.water_alert}")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_ALERT)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("⚠️ WATER DETECTED")
            .setContentText("Liquid detected in port! Use Ejector now.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri) // Fallback for pre-Oreo devices
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIF_ID_ALERT, notification)
    }

    // TRIGGER 2: The "Every 5 minutes" notification (SOUND OFF)
    private fun sendRecurringSilentAlert() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Uses the SILENT channel so it updates the text without making noise again
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_SILENT)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Water Detected (Reminder)")
            .setContentText("Port is still wet. Please check device.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIF_ID_ALERT, notification)
    }

    override fun onDestroy() {
        handler.removeCallbacks(notificationRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}