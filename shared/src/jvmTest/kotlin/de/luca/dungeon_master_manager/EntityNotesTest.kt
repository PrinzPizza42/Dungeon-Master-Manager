package de.luca.dungeon_master_manager

import de.luca.dungeon_master_manager.data.ProjectRepository
import de.luca.dungeon_master_manager.viewmodel.EntityViewModel
import de.luca.dungeonmastermanager.database.Entity
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class EntityNotesTest {

    private lateinit var tempDir: File
    private lateinit var mockGlobalDir: File

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("dmm_test_campaign").toFile()
        mockGlobalDir = Files.createTempDirectory("dmm_test_global").toFile()
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
        mockGlobalDir.deleteRecursively()
    }

    @Test
    fun testCreateTemplateFileAndConflictResolution() {
        val vm = EntityViewModel(tempDir.absolutePath, null)

        val path1 = vm.createTemplateFile(isGlobal = false, entityName = "Goblin Archer", entityType = "Monster")
        assertEquals("Monsters${File.separator}Goblin_Archer.md", path1)

        val file1 = File(tempDir, path1)
        assertTrue(file1.exists())
        assertTrue(file1.readText().contains("# Goblin Archer"))

        // Create second template with same entity name -> should not overwrite, should resolve to Goblin_Archer_1.md
        val path2 = vm.createTemplateFile(isGlobal = false, entityName = "Goblin Archer", entityType = "Monster")
        assertEquals("Monsters${File.separator}Goblin_Archer_1.md", path2)

        val file2 = File(tempDir, path2)
        assertTrue(file2.exists())
        assertNotEquals(file1.absolutePath, file2.absolutePath)
    }

    @Test
    fun testImportExternalMdFile() {
        val vm = EntityViewModel(tempDir.absolutePath, null)

        val externalFile = File(mockGlobalDir, "custom_npc.md")
        externalFile.writeText("# Custom NPC\nSome imported notes...")

        val importedPath = vm.importExternalMdFile(externalFile, isGlobal = false, entityType = "NPC")
        assertEquals("NPCs${File.separator}custom_npc.md", importedPath)

        val destinationFile = File(tempDir, importedPath)
        assertTrue(destinationFile.exists())
        assertEquals("# Custom NPC\nSome imported notes...", destinationFile.readText())

        // Importing again should resolve conflict to custom_npc_1.md
        val secondImport = vm.importExternalMdFile(externalFile, isGlobal = false, entityType = "NPC")
        assertEquals("NPCs${File.separator}custom_npc_1.md", secondImport)
        assertTrue(File(tempDir, secondImport).exists())
    }

    @Test
    fun testGetAvailableMdFilesSortsByTypeRelevance() {
        val vm = EntityViewModel(tempDir.absolutePath, null)

        File(tempDir, "NPCs").mkdirs()
        File(tempDir, "Places").mkdirs()
        File(tempDir, "Notes").mkdirs()

        File(tempDir, "Places/Tavern.md").writeText("# Tavern")
        File(tempDir, "NPCs/Barkeeper.md").writeText("# Barkeeper")
        File(tempDir, "Notes/General.md").writeText("# General")

        val npcFiles = vm.getAvailableMdFiles(isGlobal = false, entityType = "NPC")
        assertTrue(npcFiles.isNotEmpty())
        assertEquals("NPCs${File.separator}Barkeeper.md", npcFiles.first())

        val placeFiles = vm.getAvailableMdFiles(isGlobal = false, entityType = "Place")
        assertTrue(placeFiles.isNotEmpty())
        assertEquals("Places${File.separator}Tavern.md", placeFiles.first())
    }
}
