package com.tai.oeviewer

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

internal data class AppDownloadState(val button: String = "立即更新", val message: String = "", val busy: Boolean = false)

// Android owns the transfer, including network retries and persistence across app restarts.
internal class AppDownload(private val context: Context) {
    private val prefs = context.getSharedPreferences("app-download", Context.MODE_PRIVATE)
    private val manager = context.getSystemService(DownloadManager::class.java)
    fun savedRelease(): AppRelease? = prefs.getString("version", null)?.let { version ->
        val parts = versionParts(version) ?: return null
        if (compareVersions(parts, versionParts(BuildConfig.VERSION_NAME)!!) <= 0) {
            clear(); return null
        }
        AppRelease(version, "", "$APP_REPOSITORY/releases/tag/$version",
            prefs.getString("url", "")!!, prefs.getLong("size", 0), prefs.getString("digest", "")!!)
    }
    private fun file(release: AppRelease) = File(context.getExternalFilesDir(null), "updates/MAGLE-${release.version}.apk")
    fun clear() {
        val id = prefs.getLong("id", -1)
        if (id != -1L) manager.remove(id)
        prefs.getString("version", null)?.takeIf { versionParts(it) != null }?.let {
            File(context.getExternalFilesDir(null), "updates/MAGLE-$it.apk").delete()
        }
        prefs.edit().clear().commit()
    }
    fun start(release: AppRelease) {
        require(versionParts(release.version) != null && release.apkUrl.startsWith("$APP_REPOSITORY/releases/download/${release.version}/"))
        if (savedRelease()?.version == release.version && state().busy) return
        clear()
        val request = DownloadManager.Request(Uri.parse(release.apkUrl))
            .setTitle("MAGLE ${release.version}").setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, null, "updates/MAGLE-${release.version}.apk")
        val id = manager.enqueue(request)
        prefs.edit().putLong("id", id).putString("version", release.version).putString("url", release.apkUrl)
            .putLong("size", release.size).putString("digest", release.digest).commit()
    }
    fun state(): AppDownloadState {
        val id = prefs.getLong("id", -1)
        if (id == -1L) return AppDownloadState()
        manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            if (!cursor.moveToFirst()) return AppDownloadState("重新下载", "下载任务已失效")
            fun number(column: String) = cursor.getLong(cursor.getColumnIndexOrThrow(column))
            return when (number(DownloadManager.COLUMN_STATUS).toInt()) {
                DownloadManager.STATUS_SUCCESSFUL -> AppDownloadState("安装更新", "下载完成，点击安装")
                DownloadManager.STATUS_FAILED -> AppDownloadState("重新下载", "下载失败，可重试（错误 ${number(DownloadManager.COLUMN_REASON)}）")
                else -> {
                    val total = number(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val bytes = number(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val paused = number(DownloadManager.COLUMN_STATUS).toInt() == DownloadManager.STATUS_PAUSED
                    AppDownloadState(if (total > 0) "${(bytes * 100 / total).coerceIn(0, 100)}%" else "下载中",
                        if (paused) "等待网络恢复，系统会继续下载" else "正在后台下载", true)
                }
            }
        }
    }
    @Suppress("DEPRECATION")
    fun validate(release: AppRelease) {
        val apk = file(release)
        check(apk.length() == release.size) { "安装包不完整，请重新下载" }
        if (release.digest.isNotBlank()) {
            val hash = java.security.MessageDigest.getInstance("SHA-256")
            apk.inputStream().use { input -> val buffer = ByteArray(65536); while (true) {
                val count = input.read(buffer); if (count < 0) break; hash.update(buffer, 0, count)
            } }
            check("sha256:" + hash.digest().joinToString("") { "%02x".format(it) } == release.digest) { "安装包校验失败，请重新下载" }
        }
        val pm = context.packageManager
        val archive = checkNotNull(pm.getPackageArchiveInfo(apk.path, PackageManager.GET_SIGNING_CERTIFICATES)) { "无法读取安装包" }
        val installed = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        check(archive.packageName == context.packageName && archive.versionName == release.version.removePrefix("v") &&
            archive.longVersionCode > installed.longVersionCode) { "安装包版本或应用不匹配" }
        check(archive.signingInfo?.apkContentsSigners?.toSet() == installed.signingInfo?.apkContentsSigners?.toSet() &&
            archive.signingInfo != null) { "安装包签名不匹配" }
    }
    fun install(release: AppRelease) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file(release))
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }
}
