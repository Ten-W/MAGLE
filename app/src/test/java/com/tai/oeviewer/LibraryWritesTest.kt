package com.tai.oeviewer

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LibraryWritesTest {
    @Test fun conflictDoesNotReplayAnEditButManifestRebasesAndPreservesOthers() {
        var current = "{\"other\":77,\"all\":77}".toByteArray()
        var revision = 1
        var reads = 0
        var writes = 0
        val read = { reads++; VersionedJson(current.copyOf(), revision.toString()) }
        val write: (VersionedJson, ByteArray) -> Unit = { snapshot, bytes ->
            writes++
            if (writes == 1) {
                current = "{\"other\":88,\"all\":88,\"newFromComputer\":200}".toByteArray()
                revision++
            }
            if (snapshot.version != revision.toString()) throw LibraryConflict()
            current = bytes
            revision++
        }
        try { updateVersionedJson(read, write) { it.put("name", "phone") }; fail("Conflict overwritten") }
        catch (_: LibraryConflict) { }
        assertEquals(1, reads)
        assertFalse(JSONObject(String(current)).has("name"))
        reads = 0; writes = 0
        updateVersionedJson(read, write, merge = true) { mergeMtime(it, "phone", 100) }
        assertEquals(2, reads)
        val saved = JSONObject(String(current))
        assertEquals(88L, saved.getLong("other"))
        assertEquals(200L, saved.getLong("newFromComputer"))
        assertEquals(100L, saved.getLong("phone"))
        assertEquals(100L, saved.getLong("all"))
        mergeMtime(saved, "phone", 1)
        assertEquals(100L, saved.getLong("phone"))
    }

    @Test fun retryIsBoundedAndWeakOrMissingVersionsFailClosed() {
        var attempts = 0
        try {
            updateVersionedJson({ VersionedJson("{}".toByteArray(), "1") }, { _, _ -> attempts++; throw LibraryConflict() }, true) { mergeMtime(it, "x", 3) }
            fail("Unbounded or ignored conflict")
        } catch (_: LibraryConflict) { }
        assertEquals(5, attempts)
        assertEquals("\"revision\"", requireStrongEtag("\"revision\""))
        listOf(null, "", "W/\"weak\"", "not-an-etag").forEach {
            try { requireStrongEtag(it); fail("Unsafe ETag accepted") } catch (_: IllegalStateException) { }
        }
    }

    @Test fun interruptedTagRenameKeepsIdsAndDoesNotLeakAcrossLibraries() {
        val state = JSONObject().put("source", "ONEDRIVE").put("libraryId", "library-1").put("driveId", "drive-1")
            .put("old", "旧标签").put("name", "新标签").put("ids", JSONArray(listOf("a", "b", "c"))).put("done", JSONArray())
        val task = TagRenameTask(state)
        task.complete("a")
        task.complete("a")
        val resumed = TagRenameTask(JSONObject(task.json.toString()))
        assertEquals(listOf("b", "c"), resumed.ids.filterNot { it in resumed.done })
        assertEquals(setOf("a"), resumed.done)
        assertTrue(resumed.matches("ONEDRIVE", "library-1", "drive-1"))
        assertFalse(resumed.matches("ONEDRIVE", "library-1", "drive-2"))
        assertFalse(resumed.matches("SMB", "library-1", "drive-1"))
        assertFalse(resumed.matches("ONEDRIVE", "library-2", "drive-1"))
        try { resumed.complete("outside"); fail("Unknown item accepted") } catch (_: IllegalArgumentException) { }
        val asset = JSONObject("{\"tags\":[\"旧标签\",\"保留\"],\"folders\":[\"folder\"]}")
        LibraryLogic.renameTagReferences(asset, resumed.old, resumed.name)
        val once = asset.toString()
        LibraryLogic.renameTagReferences(asset, resumed.old, resumed.name)
        assertEquals(once, asset.toString())
        assertEquals("folder", asset.getJSONArray("folders").getString(0))
    }
}
