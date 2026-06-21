package com.geminiauto

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class BackgroundService : Service() {

    private var promptIndex = 0
    private var prompts = mutableListOf<Prompt>()
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private val runnable = Runnable { executeCurrentAndScheduleNext() }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        prompts = Config.loadPrompts(this)
        if (prompts.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, createNotification())
        isRunning = true
        promptIndex = 0
        handler.post(runnable)
        return START_STICKY
    }

    private fun executeCurrentAndScheduleNext() {
        if (!isRunning || prompts.isEmpty()) {
            stopSelf()
            return
        }
        val p = prompts[promptIndex]
        GeminiAutoService.execute(p.text)

        promptIndex = (promptIndex + 1) % prompts.size
        val next = prompts[promptIndex]
        handler.postDelayed(runnable, next.interval * 1000L)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gemini Automation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background automation service"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Gemini Auto")
                .setContentText("Running ${prompts.size} prompts in cycle...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Gemini Auto")
                .setContentText("Running ${prompts.size} prompts in cycle...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build()
        }
    }

    companion object {
        private const val CHANNEL_ID = "gemini_auto_channel"
        private const val NOTIFICATION_ID = 1001
        
        fun onPromptDone() {
            // Called when a prompt is successfully executed
            // Can be used for logging or further processing
        }
    }
}
