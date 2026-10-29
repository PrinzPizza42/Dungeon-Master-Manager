package de.luca.dungeon_master_manager.ui.components

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.awtTransferable
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File

data class InternalFileDragData(
    val filePath: String,
    val fileName: String
)

class InternalFileTransferable(val data: InternalFileDragData) : Transferable {
    private val fileList: List<File> = listOf(File(data.filePath))

    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(
        FLAVOR,
        DataFlavor.javaFileListFlavor,
        DataFlavor.stringFlavor
    )

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
        flavor == FLAVOR || flavor == DataFlavor.javaFileListFlavor || flavor == DataFlavor.stringFlavor

    override fun getTransferData(flavor: DataFlavor): Any {
        return when (flavor) {
            FLAVOR -> data
            DataFlavor.javaFileListFlavor -> fileList
            DataFlavor.stringFlavor -> data.filePath
            else -> throw UnsupportedFlavorException(flavor)
        }
    }

    companion object {
        val FLAVOR: DataFlavor = DataFlavor(
            "${DataFlavor.javaJVMLocalObjectMimeType};class=${InternalFileDragData::class.java.name}",
            "InternalFileDragData"
        )
    }
}

sealed interface ExplorerDropPayload {
    data class InternalFile(val data: InternalFileDragData) : ExplorerDropPayload
    data class ExternalFiles(val files: List<File>) : ExplorerDropPayload
}

@OptIn(ExperimentalComposeUiApi::class)
fun DragAndDropEvent.isExplorerFileDrag(): Boolean {
    return try {
        val t = awtTransferable
        t.isDataFlavorSupported(InternalFileTransferable.FLAVOR) ||
                t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
    } catch (_: Exception) {
        false
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun DragAndDropEvent.extractExplorerPayload(): ExplorerDropPayload? {
    return try {
        val t = awtTransferable
        if (t.isDataFlavorSupported(InternalFileTransferable.FLAVOR)) {
            val data = t.getTransferData(InternalFileTransferable.FLAVOR) as? InternalFileDragData
            data?.let { ExplorerDropPayload.InternalFile(it) }
        } else if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            val raw = t.getTransferData(DataFlavor.javaFileListFlavor)
            val files = (raw as? List<*>)?.filterIsInstance<File>() ?: emptyList()
            if (files.isNotEmpty()) ExplorerDropPayload.ExternalFiles(files) else null
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}
