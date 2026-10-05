package de.luca.dungeon_master_manager.data

/**
 * Represents an open tab in the editor.
 */
data class EditorTab(
    val filePath: String,
    val fileName: String,
    val extension: String,
) {
    companion object {
        fun fromFileNode(node: FileNode): EditorTab = EditorTab(
            filePath = node.path,
            fileName = node.name,
            extension = node.extension,
        )
    }
}
