package com.tai.oeviewer;

import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Test;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;

public class FolderTagsTest {
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
