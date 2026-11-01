package de.luca.dungeon_master_manager.data

data class CreatableFileType(
    val extension: String,
    val name: String,
    val icon: String,
    val description: String,
)

object FileExtensions {
    val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "bmp", "gif", "ico", "svg")
    val TEXT_EXTENSIONS = setOf("txt", "text", "log", "json", "csv", "yaml", "yml", "xml")
    val MARKDOWN_EXTENSIONS = setOf("md")
    val MAP_EXTENSIONS = setOf("map")
    val PDF_EXTENSIONS = setOf("pdf")
    val AUDIO_EXTENSIONS = setOf("mp3", "wav", "ogg", "flac", "m4a")
    val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz")

    /**
     * File types that can be created directly within the app,
     * complete with user-friendly descriptions and icons.
     */
    val CREATABLE_FILE_TYPES: List<CreatableFileType> = listOf(
        CreatableFileType(
            extension = "md",
            name = "Markdown Document (.md)",
            icon = "📝",
            description = "Notes, lore, entity links & chapters"
        ),
        CreatableFileType(
            extension = "map",
            name = "Interactive Map (.map)",
            icon = "🗺",
            description = "interactive map with pins and markers"
        ),
        CreatableFileType(
            extension = "txt",
            name = "Plain Text (.txt)",
            icon = "📄",
            description = "Raw unformatted text file"
        ),
        CreatableFileType(
            extension = "json",
            name = "JSON Data (.json)",
            icon = "📋",
            description = "Structured data or configuration"
        )
    )

    fun isImage(extension: String): Boolean = extension.lowercase() in IMAGE_EXTENSIONS
    fun isText(extension: String): Boolean = extension.lowercase() in TEXT_EXTENSIONS
    fun isMarkdown(extension: String): Boolean = extension.lowercase() in MARKDOWN_EXTENSIONS
    fun isMap(extension: String): Boolean = extension.lowercase() in MAP_EXTENSIONS
    fun isPdf(extension: String): Boolean = extension.lowercase() in PDF_EXTENSIONS
    fun isAudio(extension: String): Boolean = extension.lowercase() in AUDIO_EXTENSIONS
    fun isArchive(extension: String): Boolean = extension.lowercase() in ARCHIVE_EXTENSIONS

    /** True if the file content is UTF-8 text managed & auto-saved by the editor. */
    fun isEditableText(extension: String): Boolean =
        isMarkdown(extension) || isMap(extension) || isText(extension)

    /** True if ContentArea has a built-in viewer/editor for this file type. */
    fun isSupportedInApp(extension: String): Boolean =
        isEditableText(extension) || isImage(extension)

    /**
     * Determines a smart default file type for a given folder name.
     * E.g., a "Maps" folder defaults to .map, while others default to .md.
     */
    fun defaultFileTypeForFolder(folderName: String): CreatableFileType {
        val lower = folderName.lowercase()
        return if (lower.contains("map")) {
            CREATABLE_FILE_TYPES.firstOrNull { it.extension == "map" } ?: CREATABLE_FILE_TYPES.first()
        } else {
            CREATABLE_FILE_TYPES.first()
        }
    }

    /**
     * Sanitizes a base file name and appends the extension.
     * Strips accidental redundant extensions typed by the user (e.g. "note.md" with extension "md").
     */
    fun sanitizeFileName(rawName: String, extension: String): String {
        val clean = rawName.trim()
        if (clean.isBlank()) return ""
        val extWithDot = ".$extension"
        val base = if (clean.endsWith(extWithDot, ignoreCase = true)) {
            clean.dropLast(extWithDot.length).trim()
        } else {
            clean.trimEnd('.')
        }
        return if (base.isBlank()) "" else "$base.$extension"
    }
}

