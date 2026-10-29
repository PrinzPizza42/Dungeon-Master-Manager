package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.luca.dungeon_master_manager.data.ProjectInfo
import de.luca.dungeon_master_manager.data.AppSettingsRepository
import de.luca.dungeon_master_manager.ui.theme.*
import de.luca.dungeon_master_manager.viewmodel.EntityViewModel
import de.luca.dungeon_master_manager.viewmodel.ProjectLauncherViewModel
import dungeon_master_manager.shared.generated.resources.*
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProjectLauncherScreen(
    onProjectSelected: (ProjectInfo) -> Unit,
    onOpenEntityPopout: (de.luca.dungeon_master_manager.data.EntityPopoutRequest) -> Unit = {},
    viewModel: ProjectLauncherViewModel = viewModel { ProjectLauncherViewModel() },
) {
    val projects by viewModel.projects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<ProjectInfo?>(null) }
    
    val entityViewModel = remember { EntityViewModel(null, null) }
    var showGlobalEntityDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val appSettings by AppSettingsRepository.settings.collectAsState()

    if (!appSettings.isBatterySaverMode) {
        // FULL WAR ROOM ATMOSPHERE MODE
        GrimoireAmbientChamber(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                val tableWidth = minOf(maxWidth, 1260.dp)
                val tableHeight = minOf(maxHeight, 820.dp)

                // Grand War Table on the chamber floor
                Box(
                    modifier = Modifier.size(tableWidth, tableHeight)
                ) {
                    // 0. Fur Pelt Rug on stone floor peeking out from under bottom-left of table
                    TabletopFurPelt(
                        tableWidth = tableWidth,
                        tableHeight = tableHeight
                    )

                    // 1. Realistic Deep Table Drop Shadow
                    RealisticTableDropShadow(
                        shape = RoundedCornerShape(14.dp)
                    )

                    // 2. Grand War Table Surface (Cropped pure wood & carved border)
                    Image(
                        painter = painterResource(Res.drawable.tabletop_hero),
                        contentDescription = "Grand War Table",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                    )

                    // 3. Tabletop Props on the wood desk around the book:
                    val quillBitmap = imageResource(Res.drawable.prop_quill)
                    Image(
                        bitmap = quillBitmap,
                        contentDescription = "Quill and Ink",
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = 12.dp, y = (-20).dp)
                            .requiredSize(140.dp, 140.dp)
                            .graphicsLayer {
                                rotationZ = -16f
                                alpha = 0.94f
                            }
                    )

                    val diceBitmap = imageResource(Res.drawable.dnd_dice)
                    Image(
                        bitmap = diceBitmap,
                        contentDescription = "RPG Dice",
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-28).dp, y = (-36).dp)
                            .requiredSize(130.dp, 74.dp)
                            .graphicsLayer {
                                rotationZ = 14f
                                alpha = 0.94f
                            }
                    )

                    val coinsBitmap = imageResource(Res.drawable.prop_coins)
                    Image(
                        bitmap = coinsBitmap,
                        contentDescription = "Coins",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-36).dp, y = 44.dp)
                            .requiredSize(105.dp, 105.dp)
                            .graphicsLayer {
                                rotationZ = 20f
                                alpha = 0.94f
                            }
                    )

                    val sealBitmap = imageResource(Res.drawable.prop_seal)
                    Image(
                        bitmap = sealBitmap,
                        contentDescription = "Wax Seal",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = 44.dp, y = (-36).dp)
                            .requiredSize(72.dp, 78.dp)
                            .graphicsLayer {
                                rotationZ = -8f
                                alpha = 0.96f
                            }
                    )

                    // 4. Centered Campaign Tome Folio
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .widthIn(min = 340.dp, max = 780.dp)
                            .fillMaxWidth(0.72f)
                            .fillMaxHeight(0.90f)
                            .graphicsLayer {
                                shadowElevation = 18.dp.toPx()
                                shape = RoundedCornerShape(10.dp)
                            }
                            .background(
                                color = GrimoireColors.SurfaceCard,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    listOf(GrimoireColors.BorderBronze, GrimoireColors.GoldMuted, GrimoireColors.BorderBronze)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .tiledTexture(
                                resource = Res.drawable.grimoire_vellum,
                                alpha = 0.35f
                            )
                            .padding(horizontal = 36.dp, vertical = 24.dp)
                    ) {
                        LauncherFolioContent(
                            projects = projects,
                            isLoading = isLoading,
                            error = error,
                            onClearError = { viewModel.clearError() },
                            onProjectSelected = onProjectSelected,
                            onDeleteProject = { projectToDelete = it },
                            onOpenGlobalEntities = { showGlobalEntityDialog = true },
                            onOpenSettings = { showSettingsDialog = true },
                            onCreateProject = { showCreateDialog = true }
                        )
                    }
                }
            }
        }
    } else {
        // BATTERY SAVER / PERFORMANCE MODE (Ultra-clean solid surface, no textures or shadows)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GrimoireColors.Background)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 420.dp, max = 880.dp)
                    .fillMaxWidth(0.82f)
                    .fillMaxHeight(0.92f)
                    .background(
                        color = GrimoireColors.SurfaceCard,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = GrimoireColors.BorderBronze,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 36.dp, vertical = 24.dp)
            ) {
                LauncherFolioContent(
                    projects = projects,
                    isLoading = isLoading,
                    error = error,
                    onClearError = { viewModel.clearError() },
                    onProjectSelected = onProjectSelected,
                    onDeleteProject = { projectToDelete = it },
                    onOpenGlobalEntities = { showGlobalEntityDialog = true },
                    onOpenSettings = { showSettingsDialog = true },
                    onCreateProject = { showCreateDialog = true }
                )
            }
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(onDismiss = { showSettingsDialog = false })
    }

// Create Dialog
    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createProject(name)
                showCreateDialog = false
            },
        )
    }

    // Delete Confirmation
    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = {
                Text(
                    "Delete Campaign",
                    style = MaterialTheme.typography.titleLarge,
                    color = GrimoireColors.GoldPrimary
                )
            },
            text = {
                Text(
                    "Delete \"${project.config.name}\"? This cannot be undone.",
                    color = GrimoireColors.ParchmentLight
                )
            },
            containerColor = GrimoireColors.SurfaceCard,
            shape = RoundedCornerShape(8.dp),
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProject(project)
                    projectToDelete = null
                }) {
                    Text("Delete", color = GrimoireColors.CrimsonError)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel", color = GrimoireColors.ParchmentMuted)
                }
            },
        )
    }

    if (showGlobalEntityDialog) {
        de.luca.dungeon_master_manager.ui.EntityManagerDialog(
            viewModel = entityViewModel,
            onDismiss = { showGlobalEntityDialog = false },
            isGlobalOnly = true,
            projectPath = null,
            onOpenEntityPopout = onOpenEntityPopout
        )
    }
}

@Composable
private fun ProjectCard(
    project: ProjectInfo,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    GrimoireFolioCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        onClick = onClick,
        borderHoverColor = GrimoireColors.GoldBright,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.config.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = GrimoireColors.GoldPrimary,
                )
                if (project.config.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        project.config.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = GrimoireColors.ParchmentMuted,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Campaign recorded on ${dateFormat.format(Date(project.config.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrimoireColors.ParchmentDim,
                )
            }
            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = GrimoireColors.ParchmentMuted
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = GrimoireColors.ParchmentMuted
                )
            }
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }

    val invalidChars = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')

    fun validateName(input: String): String? {
        return when {
            input.isBlank() -> "Name cannot be empty"
            input.any { it in invalidChars } -> "Name cannot contain: / \\ : * ? \" < > |"
            input.startsWith(".") -> "Name cannot start with a dot"
            else -> null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "New Campaign",
                style = MaterialTheme.typography.titleLarge,
                color = GrimoireColors.GoldPrimary
            )
        },
        containerColor = GrimoireColors.SurfaceCard,
        shape = RoundedCornerShape(8.dp),
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = if (it.isNotBlank()) validateName(it) else null
                    },
                    label = { Text("Campaign Name", color = GrimoireColors.ParchmentMuted) },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = GrimoireColors.CrimsonError) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GrimoireColors.GoldPrimary,
                        unfocusedBorderColor = GrimoireColors.BorderBronze,
                        focusedTextColor = GrimoireColors.ParchmentLight,
                        unfocusedTextColor = GrimoireColors.ParchmentLight,
                        cursorColor = GrimoireColors.GoldPrimary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val error = validateName(name)
                    if (error == null) {
                        onCreate(name.trim())
                    } else {
                        nameError = error
                    }
                },
                enabled = name.isNotBlank() && nameError == null,
            ) {
                Text(
                    "Create",
                    color = if (name.isNotBlank() && nameError == null) GrimoireColors.GoldPrimary else GrimoireColors.ParchmentDim
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = GrimoireColors.ParchmentMuted)
            }
        },
    )
}

@Composable
private fun LauncherFolioContent(
    projects: List<ProjectInfo>,
    isLoading: Boolean,
    error: String?,
    onClearError: () -> Unit,
    onProjectSelected: (ProjectInfo) -> Unit,
    onDeleteProject: (ProjectInfo) -> Unit,
    onOpenGlobalEntities: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar with title + action buttons (Spacious and responsive)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false).padding(end = 16.dp)
            ) {
                Text(
                    "Dungeon Master Manager",
                    style = MaterialTheme.typography.headlineMedium,
                    color = GrimoireColors.GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Select a campaign",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrimoireColors.ParchmentMuted,
                    maxLines = 1
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOpenGlobalEntities,
                    border = BorderStroke(1.dp, GrimoireColors.BorderBronze),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GrimoireColors.GoldPrimary
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Global Entities", maxLines = 1, softWrap = false)
                }

                OutlinedButton(
                    onClick = onOpenSettings,
                    border = BorderStroke(1.dp, GrimoireColors.BorderBronze),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GrimoireColors.GoldPrimary
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Settings", maxLines = 1, softWrap = false)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        GrimoireDivider(
            modifier = Modifier.fillMaxWidth(),
            showDiamond = true,
            color = GrimoireColors.BorderBronze
        )
        Spacer(Modifier.height(18.dp))

        // Error banner
        error?.let { errorMessage ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, GrimoireColors.CrimsonError),
                colors = CardDefaults.cardColors(
                    containerColor = GrimoireColors.ErrorContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        errorMessage,
                        modifier = Modifier.weight(1f),
                        color = GrimoireColors.OnErrorContainer,
                    )
                    TextButton(onClick = onClearError) {
                        Text("Dismiss", color = GrimoireColors.OnErrorContainer)
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GrimoireColors.GoldPrimary)
            }
        } else if (projects.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No campaigns yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = GrimoireColors.ParchmentLight,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Create your first campaign to begin your adventure",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrimoireColors.ParchmentMuted,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(projects, key = { it.path }) { project ->
                    ProjectCard(
                        project = project,
                        onClick = { onProjectSelected(project) },
                        onDelete = { onDeleteProject(project) },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onCreateProject,
            colors = ButtonDefaults.buttonColors(
                containerColor = GrimoireColors.GoldPrimary,
                contentColor = GrimoireColors.Background
            ),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                "+ New Campaign",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

