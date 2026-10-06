package com.tai.oeviewer

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class IndexBackupTest {
    @Test fun roundTripKeepsOneLibraryThumbnailsAndRejectsUnsafeArchives() {
        val root = Files.createTempDirectory("index-backup-test").toFile()
        try {
            val image = File(root, "cached.thumb").apply { writeBytes(byteArrayOf(1, 2, 3)) }
            val index = JSONObject().put("schemaVersion", 2).put("libraryId", "private-source-location")
                .put("accessToken", "never-export-this")
                .put("libraryName", "One library").put("items", JSONObject().put("asset-1", JSONObject()
                    .put("name", "picture").put("ext", "png").put("mtime", 123)))
            index.getJSONObject("items").put("MKIFXUOKFRRU7(1)", JSONObject()
                .put("name", "copy").put("ext", "png").put("mtime", 456))
            val output = ByteArrayOutputStream()
            writeIndexBackup(output, index, JSONObject().put("folders", org.json.JSONArray())) { _, _ -> image }
            val restored = readIndexBackup(output.toByteArray().inputStream(), File(root, "restored"))
            assertEquals("backup", restored.index.getString("libraryId"))
            assertFalse(restored.index.has("accessToken"))
            assertEquals("One library", restored.index.getString("libraryName"))
            assertEquals(2, restored.index.getJSONObject("items").length())
            assertEquals("copy", restored.index.getJSONObject("items").getJSONObject("MKIFXUOKFRRU7(1)").getString("name"))
            assertArrayEquals(image.readBytes(), File(restored.directory, "thumbnails/asset-1.thumb").readBytes())
            assertArrayEquals(image.readBytes(), File(restored.directory, "thumbnails/MKIFXUOKFRRU7(1).thumb").readBytes())
            assertEquals("private-source-location", index.getString("libraryId"))
            val bad = ByteArrayOutputStream()
            ZipOutputStream(bad).use { it.putNextEntry(ZipEntry("../escape")); it.write(1); it.closeEntry() }
            assertThrows(IllegalArgumentException::class.java) {
                readIndexBackup(bad.toByteArray().inputStream(), File(root, "rejected"))
            }
            assertFalse(File(root, "escape").exists())
            assertFalse(File(root, "rejected").exists())
            listOf("thumbnails/../escape.thumb", "thumbnails/a/b.thumb", "thumbnails/a\\b.thumb").forEachIndexed { i, path ->
                val unsafe = ByteArrayOutputStream()
                ZipOutputStream(unsafe).use { it.putNextEntry(ZipEntry(path)); it.write(1); it.closeEntry() }
                assertThrows(IllegalArgumentException::class.java) {
                    readIndexBackup(unsafe.toByteArray().inputStream(), File(root, "unsafe-$i"))
                }
                assertFalse(File(root, "unsafe-$i").exists())
            }
        } finally { root.deleteRecursively() }
    }
}
