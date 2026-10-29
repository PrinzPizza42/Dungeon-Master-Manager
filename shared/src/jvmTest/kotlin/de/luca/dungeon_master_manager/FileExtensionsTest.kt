package de.luca.dungeon_master_manager

import de.luca.dungeon_master_manager.data.FileExtensions
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileExtensionsTest {

    @Test
    fun testImageExtensions() {
        val validImages = listOf("png", "jpg", "jpeg", "webp", "bmp", "gif", "ico", "PNG", "JPG", "WebP")
        for (ext in validImages) {
            assertTrue(FileExtensions.isImage(ext), "Expected $ext to be recognized as an image")
        }

        val nonImages = listOf("txt", "md", "map", "pdf", "docx")
        for (ext in nonImages) {
            assertFalse(FileExtensions.isImage(ext), "Expected $ext NOT to be recognized as an image")
        }
    }

    @Test
    fun testTextExtensions() {
        val validTexts = listOf("txt", "text", "log", "json", "csv", "yaml", "yml", "xml", "TXT", "JSON")
        for (ext in validTexts) {
            assertTrue(FileExtensions.isText(ext), "Expected $ext to be recognized as text")
        }

        val nonTexts = listOf("png", "jpg", "map", "exe", "pdf")
        for (ext in nonTexts) {
            assertFalse(FileExtensions.isText(ext), "Expected $ext NOT to be recognized as text")
        }
    }

    @Test
    fun testMarkdownAndMap() {
        assertTrue(FileExtensions.isMarkdown("md"))
        assertTrue(FileExtensions.isMarkdown("MD"))
        assertFalse(FileExtensions.isMarkdown("txt"))

        assertTrue(FileExtensions.isMap("map"))
        assertTrue(FileExtensions.isMap("MAP"))
        assertFalse(FileExtensions.isMap("png"))
    }

    @Test
    fun testEditableAndSupported() {
        assertTrue(FileExtensions.isEditableText("md"))
        assertTrue(FileExtensions.isEditableText("txt"))
        assertTrue(FileExtensions.isEditableText("map"))
        assertTrue(FileExtensions.isEditableText("json"))
        assertFalse(FileExtensions.isEditableText("png"))
        assertFalse(FileExtensions.isEditableText("pdf"))
        assertFalse(FileExtensions.isEditableText("mp3"))

        assertTrue(FileExtensions.isSupportedInApp("md"))
        assertTrue(FileExtensions.isSupportedInApp("png"))
        assertTrue(FileExtensions.isSupportedInApp("map"))
        assertFalse(FileExtensions.isSupportedInApp("pdf"))
        assertFalse(FileExtensions.isSupportedInApp("mp3"))
        assertFalse(FileExtensions.isSupportedInApp("zip"))

        assertTrue(FileExtensions.isPdf("pdf"))
        assertTrue(FileExtensions.isAudio("mp3"))
        assertTrue(FileExtensions.isArchive("zip"))
    }
}
