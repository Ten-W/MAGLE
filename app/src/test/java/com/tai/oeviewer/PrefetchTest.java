package com.tai.oeviewer;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class PrefetchTest {
    @Test public void decodeTargetsCoverCropAndNeverUndersampleRequestedSize() {
        assertEquals(128, LibraryLogic.thumbnailEdge(1000, 1000, 100, false));
        assertEquals(256, LibraryLogic.thumbnailEdge(2000, 1000, 100, false));
        assertEquals(256, LibraryLogic.thumbnailEdge(1000, 2000, 100, true));
        assertEquals(600, LibraryLogic.thumbnailEdge(0, 0, 100, true));
        assertEquals(2, LibraryLogic.bitmapSampleSize(1000, 1000, 400));
        assertEquals(1, LibraryLogic.bitmapSampleSize(600, 400, 600));
        assertEquals(4, LibraryLogic.bitmapSampleSize(2048, 1024, 512));
        for (boolean waterfall : new boolean[] {false, true}) {
            for (int[] size : new int[][] {{1000,1000}, {2000,1000}, {1000,2000}, {4000,100}}) {
                int edge = LibraryLogic.thumbnailEdge(size[0], size[1], 100, waterfall);
                double ratio = waterfall ? Math.max(.55, Math.min(1.8, (double)size[0]/size[1])) : 1;
                assertTrue(edge >= Math.max(size[0], size[1]) * Math.max(100.0/size[0], 100.0/ratio/size[1]));
            }
        }
    }
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
