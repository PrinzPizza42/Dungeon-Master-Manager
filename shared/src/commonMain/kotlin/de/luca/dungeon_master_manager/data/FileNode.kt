package de.luca.dungeon_master_manager.data

/**
 * Represents a node in the project file tree.
 */
data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: List<FileNode> = emptyList(),
    val isVirtualHeading: Boolean = false,
) {
    /** File extension (lowercase), or empty string for directories/headings. */
    val extension: String
        get() = if (isDirectory || isVirtualHeading) "" else name.substringAfterLast('.', "").lowercase()
}
