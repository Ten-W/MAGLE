package com.tai.oeviewer

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Files
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test

class AppDownloadTest {
    @Test fun resumeRestartAndCorruptResponse() {
        val data = ByteArray(8192) { (it % 251).toByte() }
        val digest = "sha256:" + java.security.MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var mode = "resume"
        var requested = ""
        server.createContext("/app.apk") { exchange ->
            requested = exchange.requestHeaders.getFirst("Range") ?: ""
            val start = if (mode == "resume" || mode == "badRange") 1024 else 0
            if (start > 0) exchange.responseHeaders.set("Content-Range", "bytes ${if (mode == "badRange") 0 else start}-${data.size - 1}/${data.size}")
            exchange.sendResponseHeaders(if (start > 0) 206 else 200, (data.size - start).toLong())
            exchange.responseBody.use { it.write(if (mode == "badHash") ByteArray(data.size) else data, start, data.size - start) }
        }
        server.start()
        val directory = Files.createTempDirectory("magle-download-test").toFile()
        try {
            val target = java.io.File(directory, "app.apk")
            val partial = java.io.File(target.path + ".part")
            val release = AppRelease("v1.0.0", "", "", "http://127.0.0.1:${server.address.port}/app.apk", data.size.toLong(), digest)
            val client = OkHttpClient()
            for (response in listOf("resume", "restart")) {
                mode = response; target.delete(); partial.writeBytes(data.copyOf(1024))
                downloadAppFile(client, release, target)
                assertEquals("bytes=1024-", requested)
                assertArrayEquals(data, target.readBytes()); assertFalse(partial.exists())
            }
            target.delete(); partial.writeBytes(data.copyOf(1024)); mode = "badRange"
            assertTrue(runCatching { downloadAppFile(client, release, target) }.isFailure)
            assertEquals(1024L, partial.length()); assertFalse(target.exists())
            mode = "badHash"; partial.delete()
            assertTrue(runCatching { downloadAppFile(client, release, target) }.isFailure)
            assertFalse(partial.exists()); assertFalse(target.exists())
        } finally { server.stop(0); directory.listFiles()?.forEach { it.delete() }; directory.delete() }
    }
}
