package de.luca.dungeon_master_manager.ui.components

import de.luca.dungeon_master_manager.data.FileNode
import java.io.File

data class ExplorerFolderOption(
    val relativePath: String, // "" for Project Root, otherwise e.g. "Story" or "Images/Entities"
    val displayName: String,  // "(Project Root)" or folder name
    val depth: Int,           // 0 for Root, 1 for top-level, 2 for nested, etc.
)

/**
 * Traverses a [FileNode] tree and returns all directories in hierarchical order,
 * always starting with the Project Root.
 */
fun collectAllFolderOptions(rootNode: FileNode?, projectPath: String): List<ExplorerFolderOption> {
    if (rootNode == null) return emptyList()
    val options = mutableListOf<ExplorerFolderOption>()

    // Project root is always an available destination
    options.add(
        ExplorerFolderOption(
            relativePath = "",
            displayName = "(Project Root)",
            depth = 0
        )
    )

    fun traverse(node: FileNode, currentDepth: Int) {
        val rootFile = File(projectPath)
        for (child in node.children) {
            if (child.isDirectory) {
                val rel = try {
                    File(child.path).relativeTo(rootFile).path.replace('\\', '/')
                } catch (_: Exception) {
                    child.name
                }
                options.add(
                    ExplorerFolderOption(
                        relativePath = rel,
                        displayName = child.name,
                        depth = currentDepth
                    )
                )
                traverse(child, currentDepth + 1)
            }
        }
    }

    traverse(rootNode, 1)
    return options
}
