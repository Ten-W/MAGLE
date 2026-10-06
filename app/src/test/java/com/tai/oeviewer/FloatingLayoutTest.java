package com.tai.oeviewer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class FloatingLayoutTest {
    @Test public void allViewsStartClearOfToolbarWithScrollablePadding() {
        assertEquals(84f, LibraryLogic.galleryTopPadding(24f), 0f);
        assertEquals(60f, LibraryLogic.galleryTopPadding(0f), 0f);
    }
    @Test public void searchFitsPhonesButDoesNotStretchAcrossTablets() {
        assertEquals(255f, LibraryLogic.searchPillWidth(393f, 138f), 0f);
        assertEquals(360f, LibraryLogic.searchPillWidth(1280f, 138f), 0f);
        assertEquals(48f, LibraryLogic.searchPillWidth(200f, 240f), 0f);
    }
}
