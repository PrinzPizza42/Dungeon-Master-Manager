package de.luca.dungeon_master_manager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.luca.dungeon_master_manager.data.FileExtensions
import de.luca.dungeon_master_manager.ui.theme.GrimoireCard
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import de.luca.dungeon_master_manager.ui.theme.GrimoireDivider
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePickerDialog(
    projectPath: String,
    initialTitle: String = "",
    onDismiss: () -> Unit,
    onInsert: (title: String, relativePath: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var images by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var titleText by remember { mutableStateOf(initialTitle) }

    fun refreshImages() {
        images = ImagePickerHelper.scanProjectImages(projectPath)
    }

    LaunchedEffect(projectPath) {
        refreshImages()
    }

    val filteredImages = remember(images, searchQuery) {
        if (searchQuery.isBlank()) images
        else images.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            ImagePickerHelper.toRelativeProjectPath(it, projectPath).contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        GrimoireCard(
            modifier = Modifier
                .widthIn(min = 520.dp, max = 640.dp)
                .heightIn(min = 450.dp, max = 600.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            borderColor = GrimoireColors.BorderBronze,
            containerColor = GrimoireColors.SurfaceCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bild verlinken",
                        style = MaterialTheme.typography.titleLarge,
                        color = GrimoireColors.GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("✕", color = GrimoireColors.ParchmentMuted, fontSize = 16.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Search and Import bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text("Bilder filtern...", color = GrimoireColors.ParchmentMuted, fontSize = 13.sp)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GrimoireColors.GoldPrimary,
                            unfocusedBorderColor = GrimoireColors.BorderBronze,
                            focusedTextColor = GrimoireColors.ParchmentLight,
                            unfocusedTextColor = GrimoireColors.ParchmentLight,
                            cursorColor = GrimoireColors.GoldPrimary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                        textStyle = TextStyle(fontSize = 13.sp)
                    )

                    OutlinedButton(
                        onClick = {
                            val picked = pickExternalImageFileDialog()
                            if (picked != null && picked.exists()) {
                                val imported = ImagePickerHelper.importExternalImage(picked, projectPath)
                                if (imported != null) {
                                    refreshImages()
                                    selectedFile = imported
                                    if (titleText.isBlank()) {
                                        titleText = imported.nameWithoutExtension
                                    }
                                }
                            }
                        },
                        border = BorderStroke(1.dp, GrimoireColors.BorderGold),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = GrimoireColors.GoldContainer.copy(alpha = 0.5f),
                            contentColor = GrimoireColors.GoldBright
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("+ Import...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(10.dp))
                GrimoireDivider()
                Spacer(Modifier.height(10.dp))

                // Image list area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(GrimoireColors.SurfaceLow, RoundedCornerShape(6.dp))
                        .border(1.dp, GrimoireColors.BorderBronze.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    if (images.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Keine Bilder im Projekt gefunden.",
                                color = GrimoireColors.ParchmentMuted,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val picked = pickExternalImageFileDialog()
                                    if (picked != null && picked.exists()) {
                                        val imported = ImagePickerHelper.importExternalImage(picked, projectPath)
                                        if (imported != null) {
                                            refreshImages()
                                            selectedFile = imported
                                            if (titleText.isBlank()) {
                                                titleText = imported.nameWithoutExtension
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GrimoireColors.GoldContainer,
                                    contentColor = GrimoireColors.GoldBright
                                ),
                                border = BorderStroke(1.dp, GrimoireColors.BorderGold),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("+ Externes Bild importieren", fontSize = 12.sp)
                            }
                        }
                    } else if (filteredImages.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Keine passenden Bilder für '$searchQuery' gefunden.",
                                color = GrimoireColors.ParchmentMuted,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredImages) { img ->
                                val isSelected = img.absolutePath == selectedFile?.absolutePath
                                val relPath = ImagePickerHelper.toRelativeProjectPath(img, projectPath)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isSelected) GrimoireColors.GoldPrimary.copy(alpha = 0.18f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) GrimoireColors.GoldPrimary else GrimoireColors.BorderBronze.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                selectedFile = img
                                                if (titleText.isBlank()) {
                                                    titleText = img.nameWithoutExtension
                                                }
                                            },
                                            onDoubleClick = {
                                                selectedFile = img
                                                val effectiveTitle = titleText.ifBlank { img.nameWithoutExtension }
                                                onInsert(effectiveTitle, relPath)
                                            }
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Thumbnail preview
                                    val bitmap = rememberEntityImage(img)
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(GrimoireColors.SurfaceCard)
                                            .border(0.5.dp, GrimoireColors.BorderBronze, RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap,
                                                contentDescription = img.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = GrimoireColors.GoldPrimary
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = img.name,
                                            color = if (isSelected) GrimoireColors.GoldPrimary else GrimoireColors.ParchmentLight,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = relPath,
                                            color = GrimoireColors.ParchmentMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Title input
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = {
                        Text("Link-Titel", color = GrimoireColors.GoldPrimary, fontSize = 11.sp)
                    },
                    placeholder = {
                        Text(selectedFile?.nameWithoutExtension ?: "Bildtitel eingeben...", color = GrimoireColors.ParchmentMuted, fontSize = 13.sp)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GrimoireColors.GoldPrimary,
                        unfocusedBorderColor = GrimoireColors.BorderBronze,
                        focusedTextColor = GrimoireColors.ParchmentLight,
                        unfocusedTextColor = GrimoireColors.ParchmentLight,
                        cursorColor = GrimoireColors.GoldPrimary
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    textStyle = TextStyle(fontSize = 13.sp)
                )

                Spacer(Modifier.height(14.dp))

                // Footer action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = GrimoireColors.ParchmentMuted)
                    ) {
                        Text("Abbrechen", fontSize = 13.sp)
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val file = selectedFile
                            if (file != null) {
                                val effectiveTitle = titleText.ifBlank { file.nameWithoutExtension }
                                val relPath = ImagePickerHelper.toRelativeProjectPath(file, projectPath)
                                onInsert(effectiveTitle, relPath)
                            }
                        },
                        enabled = selectedFile != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GrimoireColors.GoldPrimary,
                            contentColor = GrimoireColors.Background,
                            disabledContainerColor = GrimoireColors.BorderBronze.copy(alpha = 0.5f),
                            disabledContentColor = GrimoireColors.ParchmentMuted.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Einfügen", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun pickExternalImageFileDialog(): File? {
    val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Select Image", java.awt.FileDialog.LOAD)
    dialog.setFilenameFilter { _, name ->
        val ext = name.substringAfterLast('.', "").lowercase()
        FileExtensions.isImage(ext)
    }
    dialog.isVisible = true
    val dir = dialog.directory
    val file = dialog.file
    return if (dir != null && file != null) File(dir, file) else null
}
