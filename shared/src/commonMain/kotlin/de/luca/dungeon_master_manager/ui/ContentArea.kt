package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.luca.dungeon_master_manager.data.EditorTab

/**
 * Dispatches content rendering based on the active tab's file extension.
 * Phase 3+ will add specialized editors; for now, shows raw text or placeholders.
 */
@Composable
fun ContentArea(
    activeTab: EditorTab?,
    fileContent: String?,
    isLoading: Boolean,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenEntityPopout: (String) -> Unit = {},
    entityColors: Map<String, String> = emptyMap(),
    projectPath: String = ""
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            activeTab == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "⚔",
                            style = MaterialTheme.typography.displayLarge,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Open a file from the sidebar",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Loading...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                // Use viewer based on file extension
                when (activeTab.extension) {
                    "md" -> key(activeTab.filePath) {
                        MarkdownEditor(
                            content = fileContent ?: "", 
                            onContentChange = onContentChange,
                            modifier = Modifier.fillMaxSize(),
                            onOpenEntityPopout = onOpenEntityPopout,
                            entityColors = entityColors
                        )
                    }
                    "map" -> key(activeTab.filePath) {
                        MapViewer(
                            projectPath = projectPath,
                            content = fileContent ?: "",
                            onContentChange = onContentChange,
                            onOpenEntityPopout = onOpenEntityPopout
                        )
                    }
                    "png", "jpg", "jpeg", "gif", "bmp" -> PlaceholderContentView(
                        "Image Viewer",
                        "Image display coming in a future phase"
                    )
                    else -> RawTextContentView(fileContent ?: "", modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}



/**
 * Raw text viewer for unknown file types.
 */
@Composable
fun RawTextContentView(content: String, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    Box(modifier = modifier.padding(16.dp).verticalScroll(scrollState)) {
        Text(
            content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        )
    }
}

/**
 * Placeholder for content types not yet implemented.
 */
@Composable
fun PlaceholderContentView(title: String, message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
