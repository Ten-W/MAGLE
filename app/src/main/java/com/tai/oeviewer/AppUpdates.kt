package com.tai.oeviewer

import org.json.JSONArray

internal const val APP_REPOSITORY = "https://github.com/Ten-W/MAGLE"
internal const val APP_RELEASES_API = "https://api.github.com/repos/Ten-W/MAGLE/releases?per_page=100"
internal const val APP_RELEASES_FALLBACK = "https://raw.githubusercontent.com/Ten-W/MAGLE/main/update.json"

internal fun fetchAppRelease(currentVersion: String, urls: List<String> = listOf(APP_RELEASES_API, APP_RELEASES_FALLBACK)): AppRelease? {
    val client = okhttp3.OkHttpClient.Builder().callTimeout(15, java.util.concurrent.TimeUnit.SECONDS).build()
    var failure: Exception? = null
    for (url in urls) {
        try {
            client.newCall(okhttp3.Request.Builder().url(url).header("User-Agent", "MAGLE/$currentVersion").build()).execute().use { response ->
                check(response.isSuccessful) { "检查失败：HTTP ${response.code}" }
                val bytes = response.body!!.byteStream().use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val size = input.read(buffer); if (size < 0) break
                        check(output.size() + size <= 1024 * 1024) { "更新信息过大" }
                        output.write(buffer, 0, size)
                    }
                    output.toByteArray()
                }
                return newerAppRelease(JSONArray(String(bytes, Charsets.UTF_8)), currentVersion)
            }
        } catch (error: Exception) { failure = error }
    }
    throw java.io.IOException("无法检查更新，请检查网络后重试", failure)
}
internal data class AppRelease(val version: String, val notes: String, val page: String,
    val apkUrl: String, val size: Long, val digest: String)

internal fun versionParts(value: String): List<Int>? {
    return Regex("v?(\\d+)\\.(\\d+)\\.(\\d+)").matchEntire(value)?.groupValues?.drop(1)
        ?.map { it.toIntOrNull() ?: return null }
}

internal fun compareVersions(left: List<Int>, right: List<Int>): Int {
    for (i in 0..2) if (left[i] != right[i]) return left[i].compareTo(right[i])
    return 0
}

internal fun newerAppRelease(releases: JSONArray, currentVersion: String): AppRelease? {
    val current = requireNotNull(versionParts(currentVersion)) { "无法识别当前版本号" }
    return (0 until releases.length()).mapNotNull { index ->
        val release = releases.getJSONObject(index)
        val version = release.optString("tag_name")
        val parts = versionParts(version) ?: return@mapNotNull null
        val page = release.optString("html_url")
        val assets = release.optJSONArray("assets") ?: return@mapNotNull null
        val apk = (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull {
            it.optString("name").endsWith(".apk", true) &&
                it.optString("browser_download_url").startsWith("$APP_REPOSITORY/releases/download/$version/") &&
                it.optLong("size") in 1..500_000_000 &&
                (it.optString("digest").isBlank() || Regex("sha256:[0-9a-f]{64}").matches(it.optString("digest")))
        } ?: return@mapNotNull null
        if (release.optBoolean("draft") || compareVersions(parts, current) <= 0 ||
            page != "$APP_REPOSITORY/releases/tag/$version" ||
            !(0 until assets.length()).any { assets.getJSONObject(it).optString("name").endsWith(".apk", true) }) return@mapNotNull null
        AppRelease(version, release.optString("body").take(4000), page,
            apk.getString("browser_download_url"), apk.getLong("size"), apk.optString("digest"))
    }.maxWithOrNull { a, b -> compareVersions(versionParts(a.version)!!, versionParts(b.version)!!) }
}
