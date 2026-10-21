package de.luca.dungeon_master_manager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import de.luca.dungeon_master_manager.data.EntityPopoutRequest
import de.luca.dungeon_master_manager.data.ProjectInfo
import de.luca.dungeon_master_manager.ui.ProjectEditorScreen
import de.luca.dungeon_master_manager.ui.ProjectLauncherScreen
import de.luca.dungeon_master_manager.ui.components.GrimoireTitleBar
import de.luca.dungeon_master_manager.ui.theme.GrimoireTheme
import de.luca.dungeon_master_manager.viewmodel.ProjectEditorViewModel
import java.awt.Window

sealed class Screen {
    data object Launcher : Screen()
    data class Editor(val project: ProjectInfo) : Screen()
}

@Composable
fun App(
    applicationScope: androidx.compose.ui.window.ApplicationScope? = null,
    window: Window? = null,
    windowState: androidx.compose.ui.window.WindowState? = null,
    onCloseRequest: () -> Unit = {},
    onTitleChange: (String) -> Unit = {},
    onOpenEntityPopout: (EntityPopoutRequest) -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Launcher) }

    GrimoireTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (val screen = currentScreen) {
                is Screen.Launcher -> {
                    LaunchedEffect(Unit) {
                        onTitleChange("Dungeon Master Manager")
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (window != null && windowState != null) {
                            GrimoireTitleBar(
                                window = window,
                                title = "Dungeon Master Manager",
                                windowState = windowState,
                                onCloseRequest = onCloseRequest,
                            )
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            ProjectLauncherScreen(
                                onProjectSelected = { currentScreen = Screen.Editor(it) }
                            )
                        }
                    }
                }
                is Screen.Editor -> {
                    // key() ensures ViewModel is recreated when switching projects
                    key(screen.project.path) {
                        val viewModel = remember { ProjectEditorViewModel(screen.project) }
                        ProjectEditorScreen(
                            viewModel = viewModel,
                            applicationScope = applicationScope,
                            window = window,
                            windowState = windowState,
                            onCloseRequest = onCloseRequest,
                            onTitleChange = onTitleChange,
                            onBackToLauncher = { currentScreen = Screen.Launcher },
                            onOpenEntityPopout = onOpenEntityPopout,
                        )
                    }
                }
            }
        }
    }
}
