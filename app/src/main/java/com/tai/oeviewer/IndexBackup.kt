package com.tai.oeviewer

import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal data class ImportedIndex(val index: JSONObject, val folders: JSONObject, val directory: File)

// Eagle copies can have IDs such as MKIFXUOKFRRU7(1); keep path separators and dots forbidden.
private val backupAssetId = Regex("[A-Za-z0-9_()-]{1,128}")
private const val BACKUP_LIMIT = 256L * 1024 * 1024

/** Only index data and cached thumbnails; never serialize connection records or credentials. */
internal fun writeIndexBackup(output: OutputStream, index: JSONObject, folders: JSONObject,
                              thumbnail: (String, Long) -> File?) {
    require(index.optInt("schemaVersion") == 2) { "不支持的索引版本" }
    val copy = JSONObject().put("libraryId", "backup")
    // Export index fields only, even if future internal cache versions add connection-related data.
    listOf("schemaVersion", "libraryName", "items", "ignored", "scanTotal", "failedCount", "unlisted")
        .forEach { if (index.has(it)) copy.put(it, index.get(it)) }
    val items = copy.getJSONObject("items")
    var total = 0L
    ZipOutputStream(output).use { zip ->
        fun entry(name: String, input: InputStream) {
            zip.putNextEntry(ZipEntry(name))
            input.use {
                val buffer = ByteArray(8192)
                var size = 0L
                while (true) {
                    val count = it.read(buffer)
                    if (count < 0) break
                    total += count; size += count
                    require(total <= BACKUP_LIMIT && size <= (if (name.endsWith(".json")) 32L * 1024 * 1024 else 16L * 1024 * 1024)) { "备份过大，请先减少缩略图缓存" }
                    zip.write(buffer, 0, count)
                }
            }
            zip.closeEntry()
        }
        entry("backup.json", JSONObject().put("format", "MAGLE-index").put("version", 1)
            .toString().byteInputStream())
        entry("index.json", copy.toString().byteInputStream())
        entry("folders.json", folders.toString().byteInputStream())
        items.keys().forEach { id ->
            require(backupAssetId.matches(id)) { "不支持的素材 ID" }
            thumbnail(id, items.getJSONObject(id).getLong("mtime"))?.takeIf(File::isFile)?.let {
                entry("thumbnails/$id.thumb", it.inputStream())
            }
        }
    }
}

/** Extract only whitelisted entries into a caller-owned temporary directory, with a zip-bomb limit. */
internal fun readIndexBackup(input: InputStream, directory: File): ImportedIndex {
    require(directory.mkdir()) { "无法创建导入临时目录" }
    try {
        var total = 0L
        val names = mutableSetOf<String>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                val asset = name.removePrefix("thumbnails/").removeSuffix(".thumb")
                require(!entry.isDirectory && names.add(name) &&
                    (name in setOf("backup.json", "index.json", "folders.json") ||
                        name == "thumbnails/$asset.thumb" && backupAssetId.matches(asset))) { "不是有效的 MAGLE 索引备份" }
                val file = File(directory, name)
                file.parentFile!!.mkdirs()
                file.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var size = 0L
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        total += count; size += count
                        require(total <= BACKUP_LIMIT && size <= (if (name.endsWith(".json")) 32L * 1024 * 1024 else 16L * 1024 * 1024)) { "备份文件过大" }
                        output.write(buffer, 0, count)
                    }
                }
                zip.closeEntry()
            }
        }
        val header = JSONObject(File(directory, "backup.json").readText())
        require(header.optString("format") == "MAGLE-index" && header.optInt("version") == 1) { "不支持的备份格式" }
        val index = JSONObject(File(directory, "index.json").readText())
        require(index.optInt("schemaVersion") == 2 && index.getJSONObject("items").keys().asSequence().all(backupAssetId::matches)) { "无效索引" }
        val items = index.getJSONObject("items")
        require(names.filter { it.startsWith("thumbnails/") }.all { items.has(it.removePrefix("thumbnails/").removeSuffix(".thumb")) }) { "缩略图与索引不匹配" }
        items.keys().forEach { id ->
            val item = items.getJSONObject(id)
            item.getString("name"); item.getString("ext"); item.getLong("mtime")
        }
        return ImportedIndex(index, JSONObject(File(directory, "folders.json").readText()), directory)
    } catch (error: Exception) {
        directory.deleteRecursively()
        throw error
    }
}
