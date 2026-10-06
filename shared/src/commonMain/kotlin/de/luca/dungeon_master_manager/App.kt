package de.luca.dungeon_master_manager

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import de.luca.dungeon_master_manager.data.EntityPopoutRequest
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import de.luca.dungeon_master_manager.data.ProjectInfo
import de.luca.dungeon_master_manager.ui.ProjectEditorScreen
import de.luca.dungeon_master_manager.ui.ProjectLauncherScreen
import de.luca.dungeon_master_manager.viewmodel.ProjectEditorViewModel

sealed class Screen {
    data object Launcher : Screen()
    data class Editor(val project: ProjectInfo) : Screen()
}

@Composable
fun App(
    applicationScope: androidx.compose.ui.window.ApplicationScope? = null,
    onOpenEntityPopout: (EntityPopoutRequest) -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Launcher) }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (val screen = currentScreen) {
                is Screen.Launcher -> ProjectLauncherScreen(
                    onProjectSelected = { currentScreen = Screen.Editor(it) }
                )
                is Screen.Editor -> {
                    // key() ensures ViewModel is recreated when switching projects
                    key(screen.project.path) {
                        val viewModel = remember { ProjectEditorViewModel(screen.project) }
                        ProjectEditorScreen(
                            viewModel = viewModel,
                            applicationScope = applicationScope,
                            onBackToLauncher = { currentScreen = Screen.Launcher },
                            onOpenEntityPopout = onOpenEntityPopout,
                        )
                    }
                }
            }
        }
    }
}