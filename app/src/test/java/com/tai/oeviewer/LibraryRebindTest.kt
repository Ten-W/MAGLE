package com.tai.oeviewer

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LibraryRebindTest {
    @Test fun preservesVersionsRejectsOtherLibrariesAndDropsChangedOrMissingItems() {
        val item = JSONObject().put("name", "a").put("ext", "png").put("size", 10)
            .put("width", 2).put("height", 3).put("mtime", 1)
        val old = JSONObject().put("items", JSONObject().put("a", item).put("b", item).put("c", item).put("d", item).put("extra", item))
        val samples = mapOf("a" to item, "b" to item, "c" to item)
        val result = rebindIndex(old, null, JSONObject().put("a", 1).put("b", 2).put("c", 1).put("d", 1), samples, "new", "Library")
        assertEquals(3, result.getJSONObject("items").length())
        assertFalse(result.getJSONObject("items").has("extra"))
        assertFalse(result.getJSONObject("items").has("b"))
        assertEquals("new", result.getString("libraryId"))
        assertEquals(5, old.getJSONObject("items").length())
        assertThrows(IllegalArgumentException::class.java) {
            rebindIndex(old, null, JSONObject().put("other", 1), emptyMap(), "new", "Library")
        }
        assertThrows(IllegalArgumentException::class.java) {
            rebindIndex(old, null, JSONObject().put("a", 1).put("b", 1).put("c", 1).put("d", 1),
                samples + ("a" to JSONObject(item.toString()).put("size", 20)), "new", "Library")
        }
    }
}
