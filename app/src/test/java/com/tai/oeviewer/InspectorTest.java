package com.tai.oeviewer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class InspectorTest {
    @Test public void editsOnlyChosenFieldAndReadsExistingPalette() throws Exception {
        JSONObject meta = new JSONObject().put("id", "A").put("annotation", "保留注释")
                .put("folders", new JSONArray(List.of("F1", "F2"))).put("custom", new JSONObject().put("value", 42));
        LibraryLogic.setMetadataList(meta, "tags", List.of(" 标签 ", "标签", "", "另一个"));
        assertEquals(2, meta.getJSONArray("tags").length());
        assertEquals("标签", meta.getJSONArray("tags").getString(0));
        assertEquals("另一个", meta.getJSONArray("tags").getString(1));
        assertEquals(2, meta.getJSONArray("folders").length());
        assertEquals("保留注释", meta.getString("annotation"));
        assertEquals(42, meta.getJSONObject("custom").getInt("value"));
        LibraryLogic.setMetadataList(meta, "folders", List.of());
        assertEquals(0, meta.getJSONArray("folders").length());
        try { LibraryLogic.setMetadataList(meta, "name", List.of("bad")); fail("Unexpected field allowed"); }
        catch (IllegalArgumentException expected) { }
        assertTrue(LibraryLogic.paletteColors(meta).isEmpty());
        meta.put("palettes", new JSONArray().put(new JSONObject().put("color", new JSONArray(List.of(227,225,227))))
                .put(new JSONObject().put("color", new JSONArray(List.of(999,0,0)))).put("invalid"));
        assertEquals(List.of(0xffe3e1e3), LibraryLogic.paletteColors(meta));
    }
}
