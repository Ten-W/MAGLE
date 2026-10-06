package com.tai.oeviewer;

import org.junit.Test;
import static org.junit.Assert.*;

public class ViewerZoomTest {
    @Test public void refreshFeedbackDoesNotCallPartialReadsSuccessful() {
        assertEquals("刷新完成 · 索引已更新", LibraryLogic.refreshResultMessage(0));
        assertEquals("刷新未完成 · 仍有 3 项待读取", LibraryLogic.refreshResultMessage(3));
    }
    @Test public void refreshContentFollowsPullAndReturnsToTop() {
        assertEquals(0f, LibraryLogic.refreshContentOffset(0f, 80f), .001f);
        assertEquals(40f, LibraryLogic.refreshContentOffset(.5f, 80f), .001f);
        assertEquals(80f, LibraryLogic.refreshContentOffset(1f, 80f), .001f);
        assertEquals(160f, LibraryLogic.refreshContentOffset(3f, 80f), .001f);
        assertEquals(0f, LibraryLogic.refreshContentOffset(-1f, 80f), .001f);
        assertEquals(0f, LibraryLogic.refreshContentOffset(Float.NaN, 80f), .001f);
    }
    @Test public void inspectorAdaptsToAvailableWidth() {
        assertFalse(LibraryLogic.usesSideInspector(393f));
        assertFalse(LibraryLogic.usesSideInspector(699f));
        assertTrue(LibraryLogic.usesSideInspector(700f));
        assertTrue(LibraryLogic.usesSideInspector(1280f));
    }
    @Test public void clampsListHeightAndFitsSixColumns() {
        assertEquals(88f / 3f, LibraryLogic.resizeCell(88f, .1f, 88f / 3f, 88f), .001f);
        assertEquals(88f, LibraryLogic.resizeCell(88f, 10f, 88f / 3f, 88f), .001f);
        assertEquals(3, LibraryLogic.columnCount(393f, 112f));
        assertEquals(6, LibraryLogic.columnCount(393f, 32f));
        assertEquals(6, LibraryLogic.columnCount(320f, 32f));
        assertEquals(8, LibraryLogic.columnCount(800f, 32f));
        for (float width : new float[] {600f, 800f, 1280f}) {
            float smallest = LibraryLogic.resizeGalleryCell(width, 140f, .01f, 260f);
            float largest = LibraryLogic.resizeGalleryCell(width, smallest, 100f, 260f);
            assertEquals(8, LibraryLogic.columnCount(width, smallest));
            assertEquals(2, LibraryLogic.columnCount(width, largest));
            assertTrue(LibraryLogic.resizeGalleryCell(width, smallest, 1.1f, 260f) > smallest);
            assertTrue(LibraryLogic.resizeGalleryCell(width, largest, .9f, 260f) < largest);
        }
        assertEquals(220f, LibraryLogic.resizeGalleryCell(393f, 112f, 10f, 220f), .001f);
        assertEquals(1, LibraryLogic.columnCount(180f, 220f));
        assertEquals(4, LibraryLogic.folderColumnCount(393f, 1f));
        assertEquals(6, LibraryLogic.folderColumnCount(393f, 2f / 3f));
        assertEquals(2, LibraryLogic.folderColumnCount(393f, 2f));
        assertEquals(2, LibraryLogic.folderColumnCount(393f, 10f));
        assertEquals(6, LibraryLogic.folderColumnCount(393f, .1f));
        assertEquals(10, LibraryLogic.folderColumnCount(1280f, 1f));
        assertEquals(6, LibraryLogic.folderColumnCount(800f, 1f));
        assertEquals(16, LibraryLogic.folderColumnCount(1280f, .1f));
    }
}
