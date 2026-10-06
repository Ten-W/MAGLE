package com.tai.oeviewer;
import org.json.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class FileActionsTest {
    @Test public void restorePreservesOriginalFileFoldersAndTags() throws Exception {
        JSONObject metadata = new JSONObject().put("name", "image").put("ext", "png")
                .put("folders", new JSONArray(List.of("F"))).put("tags", new JSONArray(List.of("T"))).put("custom", 42);
        assertTrue(LibraryLogic.setRecycled(metadata, "回收站"));
        assertTrue(metadata.getBoolean("isDeleted"));
        assertFalse(LibraryLogic.setRecycled(metadata, "还原"));
        assertFalse(metadata.getBoolean("isDeleted"));
        assertEquals("F", metadata.getJSONArray("folders").getString(0));
        assertEquals("T", metadata.getJSONArray("tags").getString(0));
        assertEquals("png", metadata.getString("ext"));
        assertEquals("image", metadata.getString("name"));
        assertEquals(42, metadata.getInt("custom"));
        assertFalse(LibraryLogic.setRecycled(metadata, "还原"));
        try { LibraryLogic.setRecycled(metadata, "删除"); fail(); }
        catch (IllegalArgumentException expected) { }
        assertFalse(metadata.getBoolean("isDeleted"));
    }

    @Test public void navigationNamesAndFolderMovesPreserveMetadata() throws Exception {
        assertEquals("图片", LibraryLogic.validName(" 图片 "));
        for (String bad : List.of("", "../x", "CON", "x.")) {
            try { LibraryLogic.validName(bad); fail(bad); } catch (IllegalArgumentException expected) { }
        }
        JSONObject child = new JSONObject().put("id", "B").put("name", "child").put("color", "red");
        JSONObject parent = new JSONObject().put("id", "A").put("name", "parent").put("children", new JSONArray().put(child));
        JSONObject metadata = new JSONObject().put("folders", new JSONArray().put(parent)).put("custom", 42);
        String before = metadata.toString();
        try { LibraryLogic.changeFolders(metadata, Set.of("A"), "B", null); fail(); } catch (IllegalArgumentException expected) { }
        assertEquals(before, metadata.toString());
        LibraryLogic.changeFolders(metadata, Set.of("B"), null, null);
        assertEquals(2, metadata.getJSONArray("folders").length());
        assertEquals(0, parent.getJSONArray("children").length());
        assertEquals("red", metadata.getJSONArray("folders").getJSONObject(1).getString("color"));
        LibraryLogic.changeFolders(metadata, Set.of("B"), null, "renamed");
        assertEquals("renamed", child.getString("name"));
        assertEquals(42, metadata.getInt("custom"));
    }
}
