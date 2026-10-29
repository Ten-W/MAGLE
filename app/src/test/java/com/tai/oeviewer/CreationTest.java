package com.tai.oeviewer;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class CreationTest {
    @Test public void destinationsOnlyFollowTheCurrentFolderOrGroup() {
        for (String section : new String[]{"ALL", "UNFILED", "UNTAGGED", "TRASH", "TAG", "TAGGROUPS"}) {
            assertNull(LibraryLogic.creationTarget(section, "stale-selection", true));
            assertNull(LibraryLogic.creationTarget(section, "stale-selection", false));
        }
        assertEquals("child", LibraryLogic.creationTarget("FOLDER", "child", true));
        assertNull(LibraryLogic.creationTarget("FOLDER", null, true));
        assertEquals("group", LibraryLogic.creationTarget("TAGGROUP", "group", false));
        assertNull(LibraryLogic.creationTarget("TAGGROUP", "magle:ungrouped", false));
        assertNull(LibraryLogic.creationTarget("FOLDER", "child", false));
        assertNull(LibraryLogic.creationTarget("TAGGROUP", "group", true));
    }
    @Test public void creationSurvivesReloadAndRejectsMissingParentsWithoutPartialWrites() throws Exception {
        JSONObject root = new JSONObject("{\"folders\":[{\"id\":\"P\",\"name\":\"Parent\",\"children\":[]}],\"tagsGroups\":[{\"id\":\"G\",\"name\":\"Group\",\"tags\":[]}]}");
        LibraryLogic.createFolder(root, "C", " Child ", "P");
        LibraryLogic.createFolder(root, "R", "Root", null);
        LibraryLogic.createTag(root, "Grouped", "G");
        LibraryLogic.createTag(root, "Loose", null);
        JSONObject reload = new JSONObject(root.toString());
        assertEquals("Child", LibraryLogic.findFolder(reload.getJSONArray("folders"), "P").getJSONArray("children").getJSONObject(0).getString("name"));
        assertEquals("Root", reload.getJSONArray("folders").getJSONObject(1).getString("name"));
        assertEquals("Loose", reload.getJSONArray("tags").getString(0));
        assertEquals("Grouped", LibraryLogic.tagGroups(reload).get(0).getJSONArray("tags").getString(0));
        String before = reload.toString();
        try { LibraryLogic.createFolder(reload, "X", "Lost", "missing"); fail(); } catch (IllegalArgumentException expected) { }
        try { LibraryLogic.createFolder(reload, "Y", "child", "P"); fail(); } catch (IllegalArgumentException expected) { }
        try { LibraryLogic.createTag(reload, "Lost", "missing"); fail(); } catch (IllegalArgumentException expected) { }
        try { LibraryLogic.createTag(reload, "Loose", "G"); fail(); } catch (IllegalArgumentException expected) { }
        assertEquals(before, reload.toString());
        LibraryLogic.renameTagReferences(reload, "Loose", "Renamed");
        LibraryLogic.deleteTagReferences(reload, java.util.Set.of("Renamed"));
        assertEquals(0, reload.getJSONArray("tags").length());
        LibraryLogic.moveTags(reload, java.util.Set.of("Grouped"), null);
        assertEquals("Grouped", reload.getJSONArray("tags").getString(0));
        assertEquals(0, LibraryLogic.tagGroups(reload).get(0).getJSONArray("tags").length());
        LibraryLogic.moveTags(reload, java.util.Set.of("Grouped"), "G");
        assertEquals("Grouped", LibraryLogic.tagGroups(reload).get(0).getJSONArray("tags").getString(0));
    }
}
