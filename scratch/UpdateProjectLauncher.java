import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class UpdateProjectLauncher {
    public static void main(String[] args) throws Exception {
        File file = new File("c:\\Users\\Luca\\Desktop\\IdeaProjects\\Dungeon-Master-Manager\\shared\\src\\commonMain\\kotlin\\de\\luca\\dungeon_master_manager\\ui\\ProjectLauncherScreen.kt");
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8).replace("\r\n", "\n");

        // 1. Update imports
        String oldImports = "import de.luca.dungeon_master_manager.ui.theme.GrimoireDeskSurface";
        String newImports = "import de.luca.dungeon_master_manager.ui.theme.GrimoireAmbientChamber\n" +
                            "import de.luca.dungeon_master_manager.ui.theme.RealisticTableDropShadow\n" +
                            "import de.luca.dungeon_master_manager.ui.theme.TabletopFurPelt\n" +
                            "import de.luca.dungeon_master_manager.ui.theme.tiledTexture\n" +
                            "import dungeon_master_manager.shared.generated.resources.Res\n" +
                            "import dungeon_master_manager.shared.generated.resources.tabletop_hero\n" +
                            "import dungeon_master_manager.shared.generated.resources.grimoire_vellum\n" +
                            "import dungeon_master_manager.shared.generated.resources.prop_quill\n" +
                            "import dungeon_master_manager.shared.generated.resources.prop_dice\n" +
                            "import dungeon_master_manager.shared.generated.resources.prop_coins\n" +
                            "import dungeon_master_manager.shared.generated.resources.prop_seal\n" +
                            "import org.jetbrains.compose.resources.painterResource\n" +
                            "import org.jetbrains.compose.resources.imageResource\n" +
                            "import androidx.compose.foundation.Image\n" +
                            "import androidx.compose.foundation.border\n" +
                            "import androidx.compose.foundation.background\n" +
                            "import androidx.compose.ui.layout.ContentScale\n" +
                            "import androidx.compose.ui.draw.clip\n" +
                            "import androidx.compose.ui.graphics.graphicsLayer\n" +
                            "import androidx.compose.ui.graphics.Brush";

        if (content.contains(oldImports)) {
            content = content.replace(oldImports, newImports);
            System.out.println("Updated imports in ProjectLauncherScreen.kt");
        } else {
            System.err.println("Could not find oldImports in ProjectLauncherScreen.kt!");
        }

        // 2. Replace the body from GrimoireDeskSurface to closing brace
        String oldBody = "    GrimoireDeskSurface(modifier = Modifier.fillMaxSize()) {\n" +
                         "        Column(\n" +
                         "            modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 32.dp),\n" +
                         "            horizontalAlignment = Alignment.CenterHorizontally,\n" +
                         "        ) {\n" +
                         "            // Top bar with title + global entities button\n" +
                         "            Row(\n" +
                         "                modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),\n" +
                         "                horizontalArrangement = Arrangement.SpaceBetween,\n" +
                         "                verticalAlignment = Alignment.CenterVertically,\n" +
                         "            ) {\n" +
                         "                Column {\n" +
                         "                    Text(\n" +
                         "                        \"Dungeon Master Manager\",\n" +
                         "                        style = MaterialTheme.typography.displaySmall,\n" +
                         "                        color = GrimoireColors.GoldPrimary,\n" +
                         "                    )\n" +
                         "                    Spacer(Modifier.height(4.dp))\n" +
                         "                    Text(\n" +
                         "                        \"Select a campaign or create a new chronicle\",\n" +
                         "                        style = MaterialTheme.typography.bodyMedium,\n" +
                         "                        color = GrimoireColors.ParchmentMuted,\n" +
                         "                    )\n" +
                         "                }\n" +
                         "\n" +
                         "                OutlinedButton(\n" +
                         "                    onClick = { showGlobalEntityDialog = true },\n" +
                         "                    border = BorderStroke(1.dp, GrimoireColors.BorderBronze),\n" +
                         "                    colors = ButtonDefaults.outlinedButtonColors(\n" +
                         "                        contentColor = GrimoireColors.GoldPrimary\n" +
                         "                    ),\n" +
                         "                    shape = RoundedCornerShape(6.dp)\n" +
                         "                ) {\n" +
                         "                    Text(\"Global Entities\")\n" +
                         "                }\n" +
                         "            }\n" +
                         "\n" +
                         "            Spacer(Modifier.height(20.dp))\n" +
                         "            GrimoireDivider(\n" +
                         "                modifier = Modifier.widthIn(max = 760.dp),\n" +
                         "                showDiamond = true,\n" +
                         "                color = GrimoireColors.BorderBronze\n" +
                         "            )\n" +
                         "            Spacer(Modifier.height(24.dp))\n" +
                         "\n" +
                         "            // Error banner\n" +
                         "            error?.let { errorMessage ->\n" +
                         "                Card(\n" +
                         "                    modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth().padding(bottom = 16.dp),\n" +
                         "                    shape = RoundedCornerShape(6.dp),\n" +
                         "                    border = BorderStroke(1.dp, GrimoireColors.CrimsonError),\n" +
                         "                    colors = CardDefaults.cardColors(\n" +
                         "                        containerColor = GrimoireColors.ErrorContainer,\n" +
                         "                    ),\n" +
                         "                ) {\n" +
                         "                    Row(\n" +
                         "                        modifier = Modifier.padding(16.dp),\n" +
                         "                        verticalAlignment = Alignment.CenterVertically,\n" +
                         "                    ) {\n" +
                         "                        Text(\n" +
                         "                            errorMessage,\n" +
                         "                            modifier = Modifier.weight(1f),\n" +
                         "                            color = GrimoireColors.OnErrorContainer,\n" +
                         "                        )\n" +
                         "                        TextButton(onClick = { viewModel.clearError() }) {\n" +
                         "                            Text(\"Dismiss\", color = GrimoireColors.OnErrorContainer)\n" +
                         "                        }\n" +
                         "                    }\n" +
                         "                }\n" +
                         "            }\n" +
                         "\n" +
                         "            if (isLoading) {\n" +
                         "                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {\n" +
                         "                    CircularProgressIndicator(color = GrimoireColors.GoldPrimary)\n" +
                         "                }\n" +
                         "            } else if (projects.isEmpty()) {\n" +
                         "                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {\n" +
                         "                    Column(horizontalAlignment = Alignment.CenterHorizontally) {\n" +
                         "                        Text(\n" +
                         "                            \"No campaigns yet\",\n" +
                         "                            style = MaterialTheme.typography.titleLarge,\n" +
                         "                            color = GrimoireColors.ParchmentLight,\n" +
                         "                        )\n" +
                         "                        Spacer(Modifier.height(8.dp))\n" +
                         "                        Text(\n" +
                         "                            \"Create your first campaign to begin your adventure\",\n" +
                         "                            style = MaterialTheme.typography.bodyMedium,\n" +
                         "                            color = GrimoireColors.ParchmentMuted,\n" +
                         "                        )\n" +
                         "                    }\n" +
                         "                }\n" +
                         "            } else {\n" +
                         "                LazyColumn(\n" +
                         "                    modifier = Modifier.weight(1f).widthIn(max = 760.dp).fillMaxWidth(),\n" +
                         "                    verticalArrangement = Arrangement.spacedBy(14.dp),\n" +
                         "                ) {\n" +
                         "                    items(projects, key = { it.path }) {\ project ->\n" +
                         "                        ProjectCard(\n" +
                         "                            project = project,\n" +
                         "                            onClick = { onProjectSelected(project) },\n" +
                         "                            onDelete = { projectToDelete = project },\n" +
                         "                        )\n" +
                         "                    }\n" +
                         "                }\n" +
                         "            }\n" +
                         "\n" +
                         "            Spacer(Modifier.height(20.dp))\n" +
                         "            Button(\n" +
                         "                onClick = { showCreateDialog = true },\n" +
                         "                colors = ButtonDefaults.buttonColors(\n" +
                         "                    containerColor = GrimoireColors.GoldPrimary,\n" +
                         "                    contentColor = GrimoireColors.Background\n" +
                         "                ),\n" +
                         "                shape = RoundedCornerShape(6.dp),\n" +
                         "                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)\n" +
                         "            ) {\n" +
                         "                Text(\n" +
                         "                    \"+ New Campaign\",\n" +
                         "                    style = MaterialTheme.typography.titleSmall,\n" +
                         "                    fontWeight = FontWeight.Bold\n" +
                         "                )\n" +
                         "            }\n" +
                         "        }\n" +
                         "    }";

        String newBody = "    GrimoireAmbientChamber(modifier = Modifier.fillMaxSize()) {\n" +
                         "        Box(\n" +
                         "            modifier = Modifier\n" +
                         "                .fillMaxSize()\n" +
                         "                .padding(horizontal = 28.dp, vertical = 20.dp),\n" +
                         "            contentAlignment = Alignment.Center\n" +
                         "        ) {\n" +
                         "            // Grand War Table on the chamber floor\n" +
                         "            Box(\n" +
                         "                modifier = Modifier\n" +
                         "                    .widthIn(max = 1260.dp)\n" +
                         "                    .heightIn(max = 840.dp)\n" +
                         "                    .fillMaxSize()\n" +
                         "            ) {\n" +
                         "                // 0. Fur Pelt Rug on stone floor peeking out from under bottom-left of table\n" +
                         "                TabletopFurPelt(\n" +
                         "                    tableWidth = 1100.dp,\n" +
                         "                    tableHeight = 720.dp\n" +
                         "                )\n" +
                         "\n" +
                         "                // 1. Realistic Deep Table Drop Shadow\n" +
                         "                RealisticTableDropShadow(\n" +
                         "                    shape = RoundedCornerShape(14.dp)\n" +
                         "                )\n" +
                         "\n" +
                         "                // 2. Grand War Table Surface (Cropped pure wood & carved border)\n" +
                         "                Image(\n" +
                         "                    painter = painterResource(Res.drawable.tabletop_hero),\n" +
                         "                    contentDescription = \"Grand War Table\",\n" +
                         "                    contentScale = ContentScale.FillBounds,\n" +
                         "                    modifier = Modifier\n" +
                         "                        .fillMaxSize()\n" +
                         "                        .clip(RoundedCornerShape(14.dp))\n" +
                         "                )\n" +
                         "\n" +
                         "                // 3. Tabletop Props on the wood desk around the book:\n" +
                         "                // - Raven quill & inkpot resting on the left flank\n" +
                         "                val quillBitmap = imageResource(Res.drawable.prop_quill)\n" +
                         "                Image(\n" +
                         "                    bitmap = quillBitmap,\n" +
                         "                    contentDescription = \"Quill and Ink\",\n" +
                         "                    modifier = Modifier\n" +
                         "                        .align(Alignment.CenterStart)\n" +
                         "                        .offset(x = 10.dp, y = (-20).dp)\n" +
                         "                        .requiredSize(140.dp, 140.dp)\n" +
                         "                        .graphicsLayer {\n" +
                         "                            rotationZ = -16f\n" +
                         "                            alpha = 0.94f\n" +
                         "                        }\n" +
                         "                )\n" +
                         "\n" +
                         "                // - Vintage metal RPG dice resting on the right side\n" +
                         "                val diceBitmap = imageResource(Res.drawable.prop_dice)\n" +
                         "                Image(\n" +
                         "                    bitmap = diceBitmap,\n" +
                         "                    contentDescription = \"RPG Dice\",\n" +
                         "                    modifier = Modifier\n" +
                         "                        .align(Alignment.BottomEnd)\n" +
                         "                        .offset(x = (-30).dp, y = (-40).dp)\n" +
                         "                        .requiredSize(130.dp, 74.dp)\n" +
                         "                        .graphicsLayer {\n" +
                         "                            rotationZ = 14f\n" +
                         "                            alpha = 0.94f\n" +
                         "                        }\n" +
                         "                )\n" +
                         "\n" +
                         "                // - Ancient gold & silver coins near the dice\n" +
                         "                val coinsBitmap = imageResource(Res.drawable.prop_coins)\n" +
                         "                Image(\n" +
                         "                    bitmap = coinsBitmap,\n" +
                         "                    contentDescription = \"Coins\",\n" +
                         "                    modifier = Modifier\n" +
                         "                        .align(Alignment.TopEnd)\n" +
                         "                        .offset(x = (-40).dp, y = 50.dp)\n" +
                         "                        .requiredSize(105.dp, 105.dp)\n" +
                         "                        .graphicsLayer {\n" +
                         "                            rotationZ = 20f\n" +
                         "                            alpha = 0.94f\n" +
                         "                        }\n" +
                         "                )\n" +
                         "\n" +
                         "                // - Crimson wax seal stamped near top of the folio\n" +
                         "                val sealBitmap = imageResource(Res.drawable.prop_seal)\n" +
                         "                Image(\n" +
                         "                    bitmap = sealBitmap,\n" +
                         "                    contentDescription = \"Wax Seal\",\n" +
                         "                    modifier = Modifier\n" +
                         "                        .align(Alignment.TopCenter)\n" +
                         "                        .offset(x = (-180).dp, y = 8.dp)\n" +
                         "                        .requiredSize(72.dp, 78.dp)\n" +
                         "                        .graphicsLayer {\n" +
                         "                            rotationZ = -8f\n" +
                         "                            alpha = 0.96f\n" +
                         "                        }\n" +
                         "                )\n" +
                         "\n" +
                         "                // 4. Centered Campaign Tome Folio (The opened book on the desk)\n" +
                         "                Box(\n" +
                         "                    modifier = Modifier\n" +
                         "                        .align(Alignment.Center)\n" +
                         "                        .widthIn(max = 780.dp)\n" +
                         "                        .fillMaxWidth(0.74f)\n" +
                         "                        .fillMaxHeight(0.92f)\n" +
                         "                        .graphicsLayer {\n" +
                         "                            shadowElevation = 18.dp.toPx()\n" +
                         "                            shape = RoundedCornerShape(10.dp)\n" +
                         "                        }\n" +
                         "                        .background(\n" +
                         "                            color = GrimoireColors.SurfaceCard,\n" +
                         "                            shape = RoundedCornerShape(10.dp)\n" +
                         "                        )\n" +
                         "                        .border(\n" +
                         "                            width = 1.5.dp,\n" +
                         "                            brush = Brush.linearGradient(\n" +
                         "                                listOf(GrimoireColors.BorderBronze, GrimoireColors.GoldMuted, GrimoireColors.BorderBronze)\n" +
                         "                            ),\n" +
                         "                            shape = RoundedCornerShape(10.dp)\n" +
                         "                        )\n" +
                         "                        .tiledTexture(\n" +
                         "                            resource = Res.drawable.grimoire_vellum,\n" +
                         "                            alpha = 0.35f\n" +
                         "                        )\n" +
                         "                        .padding(horizontal = 36.dp, vertical = 24.dp)\n" +
                         "                ) {\n" +
                         "                    Column(\n" +
                         "                        modifier = Modifier.fillMaxSize(),\n" +
                         "                        horizontalAlignment = Alignment.CenterHorizontally,\n" +
                         "                    ) {\n" +
                         "                        // Top bar with title + global entities button\n" +
                         "                        Row(\n" +
                         "                            modifier = Modifier.fillMaxWidth(),\n" +
                         "                            horizontalArrangement = Arrangement.SpaceBetween,\n" +
                         "                            verticalAlignment = Alignment.CenterVertically,\n" +
                         "                        ) {\n" +
                         "                            Column {\n" +
                         "                                Text(\n" +
                         "                                    \"Dungeon Master Manager\",\n" +
                         "                                    style = MaterialTheme.typography.displaySmall,\n" +
                         "                                    color = GrimoireColors.GoldPrimary,\n" +
                         "                                )\n" +
                         "                                Spacer(Modifier.height(4.dp))\n" +
                         "                                Text(\n" +
                         "                                    \"Select a campaign or create a new chronicle\",\n" +
                         "                                    style = MaterialTheme.typography.bodyMedium,\n" +
                         "                                    color = GrimoireColors.ParchmentMuted,\n" +
                         "                                )\n" +
                         "                            }\n" +
                         "\n" +
                         "                            OutlinedButton(\n" +
                         "                                onClick = { showGlobalEntityDialog = true },\n" +
                         "                                border = BorderStroke(1.dp, GrimoireColors.BorderBronze),\n" +
                         "                                colors = ButtonDefaults.outlinedButtonColors(\n" +
                         "                                    contentColor = GrimoireColors.GoldPrimary\n" +
                         "                                ),\n" +
                         "                                shape = RoundedCornerShape(6.dp)\n" +
                         "                            ) {\n" +
                         "                                Text(\"Global Entities\")\n" +
                         "                            }\n" +
                         "                        }\n" +
                         "\n" +
                         "                        Spacer(Modifier.height(16.dp))\n" +
                         "                        GrimoireDivider(\n" +
                         "                            modifier = Modifier.fillMaxWidth(),\n" +
                         "                            showDiamond = true,\n" +
                         "                            color = GrimoireColors.BorderBronze\n" +
                         "                        )\n" +
                         "                        Spacer(Modifier.height(18.dp))\n" +
                         "\n" +
                         "                        // Error banner\n" +
                         "                        error?.let { errorMessage ->\n" +
                         "                            Card(\n" +
                         "                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),\n" +
                         "                                shape = RoundedCornerShape(6.dp),\n" +
                         "                                border = BorderStroke(1.dp, GrimoireColors.CrimsonError),\n" +
                         "                                colors = CardDefaults.cardColors(\n" +
                         "                                    containerColor = GrimoireColors.ErrorContainer,\n" +
                         "                                ),\n" +
                         "                            ) {\n" +
                         "                                Row(\n" +
                         "                                    modifier = Modifier.padding(14.dp),\n" +
                         "                                    verticalAlignment = Alignment.CenterVertically,\n" +
                         "                                ) {\n" +
                         "                                    Text(\n" +
                         "                                        errorMessage,\n" +
                         "                                        modifier = Modifier.weight(1f),\n" +
                         "                                        color = GrimoireColors.OnErrorContainer,\n" +
                         "                                    )\n" +
                         "                                    TextButton(onClick = { viewModel.clearError() }) {\n" +
                         "                                        Text(\"Dismiss\", color = GrimoireColors.OnErrorContainer)\n" +
                         "                                    }\n" +
                         "                                }\n" +
                         "                            }\n" +
                         "                        }\n" +
                         "\n" +
                         "                        if (isLoading) {\n" +
                         "                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {\n" +
                         "                                CircularProgressIndicator(color = GrimoireColors.GoldPrimary)\n" +
                         "                            }\n" +
                         "                        } else if (projects.isEmpty()) {\n" +
                         "                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {\n" +
                         "                                Column(horizontalAlignment = Alignment.CenterHorizontally) {\n" +
                         "                                    Text(\n" +
                         "                                        \"No campaigns yet\",\n" +
                         "                                        style = MaterialTheme.typography.titleLarge,\n" +
                         "                                        color = GrimoireColors.ParchmentLight,\n" +
                         "                                    )\n" +
                         "                                    Spacer(Modifier.height(8.dp))\n" +
                         "                                    Text(\n" +
                         "                                        \"Create your first campaign to begin your adventure\",\n" +
                         "                                        style = MaterialTheme.typography.bodyMedium,\n" +
                         "                                        color = GrimoireColors.ParchmentMuted,\n" +
                         "                                    )\n" +
                         "                                }\n" +
                         "                            }\n" +
                         "                        } else {\n" +
                         "                            LazyColumn(\n" +
                         "                                modifier = Modifier.weight(1f).fillMaxWidth(),\n" +
                         "                                verticalArrangement = Arrangement.spacedBy(12.dp),\n" +
                         "                            ) {\n" +
                         "                                items(projects, key = { it.path }) { project ->\n" +
                         "                                    ProjectCard(\n" +
                         "                                        project = project,\n" +
                         "                                        onClick = { onProjectSelected(project) },\n" +
                         "                                        onDelete = { projectToDelete = project },\n" +
                         "                                    )\n" +
                         "                                }\n" +
                         "                            }\n" +
                         "                        }\n" +
                         "\n" +
                         "                        Spacer(Modifier.height(16.dp))\n" +
                         "                        Button(\n" +
                         "                            onClick = { showCreateDialog = true },\n" +
                         "                            colors = ButtonDefaults.buttonColors(\n" +
                         "                                containerColor = GrimoireColors.GoldPrimary,\n" +
                         "                                contentColor = GrimoireColors.Background\n" +
                         "                            ),\n" +
                         "                            shape = RoundedCornerShape(6.dp),\n" +
                         "                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)\n" +
                         "                        ) {\n" +
                         "                            Text(\n" +
                         "                                \"+ New Campaign\",\n" +
                         "                                style = MaterialTheme.typography.titleSmall,\n" +
                         "                                fontWeight = FontWeight.Bold\n" +
                         "                            )\n" +
                         "                        }\n" +
                         "                    }\n" +
                         "                }\n" +
                         "            }\n" +
                         "        }\n" +
                         "    }";

        if (content.contains(oldBody)) {
            content = content.replace(oldBody, newBody);
            System.out.println("Updated ProjectLauncherScreen body with War Room Scene & Folio Tome.");
        } else {
            System.err.println("Could not find oldBody in ProjectLauncherScreen.kt!");
        }

        Files.writeString(file.toPath(), content.replace("\n", "\r\n"), StandardCharsets.UTF_8);
    }
}
