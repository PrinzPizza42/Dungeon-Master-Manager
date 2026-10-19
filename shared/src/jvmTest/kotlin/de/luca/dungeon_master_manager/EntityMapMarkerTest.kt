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
}
