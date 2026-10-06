package com.tai.oeviewer

import java.io.File
import java.security.MessageDigest
import com.dropbox.core.v2.files.CommitInfo
import com.dropbox.core.v2.files.WriteMode

internal fun cloudFileHash(file: File, dropbox: Boolean): String {
    val result = MessageDigest.getInstance(if (dropbox) "SHA-256" else "MD5")
    file.inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        var block = MessageDigest.getInstance("SHA-256")
        var blockBytes = 0
        while (true) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            val count = input.read(buffer, 0, if (dropbox) minOf(buffer.size, 4 * 1024 * 1024 - blockBytes) else buffer.size)
            if (count < 0) break
            if (!dropbox) result.update(buffer, 0, count)
            else {
                block.update(buffer, 0, count); blockBytes += count
                if (blockBytes == 4 * 1024 * 1024) {
                    result.update(block.digest()); block = MessageDigest.getInstance("SHA-256"); blockBytes = 0
                }
            }
        }
        if (dropbox && blockBytes > 0) result.update(block.digest())
    }
    return result.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
}

internal fun dropboxCommit(path: String, revision: String? = null): CommitInfo = CommitInfo.newBuilder(path)
    .withMode(if (revision == null) WriteMode.ADD else WriteMode.update(revision))
    .withAutorename(false).withStrictConflict(true).build()

internal fun verifyCloudUpload(size: Long, hash: String?, file: File, expectedHash: String) {
    if (size != file.length() || hash != expectedHash) throw LibraryConflict("同名文件内容不同或校验失败，未覆盖；上传任务已保留")
}
