package de.luca.dungeon_master_manager

import de.luca.dungeon_master_manager.data.FileService
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExplorerFileDropTest {

    private val fileService = FileService()

    @Test
    fun testValidateExternalDropPartitionsCorrectly() {
        val tempDir = Files.createTempDirectory("dmm_test_drop").toFile()
        try {
            val normalFile1 = File(tempDir, "sample1.txt").also { it.writeText("content 1") }
            val normalFile2 = File(tempDir, "sample2.png").also { it.writeText("image") }
            val subDir = File(tempDir, "subfolder").also { it.mkdirs() }
            val dotFile = File(tempDir, ".project").also { it.writeText("config") }

            val targetDir = Files.createTempDirectory("dmm_test_target").toFile()
            try {
                val validation = fileService.validateExternalDrop(
                    listOf(normalFile1, normalFile2, subDir, dotFile),
                    targetDir
                )

                assertEquals(2, validation.validFiles.size)
                assertTrue(validation.validFiles.any { it.name == "sample1.txt" })
                assertTrue(validation.validFiles.any { it.name == "sample2.png" })
                assertEquals(1, validation.skippedDirectories)
                assertEquals(1, validation.skippedDotFiles)
                assertEquals(0, validation.skippedSameDirectory)
            } finally {
                targetDir.deleteRecursively()
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testValidateExternalDropDetectsSameDirectory() {
        val targetDir = Files.createTempDirectory("dmm_test_target").toFile()
        try {
            val fileInTarget = File(targetDir, "existing.txt").also { it.writeText("existing") }
            val validation = fileService.validateExternalDrop(listOf(fileInTarget), targetDir)

            assertEquals(0, validation.validFiles.size)
            assertEquals(1, validation.skippedSameDirectory)
        } finally {
            targetDir.deleteRecursively()
        }
    }

    @Test
    fun testGenerateUniqueTargetFile() {
        val targetDir = Files.createTempDirectory("dmm_test_unique").toFile()
        try {
            File(targetDir, "notes.md").writeText("v1")
            
            val unique1 = fileService.generateUniqueTargetFile(targetDir, "notes.md")
            assertEquals("notes (1).md", unique1.name)
            unique1.writeText("v2")

            val unique2 = fileService.generateUniqueTargetFile(targetDir, "notes.md")
            assertEquals("notes (2).md", unique2.name)

            // Test without extension
            File(targetDir, "LICENSE").writeText("lic")
            val uniqueLicense = fileService.generateUniqueTargetFile(targetDir, "LICENSE")
            assertEquals("LICENSE (1)", uniqueLicense.name)
        } finally {
            targetDir.deleteRecursively()
        }
    }

    @Test
    fun testCopyAndMoveOperations() = runBlocking {
        val sourceDir = Files.createTempDirectory("dmm_test_src").toFile()
        val targetDir = Files.createTempDirectory("dmm_test_dst").toFile()
        try {
            val srcFile = File(sourceDir, "test.txt").also { it.writeText("Hello Drag Drop") }
            val dstFile = File(targetDir, "test.txt")

            // Copy
            fileService.copyFile(srcFile, dstFile, overwrite = false)
            assertTrue(dstFile.exists())
            assertEquals("Hello Drag Drop", dstFile.readText())
            assertTrue(srcFile.exists())

            // Overwrite copy
            srcFile.writeText("Updated Content")
            fileService.copyFile(srcFile, dstFile, overwrite = true)
            assertEquals("Updated Content", dstFile.readText())

            // Move
            val moveDst = File(targetDir, "moved.txt")
            fileService.moveFileOnDisk(srcFile, moveDst, overwrite = false)
            assertFalse(srcFile.exists())
            assertTrue(moveDst.exists())
            assertEquals("Updated Content", moveDst.readText())
        } finally {
            sourceDir.deleteRecursively()
            targetDir.deleteRecursively()
        }
    }

    @Test
    fun testInternalFileTransferableSupportsFileListAndLocalFlavor() {
        val data = de.luca.dungeon_master_manager.ui.components.InternalFileDragData(
            filePath = "C:/test/sample.png",
            fileName = "sample.png"
        )
        val transferable = de.luca.dungeon_master_manager.ui.components.InternalFileTransferable(data)

        // Internal flavor
        assertTrue(transferable.isDataFlavorSupported(de.luca.dungeon_master_manager.ui.components.InternalFileTransferable.FLAVOR))
        assertEquals(data, transferable.getTransferData(de.luca.dungeon_master_manager.ui.components.InternalFileTransferable.FLAVOR))

        // OS file list flavor (allows dropping into Windows Explorer / Desktop)
        assertTrue(transferable.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor))
        val fileList = transferable.getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor) as List<*>
        assertEquals(1, fileList.size)
        assertEquals(File("C:/test/sample.png"), fileList.first())

        // String flavor
        assertTrue(transferable.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor))
        assertEquals("C:/test/sample.png", transferable.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor))
    }
}
