package com.nahommerk.screenshield

import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class UsageMonitorService : Service() {
    private lateinit var prefs: Prefs
    private val handler = Handler(Looper.getMainLooper())
    private var lastTickMs = 0L
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        lastTickMs = System.currentTimeMillis()
        startForegroundWithNotification()
        handler.postDelayed(tick, TICK_MS)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    
    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }
    
    private val tick: Runnable = object : Runnable {
        override fun run() {
            try {
                val now = System.currentTimeMillis()
                val delta = now - lastTickMs
                lastTickMs = now
                if (prefs.timeLimitEnabled()) {
                    // Midnight rollover: reset today's watched time.
                    val today = TimeBudget.dayKey(now)
                    if (prefs.lastDay() != today) {
                        prefs.setLastDay(today)
                        prefs.setWatchedMs(0)
                    }
                    val onYouTube = foregroundYouTube() != null
                    if (onYouTube) {
                        prefs.setWatchedMs(prefs.watchedMs() + delta)
                    }
                    val limitMs = prefs.limitMinutes().toLong() * 60_000L
                    if (onYouTube &&
                        TimeBudget.isExceeded(prefs.watchedMs(), limitMs) &&
                        now >= prefs.graceUntil()
                    ) {
                        goHome()
                        BlockOverlayActivity.launchTimeUp(this@UsageMonitorService)
                    }
                }
            } catch (e: Exception) {
                // never crash the monitor
            }
            handler.postDelayed(this, TICK_MS)
        }
    }
    
    /** Returns the YouTube package if it is currently in the foreground, else null. */
    private fun foregroundYouTube(): String? {
        if (!hasUsageAccess(this)) return null
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 15_000, now)
        var lastPkg: String? = null
        val ev = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            when (ev.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> lastPkg = ev.packageName
                UsageEvents.Event.ACTIVITY_PAUSED -> if (ev.packageName == lastPkg) lastPkg = null
            }
        }
        return if (lastPkg == YOUTUBE_TV || lastPkg == YOUTUBE_MOBILE) lastPkg else null
    }
    
    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
    
    private fun startForegroundWithNotification() {
        val channelId = "screen_time"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId, "Screen time monitor", NotificationManager.IMPORTANCE_MIN
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
        val n: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ScreenShield")
            .setContentText("Monitoring screen time...")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }
    
    companion object {
        private const val TICK_MS = 5_000L
        const val YOUTUBE_TV = "com.google.android.youtube.tv"
        const val YOUTUBE_MOBILE = "com.google.android.youtube"
        
        fun start(context: Context) {
            val intent = Intent(context, UsageMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        fun stop(context: Context) = 
            context.stopService(Intent(context, UsageMonitorService::class.java))
        
        @Suppress("DEPRECATION")
        fun hasUsageAccess(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= 29) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), context.packageName
                )
            } else {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), context.packageName
                )
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }
    }
}
