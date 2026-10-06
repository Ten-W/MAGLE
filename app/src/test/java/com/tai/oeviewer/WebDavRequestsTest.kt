package com.tai.oeviewer

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import org.junit.Test
import org.junit.Assert.*

class WebDavRequestsTest {
    @Test(timeout = 10000) fun conditionalWritesRejectStaleVersionsAndMergeOnlyOnFreshRead() {
        var revision = 1
        var content = "{\"desktop\":1}".toByteArray()
        var puts = 0
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            if (exchange.requestMethod == "GET") {
                exchange.responseHeaders.add("ETag", "\"$revision\"")
                exchange.sendResponseHeaders(200, content.size.toLong())
                exchange.responseBody.use { it.write(content) }
            } else {
                puts++
                if (puts == 1) { revision++; content = "{\"desktop\":2,\"newItem\":55}".toByteArray() }
                if (exchange.requestHeaders.getFirst("If-Match") != "\"$revision\"") exchange.sendResponseHeaders(412, -1)
                else { content = exchange.requestBody.readBytes(); revision++; exchange.sendResponseHeaders(204, -1) }
            }
            exchange.close()
        }
        server.start()
        try {
            val client = OkHttpClient()
            val url = "http://127.0.0.1:${server.address.port}/mtime.json"
            updateVersionedJson(read = {
                val request = buildWebDavRequest("https://192.168.1.1/mtime.json", null, null, false).newBuilder().url(url).build()
                client.newCall(request).execute().use { VersionedJson(it.body.bytes(), requireStrongEtag(it.header("ETag"))) }
            }, write = { snapshot, bytes ->
                val request = buildWebDavRequest("https://192.168.1.1/mtime.json", null, null, false,
                    "PUT", bytes.toRequestBody(), ifMatch = snapshot.version).newBuilder().url(url).build()
                client.newCall(request).execute().use {
                    if (it.code == 412) throw LibraryConflict()
                    assertTrue(it.isSuccessful)
                }
            }, merge = true) { mergeMtime(it, "phone", 66) }
            val result = org.json.JSONObject(String(content))
            assertEquals(2, puts)
            assertEquals(2, result.getInt("desktop"))
            assertEquals(55, result.getInt("newItem"))
            assertEquals(66, result.getInt("phone"))
        } finally { server.stop(0) }
    }

    @Test fun nativeTransportAcceptsWebDavVerbsAndBodies() {
        val seen = mutableListOf<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") {
            seen += it.requestMethod + ":" + String(it.requestBody.readBytes())
            assertTrue(it.requestHeaders.getFirst("Authorization").startsWith("Basic "))
            it.sendResponseHeaders(200, -1)
            it.close()
        }
        server.start()
        try {
            val client = OkHttpClient()
            for (method in listOf("MKCOL", "PROPFIND", "PUT", "GET", "HEAD")) {
                val request = buildWebDavRequest("https://192.168.1.1/test/", "test", "test-only", false,
                    method, if (method == "PUT" || method == "PROPFIND") "payload".toRequestBody() else null, "1")
                    .newBuilder().url("http://127.0.0.1:${server.address.port}/test/").build()
                client.newCall(request).execute().use { assertTrue(it.isSuccessful) }
            }
            assertEquals(listOf("MKCOL:", "PROPFIND:payload", "PUT:payload", "GET:", "HEAD:"), seen)
            try { buildWebDavRequest("http://8.8.8.8/", "test", "test-only", true); fail("Public HTTP accepted") }
            catch (expected: IllegalArgumentException) { }
        } finally { server.stop(0) }
    }
}
