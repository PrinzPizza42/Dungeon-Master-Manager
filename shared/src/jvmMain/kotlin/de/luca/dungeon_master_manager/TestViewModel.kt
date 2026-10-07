package de.luca.dungeon_master_manager

import de.luca.dungeon_master_manager.viewmodel.EntityViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay

fun main() = runBlocking {
    println("Initializing EntityViewModel...")
    val viewModel = EntityViewModel(null, null)
    
    // Wait for init block
    delay(1000)
    
    println("Calling addEntity...")
    viewModel.addEntity(
        isGlobal = true,
        name = "Global Test User",
        type = "NPC",
        color = "4294901760",
        notesFilePath = ""
    )
    
    delay(1000)
    
    println("Global Entities List:")
    viewModel.globalEntities.value.forEach {
        println(" - ${it.name} (${it.id})")
    }
}
