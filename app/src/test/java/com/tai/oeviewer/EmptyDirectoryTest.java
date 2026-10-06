package com.tai.oeviewer;

import org.junit.Test;
import static org.junit.Assert.*;

public class EmptyDirectoryTest {
    @Test public void onlyConfirmedEmptyDirectoriesAreSkipped() throws Exception {
        assertEquals(java.util.Set.of("changed", "new", "unlisted"), LibraryLogic.refreshItemIds(
                java.util.List.of("changed", "new"), new org.json.JSONArray(java.util.List.of("unlisted", "changed"))));
        assertEquals(java.util.Set.of("new"), LibraryLogic.refreshItemIds(java.util.List.of("new"), new org.json.JSONArray()));
        assertTrue(LibraryLogic.refreshItemIds(java.util.List.of(), new org.json.JSONArray()).isEmpty());
        assertTrue(LibraryLogic.isConfirmedEmptyDirectory(404, 0));
        assertFalse(LibraryLogic.isConfirmedEmptyDirectory(404, 1));
        assertFalse(LibraryLogic.isConfirmedEmptyDirectory(403, 0));
        assertFalse(LibraryLogic.isConfirmedEmptyDirectory(429, 0));
        assertFalse(LibraryLogic.isConfirmedEmptyDirectory(500, 0));
    }
}
