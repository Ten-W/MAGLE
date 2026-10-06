package com.tai.oeviewer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class IndexingService : Service() {
    companion object {
        private const val CHANNEL = "library-index"
        private const val NOTIFICATION = 31
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "素材库索引", NotificationManager.IMPORTANCE_LOW)
        )
        startForeground(
            NOTIFICATION,
            NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_grid_view_24)
                .setContentTitle("MAGLE 正在建立素材索引")
                .setContentText("锁屏后会继续，已完成的部分会自动保存")
                .setOngoing(true)
                .build()
        )
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MAGLE:index")
            .apply { acquire(6 * 60 * 60 * 1000L) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra("editing", false) == true) {
            startForeground(NOTIFICATION, NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_grid_view_24)
                .setContentTitle("MAGLE 正在更新素材库")
                .setContentText("标签改名进度会保存，中断后可继续")
                .setOngoing(true).build())
        }
        if (intent?.getBooleanExtra("upload", false) == true) {
            startForeground(NOTIFICATION,
                NotificationCompat.Builder(this, CHANNEL)
                    .setSmallIcon(R.drawable.ic_grid_view_24)
                    .setContentTitle("MAGLE 正在上传图片")
                    .setContentText("未完成任务保留在手机，可在设置中重试")
                    .setOngoing(true).build())
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
