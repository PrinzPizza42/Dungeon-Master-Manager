package de.luca.dungeon_master_manager.data

object FileExtensions {
    val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "bmp", "gif", "ico")
    val TEXT_EXTENSIONS = setOf("txt", "text", "log")
    val MARKDOWN_EXTENSIONS = setOf("md")
    val MAP_EXTENSIONS = setOf("map")

    fun isImage(extension: String): Boolean = extension.lowercase() in IMAGE_EXTENSIONS
    fun isText(extension: String): Boolean = extension.lowercase() in TEXT_EXTENSIONS
    fun isMarkdown(extension: String): Boolean = extension.lowercase() in MARKDOWN_EXTENSIONS
    fun isMap(extension: String): Boolean = extension.lowercase() in MAP_EXTENSIONS
}
