package com.tai.oeviewer

import org.junit.Assert.*
import org.junit.Test

class BrowseLocationTest {
    @Test fun folderBackFollowsHierarchyAndPreservesSearchAndSavedView() {
        val all = BrowseLocation(1, LibrarySection.ALL, null, null, "全部", "", false, AssetViewMode.LIST)
        val parent = all.copy(key = 2, section = LibrarySection.FOLDER, folderId = "parent", name = "父目录")
        val child = parent.copy(key = 3, folderId = "child", name = "子目录")
        // Sidebar deep links must return to the parent, even if it was never visited.
        assertEquals(parent.copy(key = 4), child.backTarget(listOf(all), "parent" to "父目录", 4))
        assertEquals(parent.copy(key = 4), child.backTarget(emptyList(), "parent" to "父目录", 4))
        // Reuse the parent's key so its saved scroll position survives.
        assertEquals(parent, child.backTarget(listOf(all, parent), "parent" to "父目录", 4))
        assertEquals(all, parent.backTarget(listOf(all), null, 4))
        assertEquals(all.copy(key = 4), parent.backTarget(emptyList(), null, 4))
        assertEquals(all, parent.copy(folderId = null).backTarget(listOf(all), null, 4))
        val search = all.copy(key = 5, query = "壁纸")
        assertEquals(search, child.backTarget(listOf(all, search), "parent" to "父目录", 6))
        assertEquals(all, all.copy(section = LibrarySection.TAG, tag = "标签").backTarget(listOf(all), null, 4))
        assertNull(all.backTarget(emptyList(), null, 4))
    }
    @Test fun historyDistinguishesDestinationsAndPreservesPreviousState() {
        val all = BrowseLocation(1, LibrarySection.ALL, null, null, "全部", "test", false, AssetViewMode.LIST)
        val tag = all.copy(key = 2, section = LibrarySection.TAG, tag = "A", name = "A", query = "")
        val folder = tag.copy(key = 3, section = LibrarySection.FOLDER, folderId = "child", tag = null)
        assertFalse(all.sameDestination(tag))
        assertFalse(tag.sameDestination(folder))
        assertTrue(folder.sameDestination(folder.copy(key = 4, query = "other", mode = AssetViewMode.GRID)))
        assertFalse(folder.sameDestination(folder.copy(folderId = "grandchild")))
        assertFalse(folder.sameDestination(folder.copy(folderId = null)))
        assertFalse(tag.sameDestination(tag.copy(section = LibrarySection.TAGGROUP)))
        assertFalse(tag.sameDestination(tag.copy(section = LibrarySection.TAGGROUPS, tag = null)))
        AssetViewMode.entries.forEach { assertEquals(it, AssetViewMode.valueOf(it.name)) }
        var history = listOf(all, tag)
        assertEquals(tag, history.last())
        history = history.dropLast(1)
        assertEquals("test", history.last().query)
        assertEquals(AssetViewMode.LIST, history.last().mode)
        assertTrue(history.dropLast(1).isEmpty())
    }
}
