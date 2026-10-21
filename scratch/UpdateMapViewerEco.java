import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class UpdateMapViewerEco {
    public static void main(String[] args) throws Exception {
        File file = new File("shared/src/commonMain/kotlin/de/luca/dungeon_master_manager/ui/MapViewer.kt");
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8).replace("\r\n", "\n");

        // 1. Add import for AppSettingsRepository and kotlinx.coroutines.launch
        String oldImport = "import de.luca.dungeon_master_manager.ui.theme.GrimoireColors";
        String newImport = "import de.luca.dungeon_master_manager.ui.theme.GrimoireColors\n" +
                           "import de.luca.dungeon_master_manager.data.AppSettingsRepository\n" +
                           "import kotlinx.coroutines.launch\n" +
                           "import androidx.compose.foundation.BorderStroke\n" +
                           "import androidx.compose.foundation.shape.RoundedCornerShape";
        if (content.contains(oldImport)) {
            content = content.replace(oldImport, newImport);
        }

        // 2. Add appSettings state and coroutineScope in MapViewer
        String oldState = "    var addPinMode by remember { mutableStateOf(false) }";
        String newState = "    var addPinMode by remember { mutableStateOf(false) }\n" +
                          "    val appSettings by AppSettingsRepository.settings.collectAsState()\n" +
                          "    val isEcoMode = appSettings.isBatterySaverMode\n" +
                          "    val coroutineScope = rememberCoroutineScope()";
        if (content.contains(oldState)) {
            content = content.replace(oldState, newState);
        }

        // 3. Conditional Chamber vs Solid Background
        String oldChamber = "        // The Dungeon Master's ambient chamber stone floor surrounding the war table\n" +
                            "        GrimoireAmbientChamber(\n" +
                            "            modifier = Modifier.fillMaxSize(),\n" +
                            "            panOffset = androidx.compose.ui.geometry.Offset(animatedOffsetX.value, animatedOffsetY.value),\n" +
                            "            zoomScale = animatedScale.value\n" +
                            "        )";

        String newChamber = "        // Ambient background: Full stone chamber in normal mode, clean solid slate in Battery Saver mode\n" +
                            "        if (!isEcoMode) {\n" +
                            "            GrimoireAmbientChamber(\n" +
                            "                modifier = Modifier.fillMaxSize(),\n" +
                            "                panOffset = androidx.compose.ui.geometry.Offset(animatedOffsetX.value, animatedOffsetY.value),\n" +
                            "                zoomScale = animatedScale.value\n" +
                            "            )\n" +
                            "        } else {\n" +
                            "            Box(modifier = Modifier.fillMaxSize().background(GrimoireColors.Background))\n" +
                            "        }";

        if (content.contains(oldChamber)) {
            content = content.replace(oldChamber, newChamber);
        }

        // 4. Update maxPan calculation to adapt if ecoMode
        String oldMaxPan = "                                val maxPanX = (viewportSize.width / 2f) + (tableWidth * newScale / 2f) - 100f\n" +
                           "                                val maxPanY = (viewportSize.height / 2f) + (tableHeight * newScale / 2f) - 100f";
        String newMaxPan = "                                val effectiveW = if (!isEcoMode) tableWidth else baseMapWidth\n" +
                           "                                val effectiveH = if (!isEcoMode) tableHeight else baseMapHeight\n" +
                           "                                val maxPanX = (viewportSize.width / 2f) + (effectiveW * newScale / 2f) - 100f\n" +
                           "                                val maxPanY = (viewportSize.height / 2f) + (effectiveH * newScale / 2f) - 100f";
        if (content.contains(oldMaxPan)) {
            content = content.replace(oldMaxPan, newMaxPan);
        }

        // 5. Update Table container & Map Parchment container
        String oldTableContainer =
"                // Grand War Table container\n" +
"                Box(\n" +
"                    modifier = Modifier\n" +
"                        .align(Alignment.Center)\n" +
"                        .graphicsLayer(\n" +
"                            scaleX = animatedScale.value,\n" +
"                            scaleY = animatedScale.value,\n" +
"                            translationX = animatedOffsetX.value,\n" +
"                            translationY = animatedOffsetY.value\n" +
"                        )\n" +
"                        .size(tableWidthDp, tableHeightDp)\n" +
"                ) {\n" +
"                    // 0. Fur Pelt Rug on stone floor peeking out from under the table\n" +
"                    TabletopFurPelt(\n" +
"                        tableWidth = tableWidthDp,\n" +
"                        tableHeight = tableHeightDp\n" +
"                    )\n" +
"\n" +
"                    // 1. Realistic Deep Table Drop Shadow on Chamber Stone Floor\n" +
"                    RealisticTableDropShadow(\n" +
"                        shape = RoundedCornerShape(10.dp)\n" +
"                    )\n" +
"\n" +
"                    // 2. Grand War Table Surface (Cropped pure wood & carved border)\n" +
"                    Image(\n" +
"                        painter = painterResource(Res.drawable.tabletop_hero),\n" +
"                        contentDescription = \"Grand War Table\",\n" +
"                        contentScale = ContentScale.FillBounds,\n" +
"                        modifier = Modifier\n" +
"                            .fillMaxSize()\n" +
"                            .clip(RoundedCornerShape(10.dp))\n" +
"                    )\n" +
"\n" +
"                    // 3. Map Parchment & Accents Container (Centered on the War Table)\n" +
"                    Box(\n" +
"                        modifier = Modifier\n" +
"                            .align(Alignment.Center)\n" +
"                            .size(baseMapWidthDp, baseMapHeightDp)\n" +
"                    ) {\n" +
"                        // 0. Tabletop Props on the wood desk (Dice, Coins, Quill)\n" +
"                        TabletopPropsDeskLayer(\n" +
"                            mapWidth = baseMapWidthDp,\n" +
"                            mapHeight = baseMapHeightDp\n" +
"                        )\n" +
"\n" +
"                    // 1. Soft Parchment Drop Shadow on the wooden desk\n" +
"                    Box(\n" +
"                        modifier = Modifier\n" +
"                            .fillMaxSize()\n" +
"                            .graphicsLayer {\n" +
"                                shadowElevation = 18.dp.toPx()\n" +
"                                shape = RoundedCornerShape(8.dp)\n" +
"                                clip = false\n" +
"                            }\n" +
"                    )";

        String newTableContainer =
"                // Grand War Table (Normal) or Pure Map Frame (Battery Saver)\n" +
"                Box(\n" +
"                    modifier = Modifier\n" +
"                        .align(Alignment.Center)\n" +
"                        .graphicsLayer(\n" +
"                            scaleX = animatedScale.value,\n" +
"                            scaleY = animatedScale.value,\n" +
"                            translationX = animatedOffsetX.value,\n" +
"                            translationY = animatedOffsetY.value\n" +
"                        )\n" +
"                        .size(\n" +
"                            if (!isEcoMode) tableWidthDp else baseMapWidthDp,\n" +
"                            if (!isEcoMode) tableHeightDp else baseMapHeightDp\n" +
"                        )\n" +
"                ) {\n" +
"                    if (!isEcoMode) {\n" +
"                        // 0. Fur Pelt Rug on stone floor peeking out from under the table\n" +
"                        TabletopFurPelt(\n" +
"                            tableWidth = tableWidthDp,\n" +
"                            tableHeight = tableHeightDp\n" +
"                        )\n" +
"\n" +
"                        // 1. Realistic Deep Table Drop Shadow on Chamber Stone Floor\n" +
"                        RealisticTableDropShadow(\n" +
"                            shape = RoundedCornerShape(10.dp)\n" +
"                        )\n" +
"\n" +
"                        // 2. Grand War Table Surface (Cropped pure wood & carved border)\n" +
"                        Image(\n" +
"                            painter = painterResource(Res.drawable.tabletop_hero),\n" +
"                            contentDescription = \"Grand War Table\",\n" +
"                            contentScale = ContentScale.FillBounds,\n" +
"                            modifier = Modifier\n" +
"                                .fillMaxSize()\n" +
"                                .clip(RoundedCornerShape(10.dp))\n" +
"                        )\n" +
"                    }\n" +
"\n" +
"                    // 3. Map Parchment & Accents Container\n" +
"                    Box(\n" +
"                        modifier = Modifier\n" +
"                            .align(Alignment.Center)\n" +
"                            .size(baseMapWidthDp, baseMapHeightDp)\n" +
"                            .then(\n" +
"                                if (isEcoMode) {\n" +
"                                    Modifier\n" +
"                                        .background(GrimoireColors.SurfaceCard, RoundedCornerShape(6.dp))\n" +
"                                        .border(BorderStroke(1.dp, GrimoireColors.BorderBronze), RoundedCornerShape(6.dp))\n" +
"                                } else Modifier\n" +
"                            )\n" +
"                    ) {\n" +
"                        if (!isEcoMode) {\n" +
"                            // 0. Tabletop Props on the wood desk (Dice, Coins, Quill)\n" +
"                            TabletopPropsDeskLayer(\n" +
"                                mapWidth = baseMapWidthDp,\n" +
"                                mapHeight = baseMapHeightDp\n" +
"                            )\n" +
"\n" +
"                            // 1. Soft Parchment Drop Shadow on the wooden desk\n" +
"                            Box(\n" +
"                                modifier = Modifier\n" +
"                                    .fillMaxSize()\n" +
"                                    .graphicsLayer {\n" +
"                                        shadowElevation = 18.dp.toPx()\n" +
"                                        shape = RoundedCornerShape(8.dp)\n" +
"                                        clip = false\n" +
"                                    }\n" +
"                            )\n" +
"                        }";

        if (content.contains(oldTableContainer)) {
            content = content.replace(oldTableContainer, newTableContainer);
            System.out.println("Updated MapViewer table/map containers with Eco mode.");
        } else {
            System.err.println("Could not find oldTableContainer in MapViewer.kt!");
        }

        // 6. Wrap TabletopPropsMapAccentsLayer in if (!isEcoMode)
        String oldAccents = "                    // Tabletop Accents holding down the parchment map (Compass, Wax Seal)\n" +
                            "                    TabletopPropsMapAccentsLayer(\n" +
                            "                            mapWidth = baseMapWidthDp,\n" +
                            "                            mapHeight = baseMapHeightDp\n" +
                            "                        )";
        String newAccents = "                    if (!isEcoMode) {\n" +
                            "                        // Tabletop Accents holding down the parchment map (Compass, Wax Seal)\n" +
                            "                        TabletopPropsMapAccentsLayer(\n" +
                            "                            mapWidth = baseMapWidthDp,\n" +
                            "                            mapHeight = baseMapHeightDp\n" +
                            "                        )\n" +
                            "                    }";
        if (content.contains(oldAccents)) {
            content = content.replace(oldAccents, newAccents);
        }

        // 7. Add Eco toggle button to Toolbar
        String oldToolbar =
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

        String newToolbar =
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

        if (content.contains(oldToolbar)) {
            content = content.replace(oldToolbar, newToolbar);
            System.out.println("Updated MapViewer toolbar with Eco toggle button.");
        } else {
            System.err.println("Could not find oldToolbar in MapViewer.kt!");
        }

        Files.writeString(file.toPath(), content.replace("\n", "\r\n"), StandardCharsets.UTF_8);
    }
}
