package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.luca.dungeon_master_manager.data.EditorTab
import de.luca.dungeon_master_manager.data.FileExtensions
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import java.awt.Desktop
import java.io.File

/**
 * Dispatches content rendering based on the active tab's file extension.
 */
@Composable
fun ContentArea(
    activeTab: EditorTab?,
    fileContent: String?,
    isLoading: Boolean,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenEntityPopout: (String) -> Unit = {},
    entities: List<de.luca.dungeonmastermanager.database.Entity> = emptyList(),
    projectPath: String = ""
) {
    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        when {
            activeTab == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                when {
                    FileExtensions.isMarkdown(activeTab.extension) -> key(activeTab.filePath) {
                        MarkdownEditor(
                            content = fileContent ?: "",
                            onContentChange = onContentChange,
                            modifier = Modifier.fillMaxSize(),
                            onOpenEntityPopout = onOpenEntityPopout,
                            entities = entities
                        )
                    }
                    FileExtensions.isMap(activeTab.extension) -> key(activeTab.filePath) {
                        MapViewer(
                            projectPath = projectPath,
                            content = fileContent ?: "",
                            onContentChange = onContentChange,
                            onOpenEntityPopout = onOpenEntityPopout
                        )
                    }
                    FileExtensions.isImage(activeTab.extension) -> key(activeTab.filePath) {
                        ImageViewer(
                            filePath = activeTab.filePath,
                            fileName = activeTab.fileName,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    FileExtensions.isText(activeTab.extension) -> key(activeTab.filePath) {
                        PlainTextEditor(
                            content = fileContent ?: "",
                            onContentChange = onContentChange,
                            fileName = activeTab.fileName,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> key(activeTab.filePath) {
                        ExternalFileContentView(
                            filePath = activeTab.filePath,
                            fileName = activeTab.fileName,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preview and external open view for arbitrary file types not directly editable inside the app.
 */
@Composable
fun ExternalFileContentView(
    filePath: String,
    fileName: String,
    modifier: Modifier = Modifier
) {
    val file = File(filePath)
    val ext = file.extension.lowercase()
    val exists = file.exists()
    val fileSizeFormatted = if (exists) {
        val bytes = file.length()
        when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes Bytes"
        }
    } else {
        "Datei nicht gefunden"
    }

    val icon = when {
        FileExtensions.isPdf(ext) -> "📕"
        FileExtensions.isAudio(ext) -> "🎵"
        FileExtensions.isArchive(ext) -> "📦"
        else -> "📄"
    }

    Box(
        modifier = modifier.background(GrimoireColors.Background).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .background(GrimoireColors.SurfaceCard, RoundedCornerShape(8.dp))
                .border(1.dp, GrimoireColors.BorderBronze, RoundedCornerShape(8.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(icon, fontSize = 48.sp)

                Text(
                    text = fileName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GrimoireColors.GoldPrimary
                )

                Text(
                    text = "Format: .${ext.uppercase()} • $fileSizeFormatted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrimoireColors.ParchmentMuted
                )

                Text(
                    text = "Dieser Dateityp kann nicht direkt im Grimoire editiert werden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrimoireColors.ParchmentDim
                )

                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            try {
                                if (Desktop.isDesktopSupported() && exists) {
                                    Desktop.getDesktop().open(file)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GrimoireColors.GoldPrimary,
                            contentColor = GrimoireColors.Background
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Im Standardprogramm öffnen", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                if (Desktop.isDesktopSupported()) {
                                    val parent = file.parentFile ?: file
                                    Desktop.getDesktop().open(parent)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = GrimoireColors.ParchmentLight
                        ),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GrimoireColors.BorderBronze)
                    ) {
                        Text("Ordner anzeigen")
                    }
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
