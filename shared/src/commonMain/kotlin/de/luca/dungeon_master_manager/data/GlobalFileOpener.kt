package de.luca.dungeon_master_manager.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

object GlobalFileOpener {
    private val _openFileRequests = MutableSharedFlow<File>(extraBufferCapacity = 16)
    val openFileRequests: SharedFlow<File> = _openFileRequests.asSharedFlow()

    fun openFile(file: File) {
        _openFileRequests.tryEmit(file)
    }
}
