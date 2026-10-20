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
        val validTexts = listOf("txt", "text", "log", "TXT", "LOG")
        for (ext in validTexts) {
            assertTrue(FileExtensions.isText(ext), "Expected $ext to be recognized as text")
        }

        val nonTexts = listOf("png", "jpg", "map", "exe")
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
}
