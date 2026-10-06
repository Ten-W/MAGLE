package com.tai.oeviewer

import org.junit.Assert.*
import org.junit.Test

class ThumbnailRetryTest {
    @Test fun svgPreviewIsBoundedAndKeepsAspectRatio() {
        assertTrue(LibraryLogic.isViewableImage("SVG"))
        assertArrayEquals(intArrayOf(1024, 512), LibraryLogic.svgRenderSize(1000f, 500f, 1024))
        assertArrayEquals(intArrayOf(2048, 2048), LibraryLogic.svgRenderSize(Float.NaN, 0f, 4096))
        assertArrayEquals(intArrayOf(1, 1), LibraryLogic.svgRenderSize(1f, 10000f, 0))
        assertArrayEquals(intArrayOf(1024, 1024), LibraryLogic.svgRenderSize(Float.MIN_VALUE, Float.MIN_VALUE, 1024))
    }
    @Test fun temporaryFailuresRetryButMissingOrForbiddenFilesDoNot() {
        for (status in listOf(0, 429, 500, 503, 599)) assertTrue(LibraryLogic.isTransientThumbnailFailure(status))
        for (status in listOf(400, 401, 403, 404)) assertFalse(LibraryLogic.isTransientThumbnailFailure(status))
        assertTrue(LibraryLogic.isViewableImage("PNG"))
        assertTrue(LibraryLogic.isViewableImage("GIF"))
    }
}
