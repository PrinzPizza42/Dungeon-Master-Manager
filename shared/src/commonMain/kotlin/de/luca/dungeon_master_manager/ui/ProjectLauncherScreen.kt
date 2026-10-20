package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.luca.dungeon_master_manager.data.ProjectInfo
import de.luca.dungeon_master_manager.ui.theme.GrimoireCard
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import de.luca.dungeon_master_manager.ui.theme.GrimoireDivider
import de.luca.dungeon_master_manager.viewmodel.EntityViewModel
import de.luca.dungeon_master_manager.viewmodel.ProjectLauncherViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProjectLauncherScreen(
    onProjectSelected: (ProjectInfo) -> Unit,
    viewModel: ProjectLauncherViewModel = viewModel { ProjectLauncherViewModel() },
) {
    val projects by viewModel.projects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<ProjectInfo?>(null) }
    
    val entityViewModel = remember { EntityViewModel(null, null) }
    var showGlobalEntityDialog by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar with title + global entities button
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Dungeon Master Manager",
                        style = MaterialTheme.typography.displaySmall,
                        color = GrimoireColors.GoldPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Select a campaign or create a new chronicle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrimoireColors.ParchmentMuted,
                    )
                }

                OutlinedButton(
                    onClick = { showGlobalEntityDialog = true },
                    border = BorderStroke(1.dp, GrimoireColors.BorderBronze),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GrimoireColors.GoldPrimary
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Global Entities")
                }
            }

            Spacer(Modifier.height(20.dp))
            GrimoireDivider(
                modifier = Modifier.widthIn(max = 760.dp),
                showDiamond = true,
                color = GrimoireColors.BorderBronze
            )
            Spacer(Modifier.height(24.dp))

            // Error banner
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, GrimoireColors.CrimsonError),
                    colors = CardDefaults.cardColors(
                        containerColor = GrimoireColors.ErrorContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            errorMessage,
                            modifier = Modifier.weight(1f),
                            color = GrimoireColors.OnErrorContainer,
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
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
                    modifier = Modifier.weight(1f).widthIn(max = 760.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(projects, key = { it.path }) { project ->
                        ProjectCard(
                            project = project,
                            onClick = { onProjectSelected(project) },
                            onDelete = { projectToDelete = project },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { showCreateDialog = true },
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
                    "Delete \"${project.config.name}\" and all its chronicles? This cannot be undone.",
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
            isGlobalOnly = true
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

    GrimoireCard(
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
                    "Chronicle recorded on ${dateFormat.format(Date(project.config.createdAt))}",
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
                "New Campaign Chronicle",
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
