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

    fun backTarget(history: List<BrowseLocation>, parent: Pair<String, String>?, newKey: Long): BrowseLocation? {
        val previous = history.lastOrNull()
        if (previous?.query?.isNotBlank() == true || section != LibrarySection.FOLDER) return previous
        val target = copy(key = newKey,
            section = if (parent == null) LibrarySection.ALL else LibrarySection.FOLDER,
            folderId = parent?.first, tag = null, name = parent?.second ?: "全部", query = "")
        return history.lastOrNull { it.query.isBlank() && it.sameDestination(target) } ?: target
    }
}
