package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.luca.dungeon_master_manager.data.ProjectInfo
import de.luca.dungeon_master_manager.viewmodel.ProjectLauncherViewModel
import java.text.SimpleDateFormat
import java.util.*

import de.luca.dungeon_master_manager.viewmodel.EntityViewModel

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
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar with title + global entities button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Dungeon Master Manager",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedButton(onClick = { showGlobalEntityDialog = true }) {
                    Text("Global Entities")
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Select a campaign or create a new one",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            // Error snackbar
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            errorMessage,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (projects.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No campaigns yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Create your first campaign to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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

            Spacer(Modifier.height(16.dp))
            Button(onClick = { showCreateDialog = true }) {
                Text("+ New Campaign")
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
            title = { Text("Delete Campaign") },
            text = {
                Text(
                    "Delete \"${project.config.name}\" and all its files? This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProject(project)
                    projectToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel")
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

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.config.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (project.config.description.isNotBlank()) {
                    Text(
                        project.config.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Text(
                    "Created ${dateFormat.format(Date(project.config.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete"
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

    // Characters not allowed in folder names
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
        title = { Text("New Campaign") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = if (it.isNotBlank()) validateName(it) else null
                    },
                    label = { Text("Campaign Name") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
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
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
