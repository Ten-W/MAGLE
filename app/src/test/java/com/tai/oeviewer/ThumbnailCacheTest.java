package com.tai.oeviewer;

import org.junit.Test;
import java.nio.file.Files;
import java.io.File;
import static org.junit.Assert.*;

public class ThumbnailCacheTest {
    @Test public void numericSettingsRequirePositiveIntegersWithoutOverflow() {
        assertEquals(Integer.valueOf(1), LibraryLogic.positiveSetting("1"));
        assertEquals(Integer.valueOf(4096), LibraryLogic.positiveSetting("4096"));
        for (String value : new String[] {null, "", "0", "-1", "1.5", "abc", "2147483648"}) {
            assertNull(LibraryLogic.positiveSetting(value));
        }
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), LibraryLogic.positiveSetting("2147483647"));
    }
    @Test public void evictsOldestThumbnailsOnly() throws Exception {
        File directory = Files.createTempDirectory("magle-cache-test").toFile();
        File old = new File(directory, "old.thumb"), recent = new File(directory, "new.thumb");
        File index = new File(directory, "index.json");
        try {
            Files.write(old.toPath(), new byte[4]);
            Files.write(recent.toPath(), new byte[4]);
            Files.write(index.toPath(), new byte[8]);
            assertTrue(old.setLastModified(1000));
            assertTrue(recent.setLastModified(2000));
            LibraryLogic.trimThumbnailCache(directory, 4);
            assertFalse(old.exists()); assertTrue(recent.exists()); assertTrue(index.exists());
            LibraryLogic.trimThumbnailCache(directory, 0);
            assertFalse(recent.exists()); assertTrue(index.exists());
        } finally {
            old.delete(); recent.delete(); index.delete(); directory.delete();
        }
    }
}
