package com.tai.oeviewer

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

internal class LibraryConflict(message: String = "文件已被其他设备修改，请更新索引后重试；未覆盖对方修改") : IOException(message)
internal data class VersionedJson(val bytes: ByteArray, val version: String)
internal data class JsonChange(val before: ByteArray, val after: ByteArray)

internal fun requireStrongEtag(value: String?): String = value.orEmpty().also {
    check(it.length > 2 && it.startsWith('"') && it.endsWith('"')) {
        "WebDAV 未提供强 ETag，不能保证多设备安全写入；请使用支持版本校验的服务器"
    }
}

/** Only merge operations such as mtime may replay against a newer revision. */
internal fun updateVersionedJson(read: () -> VersionedJson, write: (VersionedJson, ByteArray) -> Unit,
    merge: Boolean = false, mutate: (JSONObject) -> Unit): JsonChange {
    repeat(if (merge) 5 else 1) {
        val snapshot = read()
        val json = JSONObject(String(snapshot.bytes, Charsets.UTF_8))
        mutate(json)
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        try {
            write(snapshot, bytes)
            return JsonChange(snapshot.bytes, bytes)
        } catch (conflict: LibraryConflict) {
            if (!merge) throw conflict
        }
    }
    throw LibraryConflict("其他设备持续修改清单，请稍后重试；未覆盖对方修改")
}

internal fun mergeMtime(json: JSONObject, id: String, now: Long) {
    json.put(id, maxOf(json.optLong(id), now))
        .put("all", maxOf(json.optLong("all"), now))
}

/** A durable list of item IDs, not a count: a lost response is safe to replay. */
// ponytail: one global rename task at a time; per-library queues only if simultaneous bulk editing is needed.
internal class TagRenameTask(val json: JSONObject) {
    val old: String get() = json.getString("old")
    val name: String get() = json.getString("name")
    val ids: List<String> = strings("ids")
    private val completed = strings("done").toMutableSet()
    val done: Set<String> get() = completed.toSet()
    init {
        LibraryLogic.validTagName(old)
        LibraryLogic.validTagName(name)
        require(ids.size == ids.distinct().size && ids.all { it.matches(Regex("[A-Za-z0-9_-]{1,128}")) })
        require(completed.all { it in ids })
    }
    fun matches(source: String, libraryId: String?, driveId: String?) =
        json.getString("source") == source && json.getString("libraryId") == libraryId &&
            (source != "ONEDRIVE" || json.optString("driveId") == driveId.orEmpty())
    fun complete(id: String) {
        require(id in ids)
        if (completed.add(id)) json.put("done", JSONArray(completed))
    }
    private fun strings(key: String): List<String> = json.getJSONArray(key).let { array ->
        (0 until array.length()).map { array.getString(it) }
    }
}
