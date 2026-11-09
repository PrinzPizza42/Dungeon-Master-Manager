package de.luca.dungeon_master_manager.data

import de.luca.dungeonmastermanager.database.Entity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

enum class SearchItemType {
    FILE,
    HEADING,
    ENTITY,
    MAP_MARKER,
    COMMAND,
    CREATE_NOTE
}

enum class SearchScope {
    PROJECT,
    GLOBAL
}

data class SearchResultItem(
    val id: String,
    val type: SearchItemType,
    val title: String,
    val subtitle: String,
    val filePath: String? = null,
    val relativePath: String? = null,
    val scope: SearchScope = SearchScope.PROJECT,
    val extension: String = "",
    val isImage: Boolean = false,
    val thumbnailFile: File? = null,
    val entityId: String? = null,
    val entityType: String? = null,
    val entityColor: String? = null,
    val markerId: String? = null,
    val headingIndex: Int? = null,
    val headingLevel: Int? = null,
    val matchedIndices: List<Int> = emptyList(),
    val previewSnippet: String? = null,
    val commandActionId: String? = null,
    val score: Double = 0.0
)

data class ParsedPrefix(
    val cleanQuery: String,
    val scopeFilter: SearchScope? = null,
    val typeFilter: SearchItemType? = null,
    val extensionFilter: Set<String>? = null,
    val isCommandMode: Boolean = false
)

class UniversalSearchService(
    private val projectPath: String
) {
    private val json = Json { ignoreUnknownKeys = true }

    data class CachedMarker(
        val id: String,
        val title: String,
        val entityId: String?,
        val notes: String?,
        val iconFileName: String?
    )

    data class CachedHeading(
        val lineIndex: Int,
        val text: String,
        val level: Int
    )

    private data class CachedFile(
        val file: File,
        val relativePath: String,
        val scope: SearchScope,
        val extension: String,
        val isImage: Boolean,
        val headings: List<CachedHeading> = emptyList(),
        val markers: List<CachedMarker> = emptyList()
    )

    private var cachedProjectFiles: List<CachedFile>? = null
    private var cachedGlobalFiles: List<CachedFile>? = null
    private var cachedEntities: List<Pair<Entity, SearchScope>>? = null
    private var lastCacheTime: Long = 0L

    private val cacheTtlMs = 4000L // 4 seconds TTL

    fun invalidateCache() {
        cachedProjectFiles = null
        cachedGlobalFiles = null
        cachedEntities = null
        lastCacheTime = 0L
    }

    private suspend fun ensureIndex() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedProjectFiles != null && (now - lastCacheTime) < cacheTtlMs) {
            return@withContext
        }

        // 1. Index Project Files
        val pDir = File(projectPath)
        val pList = mutableListOf<CachedFile>()
        if (pDir.exists() && pDir.isDirectory) {
            collectFiles(pDir, pDir, SearchScope.PROJECT, pList)
        }
        cachedProjectFiles = pList

        // 2. Index Global Files
        ProjectRepository.ensureGlobalDirs()
        val gDir = ProjectRepository.globalDir
        val gList = mutableListOf<CachedFile>()
        if (gDir.exists() && gDir.isDirectory) {
            collectFiles(gDir, gDir, SearchScope.GLOBAL, gList)
        }
        cachedGlobalFiles = gList

        // 3. Index SQLite Entities
        val entityList = mutableListOf<Pair<Entity, SearchScope>>()
        try {
            val globalDb = DatabaseHelper.globalDatabase
            val gEntities = globalDb.entityQueries.selectAll().executeAsList()
            for (ent in gEntities) {
                entityList.add(ent to SearchScope.GLOBAL)
            }
        } catch (_: Exception) {}

        try {
            val projDb = DatabaseHelper.getProjectDatabase(projectPath)
            val pEntities = projDb.entityQueries.selectAll().executeAsList()
            for (ent in pEntities) {
                entityList.add(ent to SearchScope.PROJECT)
            }
        } catch (_: Exception) {}
        cachedEntities = entityList

        lastCacheTime = now
    }

    private fun collectFiles(
        root: File,
        current: File,
        scope: SearchScope,
        destination: MutableList<CachedFile>
    ) {
        val files = current.listFiles() ?: return
        for (f in files) {
            // Exclude hidden files / system directories / cache
            if (f.name.startsWith(".")) continue
            if (f.isDirectory) {
                if (f.name == "map-cache" || f.name == "build" || f.name == "node_modules") continue
                collectFiles(root, f, scope, destination)
            } else {
                if (f.extension == "db" || f.extension == "db-journal") continue

                val rel = f.relativeTo(root).path
                val ext = f.extension.lowercase()
                val isImg = FileExtensions.isImage(ext)

                // If markdown file, extract outline headings (H1, H2, H3)
                val headings = if (ext == "md") {
                    try {
                        f.readLines()
                            .mapIndexedNotNull { idx, line ->
                                val trimmed = line.trimStart()
                                val (rawText, level) = when {
                                    trimmed.startsWith("### ") -> trimmed.removePrefix("### ").trim() to 3
                                    trimmed.startsWith("## ") -> trimmed.removePrefix("## ").trim() to 2
                                    trimmed.startsWith("# ") -> trimmed.removePrefix("# ").trim() to 1
                                    else -> null to null
                                }
                                if (rawText != null && level != null && rawText.isNotBlank()) {
                                    CachedHeading(lineIndex = idx, text = rawText, level = level)
                                } else null
                            }
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                // If map file, extract markers
                val markers = if (ext == "map") {
                    try {
                        val text = f.readText()
                        if (text.isNotBlank()) {
                            val mapData = json.decodeFromString<MapData>(text)
                            mapData.markers.map { m ->
                                CachedMarker(
                                    id = m.id,
                                    title = m.title ?: "",
                                    entityId = m.entityId,
                                    notes = m.notes,
                                    iconFileName = m.iconFileName
                                )
                            }
                        } else emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                destination.add(
                    CachedFile(
                        file = f,
                        relativePath = rel,
                        scope = scope,
                        extension = ext,
                        isImage = isImg,
                        headings = headings,
                        markers = markers
                    )
                )
            }
        }
    }

    fun parseQuery(rawQuery: String): ParsedPrefix {
        val trimmed = rawQuery.trim()
        if (trimmed.startsWith(">")) {
            return ParsedPrefix(
                cleanQuery = trimmed.removePrefix(">").trim(),
                isCommandMode = true
            )
        }

        if (trimmed.startsWith("@")) {
            return ParsedPrefix(
                cleanQuery = trimmed.removePrefix("@").trim(),
                typeFilter = SearchItemType.HEADING
            )
        }

        if (trimmed.startsWith("#")) {
            return ParsedPrefix(
                cleanQuery = trimmed.dropWhile { it == '#' }.trim(),
                typeFilter = SearchItemType.HEADING
            )
        }

        val prefixColon = trimmed.indexOf(':')
        if (prefixColon in 1..8) {
            val prefix = trimmed.substring(0, prefixColon).lowercase().trim()
            val queryAfter = trimmed.substring(prefixColon + 1).trim()

            when (prefix) {
                "m", "map" -> return ParsedPrefix(
                    cleanQuery = queryAfter,
                    extensionFilter = setOf("map")
                )
                "img", "b", "image" -> return ParsedPrefix(
                    cleanQuery = queryAfter,
                    extensionFilter = FileExtensions.IMAGE_EXTENSIONS
                )
                "h", "heading" -> return ParsedPrefix(
                    cleanQuery = queryAfter,
                    typeFilter = SearchItemType.HEADING
                )
                "pin", "marker" -> return ParsedPrefix(
                    cleanQuery = queryAfter,
                    typeFilter = SearchItemType.MAP_MARKER
                )
                "e", "npc", "ent", "entity" -> return ParsedPrefix(
                    cleanQuery = queryAfter,
                    typeFilter = SearchItemType.ENTITY
                )
                "p", "proj", "project" -> return ParsedPrefix(
                    cleanQuery = queryAfter,
                    scopeFilter = SearchScope.PROJECT
                )
                "g", "glob", "global" -> return ParsedPrefix(
                    cleanQuery = queryAfter,
                    scopeFilter = SearchScope.GLOBAL
                )
            }
        }

        return ParsedPrefix(cleanQuery = trimmed)
    }

    suspend fun search(
        rawQuery: String,
        openTabPaths: List<String> = emptyList(),
        scopeFilterOverride: SearchScope? = null
    ): List<SearchResultItem> = withContext(Dispatchers.Default) {
        ensureIndex()

        val parsed = parseQuery(rawQuery)
        val activeScopeFilter = scopeFilterOverride ?: parsed.scopeFilter

        // 1. Command Mode
        if (parsed.isCommandMode) {
            return@withContext searchCommands(parsed.cleanQuery)
        }

        val projectFiles = cachedProjectFiles ?: emptyList()
        val globalFiles = cachedGlobalFiles ?: emptyList()
        val entities = cachedEntities ?: emptyList()

        // 2. Empty query: Recent / Active files
        if (parsed.cleanQuery.isEmpty() && parsed.typeFilter == null && parsed.extensionFilter == null) {
            return@withContext buildRecentItems(openTabPaths, projectFiles, globalFiles, activeScopeFilter)
        }

        val results = mutableListOf<SearchResultItem>()
        val query = parsed.cleanQuery

        // 3. Search Files & Headings
        val candidateFiles = when (activeScopeFilter) {
            SearchScope.PROJECT -> projectFiles
            SearchScope.GLOBAL -> globalFiles
            null -> projectFiles + globalFiles
        }

        for (cf in candidateFiles) {
            if (parsed.extensionFilter != null && cf.extension !in parsed.extensionFilter) {
                continue
            }

            // Headings search
            if (parsed.typeFilter == null || parsed.typeFilter == SearchItemType.HEADING) {
                for (heading in cf.headings) {
                    val hMatch = FuzzyMatcher.match(query, heading.text)
                    if (hMatch != null) {
                        val scopeLabel = if (cf.scope == SearchScope.PROJECT) "Project" else "Global"
                        val levelHashes = "#".repeat(heading.level)
                        val levelWeight = when (heading.level) {
                            1 -> 1.0
                            2 -> 0.96
                            3 -> 0.92
                            else -> 0.90
                        }
                        results.add(
                            SearchResultItem(
                                id = "heading::${cf.file.absolutePath}::${heading.lineIndex}",
                                type = SearchItemType.HEADING,
                                title = heading.text,
                                subtitle = "$scopeLabel › ${cf.relativePath} ($levelHashes)",
                                filePath = cf.file.absolutePath,
                                relativePath = cf.relativePath,
                                scope = cf.scope,
                                extension = "md",
                                headingIndex = heading.lineIndex,
                                headingLevel = heading.level,
                                matchedIndices = hMatch.matchedIndices,
                                previewSnippet = loadHeadingPreviewSnippet(cf.file, heading.lineIndex),
                                score = hMatch.score * 0.95 * levelWeight
                            )
                        )
                    }
                }
            }

            // File search
            if (parsed.typeFilter == null || parsed.typeFilter == SearchItemType.FILE) {
                val nameMatch = FuzzyMatcher.match(query, cf.file.name)
                val pathMatch = if (nameMatch == null) FuzzyMatcher.match(query, cf.relativePath) else null

                if (nameMatch != null || pathMatch != null) {
                    val score = nameMatch?.score ?: ((pathMatch?.score ?: 0.0) * 0.7)
                    val matchedIndices = nameMatch?.matchedIndices ?: emptyList()

                    val scopeLabel = if (cf.scope == SearchScope.PROJECT) "Project" else "Global"
                    val parentFolder = cf.file.parentFile?.name ?: ""
                    val subtitle = if (parentFolder.isNotEmpty()) "$scopeLabel › $parentFolder" else scopeLabel

                    results.add(
                        SearchResultItem(
                            id = "file::${cf.file.absolutePath}",
                            type = SearchItemType.FILE,
                            title = cf.file.name,
                            subtitle = subtitle,
                            filePath = cf.file.absolutePath,
                            relativePath = cf.relativePath,
                            scope = cf.scope,
                            extension = cf.extension,
                            isImage = cf.isImage,
                            thumbnailFile = if (cf.isImage) cf.file else null,
                            matchedIndices = matchedIndices,
                            previewSnippet = if (cf.extension == "md") loadPreviewSnippet(cf.file) else null,
                            score = score
                        )
                    )
                }
            }
        }

        // 4. Search Map Markers
        val entityById = entities.associate { it.first.id to it.first }
        if (parsed.typeFilter == null || parsed.typeFilter == SearchItemType.MAP_MARKER || parsed.extensionFilter?.contains("map") == true) {
            for (cf in candidateFiles) {
                if (cf.extension != "map") continue
                for (marker in cf.markers) {
                    val linkedEntity = marker.entityId?.let { entityById[it] }
                    val markerName = when {
                        marker.title.isNotBlank() -> marker.title
                        linkedEntity != null -> linkedEntity.name
                        else -> "Marker (${marker.id.take(6)})"
                    }

                    val markerMatch = FuzzyMatcher.match(query, markerName)
                    val notesMatch = if (markerMatch == null && !marker.notes.isNullOrBlank()) FuzzyMatcher.match(query, marker.notes) else null

                    if (markerMatch != null || notesMatch != null) {
                        val score = markerMatch?.score ?: ((notesMatch?.score ?: 0.0) * 0.7)
                        val matchedIndices = markerMatch?.matchedIndices ?: emptyList()

                        val scopeLabel = if (cf.scope == SearchScope.PROJECT) "Project" else "Global"
                        val subtitle = "$scopeLabel › Map: ${cf.file.name} › Marker"

                        results.add(
                            SearchResultItem(
                                id = "marker::${cf.file.absolutePath}::${marker.id}",
                                type = SearchItemType.MAP_MARKER,
                                title = markerName,
                                subtitle = subtitle,
                                filePath = cf.file.absolutePath,
                                relativePath = cf.relativePath,
                                scope = cf.scope,
                                extension = "map",
                                entityId = marker.entityId,
                                entityType = linkedEntity?.type,
                                entityColor = linkedEntity?.color,
                                markerId = marker.id,
                                matchedIndices = matchedIndices,
                                previewSnippet = marker.notes,
                                score = score * 0.98
                            )
                        )
                    }
                }
            }
        }

        // 5. Search Entities
        if (parsed.typeFilter == null || parsed.typeFilter == SearchItemType.ENTITY) {
            for ((ent, scope) in entities) {
                if (activeScopeFilter != null && scope != activeScopeFilter) continue

                val entMatch = FuzzyMatcher.match(query, ent.name)
                if (entMatch != null) {
                    val scopeLabel = if (scope == SearchScope.PROJECT) "Project" else "Global"
                    val sub = "$scopeLabel › ${ent.type}"

                    val thumbFile = ent.imageFilePath?.takeUnless { it.isBlank() }?.let { imgRel ->
                        val base = if (scope == SearchScope.PROJECT) File(projectPath) else ProjectRepository.globalImagesDir
                        val f = File(base, imgRel)
                        if (f.exists()) f else File(imgRel)
                    }

                    results.add(
                        SearchResultItem(
                            id = "entity::${ent.id}",
                            type = SearchItemType.ENTITY,
                            title = ent.name,
                            subtitle = sub,
                            filePath = ent.notesFilePath?.takeUnless { it.isBlank() },
                            scope = scope,
                            entityId = ent.id,
                            entityType = ent.type,
                            entityColor = ent.color,
                            isImage = thumbFile != null && thumbFile.exists(),
                            thumbnailFile = if (thumbFile != null && thumbFile.exists()) thumbFile else null,
                            matchedIndices = entMatch.matchedIndices,
                            score = entMatch.score * 1.05 // Entities prioritized
                        )
                    )
                }
            }
        }

        // 5. Sort by score descending
        val sorted = results.sortedByDescending { it.score }

        // 6. Instant Note Creation (if query doesn't match an existing file)
        val finalResults = sorted.toMutableList()
        if (query.isNotEmpty() && (parsed.typeFilter == null || parsed.typeFilter == SearchItemType.FILE)) {
            val fileName = if (query.endsWith(".md", true)) query else "$query.md"
            val exactExists = candidateFiles.any { it.file.name.equals(fileName, true) }

            if (!exactExists) {
                finalResults.add(
                    SearchResultItem(
                        id = "create_note::$fileName",
                        type = SearchItemType.CREATE_NOTE,
                        title = "➕ Create note '$fileName' [Shift + Enter]",
                        subtitle = "Create in project and open in tab",
                        filePath = File(projectPath, fileName).absolutePath,
                        scope = SearchScope.PROJECT,
                        extension = "md",
                        score = 0.0
                    )
                )
            }
        }

        finalResults
    }

    private fun buildRecentItems(
        openTabPaths: List<String>,
        projectFiles: List<CachedFile>,
        globalFiles: List<CachedFile>,
        scopeFilter: SearchScope?
    ): List<SearchResultItem> {
        val results = mutableListOf<SearchResultItem>()
        val seenPaths = mutableSetOf<String>()

        val allCandidates = when (scopeFilter) {
            SearchScope.PROJECT -> projectFiles
            SearchScope.GLOBAL -> globalFiles
            null -> projectFiles + globalFiles
        }
        val fileMap = allCandidates.associateBy { it.file.absolutePath }

        // 1. Open tabs first
        for (tabPath in openTabPaths) {
            val cf = fileMap[tabPath]
            if (cf != null && seenPaths.add(cf.file.absolutePath)) {
                val scopeLabel = if (cf.scope == SearchScope.PROJECT) "Project" else "Global"
                results.add(
                    SearchResultItem(
                        id = "recent::${cf.file.absolutePath}",
                        type = SearchItemType.FILE,
                        title = cf.file.name,
                        subtitle = "$scopeLabel (Active tab) › ${cf.relativePath}",
                        filePath = cf.file.absolutePath,
                        relativePath = cf.relativePath,
                        scope = cf.scope,
                        extension = cf.extension,
                        isImage = cf.isImage,
                        thumbnailFile = if (cf.isImage) cf.file else null,
                        previewSnippet = if (cf.extension == "md") loadPreviewSnippet(cf.file) else null,
                        score = 1000.0
                    )
                )
            }
        }

        // 2. Recent modified files
        val sortedByModified = allCandidates
            .filter { it.file.absolutePath !in seenPaths }
            .sortedByDescending { it.file.lastModified() }
            .take(8)

        for (cf in sortedByModified) {
            val scopeLabel = if (cf.scope == SearchScope.PROJECT) "Project" else "Global"
            results.add(
                SearchResultItem(
                    id = "recent_mod::${cf.file.absolutePath}",
                    type = SearchItemType.FILE,
                    title = cf.file.name,
                    subtitle = "$scopeLabel › ${cf.relativePath}",
                    filePath = cf.file.absolutePath,
                    relativePath = cf.relativePath,
                    scope = cf.scope,
                    extension = cf.extension,
                    isImage = cf.isImage,
                    thumbnailFile = if (cf.isImage) cf.file else null,
                    previewSnippet = if (cf.extension == "md") loadPreviewSnippet(cf.file) else null,
                    score = 500.0
                )
            )
        }

        return results
    }

    private fun searchCommands(query: String): List<SearchResultItem> {
        val commands = listOf(
            Triple("cmd_new_note", "Create new Markdown note", "Creates an empty .md file"),
            Triple("cmd_new_map", "Create new interactive map", "Creates a new .map file"),
            Triple("cmd_open_explorer", "Show project in file manager", "Opens OS file explorer"),
            Triple("cmd_open_settings", "Open settings", "Opens Grimoire settings window"),
            Triple("cmd_toggle_battery", "Toggle Battery Saver / Performance mode", "Toggles animations and atmospheric effects")
        )

        val results = mutableListOf<SearchResultItem>()
        for ((id, title, desc) in commands) {
            val match = if (query.isEmpty()) FuzzyMatchResult(100.0, emptyList()) else FuzzyMatcher.match(query, title)
            if (match != null) {
                results.add(
                    SearchResultItem(
                        id = "command::$id",
                        type = SearchItemType.COMMAND,
                        title = "> $title",
                        subtitle = desc,
                        commandActionId = id,
                        matchedIndices = match.matchedIndices,
                        score = match.score
                    )
                )
            }
        }
        return results.sortedByDescending { it.score }
    }

    private fun loadPreviewSnippet(file: File): String? {
        return try {
            file.bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() }
                    .take(6)
                    .joinToString("\n")
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun loadHeadingPreviewSnippet(file: File, headingLineIndex: Int): String? {
        return try {
            file.bufferedReader().useLines { linesSequence ->
                val lines = linesSequence.toList()
                val contentLines = mutableListOf<String>()
                for (i in (headingLineIndex + 1) until lines.size) {
                    val line = lines[i]
                    val trimmed = line.trimStart()
                    if (trimmed.startsWith("#")) {
                        break
                    }
                    if (line.isNotBlank()) {
                        contentLines.add(line.trim())
                        if (contentLines.size >= 5) break
                    }
                }
                if (contentLines.isNotEmpty()) contentLines.joinToString("\n") else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
