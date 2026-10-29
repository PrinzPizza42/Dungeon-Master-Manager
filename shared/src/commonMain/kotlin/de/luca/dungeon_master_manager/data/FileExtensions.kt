package de.luca.dungeon_master_manager.data

object FileExtensions {
    val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "bmp", "gif", "ico")
    val TEXT_EXTENSIONS = setOf("txt", "text", "log", "json", "csv", "yaml", "yml", "xml")
    val MARKDOWN_EXTENSIONS = setOf("md")
    val MAP_EXTENSIONS = setOf("map")
    val PDF_EXTENSIONS = setOf("pdf")
    val AUDIO_EXTENSIONS = setOf("mp3", "wav", "ogg", "flac", "m4a")
    val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz")

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
}
