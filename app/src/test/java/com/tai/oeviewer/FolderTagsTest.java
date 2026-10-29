package com.tai.oeviewer;

import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Test;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;

public class FolderTagsTest {
    @Test public void deletingFoldersPreservesOtherMembershipAndNeverDeletesFiles() throws Exception {
        JSONObject metadata = new JSONObject("{\"folders\":[{\"id\":\"P\",\"name\":\"Parent\",\"children\":[{\"id\":\"C\",\"name\":\"Child\"}]},{\"id\":\"Other\",\"name\":\"Keep\"}]}");
        JSONArray snapshot = LibraryLogic.folderDeletionSnapshot(metadata, Set.of("P", "C"));
        assertEquals(1, snapshot.length());
        assertEquals(Set.of("P", "C"), LibraryLogic.folderIds(snapshot));
        JSONObject asset = new JSONObject("{\"name\":\"image\",\"folders\":[\"C\",\"Other\"],\"tags\":[\"Tag\"],\"isDeleted\":false}");
        LibraryLogic.removeFolderReferences(asset, LibraryLogic.folderIds(snapshot), false);
        assertEquals("Other", asset.getJSONArray("folders").getString(0));
        assertEquals("Tag", asset.getJSONArray("tags").getString(0));
        assertFalse(asset.getBoolean("isDeleted"));
        LibraryLogic.removeFolders(metadata, snapshot);
        assertNull(LibraryLogic.findFolder(metadata.getJSONArray("folders"), "C"));
        assertNotNull(LibraryLogic.findFolder(metadata.getJSONArray("folders"), "Other"));
        LibraryLogic.removeFolders(metadata, snapshot); // Lost successful response can replay.
        JSONObject recycled = new JSONObject("{\"folders\":[\"C\",\"Other\"],\"name\":\"original\"}");
        LibraryLogic.removeFolderReferences(recycled, Set.of("C"), true);
        assertTrue(recycled.getBoolean("isDeleted"));
        assertEquals("original", recycled.getString("name"));
        assertEquals("Other", recycled.getJSONArray("folders").getString(0));
        JSONObject unfiled = new JSONObject("{\"folders\":[\"C\"]}");
        LibraryLogic.removeFolderReferences(unfiled, Set.of("C"), false);
        assertEquals(0, unfiled.getJSONArray("folders").length());
    }

    @Test public void folderDeleteDoesNotDropNewOrMovedChildren() throws Exception {
        JSONObject metadata = new JSONObject("{\"folders\":[{\"id\":\"P\",\"children\":[{\"id\":\"C\"}]}]}");
        JSONArray snapshot = LibraryLogic.folderDeletionSnapshot(metadata, Set.of("P"));
        metadata.getJSONArray("folders").getJSONObject(0).getJSONArray("children").put(new JSONObject().put("id", "New"));
        try { LibraryLogic.removeFolders(metadata, snapshot); fail(); } catch (IllegalArgumentException expected) { }
        assertNotNull(LibraryLogic.findFolder(metadata.getJSONArray("folders"), "New"));
        JSONObject moved = new JSONObject("{\"folders\":[{\"id\":\"C\"}]}");
        try { LibraryLogic.removeFolders(moved, snapshot); fail(); } catch (IllegalArgumentException expected) { }
        assertNotNull(LibraryLogic.findFolder(moved.getJSONArray("folders"), "C"));
    }

    @Test public void colorsAndTagMovesOnlyChangeRequestedMetadata() throws Exception {
        JSONObject metadata = new JSONObject("{\"folders\":[{\"id\":\"F\",\"name\":\"Keep\",\"icon\":\"custom\",\"children\":[]}],\"tagsGroups\":[{\"id\":\"A\",\"name\":\"One\",\"tags\":[\"X\",\"Keep\"]},{\"id\":\"B\",\"tags\":[\"Y\"]}]}");
        LibraryLogic.setFolderColors(metadata, Set.of("F"), "aqua");
        assertEquals("aqua", metadata.getJSONArray("folders").getJSONObject(0).getString("iconColor"));
        assertEquals("custom", metadata.getJSONArray("folders").getJSONObject(0).getString("icon"));
        LibraryLogic.setTagGroupColors(metadata, Set.of("A"), "red");
        LibraryLogic.moveTags(metadata, Set.of("X"), "B");
        assertEquals("Keep", metadata.getJSONArray("tagsGroups").getJSONObject(0).getJSONArray("tags").getString(0));
        assertEquals("red", metadata.getJSONArray("tagsGroups").getJSONObject(0).getString("color"));
        assertEquals(2, metadata.getJSONArray("tagsGroups").getJSONObject(1).getJSONArray("tags").length());
        LibraryLogic.moveTags(metadata, Set.of("X"), "B");
        assertEquals(2, metadata.getJSONArray("tagsGroups").getJSONObject(1).getJSONArray("tags").length());
        LibraryLogic.moveTags(metadata, Set.of("X"), null);
        assertEquals(1, metadata.getJSONArray("tagsGroups").getJSONObject(1).getJSONArray("tags").length());
        String before = metadata.toString();
        try { LibraryLogic.moveTags(metadata, Set.of("Keep"), "missing"); fail(); } catch (IllegalArgumentException expected) { }
        assertEquals(before, metadata.toString());
        try { LibraryLogic.setFolderColors(metadata, Set.of("F"), "invalid"); fail(); } catch (IllegalArgumentException expected) { }
        assertEquals(before, metadata.toString());
    }

    @Test public void folderCountsUnionDescendantsWithoutCountingSameAssetTwice() {
        java.util.Map<String, Set<String>> items = java.util.Map.of("parent", Set.of("A", "B"), "child", Set.of("B", "C"));
        assertEquals(3, LibraryLogic.folderAssetCount(items, List.of("parent", "child", "missing")));
        assertEquals(2, LibraryLogic.folderAssetCount(items, List.of("child")));
        assertEquals(0, LibraryLogic.folderAssetCount(items, List.of()));
    }
    @Test public void renamesGroupWithoutChangingIdColorOrMembership() throws Exception {
        assertEquals("UI/Web", LibraryLogic.validTagName(" UI/Web "));
        try { LibraryLogic.validTagName("\n\t"); fail(); }
        catch (IllegalArgumentException expected) { }
        JSONObject group = new JSONObject().put("id", "G").put("name", "Old").put("color", "pink")
                .put("tags", new JSONArray(List.of("A", "B"))).put("extra", 42);
        JSONObject metadata = new JSONObject().put("tagsGroups", new JSONArray().put(group));
        LibraryLogic.renameTagGroup(metadata, "G", " New ");
        assertEquals("New", group.getString("name"));
        assertEquals("G", group.getString("id"));
        assertEquals("pink", group.getString("color"));
        assertEquals("B", group.getJSONArray("tags").getString(1));
        assertEquals(42, group.getInt("extra"));
        try { LibraryLogic.renameTagGroup(metadata, "missing", "Name"); fail(); }
        catch (IllegalArgumentException expected) { }
        assertEquals("New", group.getString("name"));
    }

    @Test public void renamesAssetAndAllGroupTagReferencesWithoutLosingOtherFields() throws Exception {
        JSONObject asset = new JSONObject().put("tags", new JSONArray(List.of("Old", "Keep"))).put("folders", new JSONArray(List.of("F")));
        assertEquals(List.of("New", "Keep"), LibraryLogic.renameTagReferences(asset, "Old", "New"));
        assertEquals("F", asset.getJSONArray("folders").getString(0));
        JSONObject group = new JSONObject().put("id", "G").put("name", "Group").put("color", "blue")
                .put("tags", new JSONArray(List.of("Old", "Keep")));
        JSONObject metadata = new JSONObject().put("tagsGroups", new JSONArray().put(group)).put("folders", new JSONArray());
        LibraryLogic.renameTagReferences(metadata, "Old", "New");
        assertEquals("New", group.getJSONArray("tags").getString(0));
        assertEquals("Keep", group.getJSONArray("tags").getString(1));
        assertEquals("Group", group.getString("name"));
        assertEquals("blue", group.getString("color"));
    }

    @Test public void folderIconsUseEagleMetadataAndPreflightBeforeChangingAnyFolder() throws Exception {
        JSONObject folder = new JSONObject().put("id", "F").put("name", "TenW").put("iconColor", "blue").put("children", new JSONArray());
        JSONObject metadata = new JSONObject().put("folders", new JSONArray().put(folder));
        LibraryLogic.setFolderIcons(metadata, Set.of("F"), "demon");
        assertEquals("demon", new JSONObject(metadata.toString()).getJSONArray("folders").getJSONObject(0).getString("icon"));
        assertEquals("blue", folder.getString("iconColor"));
        try { LibraryLogic.setFolderIcons(metadata, Set.of("F", "missing"), "star"); fail(); }
        catch (IllegalArgumentException expected) { }
        assertEquals("demon", folder.getString("icon"));
        LibraryLogic.setFolderIcons(metadata, Set.of("F"), "");
        assertFalse(folder.has("icon"));
    }

    @Test public void directAndDescendantFiltersAndEagleGroups() throws Exception {
        JSONObject metadata = new JSONObject().put("tags", new JSONArray(List.of("old", "new"))).put("name", "keep");
        assertEquals(List.of("old", "new", "added"), LibraryLogic.appendTags(metadata, List.of("new", " added ", "")));
        assertEquals(List.of("old", "new", "added"), LibraryLogic.appendTags(metadata, List.of("added")));
        assertEquals("keep", metadata.getString("name"));
        assertEquals(List.of("first"), LibraryLogic.appendTags(new JSONObject(), List.of("first")));
        var sections = LibraryLogic.tagSections(List.of("FORM_B", "brand_z", "BRAND_A", "canvas", "中文", "1tag", "canvas", ""));
        assertEquals(List.of("B", "C", "F", "#"), List.copyOf(sections.keySet()));
        assertEquals(List.of("BRAND_A", "brand_z"), sections.get("B"));
        assertEquals(List.of("canvas"), sections.get("C"));
        assertEquals(2, sections.get("#").size());
        assertTrue(LibraryLogic.tagSections(List.of()).isEmpty());
        assertFalse(LibraryLogic.belongsToAnyFolder(Set.of("child"), Set.of("parent")));
        assertTrue(LibraryLogic.belongsToAnyFolder(Set.of("child", "other"), Set.of("parent", "child", "grandchild")));
        assertTrue(LibraryLogic.belongsToAnyFolder(Set.of("parent"), Set.of("parent", "child")));
        assertFalse(LibraryLogic.belongsToAnyFolder(Set.of(), Set.of("parent", "child")));
        // One asset assigned to both parent and child must count only once.
        assertEquals(2L, List.of(Set.of("parent", "child"), Set.of("grandchild"), Set.of("other"))
                .stream().filter(folders -> LibraryLogic.belongsToAnyFolder(folders, Set.of("parent", "child", "grandchild"))).count());
        assertTrue(LibraryLogic.tagGroups(new JSONObject()).isEmpty());
        JSONObject source = new JSONObject().put("tagsGroups", new JSONArray()
                .put(new JSONObject().put("id", "A").put("name", "FORM").put("color", "pink")
                        .put("tags", new JSONArray().put("方正").put("方正").put(7).put("")))
                .put("invalid").put(new JSONObject().put("id", "A").put("name", "duplicate"))
                .put(new JSONObject().put("id", "B").put("name", "Empty")));
        List<JSONObject> groups = LibraryLogic.tagGroups(source);
        assertEquals(2, groups.size());
        assertEquals("pink", groups.get(0).getString("color"));
        assertEquals(1, groups.get(0).getJSONArray("tags").length());
        assertEquals("方正", groups.get(0).getJSONArray("tags").getString(0));
        assertEquals(0, groups.get(1).getJSONArray("tags").length());
        assertEquals(4, source.getJSONArray("tagsGroups").length());
    }
}
