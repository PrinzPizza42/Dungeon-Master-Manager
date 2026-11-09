package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.luca.dungeon_master_manager.data.AppSettingsRepository
import de.luca.dungeon_master_manager.data.KeybindingRegistry
import de.luca.dungeon_master_manager.ui.components.EditorFontSizeControl
import de.luca.dungeon_master_manager.ui.components.GrimoireShortcutDisplay
import de.luca.dungeon_master_manager.ui.theme.GrimoireCard
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import de.luca.dungeon_master_manager.ui.theme.GrimoireDivider
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    val settings by AppSettingsRepository.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var recordingActionId by remember { mutableStateOf<String?>(null) }
    val recordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(recordingActionId) {
        if (recordingActionId != null) {
            recordFocusRequester.requestFocus()
        }
    }

    DisposableEffect(recordingActionId) {
        val actionId = recordingActionId
        if (actionId != null) {
            val dispatcher = java.awt.KeyEventDispatcher { event ->
                if (event.id == java.awt.event.KeyEvent.KEY_PRESSED) {
                    if (event.keyCode == java.awt.event.KeyEvent.VK_ESCAPE) {
                        recordingActionId = null
                        return@KeyEventDispatcher true
                    }
                    val combo = KeybindingRegistry.awtEventToShortcut(event)
                    if (combo != null) {
                        scope.launch {
                            AppSettingsRepository.setShortcut(actionId, combo)
                            recordingActionId = null
                        }
                        return@KeyEventDispatcher true
                    }
                }
                false
            }
            java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher)
            onDispose {
                java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher)
            }
        } else {
            onDispose {}
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        GrimoireCard(
            modifier = Modifier
                .widthIn(min = 480.dp, max = 640.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            borderColor = GrimoireColors.BorderBronze,
            containerColor = GrimoireColors.SurfaceCard
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = GrimoireColors.GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))
                GrimoireDivider(showDiamond = true, color = GrimoireColors.BorderBronze)
                Spacer(Modifier.height(20.dp))

                // Setting: Battery Saver Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = "Battery Saver / Performance Mode",
                            style = MaterialTheme.typography.titleSmall,
                            color = GrimoireColors.ParchmentLight,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Disables heavy tabletop textures, stone floors, 3D shadows, and desk props for maximum performance and battery life.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GrimoireColors.ParchmentMuted
                        )
                    }

                    Switch(
                        checked = settings.isBatterySaverMode,
                        onCheckedChange = { isChecked ->
                            scope.launch {
                                AppSettingsRepository.setBatterySaverMode(isChecked)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GrimoireColors.GoldPrimary,
                            checkedTrackColor = GrimoireColors.GoldContainer,
                            uncheckedThumbColor = GrimoireColors.ParchmentDim,
                            uncheckedTrackColor = GrimoireColors.Surface
                        )
                    )
                }

                Spacer(Modifier.height(16.dp))
                GrimoireDivider(showDiamond = false, color = GrimoireColors.BorderBronzeSubtle)
                Spacer(Modifier.height(16.dp))

                // Setting: Editor Font Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = "Editor Font Size",
                            style = MaterialTheme.typography.titleSmall,
                            color = GrimoireColors.ParchmentLight,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Adjusts the base text size for the Markdown and Plaintext document editors.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GrimoireColors.ParchmentMuted
                        )
                    }

                    EditorFontSizeControl(
                        fontSize = settings.editorFontSize,
                        onFontSizeChange = { newSize ->
                            scope.launch {
                                AppSettingsRepository.setEditorFontSize(newSize)
                            }
                        }
                    )
                }

                Spacer(Modifier.height(20.dp))
                GrimoireDivider(showDiamond = false, color = GrimoireColors.BorderBronzeSubtle)
                Spacer(Modifier.height(16.dp))

                // Section: Keyboard Shortcuts
                Text(
                    text = "Keyboard Shortcuts",
                    style = MaterialTheme.typography.titleSmall,
                    color = GrimoireColors.GoldPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Customize global shortcuts to your preferences.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrimoireColors.ParchmentMuted
                )
                Spacer(Modifier.height(12.dp))

                for (binding in KeybindingRegistry.ALL) {
                    val currentShortcut = settings.shortcuts[binding.id] ?: binding.defaultShortcut
                    val isRecording = recordingActionId == binding.id
                    val isCustomized = currentShortcut != binding.defaultShortcut

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                text = binding.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = GrimoireColors.ParchmentLight,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = binding.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = GrimoireColors.ParchmentDim,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isRecording) {
                                Box(
                                    modifier = Modifier
                                        .focusRequester(recordFocusRequester)
                                        .focusable()
                                        .onPreviewKeyEvent { event ->
                                            if (event.key == Key.Escape) {
                                                recordingActionId = null
                                                return@onPreviewKeyEvent true
                                            }
                                            val combo = KeybindingRegistry.keyEventToShortcut(event)
                                            if (combo != null) {
                                                scope.launch {
                                                    AppSettingsRepository.setShortcut(binding.id, combo)
                                                    recordingActionId = null
                                                }
                                                return@onPreviewKeyEvent true
                                            }
                                            false
                                        }
                                        .background(
                                            GrimoireColors.GoldContainer.copy(alpha = 0.5f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            1.dp,
                                            GrimoireColors.GoldPrimary,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Press shortcut keys... (Esc to cancel)",
                                        fontSize = 12.sp,
                                        color = GrimoireColors.GoldBright,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clickable { recordingActionId = binding.id }
                                        .padding(end = 4.dp)
                                ) {
                                    GrimoireShortcutDisplay(
                                        shortcutString = currentShortcut,
                                        isHighlighted = isCustomized
                                    )
                                }

                                OutlinedButton(
                                    onClick = { recordingActionId = binding.id },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, GrimoireColors.BorderBronze),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = GrimoireColors.ParchmentLight
                                    ),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Change", fontSize = 12.sp)
                                }

                                if (isCustomized) {
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                AppSettingsRepository.resetShortcut(binding.id)
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text(
                                            "Reset",
                                            fontSize = 11.sp,
                                            color = GrimoireColors.GoldMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                GrimoireDivider(showDiamond = false, color = GrimoireColors.BorderBronzeSubtle)
                Spacer(Modifier.height(16.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GrimoireColors.GoldPrimary,
                            contentColor = GrimoireColors.Background
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
