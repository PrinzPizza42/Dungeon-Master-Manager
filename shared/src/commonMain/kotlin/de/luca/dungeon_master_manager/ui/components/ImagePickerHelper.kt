package de.luca.dungeon_master_manager.ui.components

import de.luca.dungeon_master_manager.data.FileExtensions
import java.io.File

object ImagePickerHelper {

    fun scanProjectImages(projectPath: String): List<File> {
        if (projectPath.isBlank()) return emptyList()
        val root = File(projectPath)
        if (!root.exists() || !root.isDirectory) return emptyList()

        return root.walkTopDown()
            .onEnter { dir ->
                dir == root || (!dir.name.startsWith(".") && dir.name != "node_modules" && dir.name != "build")
            }
            .filter { file ->
                file.isFile && !file.name.startsWith(".") && FileExtensions.isImage(file.extension)
            }
            .sortedBy { it.relativeTo(root).path.lowercase() }
            .toList()
    }

    fun toRelativeProjectPath(file: File, projectPath: String): String {
        if (projectPath.isBlank()) return file.name
        val root = File(projectPath)
        val rel = try {
            file.relativeTo(root).path
        } catch (_: Exception) {
            file.name
        }
        return rel.replace('\\', '/')
    }

    fun resolveNonConflictingImageFile(targetDir: File, baseName: String, extension: String): File {
        val cleanBase = baseName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "image" }
        val ext = extension.lowercase().removePrefix(".")
        var candidate = File(targetDir, "$cleanBase.$ext")
        var counter = 1
        while (candidate.exists()) {
            candidate = File(targetDir, "${cleanBase}_$counter.$ext")
            counter++
        }
        return candidate
    }

    fun importExternalImage(sourceFile: File, projectPath: String): File? {
        if (!sourceFile.exists() || !sourceFile.isFile || projectPath.isBlank()) return null
        val targetDir = File(projectPath, "Images").also { it.mkdirs() }
        val baseName = sourceFile.nameWithoutExtension
        val ext = sourceFile.extension.ifBlank { "png" }
        val targetFile = resolveNonConflictingImageFile(targetDir, baseName, ext)
        try {
            sourceFile.copyTo(targetFile, overwrite = false)
            return targetFile
        } catch (_: Exception) {
            return null
        }
    }
}
