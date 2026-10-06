package com.tai.oeviewer

import com.sun.net.httpserver.HttpServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

class RemoteLibraryTest {
    @Test fun localFixtureEndpointIsDebugOnlyAndCannotEscapeLoopback() {
        assertTrue(validS3Endpoint("http://127.0.0.1:19090", true))
        assertFalse(validS3Endpoint("http://127.0.0.1:19090", false))
        assertFalse(validS3Endpoint("http://127.0.0.1:19090@evil.example", true))
        assertFalse(validS3Endpoint("http://192.168.1.1:9000", true))
        assertTrue(validS3Endpoint("https://s3.amazonaws.com", false))
    }
    @Test(timeout = 30000) fun s3ListsAllPagesAndReadsAndWritesExactKeys() {
        val seen = CopyOnWriteArrayList<String>()
        var uploaded = byteArrayOf()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            seen += exchange.requestURI.toString()
            val signed = exchange.requestHeaders.getFirst("Authorization").orEmpty()
            assertTrue(signed.startsWith("AWS4-HMAC-SHA256 "))
            val query = exchange.requestURI.rawQuery.orEmpty()
            val response = if (query.contains("list-type=2")) {
                exchange.responseHeaders.add("Content-Type", "application/xml")
                val second = query.contains("continuation-token=page2")
                val name = if (second) "second" else "first"
                """<ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                  <Name>test-bucket</Name><Prefix>Eagle.library/images/</Prefix>
                  <KeyCount>1</KeyCount><MaxKeys>1000</MaxKeys><IsTruncated>${!second}</IsTruncated>
                  ${if (second) "" else "<NextContinuationToken>page2</NextContinuationToken>"}
                  <CommonPrefixes><Prefix>Eagle.library/images/$name.info/</Prefix></CommonPrefixes>
                </ListBucketResult>""".toByteArray()
            } else if (exchange.requestMethod == "PUT") {
                uploaded = exchange.requestBody.readBytes()
                exchange.responseHeaders.add("ETag", "\"test-etag\"")
                byteArrayOf()
            } else {
                exchange.responseHeaders.add("Content-Type", "application/json")
                "{\"folders\":[]}".toByteArray()
            }
            exchange.sendResponseHeaders(200, if (response.isEmpty()) -1 else response.size.toLong())
            exchange.responseBody.use { it.write(response) }
            exchange.close()
        }
        server.start()
        try {
            val config = JSONObject().put("root", "Eagle.library")
                .put("endpoint", "http://127.0.0.1:${server.address.port}")
                .put("bucket", "test-bucket").put("region", "us-east-1").put("accessKey", "test-access-key")
            val remote = RemoteLibrary("S3", config, JSONObject().put("secretKey", "test-secret-key")) { error("Unexpected Google call") }
            assertEquals(setOf("first", "second"), remote.assetIds())
            assertEquals("{\"folders\":[]}", String(remote.read("metadata.json")))
            remote.write("images/中文.info/metadata.json", "updated".toByteArray())
            assertEquals("updated", String(uploaded))
            assertTrue(seen.any { it.contains("continuation-token=page2") })
            assertTrue(seen.any { it.contains("/test-bucket/Eagle.library/images/%E4%B8%AD%E6%96%87.info/metadata.json") })
        } finally { server.stop(0) }
    }
}
