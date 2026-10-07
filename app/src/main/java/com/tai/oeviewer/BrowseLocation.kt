package com.tai.oeviewer

internal enum class LibrarySection { ALL, UNFILED, UNTAGGED, TAG, TAGGROUPS, TAGGROUP, TRASH, FOLDER }
internal enum class AssetViewMode(val label: String) {
    GRID("网格视图"), LIST("列表视图"), WATERFALL("瀑布流")
}

internal data class BrowseLocation(
    val key: Long, val section: LibrarySection, val folderId: String?, val tag: String?,
    val name: String, val query: String, val includeChildren: Boolean, val mode: AssetViewMode
) {
    fun sameDestination(other: BrowseLocation) = section == other.section && folderId == other.folderId && tag == other.tag

    fun tagParentId(explicitGroupId: String? = null) = explicitGroupId ?: tag.takeIf { section == LibrarySection.TAGGROUP }

    fun backTarget(history: List<BrowseLocation>, parent: Pair<String, String>?, newKey: Long): BrowseLocation? {
        val target = when (section) {
            LibrarySection.FOLDER -> if (folderId == null) return null else copy(key = newKey,
                folderId = parent?.first, tag = null, name = parent?.second ?: "文件夹", query = "")
            LibrarySection.TAG -> copy(key = newKey,
                section = if (parent == null) LibrarySection.TAGGROUPS else LibrarySection.TAGGROUP,
                folderId = null, tag = parent?.first, name = parent?.second ?: "标签管理", query = "")
            LibrarySection.TAGGROUP -> copy(key = newKey, section = LibrarySection.TAGGROUPS,
                folderId = null, tag = null, name = "标签管理", query = "")
            else -> return null
        }
        return history.lastOrNull { it.query.isBlank() && it.sameDestination(target) } ?: target
    }
}
