package de.luca.dungeon_master_manager

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform