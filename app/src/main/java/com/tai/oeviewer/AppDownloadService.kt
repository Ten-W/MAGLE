package com.tai.oeviewer

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

// A single update transfer; the partial file survives network failure and process death.
class AppDownloadService : Service() {
    companion object { @Volatile internal var running = false }
    private var worker: Thread? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS).callTimeout(10, TimeUnit.MINUTES).build()
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("app-update", "软件更新", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        startForeground(32, NotificationCompat.Builder(this, "app-update").setSmallIcon(R.drawable.ic_download_24)
            .setContentTitle("MAGLE 更新").setContentText("正在下载，返回应用可查看进度")
            .setContentIntent(open).setOngoing(true).build())
        wakeLock = getSystemService(android.os.PowerManager::class.java)
            .newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "MAGLE:update")
            .apply { acquire(10 * 60 * 1000L) }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (worker != null) return START_NOT_STICKY
        running = true
        worker = Thread {
            val prefs = getSharedPreferences("app-download", MODE_PRIVATE)
            try {
                val release = checkNotNull(AppDownload(this).savedRelease())
                prefs.edit().putBoolean("complete", false).remove("error").commit()
                val target = AppDownload(this).file(release)
                downloadAppFile(client, release, target)
                prefs.edit().putBoolean("complete", true).commit()
            } catch (error: Exception) {
                prefs.edit().putString("error", "下载中断，已保留进度；点击重试（${error.message?.take(120)}）").commit()
            } finally { running = false; stopSelf() }
        }.also { it.start() }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        client.dispatcher.cancelAll(); worker?.interrupt(); running = false
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }
    override fun onTimeout(startId: Int, fgsType: Int) { stopSelf() }
    override fun onBind(intent: Intent?): IBinder? = null
}

internal fun downloadAppFile(client: OkHttpClient, release: AppRelease, target: File) {
    require(release.size in 1..500_000_000)
    val partial = File(target.path + ".part")
    target.parentFile!!.mkdirs()
    if (partial.length() > release.size) check(partial.delete())
    val offset = partial.length()
    if (offset < release.size) {
        val request = Request.Builder().url(release.apkUrl).header("Accept-Encoding", "identity")
        if (offset > 0) request.header("Range", "bytes=$offset-")
        client.newCall(request.build()).execute().use { response ->
            check(response.code == 200 || response.code == 206) { "HTTP ${response.code}" }
            val append = response.code == 206
            if (append) {
                val range = Regex("bytes (\\d+)-(\\d+)/(\\d+)").matchEntire(response.header("Content-Range") ?: "")
                check(range != null && range.groupValues[1].toLong() == offset &&
                    range.groupValues[2].toLong() == release.size - 1 && range.groupValues[3].toLong() == release.size) { "无效的断点续传响应" }
            }
            var count = if (append) offset else 0L
            java.io.FileOutputStream(partial, append).use { output ->
                response.body!!.byteStream().use { input ->
                    val buffer = ByteArray(65536)
                    while (true) {
                        if (Thread.currentThread().isInterrupted) throw java.io.InterruptedIOException()
                        val size = input.read(buffer); if (size < 0) break
                        check(count + size <= release.size) { "安装包大小不匹配" }
                        output.write(buffer, 0, size); count += size
                    }
                }
            }
        }
    }
    check(partial.length() == release.size) { "安装包尚未下载完整" }
    if (release.digest.isNotBlank()) {
        val hash = java.security.MessageDigest.getInstance("SHA-256")
        partial.inputStream().use { input -> val buffer = ByteArray(65536); while (true) {
            val size = input.read(buffer); if (size < 0) break; hash.update(buffer, 0, size)
        } }
        if ("sha256:" + hash.digest().joinToString("") { "%02x".format(it) } != release.digest) {
            partial.delete(); error("安装包校验失败，请重新下载")
        }
    }
    check(partial.renameTo(target)) { "无法保存安装包" }
}
