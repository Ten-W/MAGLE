package com.tai.oeviewer

import org.junit.Assert.*
import org.junit.Test

class BrowseLocationTest {
    @Test fun folderBackOnlyFollowsHierarchyAndPreservesSavedView() {
        val all = BrowseLocation(1, LibrarySection.ALL, null, null, "全部", "", false, AssetViewMode.LIST)
        val parent = all.copy(key = 2, section = LibrarySection.FOLDER, folderId = "parent", name = "父目录")
        val child = parent.copy(key = 3, folderId = "child", name = "子目录")
        // Sidebar deep links must return to the parent, even if it was never visited.
        assertEquals(parent.copy(key = 4), child.backTarget(listOf(all), "parent" to "父目录", 4))
        assertEquals(parent.copy(key = 4), child.backTarget(emptyList(), "parent" to "父目录", 4))
        // Reuse the parent's key so its saved scroll position survives.
        assertEquals(parent, child.backTarget(listOf(all, parent), "parent" to "父目录", 4))
        val root = all.copy(key = 4, section = LibrarySection.FOLDER, name = "文件夹")
        assertEquals(root, parent.backTarget(listOf(all), null, 4))
        assertEquals(root, parent.backTarget(emptyList(), null, 4))
        assertEquals(root, parent.backTarget(listOf(all, root), null, 5))
        assertNull(root.backTarget(listOf(all), null, 5))
        assertNull(root.backTarget(emptyList(), null, 5))
        assertNull(parent.copy(folderId = null).backTarget(listOf(all), null, 4))
        val search = all.copy(key = 5, query = "壁纸")
        assertEquals(parent.copy(key = 6), child.backTarget(listOf(all, search), "parent" to "父目录", 6))
        assertEquals(LibrarySection.TAGGROUPS, all.copy(section = LibrarySection.TAG, tag = "标签").backTarget(listOf(all), null, 4)?.section)
        assertNull(all.backTarget(emptyList(), null, 4))
    }
    @Test fun tagsReturnThroughGroupAndManagementWithoutReplayingOtherPages() {
        val root = BrowseLocation(1, LibrarySection.TAGGROUPS, null, null, "标签管理", "", false, AssetViewMode.LIST)
        val group = root.copy(key = 2, section = LibrarySection.TAGGROUP, tag = "g", name = "编组")
        val tag = root.copy(key = 3, section = LibrarySection.TAG, tag = "A", name = "A")
        assertNull(root.tagParentId()) // Total list must not invent an ungrouped parent.
        assertEquals("g", group.tagParentId())
        assertEquals("sidebar-group", root.tagParentId("sidebar-group"))
        assertEquals(root, tag.backTarget(listOf(root), null, 4))
        assertEquals(group, tag.backTarget(listOf(root, group), "g" to "编组", 4))
        assertEquals(root, group.backTarget(listOf(root), null, 5))
        assertNull(root.backTarget(listOf(tag, group), null, 6))
        assertEquals(group.copy(key = 4), tag.backTarget(emptyList(), "g" to "编组", 4))
        assertEquals(root.copy(key = 5), tag.backTarget(emptyList(), null, 5))
        assertEquals(root.copy(key = 6), group.backTarget(emptyList(), null, 6))
    }
    @Test fun childParentRootStopsWithoutReturningToPreviousSection() {
        val all = BrowseLocation(1, LibrarySection.ALL, null, null, "全部", "", false, AssetViewMode.WATERFALL)
        val root = all.copy(key = 2, section = LibrarySection.FOLDER, name = "文件夹")
        val parent = root.copy(key = 3, folderId = "obsidian", name = "Obsidian")
        val child = parent.copy(key = 4, folderId = "note", name = "笔记")
        assertEquals(parent, child.backTarget(listOf(all, root, parent), "obsidian" to "Obsidian", 5))
        assertEquals(root, parent.backTarget(listOf(all, root), null, 6))
        assertNull(root.backTarget(listOf(all), null, 7))
        val tag = all.copy(key = 8, section = LibrarySection.TAG, tag = "tag")
        assertEquals(root, parent.backTarget(listOf(tag, root), null, 9))
        assertNull(root.backTarget(listOf(tag), null, 10))
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
