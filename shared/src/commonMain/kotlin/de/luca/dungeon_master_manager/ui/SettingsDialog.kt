package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.luca.dungeon_master_manager.data.AppSettingsRepository
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

    Dialog(onDismissRequest = onDismiss) {
        GrimoireCard(
            modifier = Modifier
                .widthIn(min = 440.dp, max = 580.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            borderColor = GrimoireColors.BorderBronze,
            containerColor = GrimoireColors.SurfaceCard
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
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
