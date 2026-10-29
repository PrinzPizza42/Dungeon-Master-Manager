package de.luca.dungeon_master_manager.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import de.luca.dungeon_master_manager.database.AppDatabase
import java.io.File

object DatabaseHelper {
    
    // Global database located in ~/.dungeon-master-manager/.global/entities.db
    val globalDatabase: AppDatabase by lazy {
        ProjectRepository.ensureGlobalDirs()
        val dbFile = File(ProjectRepository.globalDir, "entities.db")
        createDatabase(dbFile)
    }

    // A map to cache project databases
    private val projectDatabases = java.util.concurrent.ConcurrentHashMap<String, AppDatabase>()

    fun getProjectDatabase(projectPath: String): AppDatabase {
        return projectDatabases.computeIfAbsent(projectPath) {
            val dbFile = File(projectPath, ".entities.db")
            createDatabase(dbFile)
        }
    }

    private fun createDatabase(dbFile: File): AppDatabase {
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        
        // Execute schema if new database
        if (!dbFile.exists() || dbFile.length() == 0L) {
            AppDatabase.Schema.create(driver)
        } else {
            // Check if tables exist
            try {
                driver.executeQuery(null, "SELECT 1 FROM Entity LIMIT 1;", { QueryResult.Unit }, 0)
            } catch (e: Exception) {
                // Table doesn't exist, create schema
                AppDatabase.Schema.create(driver)
            }
        }
        
        return AppDatabase(driver)
    }
}
