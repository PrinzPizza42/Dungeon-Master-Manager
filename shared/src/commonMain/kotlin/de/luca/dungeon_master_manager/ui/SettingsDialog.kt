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
import de.luca.dungeon_master_manager.data.KeybindingDefinition
import de.luca.dungeon_master_manager.data.KeybindingRegistry
import de.luca.dungeon_master_manager.data.KeybindingScope
import de.luca.dungeon_master_manager.ui.components.EditorFontSizeControl
import de.luca.dungeon_master_manager.ui.components.GrimoireShortcutDisplay
import de.luca.dungeon_master_manager.ui.theme.GrimoireCard
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import de.luca.dungeon_master_manager.ui.theme.GrimoireDivider
import kotlinx.coroutines.launch

private data class ShortcutConflictInfo(
    val targetDefinition: KeybindingDefinition,
    val conflictingDefinition: KeybindingDefinition,
    val newShortcut: String
)

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    val settings by AppSettingsRepository.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var recordingActionId by remember { mutableStateOf<String?>(null) }
    var pendingConflict by remember { mutableStateOf<ShortcutConflictInfo?>(null) }
    val recordFocusRequester = remember { FocusRequester() }

    val handleAssignShortcut: (String, String) -> Unit = { actionId, combo ->
        val conflict = KeybindingRegistry.findConflict(actionId, combo, settings.shortcuts)
        if (conflict != null) {
            val targetDef = KeybindingRegistry.ALL.find { it.id == actionId }
            if (targetDef != null) {
                pendingConflict = ShortcutConflictInfo(
                    targetDefinition = targetDef,
                    conflictingDefinition = conflict,
                    newShortcut = combo
                )
            }
            recordingActionId = null
        } else {
            scope.launch {
                AppSettingsRepository.setShortcut(actionId, combo)
                recordingActionId = null
            }
        }
    }

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
                        handleAssignShortcut(actionId, combo)
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
                .widthIn(min = 520.dp, max = 700.dp)
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
                    style = MaterialTheme.typography.titleMedium,
                    color = GrimoireColors.GoldPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Customize shortcuts to your preferences. Context scopes prevent conflicts between editor formatting and window commands.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrimoireColors.ParchmentMuted
                )
                Spacer(Modifier.height(16.dp))

                val groupedShortcuts = remember { KeybindingRegistry.ALL.groupBy { it.scope } }

                for ((scopeType, bindings) in groupedShortcuts) {
                    Text(
                        text = "${scopeType.displayName} Shortcuts",
                        style = MaterialTheme.typography.titleSmall,
                        color = GrimoireColors.GoldBright,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = scopeType.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = GrimoireColors.ParchmentDim,
                        fontSize = 11.5.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    for (binding in bindings) {
                        val currentShortcut = settings.shortcuts[binding.id] ?: binding.defaultShortcut
                        val isRecording = recordingActionId == binding.id
                        val isCustomized = currentShortcut != binding.defaultShortcut
                        val isUnbound = currentShortcut.isEmpty()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                                    handleAssignShortcut(binding.id, combo)
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
                                            text = "Press keys... (Esc to cancel)",
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
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, GrimoireColors.BorderBronze),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = GrimoireColors.ParchmentLight
                                        ),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Change", fontSize = 11.5.sp)
                                    }

                                    if (!isUnbound) {
                                        TextButton(
                                            onClick = {
                                                scope.launch {
                                                    AppSettingsRepository.clearShortcut(binding.id)
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(
                                                "Clear",
                                                fontSize = 11.sp,
                                                color = GrimoireColors.ParchmentDim
                                            )
                                        }
                                    }

                                    if (isCustomized) {
                                        TextButton(
                                            onClick = {
                                                scope.launch {
                                                    AppSettingsRepository.resetShortcut(binding.id)
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
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

                    Spacer(Modifier.height(14.dp))
                }

                Spacer(Modifier.height(10.dp))
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

    // Conflict Resolution Confirmation Dialog
    pendingConflict?.let { conflict ->
        Dialog(onDismissRequest = {
            pendingConflict = null
            recordingActionId = null
        }) {
            GrimoireCard(
                modifier = Modifier
                    .widthIn(min = 400.dp, max = 500.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                borderColor = GrimoireColors.GoldPrimary,
                containerColor = GrimoireColors.SurfaceCard
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Tastenkombination bereits vergeben",
                        style = MaterialTheme.typography.titleMedium,
                        color = GrimoireColors.GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Die Kombination '${conflict.newShortcut}' ist im Bereich '${conflict.targetDefinition.scope.displayName}' bereits der Aktion '${conflict.conflictingDefinition.title}' zugewiesen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrimoireColors.ParchmentLight
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Möchtest du sie neu zuweisen? Die bisherige Zuweisung für '${conflict.conflictingDefinition.title}' wird dabei aufgehoben.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrimoireColors.ParchmentMuted
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                pendingConflict = null
                                recordingActionId = null
                            }
                        ) {
                            Text("Abbrechen", color = GrimoireColors.ParchmentDim)
                        }

                        Spacer(Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val targetId = conflict.targetDefinition.id
                                val conflictId = conflict.conflictingDefinition.id
                                val shortcut = conflict.newShortcut
                                scope.launch {
                                    AppSettingsRepository.reassignShortcut(targetId, conflictId, shortcut)
                                    pendingConflict = null
                                    recordingActionId = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GrimoireColors.GoldPrimary,
                                contentColor = GrimoireColors.Background
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Neu zuweisen", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
