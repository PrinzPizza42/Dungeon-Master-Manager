package de.luca.dungeon_master_manager

import de.luca.dungeon_master_manager.data.FuzzyMatcher
import de.luca.dungeon_master_manager.data.SearchItemType
import de.luca.dungeon_master_manager.data.SearchScope
import de.luca.dungeon_master_manager.data.UniversalSearchService
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class UniversalSearchServiceTest {

    @Test
    fun testFuzzyMatcherExactMatch() {
        val result = FuzzyMatcher.match("Chapter1.md", "Chapter1.md")
        assertNotNull(result)
        assertEquals(1000.0, result.score)
        assertEquals((0 until "Chapter1.md".length).toList(), result.matchedIndices)
    }

    @Test
    fun testFuzzyMatcherStartsWith() {
        val result = FuzzyMatcher.match("Chap", "Chapter1.md")
        assertNotNull(result)
        assertTrue(result.score > 700.0)
        assertEquals(listOf(0, 1, 2, 3), result.matchedIndices)
    }

    @Test
    fun testFuzzyMatcherSubsequence() {
        val result = FuzzyMatcher.match("gbln", "Goblin_King.md")
        assertNotNull(result)
        assertTrue(result.matchedIndices.size == 4)
        // 'G' is at 0, 'b' is at 2, 'l' is at 3, 'n' is at 5
        assertEquals(listOf(0, 2, 3, 5), result.matchedIndices)
    }

    @Test
    fun testFuzzyMatcherNoMatch() {
        val result = FuzzyMatcher.match("dragon", "Goblin_King.md")
        assertNull(result)
    }

    @Test
    fun testPrefixParsing() {
        val service = UniversalSearchService("/tmp/dummy")

        val p1 = service.parseQuery("m: world_map")
        assertEquals("world_map", p1.cleanQuery)
        assertEquals(setOf("map"), p1.extensionFilter)

        val p2 = service.parseQuery("img: portrait")
        assertEquals("portrait", p2.cleanQuery)
        assertNotNull(p2.extensionFilter)
        assertTrue("png" in p2.extensionFilter)

        val p3 = service.parseQuery("@ Der Kampf")
        assertEquals("Der Kampf", p3.cleanQuery)
        assertEquals(SearchItemType.HEADING, p3.typeFilter)

        val p4 = service.parseQuery("> Battery")
        assertEquals("Battery", p4.cleanQuery)
        assertTrue(p4.isCommandMode)

        val p5 = service.parseQuery("p: Story")
        assertEquals("Story", p5.cleanQuery)
        assertEquals(SearchScope.PROJECT, p5.scopeFilter)

        val p6 = service.parseQuery("g: Items")
        assertEquals("Items", p6.cleanQuery)
        assertEquals(SearchScope.GLOBAL, p6.scopeFilter)

        val p7 = service.parseQuery("e: Goblin")
        assertEquals("Goblin", p7.cleanQuery)
        assertEquals(SearchItemType.ENTITY, p7.typeFilter)

        val p8 = service.parseQuery("# Intro")
        assertEquals("Intro", p8.cleanQuery)
        assertEquals(SearchItemType.HEADING, p8.typeFilter)

        val p9 = service.parseQuery("### Subsection")
        assertEquals("Subsection", p9.cleanQuery)
        assertEquals(SearchItemType.HEADING, p9.typeFilter)
    }

    @Test
    fun testSearchFilesAndInstantNoteCreation() = runBlocking {
        val tempDir = Files.createTempDirectory("dmm_search_test").toFile()
        try {
            val storyDir = File(tempDir, "Story").also { it.mkdirs() }
            val ch1 = File(storyDir, "Chapter1.md").also {
                it.writeText(
                    "# Der Beginn des Abenteuers\nEin finsterer Tag bricht an.\n" +
                    "## Die Taverne\nDer Wirt schenkt Met ein.\n" +
                    "### Der finstere Keller\nRatzen lauern im Dunkeln."
                )
            }
            val mapFile = File(tempDir, "World.map").also {
                it.writeText("{\"imagePath\":\"\",\"markers\":[{\"id\":\"marker-123\",\"x\":0.5,\"y\":0.5,\"isEntityLinked\":false,\"title\":\"Alte Mühle\",\"notes\":\"Verlassene Mühle\"}]}")
            }

            val service = UniversalSearchService(tempDir.absolutePath)

            // 1. Search for existing file
            val results = service.search("Chapter1")
            assertTrue(results.any { it.type == SearchItemType.FILE && it.title == "Chapter1.md" })

            // 2. Search for H1, H2, H3 headings
            val h1Results = service.search("@ Beginn")
            val h1Item = h1Results.find { it.type == SearchItemType.HEADING && it.title == "Der Beginn des Abenteuers" }
            assertNotNull(h1Item)
            assertEquals(1, h1Item.headingLevel)

            val h2Results = service.search("@ Taverne")
            val h2Item = h2Results.find { it.type == SearchItemType.HEADING && it.title == "Die Taverne" }
            assertNotNull(h2Item)
            assertEquals(2, h2Item.headingLevel)
            assertTrue(h2Item.previewSnippet?.contains("Der Wirt schenkt Met ein.") == true)

            val h3Results = service.search("# Keller")
            val h3Item = h3Results.find { it.type == SearchItemType.HEADING && it.title == "Der finstere Keller" }
            assertNotNull(h3Item)
            assertEquals(3, h3Item.headingLevel)
            assertTrue(h3Item.previewSnippet?.contains("Ratzen lauern im Dunkeln.") == true)

            // 3. Search for map extension
            val mapResults = service.search("m: World")
            assertTrue(mapResults.any { it.title == "World.map" })

            // 4. Search for map marker
            val markerResults = service.search("Alte Mühle")
            val markerItem = markerResults.find { it.type == SearchItemType.MAP_MARKER }
            assertNotNull(markerItem)
            assertEquals("Alte Mühle", markerItem.title)
            assertEquals("marker-123", markerItem.markerId)
            assertEquals(mapFile.absolutePath, markerItem.filePath)

            // 4b. Prefix search for map marker
            val pinResults = service.search("pin: Mühle")
            val pinItem = pinResults.find { it.type == SearchItemType.MAP_MARKER }
            assertNotNull(pinItem)
            assertEquals("Alte Mühle", pinItem.title)

            // 5. Search for non-existent note should offer Instant Note Creation
            val nonExistentResults = service.search("Taverne Zum Drachen")
            val createItem = nonExistentResults.find { it.type == SearchItemType.CREATE_NOTE }
            assertNotNull(createItem)
            assertTrue(createItem.title.contains("Taverne Zum Drachen.md"))

            // 6. Empty query should return recent files
            val emptyResults = service.search("")
            assertTrue(emptyResults.isNotEmpty())

            // 7. Command mode
            val cmdResults = service.search("> Map")
            assertTrue(cmdResults.any { it.type == SearchItemType.COMMAND && it.title.contains("map", ignoreCase = true) })

        } finally {
            tempDir.deleteRecursively()
        }
    }
}
