package com.tai.oeviewer

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AppUpdatesTest {
    @Test fun aboutHasOnlyTwoRowsAndNoStartupUpdateRequest() {
        val source = java.io.File("src/main/java/com/tai/oeviewer/MainActivity.kt").readText()
        val about = source.substringAfter("private fun SettingsAbout()").substringBefore("private fun openProjectPage")
        assertEquals(2, Regex("ListItem\\(colors").findAll(about).count())
        assertFalse(about.contains("Text(APP_REPOSITORY)"))
        assertFalse(source.contains("appUpdateOnOpen"))
        val startup = source.substringAfter("override fun onCreate").substringBefore("override fun onStart")
        assertFalse(startup.contains("checkAppUpdate"))
        val layout = source.substringAfter("private fun SettingsScreen()").substringBefore("private fun SettingsAbout()")
        assertEquals(2, Regex("SettingsAbout\\(\\)").findAll(layout).count())
    }
    @Test fun onlyNewerDownloadableVersionsFromExpectedRepository() {
        fun release(tag: String, apk: Boolean = true) = JSONObject().put("tag_name", tag)
            .put("html_url", "$APP_REPOSITORY/releases/tag/$tag")
            .put("assets", if (apk) JSONArray().put(JSONObject().put("name", "MAGLE.apk")
                .put("browser_download_url", "$APP_REPOSITORY/releases/download/$tag/MAGLE.apk").put("size", 123)) else JSONArray())
        val entries = JSONArray().put(release("v0.8.9")).put(release("v0.8.57"))
            .put(release("v0.8.60").put("draft", true)).put(release("v1.0.0", false))
            .put(release("v9.0.0").put("html_url", "https://evil.example/app"))
            .put(release("v0.8.58").put("prerelease", true)).put(release("garbage"))
        assertEquals("v0.8.58", newerAppRelease(entries, "0.8.57")!!.version)
        assertNull(newerAppRelease(entries, "0.8.58"))
        assertNull(versionParts("v999999999999.1.0"))
        assertNull(newerAppRelease(JSONArray(), "0.8.57"))
        val unsafe = release("v0.8.59")
        unsafe.getJSONArray("assets").getJSONObject(0).put("browser_download_url", "https://evil.example/app.apk")
        assertNull(newerAppRelease(JSONArray().put(unsafe), "0.8.58"))
        val corrupted = release("v0.8.59")
        corrupted.getJSONArray("assets").getJSONObject(0).put("digest", "sha256:invalid")
        assertNull(newerAppRelease(JSONArray().put(corrupted), "0.8.58"))
    }
}
