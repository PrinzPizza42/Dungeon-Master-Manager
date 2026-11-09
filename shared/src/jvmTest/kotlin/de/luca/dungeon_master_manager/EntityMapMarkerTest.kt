package de.luca.dungeon_master_manager

import de.luca.dungeon_master_manager.data.MapData
import de.luca.dungeon_master_manager.data.MapMarker
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntityMapMarkerTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @Test
    fun testMarkerFilteringForDeletedEntity() {
        val entityIdToDelete = "entity-123"
        val otherEntityId = "entity-456"

        val markers = listOf(
            MapMarker(
                id = "m1",
                x = 0.1f,
                y = 0.2f,
                isEntityLinked = true,
                entityId = entityIdToDelete
            ),
            MapMarker(
                id = "m2",
                x = 0.5f,
                y = 0.6f,
                isEntityLinked = false,
                title = "Treasure Chest",
                notes = "Hidden under a rock"
            ),
            MapMarker(
                id = "m3",
                x = 0.8f,
                y = 0.9f,
                isEntityLinked = true,
                entityId = otherEntityId
            )
        )

        val mapData = MapData(imagePath = "images/map.png", markers = markers)
        val filteredMarkers = mapData.markers.filterNot { it.entityId == entityIdToDelete }

        assertEquals(2, filteredMarkers.size)
        assertTrue(filteredMarkers.none { it.entityId == entityIdToDelete })
        assertTrue(filteredMarkers.any { it.id == "m2" && !it.isEntityLinked })
        assertTrue(filteredMarkers.any { it.id == "m3" && it.entityId == otherEntityId })
    }

    @Test
    fun testFileCleanupOnDisk() {
        val tempDir = Files.createTempDirectory("dmm_test_project").toFile()
        try {
            val mapsDir = File(tempDir, "Maps").also { it.mkdirs() }
            val mapFile = File(mapsDir, "world.map")

            val entityIdToDelete = "npc-bob-uuid"
            val entityIdToKeep = "place-castle-uuid"

            val initialMap = MapData(
                imagePath = "images/world.png",
                markers = listOf(
                    MapMarker(id = "marker-1", x = 0.2f, y = 0.3f, isEntityLinked = true, entityId = entityIdToDelete),
                    MapMarker(id = "marker-2", x = 0.4f, y = 0.5f, isEntityLinked = false, title = "Standalone Pin"),
                    MapMarker(id = "marker-3", x = 0.6f, y = 0.7f, isEntityLinked = true, entityId = entityIdToKeep)
                )
            )
            mapFile.writeText(json.encodeToString(initialMap))

            // Simulate file cleanup
            val mapFiles = tempDir.walkTopDown().filter { it.isFile && it.extension == "map" && !it.name.startsWith(".") }
            for (file in mapFiles) {
                val text = file.readText()
                if (text.isNotBlank()) {
                    val parsed = json.decodeFromString<MapData>(text)
                    val remaining = parsed.markers.filterNot { it.entityId == entityIdToDelete }
                    if (remaining.size != parsed.markers.size) {
                        file.writeText(json.encodeToString(parsed.copy(markers = remaining)))
                    }
                }
            }

            // Verify file content after cleanup
            val updatedMap = json.decodeFromString<MapData>(mapFile.readText())
            assertEquals(2, updatedMap.markers.size)
            assertFalse(updatedMap.markers.any { it.entityId == entityIdToDelete })
            assertTrue(updatedMap.markers.any { it.id == "marker-2" })
            assertTrue(updatedMap.markers.any { it.id == "marker-3" })
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testVisibleMarkersLogic() {
        val entityId1 = "id-1"
        val entityId2 = "id-2"
        val existingEntityIds = setOf(entityId1)

        val markers = listOf(
            MapMarker(id = "m1", x = 0.1f, y = 0.1f, isEntityLinked = true, entityId = entityId1),
            MapMarker(id = "m2", x = 0.2f, y = 0.2f, isEntityLinked = true, entityId = entityId2), // entity deleted
            MapMarker(id = "m3", x = 0.3f, y = 0.3f, isEntityLinked = false, title = "Custom Marker")
        )

        // When not loaded, all markers remain visible
        val visibleWhenNotLoaded = markers
        assertEquals(3, visibleWhenNotLoaded.size)

        // When loaded, markers for missing entities are excluded
        val visibleWhenLoaded = markers.filterNot { marker ->
            marker.isEntityLinked && (marker.entityId == null || marker.entityId !in existingEntityIds)
        }

        assertEquals(2, visibleWhenLoaded.size)
        assertEquals(listOf("m1", "m3"), visibleWhenLoaded.map { it.id })
    }

    @Test
    fun testViewportCenterCalculation() {
        val viewportW = 1920f
        val viewportH = 1080f
        val baseMapW = 1000f
        val baseMapH = 800f
        val targetZoom = 2.0f

        // Marker at center (0.5, 0.5)
        val markerCenter = MapMarker(id = "m1", x = 0.5f, y = 0.5f, isEntityLinked = false)
        val targetXNoSidebar = -(markerCenter.x - 0.5f) * baseMapW * targetZoom
        val targetYNoSidebar = -(markerCenter.y - 0.5f) * baseMapH * targetZoom
        assertEquals(0f, targetXNoSidebar, 0.001f)
        assertEquals(0f, targetYNoSidebar, 0.001f)

        // Marker at (0.25, 0.75) with 320px sidebar open
        val sidebarWidthPx = 320f
        val markerQuarter = MapMarker(id = "m2", x = 0.25f, y = 0.75f, isEntityLinked = false)
        val targetXSidebar = (sidebarWidthPx / 2f) - (markerQuarter.x - 0.5f) * baseMapW * targetZoom
        val targetYSidebar = -(markerQuarter.y - 0.5f) * baseMapH * targetZoom

        // (320 / 2) - (-0.25 * 1000 * 2) = 160 - (-500) = 660
        assertEquals(660f, targetXSidebar)
        // -(0.25 * 800 * 2) = -400
        assertEquals(-400f, targetYSidebar)

        // Screen position check: (viewportW / 2 + targetX) + (marker.x - 0.5) * baseMapW * targetZoom
        val screenX = (viewportW / 2f + targetXSidebar) + (markerQuarter.x - 0.5f) * baseMapW * targetZoom
        // Center of unobstructed area between sidebar (320) and viewport end (1920): (1920 + 320) / 2 = 1120
        assertEquals((viewportW + sidebarWidthPx) / 2f, screenX)
    }

    @Test
    fun testSidebarSearchFiltering() {
        data class SimpleItem(
            val id: String,
            val title: String,
            val subtitle: String?,
            val notes: String?,
            val isEntityLinked: Boolean
        )

        val items = listOf(
            SimpleItem("1", "Gandalf the Grey", "NPC", "Wizard of Middle-earth", isEntityLinked = true),
            SimpleItem("2", "Minas Tirith", "Location", "Capital city of Gondor", isEntityLinked = true),
            SimpleItem("3", "Old Mill", null, "Secret meeting spot at night", isEntityLinked = false),
            SimpleItem("4", "Dragon Lair", null, "Beware the sleeping red dragon", isEntityLinked = false)
        )

        // 1. Search for "wizard"
        val query1 = "wizard"
        val results1 = items.filter { item ->
            item.title.lowercase().contains(query1) ||
                (item.subtitle?.lowercase()?.contains(query1) == true) ||
                (item.notes?.lowercase()?.contains(query1) == true)
        }
        assertEquals(1, results1.size)
        assertEquals("1", results1.first().id)

        // 2. Filter by Category Entities
        val entitiesOnly = items.filter { it.isEntityLinked }
        assertEquals(2, entitiesOnly.size)

        // 3. Filter by Category Pins
        val pinsOnly = items.filter { !it.isEntityLinked }
        assertEquals(2, pinsOnly.size)

        // 4. Combined search "secret" in pins only
        val secretPins = items.filter { !it.isEntityLinked && (it.notes?.contains("secret", ignoreCase = true) == true) }
        assertEquals(1, secretPins.size)
        assertEquals("3", secretPins.first().id)
    }

    @Test
    fun testMarkerHiddenFieldSerialization() {
        val marker = MapMarker(id = "m1", x = 0.5f, y = 0.5f, isEntityLinked = false, isHidden = true)
        val serialized = json.encodeToString(marker)
        assertTrue(serialized.contains("\"isHidden\": true"))

        val deserialized = json.decodeFromString<MapMarker>(serialized)
        assertTrue(deserialized.isHidden)

        // Backward compatibility: JSON without isHidden defaults to false
        val legacyJson = """{"id":"legacy-1","x":0.2,"y":0.3,"isEntityLinked":false}"""
        val legacyMarker = json.decodeFromString<MapMarker>(legacyJson)
        assertFalse(legacyMarker.isHidden)
    }

    @Test
    fun testSidebarSortingAndVisibilityToggling() {
        data class TestItem(val id: String, val name: String, val isHidden: Boolean)

        val items = listOf(
            TestItem("1", "Zombies", isHidden = false),
            TestItem("2", "Archer", isHidden = true),
            TestItem("3", "Bandit", isHidden = false),
            TestItem("4", "Dragon", isHidden = true)
        )

        // 1. Sort Visible First: visible items first sorted A-Z, then hidden items sorted A-Z
        val visibleFirst = items.sortedWith(
            compareBy<TestItem> { it.isHidden }.thenBy { it.name.lowercase() }
        )
        assertEquals(listOf("Bandit", "Zombies", "Archer", "Dragon"), visibleFirst.map { it.name })

        // 2. Sort Name A-Z
        val nameAsc = items.sortedBy { it.name.lowercase() }
        assertEquals(listOf("Archer", "Bandit", "Dragon", "Zombies"), nameAsc.map { it.name })

        // 3. Sort Name Z-A
        val nameDesc = items.sortedByDescending { it.name.lowercase() }
        assertEquals(listOf("Zombies", "Dragon", "Bandit", "Archer"), nameDesc.map { it.name })

        // 4. Bulk Category Visibility Toggle: if mixed, hiding all sets all to isHidden = true
        val allHidden = items.map { it.copy(isHidden = true) }
        assertTrue(allHidden.all { it.isHidden })

        // Re-showing sets all to isHidden = false
        val allShown = allHidden.map { it.copy(isHidden = false) }
        assertTrue(allShown.none { it.isHidden })
    }
}
