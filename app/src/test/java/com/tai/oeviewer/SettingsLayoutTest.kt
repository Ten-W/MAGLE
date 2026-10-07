package com.tai.oeviewer

import org.junit.Assert.*
import org.junit.Test

class SettingsLayoutTest {
    @Test fun backupLivesWithLibrariesAndSourcesKeepRequestedOrder() {
        val source = java.io.File("src/main/java/com/tai/oeviewer/MainActivity.kt").readText()
        assertFalse(source.contains("SettingsIndexBackup"))
        val settings = source.substringAfter("private fun SettingsScreen()").substringBefore("private fun SettingsAbout()")
        assertEquals(2, Regex("SettingsLibraries\\(\\)\\s+SettingsIndexUpdate\\(\\)").findAll(settings).count())
        val libraries = source.substringAfter("private fun SettingsLibraries()").substringBefore("private fun SettingsCache()")
        assertTrue(libraries.indexOf("修改素材库") < libraries.indexOf("导出索引备份"))
        assertTrue(libraries.indexOf("导出索引备份") < libraries.indexOf("移除素材库"))
        val chooser = source.substringAfter("private fun SourceChooserDialog()").substringBefore("private fun WebDavDialog()")
        val positions = listOf("Microsoft OneDrive", "LibrarySource.DROPBOX, LibrarySource.GOOGLE", "本地文件夹", "Text(\"WebDAV\")",
            "LibrarySource.SMB, LibrarySource.S3", "导入备份索引").map { chooser.indexOf(it) }
        assertTrue(positions.all { it >= 0 })
        assertEquals(positions.sorted(), positions)
        assertTrue(chooser.contains("if (newSourceForLibrary == null)"))
    }
}
