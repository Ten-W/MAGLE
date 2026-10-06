package com.tai.oeviewer;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class PrefetchTest {
    @Test public void nearbyOnlyAndBounded() {
        assertEquals(List.of(4, 5, 6, 7, 8, 9), LibraryLogic.prefetchIndices(10, 0, 3));
        assertEquals(List.of(1, 0), LibraryLogic.prefetchIndices(10, 2, 9));
        assertTrue(LibraryLogic.prefetchIndices(0, -1, -1).isEmpty());
        assertEquals(12, LibraryLogic.prefetchIndices(100, 20, 30).size());
        assertFalse(LibraryLogic.prefetchIndices(100, 20, 30).contains(25));
    }

    @Test public void shareRequiresImageContentUri() {
        assertEquals("image/png", LibraryLogic.shareMimeType(List.of("image/png")));
        assertEquals("image/*", LibraryLogic.shareMimeType(List.of("image/png", "image/jpeg")));
        assertEquals("*/*", LibraryLogic.shareMimeType(List.of("image/png", "application/pdf")));
        assertEquals("*/*", LibraryLogic.shareMimeType(List.of()));
        assertTrue(LibraryLogic.isSharedImage("content", "image/png"));
        assertFalse(LibraryLogic.isSharedImage("file", "image/png"));
        assertFalse(LibraryLogic.isSharedImage("https", "image/png"));
        assertFalse(LibraryLogic.isSharedImage("content", "text/plain"));
        assertFalse(LibraryLogic.isSharedImage("content", null));
    }
}
