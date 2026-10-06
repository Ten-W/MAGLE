package com.tai.oeviewer

import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.io.File

class GoogleCloudTest {
    @Test(timeout = 15000) fun uploadLostResponseRetriesSameIdAndEditsUseVersions() {
        val file = File.createTempFile("magle-google", ".png").apply { writeText("isolated-test-image") }
        val entries = linkedMapOf("images" to JSONObject().put("id", "images").put("name", "images")
            .put("mimeType", "application/vnd.google-apps.folder").put("parents", JSONArray(listOf("root"))))
        val contents = mutableMapOf<String, ByteArray>()
        var nextId = 0
        var creates = 0
        var loseResponse = true
        var revision = 1
        var conflict = false
        val seenVersions = mutableListOf<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val path = exchange.requestURI.path
            val query = URLDecoder.decode(exchange.requestURI.rawQuery.orEmpty(), "UTF-8")
            assertEquals("Bearer fixture-token", exchange.requestHeaders.getFirst("Authorization"))
            var code = 200
            val response = when {
                path.endsWith("generateIds") -> JSONObject().put("ids", JSONArray(listOf("reserved-${++nextId}"))).toString().toByteArray()
                path == "/drive/v3/files" && exchange.requestMethod == "GET" -> {
                    val parent = Regex("q='([^']+)' in parents").find(query)!!.groupValues[1]
                    val name = Regex("name = '([^']+)'", RegexOption.DOT_MATCHES_ALL).find(query)?.groupValues?.get(1)
                    JSONObject().put("files", JSONArray(entries.values.filter { it.getJSONArray("parents").getString(0) == parent && (name == null || it.getString("name") == name) })).toString().toByteArray()
                }
                path == "/drive/v3/files" && exchange.requestMethod == "POST" -> {
                    val item = JSONObject(String(exchange.requestBody.readBytes())); entries[item.getString("id")] = item
                    item.toString().toByteArray()
                }
                path == "/upload/drive/v3/files" -> {
                    creates++
                    val body = String(exchange.requestBody.readBytes())
                    assertTrue(body.contains("isolated-test-image"))
                    val json = body.substring(body.indexOf('{'), body.indexOf('}') + 1)
                    val item = JSONObject(json).put("size", file.length()).put("md5Checksum", cloudFileHash(file, false))
                    entries[item.getString("id")] = item; contents[item.getString("id")] = file.readBytes()
                    if (loseResponse) { loseResponse = false; code = 500 }
                    item.toString().toByteArray()
                }
                path.startsWith("/drive/v2/files/") -> JSONObject().put("id", path.substringAfterLast('/')).put("etag", "\"$revision\"").toString().toByteArray()
                path.startsWith("/upload/drive/v2/files/") -> {
                    val id = path.substringAfterLast('/')
                    val version = exchange.requestHeaders.getFirst("If-Match"); seenVersions += version
                    if (conflict) { conflict = false; revision++; contents[id] = "{\"computer\":99}".toByteArray() }
                    if (version != "\"$revision\"") { code = 412; "{}".toByteArray() }
                    else { contents[id] = exchange.requestBody.readBytes(); revision++; "{}".toByteArray() }
                }
                query.contains("alt=media") -> contents[path.substringAfterLast('/')]!!
                else -> entries[path.substringAfterLast('/')]!!.toString().toByteArray()
            }
            exchange.sendResponseHeaders(code, response.size.toLong())
            exchange.responseBody.use { it.write(response) }; exchange.close()
        }
        server.start()
        try {
            val remote = RemoteLibrary("GOOGLE", JSONObject().put("root", "root"), JSONObject(),
                googleBase = "http://127.0.0.1:${server.address.port}") { "fixture-token" }
            val state = JSONObject()
            var saved = ""
            remote.createAssetDirectory("asset", state) { saved = state.toString() }
            val path = "images/asset.info/sample.png"
            try { remote.uploadFile(path, file, state) { saved = state.toString() }; fail("Lost response not reported") } catch (_: java.io.IOException) { }
            val resumed = JSONObject(saved)
            remote.uploadFile(path, file, resumed) { saved = resumed.toString() }
            assertEquals(1, creates)
            assertArrayEquals(file.readBytes(), remote.read(path))
            val itemId = resumed.getJSONObject("googleUploadIds").getString(path)
            contents[itemId] = "{\"tags\":[],\"folders\":[],\"isDeleted\":false}".toByteArray()
            remote.mutateCloudJson(path, false) { LibraryLogic.appendTags(it, listOf("测试")) }
            remote.mutateCloudJson(path, false) { LibraryLogic.setMetadataList(it, "folders", listOf("folder")) }
            remote.mutateCloudJson(path, false) { LibraryLogic.setRecycled(it, "回收站") }
            remote.mutateCloudJson(path, false) { LibraryLogic.setRecycled(it, "还原") }
            var json = JSONObject(String(remote.read(path)))
            assertEquals("测试", json.getJSONArray("tags").getString(0)); assertEquals("folder", json.getJSONArray("folders").getString(0)); assertFalse(json.getBoolean("isDeleted"))
            conflict = true
            try { remote.mutateCloudJson(path, false) { it.put("phone", "must-not-write") }; fail("Concurrent change overwritten") } catch (_: LibraryConflict) { }
            assertFalse(JSONObject(String(remote.read(path))).has("phone"))
            conflict = true
            remote.mutateCloudJson(path, true) { mergeMtime(it, "phone", 55) }
            json = JSONObject(String(remote.read(path)))
            assertEquals(99, json.getInt("computer")); assertEquals(55, json.getInt("phone"))
            assertTrue(seenVersions.all { it.startsWith('"') })
        } finally { server.stop(0); file.delete() }
    }
}
