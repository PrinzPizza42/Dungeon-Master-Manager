package de.luca.dungeon_master_manager

import de.luca.dungeon_master_manager.data.AppSettings
import de.luca.dungeon_master_manager.data.AppSettingsRepository
import de.luca.dungeon_master_manager.data.FileNode
import de.luca.dungeon_master_manager.data.NewItemMode
import de.luca.dungeon_master_manager.ui.components.collectAllFolderOptions
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExplorerFolderHelperTest {

    @Test
    fun testCollectAllFolderOptions_Hierarchy() {
        val rootPath = "/projects/my_campaign"
        val rootNode = FileNode(
            name = "my_campaign",
            path = rootPath,
            isDirectory = true,
            children = listOf(
                FileNode(
                    name = "Images",
                    path = "$rootPath/Images",
                    isDirectory = true,
                    children = listOf(
                        FileNode(
                            name = "Entities",
                            path = "$rootPath/Images/Entities",
                            isDirectory = true,
                            children = emptyList()
                        ),
                        FileNode(
                            name = "hero.png",
                            path = "$rootPath/Images/hero.png",
                            isDirectory = false
                        )
                    )
                ),
                FileNode(
                    name = "Story",
                    path = "$rootPath/Story",
                    isDirectory = true,
                    children = listOf(
                        FileNode(
                            name = "chapter1.md",
                            path = "$rootPath/Story/chapter1.md",
                            isDirectory = false
                        )
                    )
                ),
                FileNode(
                    name = "main.md",
                    path = "$rootPath/main.md",
                    isDirectory = false
                )
            )
        )

        val options = collectAllFolderOptions(rootNode, rootPath)

        // Options should include Root, Images, Entities, Story (files should be excluded)
        assertEquals(4, options.size)

        assertEquals("", options[0].relativePath)
        assertEquals("(Project Root)", options[0].displayName)
        assertEquals(0, options[0].depth)

        assertEquals("Images", options[1].relativePath)
        assertEquals("Images", options[1].displayName)
        assertEquals(1, options[1].depth)

        assertEquals("Images/Entities", options[2].relativePath)
        assertEquals("Entities", options[2].displayName)
        assertEquals(2, options[2].depth)

        assertEquals("Story", options[3].relativePath)
        assertEquals("Story", options[3].displayName)
        assertEquals(1, options[3].depth)
    }

    @Test
    fun testCollectAllFolderOptions_NullRoot() {
        val options = collectAllFolderOptions(null, "/fake/path")
        assertTrue(options.isEmpty())
    }

    @Test
    fun testAppSettings_SidebarWidthClamping() {
        val defaultSettings = AppSettings()
        assertEquals(260, defaultSettings.explorerSidebarWidth)
        assertEquals(260, AppSettingsRepository.DEFAULT_EXPLORER_SIDEBAR_WIDTH)
        assertEquals(180, AppSettingsRepository.MIN_EXPLORER_SIDEBAR_WIDTH)
        assertEquals(650, AppSettingsRepository.MAX_EXPLORER_SIDEBAR_WIDTH)

        // Clamping checks
        val min = AppSettingsRepository.MIN_EXPLORER_SIDEBAR_WIDTH
        val max = AppSettingsRepository.MAX_EXPLORER_SIDEBAR_WIDTH
        assertEquals(min, (100).coerceIn(min, max))
        assertEquals(max, (1000).coerceIn(min, max))
        assertEquals(300, (300).coerceIn(min, max))
    }

    @Test
    fun testNewItemModeEnum() {
        assertEquals("FILE", NewItemMode.FILE.name)
        assertEquals("FOLDER", NewItemMode.FOLDER.name)
    }
}
