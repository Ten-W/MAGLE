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

// Transfer runs under MAGLE's UID, not the vendor's system download component.
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
    internal fun file(release: AppRelease) = File(context.getExternalFilesDir(null), "updates/MAGLE-${release.version}.apk")
    fun clear() {
        context.stopService(Intent(context, AppDownloadService::class.java))
        val id = prefs.getLong("id", -1)
        if (id != -1L) manager.remove(id)
        prefs.getString("version", null)?.takeIf { versionParts(it) != null }?.let {
            File(context.getExternalFilesDir(null), "updates/MAGLE-$it.apk").delete()
            File(context.getExternalFilesDir(null), "updates/MAGLE-$it.apk.part").delete()
        }
        prefs.edit().clear().commit()
    }
    fun start(release: AppRelease) {
        require(versionParts(release.version) != null && release.apkUrl.startsWith("$APP_REPOSITORY/releases/download/${release.version}/"))
        if (state().busy) return
        val same = prefs.getString("url", "") == release.apkUrl && prefs.getLong("size", 0) == release.size &&
            prefs.getString("digest", "") == release.digest
        if (!same) clear()
        val oldId = prefs.getLong("id", -1)
        if (oldId != -1L) manager.remove(oldId)
        prefs.edit().remove("id").putString("version", release.version).putString("url", release.apkUrl)
            .putLong("size", release.size).putString("digest", release.digest).commit()
        androidx.core.content.ContextCompat.startForegroundService(context, Intent(context, AppDownloadService::class.java))
    }
    fun state(): AppDownloadState {
        val version = prefs.getString("version", null) ?: return AppDownloadState()
        if (versionParts(version) == null) return AppDownloadState()
        val target = File(context.getExternalFilesDir(null), "updates/MAGLE-$version.apk")
        if (prefs.getBoolean("complete", false) && target.isFile) return AppDownloadState("安装更新", "下载完成，点击安装")
        if (AppDownloadService.running) {
            val bytes = File(target.path + ".part").length()
            val total = prefs.getLong("size", 0)
            return AppDownloadState(if (total > 0) "${(bytes * 100 / total).coerceIn(0, 100)}%" else "下载中", "正在后台下载", true)
        }
        return AppDownloadState("继续下载", prefs.getString("error", null) ?: "下载中断，点击继续下载")
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
