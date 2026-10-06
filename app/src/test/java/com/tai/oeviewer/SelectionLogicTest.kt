package com.tai.oeviewer

import org.junit.Assert.assertEquals
import org.junit.Test

class SelectionLogicTest {
    @Test fun foldersStartAtRootsAndExpandOnlyTheChosenNode() {
        assertEquals(false, LibraryLogic.shouldShowChildren("root", emptySet()))
        assertEquals(false, LibraryLogic.shouldShowChildren("child", emptySet()))
        assertEquals(true, LibraryLogic.shouldShowChildren("root", setOf("root")))
        assertEquals(false, LibraryLogic.shouldShowChildren("child", setOf("root")))
    }
    @Test fun dragRangeKeepsPreviousSelectionsAndReversesWithoutAccumulating() {
        val ids = listOf("a", "b", "c", "d", "e")
        val initial = setOf("e")
        assertEquals(setOf("b", "c", "d", "e"), LibraryLogic.selectionRange(ids, initial, 1, 3))
        assertEquals(setOf("b", "c", "e"), LibraryLogic.selectionRange(ids, initial, 1, 2))
        assertEquals(setOf("a", "b", "e"), LibraryLogic.selectionRange(ids, initial, 1, 0))
        assertEquals(initial, LibraryLogic.selectionRange(ids, initial, -1, 0))
        assertEquals(initial, LibraryLogic.selectionRange(ids, initial, 1, 9))
        assertEquals(setOf("e"), initial)
    }
}
