package com.tai.oeviewer

import org.json.JSONObject

/** Reuse only manifest-confirmed versions; never carry source-only or changed items across. */
internal fun rebindIndex(old: JSONObject, target: JSONObject?, manifest: JSONObject,
                         samples: Map<String, JSONObject>, id: String, name: String): JSONObject {
    val items = old.getJSONObject("items")
    val common = items.keys().asSequence().filter { it != "all" && manifest.has(it) }.toList()
    val targetCount = manifest.keys().asSequence().count { it != "all" }
    val minimum = minOf(3, items.length(), targetCount)
    require(minimum > 0 && common.size >= minimum &&
        common.size.toDouble() / maxOf(items.length(), targetCount) >= 0.8) {
        "目标库与原库素材 ID 不匹配；未切换，请确认是同一份库"
    }
    require(samples.size >= minimum && samples.keys.all { it in common }) { "未完成目标库核对" }
    samples.forEach { (assetId, sample) ->
        val cached = items.getJSONObject(assetId)
        require(listOf("name", "ext", "size", "width", "height").all {
            cached.optString(it) == sample.optString(it)
        }) { "目标素材与原库不一致：$assetId；未切换" }
    }
    val reused = JSONObject()
    common.forEach { assetId ->
        val candidate = target?.optJSONObject("items")?.optJSONObject(assetId)
            ?.takeIf { it.optLong("mtime", -1) == manifest.optLong(assetId, -2) }
            ?: items.getJSONObject(assetId).takeIf { it.optLong("mtime", -1) == manifest.optLong(assetId, -2) }
        if (candidate != null) reused.put(assetId, candidate)
    }
    return JSONObject().put("schemaVersion", 2).put("libraryId", id).put("libraryName", name)
        .put("items", reused).put("ignored", JSONObject()).put("scanTotal", targetCount)
        .put("failedCount", targetCount - reused.length()).put("unlisted", org.json.JSONArray())
}
