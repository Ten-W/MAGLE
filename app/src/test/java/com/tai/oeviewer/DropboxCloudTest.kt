package com.tai.oeviewer

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.http.HttpRequestor
import com.dropbox.core.v2.DbxClientV2
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File

class DropboxCloudTest {
    @Test(timeout = 15000) fun nativeSdkUploadRetryAndConditionalEditing() {
        val file = File.createTempFile("magle-dropbox", ".png").apply { writeText("isolated-test-image") }
        val data = mutableMapOf<String, ByteArray>()
        val folders = mutableSetOf("/test/images")
        var revision = 1L
        var finishes = 0
        var conflict = false
        val seen = mutableListOf<String>()
        fun metadata(path: String): JSONObject = JSONObject().put(".tag", "file").put("name", path.substringAfterLast('/'))
            .put("id", "id:test").put("rev", revision.toString(16).padStart(16, '0')).put("size", data[path]!!.size)
            .put("client_modified", "2026-10-05T00:00:00Z").put("server_modified", "2026-10-05T00:00:00Z")
            .put("content_hash", cloudFileHash(file, true))
        val transport = object : HttpRequestor() {
            override fun doGet(url: String, headers: Iterable<Header>): Response = error("Unexpected GET")
            override fun startPut(url: String, headers: Iterable<Header>): Uploader = error("Unexpected PUT")
            override fun startPost(url: String, headers: Iterable<Header>): Uploader = object : Uploader() {
                val output = ByteArrayOutputStream()
                override fun getBody() = output
                override fun close() { }
                override fun abort() { }
                override fun finish(): Response {
                    val endpoint = url.substringAfter("/2/")
                    seen += endpoint
                    val argument = headers.firstOrNull { it.key.equals("Dropbox-API-Arg", true) }?.value ?: output.toString("UTF-8")
                    val arg = JSONObject(argument.ifEmpty { "{}" })
                    val path = arg.optString("path")
                    var code = 200
                    var extraHeaders = emptyMap<String, List<String>>()
                    val body: ByteArray = when (endpoint) {
                        "files/get_metadata" -> when {
                            path in folders -> ("{\".tag\":\"folder\"," + JSONObject().put("name", path.substringAfterLast('/')).put("id", "id:folder").toString().drop(1)).toByteArray()
                            path in data -> ("{\".tag\":\"file\"," + metadata(path).apply { remove(".tag") }.toString().drop(1)).toByteArray()
                            else -> { code = 409; "{\"error_summary\":\"path/not_found/\",\"error\":{\".tag\":\"path\",\"path\":{\".tag\":\"not_found\"}}}".toByteArray() }
                        }
                        "files/create_folder_v2" -> {
                            folders += path
                            JSONObject().put("metadata", JSONObject().put("name", path.substringAfterLast('/')).put("id", "id:folder")).toString().toByteArray()
                        }
                        "files/upload_session/start" -> "{\"session_id\":\"fixture-session\"}".toByteArray()
                        "files/upload_session/finish" -> {
                            finishes++
                            val commit = arg.getJSONObject("commit")
                            assertEquals("add", commit.getString("mode")); assertFalse(commit.getBoolean("autorename")); assertTrue(commit.getBoolean("strict_conflict"))
                            val target = commit.getString("path"); data[target] = output.toByteArray()
                            metadata(target).toString().toByteArray()
                        }
                        "files/download" -> {
                            extraHeaders = mapOf("Dropbox-API-Result" to listOf(metadata(path).toString()))
                            data[path]!!
                        }
                        "files/upload" -> {
                            val mode = arg.getJSONObject("mode")
                            assertEquals("update", mode.getString(".tag")); assertTrue(arg.getBoolean("strict_conflict")); assertFalse(arg.getBoolean("autorename"))
                            if (conflict) { conflict = false; revision++; data[path] = "{\"computer\":88}".toByteArray() }
                            if (mode.getString("update") != revision.toString(16).padStart(16, '0')) {
                                code = 409
                                "{\"error_summary\":\"path/conflict/file/\",\"error\":{\".tag\":\"path\",\"reason\":{\".tag\":\"conflict\",\"conflict\":{\".tag\":\"file\"}},\"upload_session_id\":\"fixture-session\"}}".toByteArray()
                            } else { data[path] = output.toByteArray(); revision++; metadata(path).toString().toByteArray() }
                        }
                        else -> error("Unexpected endpoint: $endpoint")
                    }
                    return Response(code, body.inputStream(), mapOf("Content-Type" to listOf("application/json"), "X-Dropbox-Request-Id" to listOf("fixture")) + extraHeaders)
                }
            }
        }
        try {
            val client = DbxClientV2(DbxRequestConfig.newBuilder("MAGLE-test").withHttpRequestor(transport).build(), "fixture-token")
            val remote = RemoteLibrary("DROPBOX", JSONObject().put("root", "/test"), JSONObject(), dropboxClient = client) { error("Unexpected Google call") }
            val state = JSONObject()
            remote.createAssetDirectory("asset", state) { }
            val path = "images/asset.info/sample.png"
            remote.uploadFile(path, file, state) { }
            remote.uploadFile(path, file, state) { }
            assertEquals(1, finishes); assertArrayEquals(file.readBytes(), remote.read(path))
            data["/test/$path"] = "{\"tags\":[],\"folders\":[],\"isDeleted\":false}".toByteArray()
            remote.mutateCloudJson(path, false) { LibraryLogic.appendTags(it, listOf("测试")) }
            remote.mutateCloudJson(path, false) { LibraryLogic.setMetadataList(it, "folders", listOf("folder")) }
            remote.mutateCloudJson(path, false) { LibraryLogic.setRecycled(it, "回收站") }
            remote.mutateCloudJson(path, false) { LibraryLogic.setRecycled(it, "还原") }
            assertFalse(JSONObject(String(remote.read(path))).getBoolean("isDeleted"))
            conflict = true
            try { remote.mutateCloudJson(path, false) { it.put("phone", "must-not-write") }; fail("Stale revision overwritten") } catch (_: LibraryConflict) { }
            assertFalse(JSONObject(String(remote.read(path))).has("phone"))
            conflict = true
            remote.mutateCloudJson(path, true) { mergeMtime(it, "phone", 55) }
            val result = JSONObject(String(remote.read(path)))
            assertEquals(88, result.getInt("computer")); assertEquals(55, result.getInt("phone"))
            assertTrue(seen.contains("files/create_folder_v2"))
        } finally { file.delete() }
    }
}
