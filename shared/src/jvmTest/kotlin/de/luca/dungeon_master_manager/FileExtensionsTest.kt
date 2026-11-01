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

    @Test
    fun testCreatableFileTypes() {
        val types = FileExtensions.CREATABLE_FILE_TYPES
        assertTrue(types.isNotEmpty(), "Creatable file types should not be empty")

        val extensions = types.map { it.extension }
        assertTrue("md" in extensions, "Markdown should be creatable")
        assertTrue("map" in extensions, "Map should be creatable")
        assertTrue("txt" in extensions, "Plain text should be creatable")
        assertTrue("json" in extensions, "JSON should be creatable")

        // All creatable file types should be editable text and supported in the app
        for (type in types) {
            assertTrue(FileExtensions.isEditableText(type.extension), "${type.extension} should be editable")
            assertTrue(FileExtensions.isSupportedInApp(type.extension), "${type.extension} should be supported in app")
            assertTrue(type.name.isNotBlank(), "Name should not be blank")
            assertTrue(type.icon.isNotBlank(), "Icon should not be blank")
            assertTrue(type.description.isNotBlank(), "Description should not be blank")
        }
    }

    @Test
    fun testDefaultFileTypeForFolder() {
        val mapsDefault = FileExtensions.defaultFileTypeForFolder("Maps")
        kotlin.test.assertEquals("map", mapsDefault.extension)

        val battlemapsDefault = FileExtensions.defaultFileTypeForFolder("battle_maps")
        kotlin.test.assertEquals("map", battlemapsDefault.extension)

        val storyDefault = FileExtensions.defaultFileTypeForFolder("Story")
        kotlin.test.assertEquals("md", storyDefault.extension)

        val rootDefault = FileExtensions.defaultFileTypeForFolder("")
        kotlin.test.assertEquals("md", rootDefault.extension)
    }

    @Test
    fun testSanitizeFileName() {
        // Base name only
        kotlin.test.assertEquals("Chapter 1.md", FileExtensions.sanitizeFileName("Chapter 1", "md"))
        kotlin.test.assertEquals("Tavern.map", FileExtensions.sanitizeFileName("Tavern", "map"))
        kotlin.test.assertEquals("data.json", FileExtensions.sanitizeFileName("data", "json"))

        // Already typed extension (case-insensitive)
        kotlin.test.assertEquals("Chapter 1.md", FileExtensions.sanitizeFileName("Chapter 1.md", "md"))
        kotlin.test.assertEquals("Chapter 1.md", FileExtensions.sanitizeFileName("Chapter 1.MD", "md"))
        kotlin.test.assertEquals("Tavern.map", FileExtensions.sanitizeFileName("Tavern.map", "map"))

        // Trailing dots
        kotlin.test.assertEquals("Chapter 1.md", FileExtensions.sanitizeFileName("Chapter 1.", "md"))
        kotlin.test.assertEquals("Chapter 1.md", FileExtensions.sanitizeFileName("Chapter 1...", "md"))

        // Whitespace trimming
        kotlin.test.assertEquals("Chapter 1.md", FileExtensions.sanitizeFileName("  Chapter 1  ", "md"))
        kotlin.test.assertEquals("Chapter 1.md", FileExtensions.sanitizeFileName("  Chapter 1.md  ", "md"))

        // Blank or extension only
        kotlin.test.assertEquals("", FileExtensions.sanitizeFileName("", "md"))
        kotlin.test.assertEquals("", FileExtensions.sanitizeFileName("   ", "md"))
        kotlin.test.assertEquals("", FileExtensions.sanitizeFileName(".md", "md"))
        kotlin.test.assertEquals("", FileExtensions.sanitizeFileName("...", "md"))
    }
}
