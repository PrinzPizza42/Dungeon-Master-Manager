package de.luca.dungeon_master_manager

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val isEnteringEditor = targetState is Screen.Editor
                    val enterAnim = if (isEnteringEditor) {
                        fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                            scaleIn(initialScale = 0.97f, animationSpec = tween(240, easing = FastOutSlowInEasing))
                    } else {
                        fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                            scaleIn(initialScale = 1.03f, animationSpec = tween(240, easing = FastOutSlowInEasing))
                    }
                    val exitAnim = if (isEnteringEditor) {
                        fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                            scaleOut(targetScale = 1.03f, animationSpec = tween(200, easing = FastOutSlowInEasing))
                    } else {
                        fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                            scaleOut(targetScale = 0.97f, animationSpec = tween(200, easing = FastOutSlowInEasing))
                    }
                    enterAnim togetherWith exitAnim
                },
                contentKey = { screen ->
                    when (screen) {
                        is Screen.Launcher -> "Launcher"
                        is Screen.Editor -> screen.project.path
                    }
                },
                label = "ScreenNavigationTransition",
                modifier = Modifier.fillMaxSize()
            ) { screen ->
                when (screen) {
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
                                    onProjectSelected = { currentScreen = Screen.Editor(it) },
                                    onOpenEntityPopout = onOpenEntityPopout
                                )
                            }
                        }
                    }
                    is Screen.Editor -> {
                        // key() ensures ViewModel is recreated when switching projects
                        key(screen.project.path) {
                            val viewModel = remember(screen.project.path) { ProjectEditorViewModel(screen.project) }
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
}
