import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ApplyFixes {
    public static void main(String[] args) throws Exception {
        // --- 1. Fix ProjectLauncherScreen.kt ---
        File launcherFile = new File("shared/src/commonMain/kotlin/de/luca/dungeon_master_manager/ui/ProjectLauncherScreen.kt");
        String launcherContent = Files.readString(launcherFile.toPath(), StandardCharsets.UTF_8).replace("\r\n", "\n");

        // Widen Folio in War Room Mode:
        launcherContent = launcherContent.replace(
            ".widthIn(min = 340.dp, max = 780.dp)\n                        .fillMaxWidth(0.72f)",
            ".widthIn(min = 420.dp, max = 880.dp)\n                        .fillMaxWidth(0.78f)"
        );

        // Widen Folio in Battery Saver Mode:
        launcherContent = launcherContent.replace(
            ".widthIn(min = 340.dp, max = 780.dp)\n                    .fillMaxWidth(0.76f)",
            ".widthIn(min = 420.dp, max = 880.dp)\n                    .fillMaxWidth(0.82f)"
        );

        // Fix header in LauncherFolioContent:
        String oldHeader =
"        // Top bar with title + action buttons\n" +
"        Row(\n" +
"            modifier = Modifier.fillMaxWidth(),\n" +
"            horizontalArrangement = Arrangement.SpaceBetween,\n" +
"            verticalAlignment = Alignment.CenterVertically,\n" +
"        ) {\n" +
"            Column {\n" +
"                Text(\n" +
"                    \"Dungeon Master Manager\",\n" +
"                    style = MaterialTheme.typography.displaySmall,\n" +
"                    color = GrimoireColors.GoldPrimary,\n" +
"                )\n" +
"                Spacer(Modifier.height(4.dp))\n" +
"                Text(\n" +
"                    \"Select a campaign or create a new chronicle\",\n" +
"                    style = MaterialTheme.typography.bodyMedium,\n" +
"                    color = GrimoireColors.ParchmentMuted,\n" +
"                )\n" +
"            }\n" +
"\n" +
"            Row(\n" +
"                horizontalArrangement = Arrangement.spacedBy(8.dp),\n" +
"                verticalAlignment = Alignment.CenterVertically\n" +
"            ) {\n" +
"                OutlinedButton(\n" +
"                    onClick = onOpenGlobalEntities,\n" +
"                    border = BorderStroke(1.dp, GrimoireColors.BorderBronze),\n" +
"                    colors = ButtonDefaults.outlinedButtonColors(\n" +
"                        contentColor = GrimoireColors.GoldPrimary\n" +
"                    ),\n" +
"                    shape = RoundedCornerShape(6.dp)\n" +
"                ) {\n" +
"                    Text(\"Global Entities\")\n" +
"                }\n" +
"\n" +
"                OutlinedButton(\n" +
"                    onClick = onOpenSettings,\n" +
"                    border = BorderStroke(1.dp, GrimoireColors.BorderBronze),\n" +
"                    colors = ButtonDefaults.outlinedButtonColors(\n" +
"                        contentColor = GrimoireColors.GoldPrimary\n" +
"                    ),\n" +
"                    shape = RoundedCornerShape(6.dp)\n" +
"                ) {\n" +
"                    Text(\"Settings\")\n" +
"                }\n" +
"            }\n" +
"        }";

        String newHeader =
"        // Top bar with title + action buttons (Spacious and responsive)\n" +
"        Row(\n" +
"            modifier = Modifier.fillMaxWidth(),\n" +
"            horizontalArrangement = Arrangement.SpaceBetween,\n" +
"            verticalAlignment = Alignment.CenterVertically,\n" +
"        ) {\n" +
"            Column(\n" +
"                modifier = Modifier.weight(1f, fill = false).padding(end = 16.dp)\n" +
"            ) {\n" +
"                Text(\n" +
"                    \"Dungeon Master Manager\",\n" +
"                    style = MaterialTheme.typography.headlineMedium,\n" +
"                    color = GrimoireColors.GoldPrimary,\n" +
"                    fontWeight = FontWeight.Bold,\n" +
"                    maxLines = 1\n" +
"                )\n" +
"                Spacer(Modifier.height(4.dp))\n" +
"                Text(\n" +
"                    \"Select a campaign or create a new chronicle\",\n" +
"                    style = MaterialTheme.typography.bodyMedium,\n" +
"                    color = GrimoireColors.ParchmentMuted,\n" +
"                    maxLines = 1\n" +
"                )\n" +
"            }\n" +
"\n" +
"            Row(\n" +
"                horizontalArrangement = Arrangement.spacedBy(10.dp),\n" +
"                verticalAlignment = Alignment.CenterVertically\n" +
"            ) {\n" +
"                OutlinedButton(\n" +
"                    onClick = onOpenGlobalEntities,\n" +
"                    border = BorderStroke(1.dp, GrimoireColors.BorderBronze),\n" +
"                    colors = ButtonDefaults.outlinedButtonColors(\n" +
"                        contentColor = GrimoireColors.GoldPrimary\n" +
"                    ),\n" +
"                    shape = RoundedCornerShape(6.dp),\n" +
"                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)\n" +
"                ) {\n" +
"                    Text(\"Global Entities\", maxLines = 1, softWrap = false)\n" +
"                }\n" +
"\n" +
"                OutlinedButton(\n" +
"                    onClick = onOpenSettings,\n" +
"                    border = BorderStroke(1.dp, GrimoireColors.BorderBronze),\n" +
"                    colors = ButtonDefaults.outlinedButtonColors(\n" +
"                        contentColor = GrimoireColors.GoldPrimary\n" +
"                    ),\n" +
"                    shape = RoundedCornerShape(6.dp),\n" +
"                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)\n" +
"                ) {\n" +
"                    Text(\"Settings\", maxLines = 1, softWrap = false)\n" +
"                }\n" +
"            }\n" +
"        }";

        if (launcherContent.contains(oldHeader)) {
            launcherContent = launcherContent.replace(oldHeader, newHeader);
            Files.writeString(launcherFile.toPath(), launcherContent.replace("\n", "\r\n"), StandardCharsets.UTF_8);
            System.out.println("Updated ProjectLauncherScreen.kt header and folio width.");
        } else {
            System.err.println("Could not find oldHeader in ProjectLauncherScreen.kt!");
        }

        // --- 2. Fix SettingsDialog.kt min width ---
        File dialogFile = new File("shared/src/commonMain/kotlin/de/luca/dungeon_master_manager/ui/SettingsDialog.kt");
        String dialogContent = Files.readString(dialogFile.toPath(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        dialogContent = dialogContent.replace(
            ".widthIn(max = 520.dp)",
            ".widthIn(min = 440.dp, max = 580.dp)"
        );
        Files.writeString(dialogFile.toPath(), dialogContent.replace("\n", "\r\n"), StandardCharsets.UTF_8);
        System.out.println("Updated SettingsDialog.kt width.");

        // --- 3. Remove Eco button from MapViewer.kt ---
        File mapViewerFile = new File("shared/src/commonMain/kotlin/de/luca/dungeon_master_manager/ui/MapViewer.kt");
        String mapViewerContent = Files.readString(mapViewerFile.toPath(), StandardCharsets.UTF_8).replace("\r\n", "\n");

        // Remove unused import
        mapViewerContent = mapViewerContent.replace("import kotlinx.coroutines.launch\n", "");

        // Remove unused coroutineScope
        mapViewerContent = mapViewerContent.replace(
            "    val appSettings by AppSettingsRepository.settings.collectAsState()\n    val isEcoMode = appSettings.isBatterySaverMode\n    val coroutineScope = rememberCoroutineScope()",
            "    val appSettings by AppSettingsRepository.settings.collectAsState()\n    val isEcoMode = appSettings.isBatterySaverMode"
        );

        // Remove Eco button from toolbar
        String oldToolbarWithEco =
"            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {\n" +
"                Button(onClick = {\n" +
"                    val oldScale = scale\n" +
"                    val newScale = (scale * 1.2f).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)\n" +
"                    val actualZoom = newScale / oldScale\n" +
"                    scale = newScale\n" +
"                    offsetX *= actualZoom\n" +
"                    offsetY *= actualZoom\n" +
"                }) { Text(\"+\") }\n" +
"                Button(onClick = {\n" +
"                    val oldScale = scale\n" +
"                    val newScale = (scale / 1.2f).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)\n" +
"                    val actualZoom = newScale / oldScale\n" +
"                    scale = newScale\n" +
"                    offsetX *= actualZoom\n" +
"                    offsetY *= actualZoom\n" +
"                }) { Text(\"-\") }\n" +
"                Button(\n" +
"                    onClick = {\n" +
"                        coroutineScope.launch {\n" +
"                            AppSettingsRepository.setBatterySaverMode(!isEcoMode)\n" +
"                        }\n" +
"                    },\n" +
"                    colors = ButtonDefaults.buttonColors(\n" +
"                        containerColor = if (isEcoMode) GrimoireColors.GoldPrimary else MaterialTheme.colorScheme.secondary,\n" +
"                        contentColor = if (isEcoMode) GrimoireColors.Background else MaterialTheme.colorScheme.onSecondary\n" +
"                    )\n" +
"                ) {\n" +
"                    Text(if (isEcoMode) \"⚡ Eco: ON\" else \"⚡ Eco: OFF\")\n" +
"                }\n" +
"                Button(onClick = { addPinMode = !addPinMode }, colors = ButtonDefaults.buttonColors(containerColor = if (addPinMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)) {\n" +
"                    Text(if (addPinMode) \"Click Map to Place\" else \"Add Pin\")\n" +
"                }\n" +
"            }";

        String newCleanToolbar =
"            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {\n" +
"                Button(onClick = {\n" +
"                    val oldScale = scale\n" +
"                    val newScale = (scale * 1.2f).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)\n" +
"                    val actualZoom = newScale / oldScale\n" +
"                    scale = newScale\n" +
"                    offsetX *= actualZoom\n" +
"                    offsetY *= actualZoom\n" +
"                }) { Text(\"+\") }\n" +
"                Button(onClick = {\n" +
"                    val oldScale = scale\n" +
"                    val newScale = (scale / 1.2f).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)\n" +
"                    val actualZoom = newScale / oldScale\n" +
"                    scale = newScale\n" +
"                    offsetX *= actualZoom\n" +
"                    offsetY *= actualZoom\n" +
"                }) { Text(\"-\") }\n" +
"                Button(onClick = { addPinMode = !addPinMode }, colors = ButtonDefaults.buttonColors(containerColor = if (addPinMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)) {\n" +
"                    Text(if (addPinMode) \"Click Map to Place\" else \"Add Pin\")\n" +
"                }\n" +
"            }";

        if (mapViewerContent.contains(oldToolbarWithEco)) {
            mapViewerContent = mapViewerContent.replace(oldToolbarWithEco, newCleanToolbar);
            Files.writeString(mapViewerFile.toPath(), mapViewerContent.replace("\n", "\r\n"), StandardCharsets.UTF_8);
            System.out.println("Removed Eco button from MapViewer.kt toolbar.");
        } else {
            System.err.println("Could not find oldToolbarWithEco in MapViewer.kt!");
        }
    }
}
