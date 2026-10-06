package de.luca.dungeon_master_manager.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import de.luca.dungeon_master_manager.database.AppDatabase
import java.io.File

object DatabaseHelper {
    
    // Global database located in ~/.dungeon-master-manager/.global/entities.db
    val globalDatabase: AppDatabase by lazy {
        val userHome = System.getProperty("user.home")
        val globalDir = File(userHome, ".dungeon-master-manager/.global")
        if (!globalDir.exists()) {
            globalDir.mkdirs()
        }
        val dbFile = File(globalDir, "entities.db")
        createDatabase(dbFile)
    }

    // A map to cache project databases
    private val projectDatabases = mutableMapOf<String, AppDatabase>()

    fun getProjectDatabase(projectPath: String): AppDatabase {
        return projectDatabases.getOrPut(projectPath) {
            val dbFile = File(projectPath, ".entities.db")
            createDatabase(dbFile)
        }
    }

    private fun createDatabase(dbFile: File): AppDatabase {
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        
        if (!dbFile.exists() || dbFile.length() == 0L) {
            AppDatabase.Schema.create(driver)
        }
        
        return AppDatabase(driver)
    }
}
