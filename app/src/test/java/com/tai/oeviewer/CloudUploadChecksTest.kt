package com.tai.oeviewer

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class CloudUploadChecksTest {
    @Test fun hashesBlockBoundaryAndCommitNeverOverwritesOrAutorename() {
        val file = File.createTempFile("magle-test", ".bin")
        try {
            val bytes = ByteArray(4 * 1024 * 1024 + 17) { (it % 251).toByte() }
            file.writeBytes(bytes)
            val sha = MessageDigest.getInstance("SHA-256")
            val expected = sha.digest(sha.digest(bytes.copyOfRange(0, 4 * 1024 * 1024)) + sha.digest(bytes.copyOfRange(4 * 1024 * 1024, bytes.size)))
                .joinToString("") { "%02x".format(it.toInt() and 255) }
            assertEquals(expected, cloudFileHash(file, true))
            assertEquals(cloudFileHash(file, false), MessageDigest.getInstance("MD5").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) })
            val create = dropboxCommit("/test/file")
            assertTrue(create.mode.isAdd); assertFalse(create.autorename); assertTrue(create.strictConflict)
            val update = dropboxCommit("/test/file", "0123456789abcdef")
            assertTrue(update.mode.isUpdate); assertEquals("0123456789abcdef", update.mode.updateValue)
            verifyCloudUpload(file.length(), expected, file, expected)
            try { verifyCloudUpload(file.length(), "different", file, expected); fail("Changed remote file accepted") } catch (_: LibraryConflict) { }
        } finally { file.delete() }
    }
}
