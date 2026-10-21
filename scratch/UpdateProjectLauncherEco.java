import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class UpdateProjectLauncherEco {
    public static void main(String[] args) throws Exception {
        File file = new File("shared/src/commonMain/kotlin/de/luca/dungeon_master_manager/ui/ProjectLauncherScreen.kt");
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8).replace("\r\n", "\n");

        // 1. Add import for AppSettingsRepository
        String oldImport = "import de.luca.dungeon_master_manager.data.ProjectInfo";
        String newImport = "import de.luca.dungeon_master_manager.data.ProjectInfo\n" +
                           "import de.luca.dungeon_master_manager.data.AppSettingsRepository";
        if (content.contains(oldImport)) {
            content = content.replace(oldImport, newImport);
        }

        // 2. Locate from fun ProjectLauncherScreen down to Create Dialog
        int startIdx = content.indexOf("fun ProjectLauncherScreen(");
        int createDialogIdx = content.indexOf("// Create Dialog", startIdx);

        if (startIdx == -1 || createDialogIdx == -1) {
            System.err.println("Could not find start or createDialog marker in ProjectLauncherScreen.kt");
            return;
        }

        String before = content.substring(0, startIdx);
        String after = content.substring(createDialogIdx);

        String newLauncherFunctionAndFolio =
"fun ProjectLauncherScreen(\n" +
"    onProjectSelected: (ProjectInfo) -> Unit,\n" +
"    viewModel: ProjectLauncherViewModel = viewModel { ProjectLauncherViewModel() },\n" +
") {\n" +
"    val projects by viewModel.projects.collectAsState()\n" +
"    val isLoading by viewModel.isLoading.collectAsState()\n" +
"    val error by viewModel.error.collectAsState()\n" +
"    var showCreateDialog by remember { mutableStateOf(false) }\n" +
"    var projectToDelete by remember { mutableStateOf<ProjectInfo?>(null) }\n" +
"    \n" +
"    val entityViewModel = remember { EntityViewModel(null, null) }\n" +
"    var showGlobalEntityDialog by remember { mutableStateOf(false) }\n" +
"    var showSettingsDialog by remember { mutableStateOf(false) }\n" +
"\n" +
"    val appSettings by AppSettingsRepository.settings.collectAsState()\n" +
"\n" +
"    if (!appSettings.isBatterySaverMode) {\n" +
"        // FULL WAR ROOM ATMOSPHERE MODE\n" +
"        GrimoireAmbientChamber(modifier = Modifier.fillMaxSize()) {\n" +
"            BoxWithConstraints(\n" +
"                modifier = Modifier\n" +
"                    .fillMaxSize()\n" +
"                    .padding(horizontal = 24.dp, vertical = 20.dp),\n" +
"                contentAlignment = Alignment.Center\n" +
"            ) {\n" +
"                val tableWidth = minOf(maxWidth, 1260.dp)\n" +
"                val tableHeight = minOf(maxHeight, 820.dp)\n" +
"\n" +
"                // Grand War Table on the chamber floor\n" +
"                Box(\n" +
"                    modifier = Modifier.size(tableWidth, tableHeight)\n" +
"                ) {\n" +
"                    // 0. Fur Pelt Rug on stone floor peeking out from under bottom-left of table\n" +
"                    TabletopFurPelt(\n" +
"                        tableWidth = tableWidth,\n" +
"                        tableHeight = tableHeight\n" +
"                    )\n" +
"\n" +
"                    // 1. Realistic Deep Table Drop Shadow\n" +
"                    RealisticTableDropShadow(\n" +
"                        shape = RoundedCornerShape(14.dp)\n" +
"                    )\n" +
"\n" +
"                    // 2. Grand War Table Surface (Cropped pure wood & carved border)\n" +
"                    Image(\n" +
"                        painter = painterResource(Res.drawable.tabletop_hero),\n" +
"                        contentDescription = \"Grand War Table\",\n" +
"                        contentScale = ContentScale.FillBounds,\n" +
"                        modifier = Modifier\n" +
"                            .fillMaxSize()\n" +
"                            .clip(RoundedCornerShape(14.dp))\n" +
"                    )\n" +
"\n" +
"                    // 3. Tabletop Props on the wood desk around the book:\n" +
"                    val quillBitmap = imageResource(Res.drawable.prop_quill)\n" +
"                    Image(\n" +
"                        bitmap = quillBitmap,\n" +
"                        contentDescription = \"Quill and Ink\",\n" +
"                        modifier = Modifier\n" +
"                            .align(Alignment.CenterStart)\n" +
"                            .offset(x = 12.dp, y = (-20).dp)\n" +
"                            .requiredSize(140.dp, 140.dp)\n" +
"                            .graphicsLayer {\n" +
"                                rotationZ = -16f\n" +
"                                alpha = 0.94f\n" +
"                            }\n" +
"                    )\n" +
"\n" +
"                    val diceBitmap = imageResource(Res.drawable.prop_dice)\n" +
"                    Image(\n" +
"                        bitmap = diceBitmap,\n" +
"                        contentDescription = \"RPG Dice\",\n" +
"                        modifier = Modifier\n" +
"                            .align(Alignment.BottomEnd)\n" +
"                            .offset(x = (-28).dp, y = (-36).dp)\n" +
"                            .requiredSize(130.dp, 74.dp)\n" +
"                            .graphicsLayer {\n" +
"                                rotationZ = 14f\n" +
"                                alpha = 0.94f\n" +
"                            }\n" +
"                    )\n" +
"\n" +
"                    val coinsBitmap = imageResource(Res.drawable.prop_coins)\n" +
"                    Image(\n" +
"                        bitmap = coinsBitmap,\n" +
"                        contentDescription = \"Coins\",\n" +
"                        modifier = Modifier\n" +
"                            .align(Alignment.TopEnd)\n" +
"                            .offset(x = (-36).dp, y = 44.dp)\n" +
"                            .requiredSize(105.dp, 105.dp)\n" +
"                            .graphicsLayer {\n" +
"                                rotationZ = 20f\n" +
"                                alpha = 0.94f\n" +
"                            }\n" +
"                    )\n" +
"\n" +
"                    val sealBitmap = imageResource(Res.drawable.prop_seal)\n" +
"                    Image(\n" +
"                        bitmap = sealBitmap,\n" +
"                        contentDescription = \"Wax Seal\",\n" +
"                        modifier = Modifier\n" +
"                            .align(Alignment.BottomStart)\n" +
"                            .offset(x = 44.dp, y = (-36).dp)\n" +
"                            .requiredSize(72.dp, 78.dp)\n" +
"                            .graphicsLayer {\n" +
"                                rotationZ = -8f\n" +
"                                alpha = 0.96f\n" +
"                            }\n" +
"                    )\n" +
"\n" +
"                    // 4. Centered Campaign Tome Folio\n" +
"                    Box(\n" +
"                        modifier = Modifier\n" +
"                            .align(Alignment.Center)\n" +
"                            .widthIn(min = 340.dp, max = 780.dp)\n" +
"                            .fillMaxWidth(0.72f)\n" +
"                            .fillMaxHeight(0.90f)\n" +
"                            .graphicsLayer {\n" +
"                                shadowElevation = 18.dp.toPx()\n" +
"                                shape = RoundedCornerShape(10.dp)\n" +
"                            }\n" +
"                            .background(\n" +
"                                color = GrimoireColors.SurfaceCard,\n" +
"                                shape = RoundedCornerShape(10.dp)\n" +
"                            )\n" +
"                            .border(\n" +
"                                width = 1.5.dp,\n" +
"                                brush = Brush.linearGradient(\n" +
"                                    listOf(GrimoireColors.BorderBronze, GrimoireColors.GoldMuted, GrimoireColors.BorderBronze)\n" +
"                                ),\n" +
"                                shape = RoundedCornerShape(10.dp)\n" +
"                            )\n" +
"                            .tiledTexture(\n" +
"                                resource = Res.drawable.grimoire_vellum,\n" +
"                                alpha = 0.35f\n" +
"                            )\n" +
"                            .padding(horizontal = 36.dp, vertical = 24.dp)\n" +
"                    ) {\n" +
"                        LauncherFolioContent(\n" +
"                            projects = projects,\n" +
"                            isLoading = isLoading,\n" +
"                            error = error,\n" +
"                            onClearError = { viewModel.clearError() },\n" +
"                            onProjectSelected = onProjectSelected,\n" +
"                            onDeleteProject = { projectToDelete = it },\n" +
"                            onOpenGlobalEntities = { showGlobalEntityDialog = true },\n" +
"                            onOpenSettings = { showSettingsDialog = true },\n" +
"                            onCreateProject = { showCreateDialog = true }\n" +
"                        )\n" +
"                    }\n" +
"                }\n" +
"            }\n" +
"        }\n" +
"    } else {\n" +
"        // BATTERY SAVER / PERFORMANCE MODE (Ultra-clean solid surface, no textures or shadows)\n" +
"        Box(\n" +
"            modifier = Modifier\n" +
"                .fillMaxSize()\n" +
"                .background(GrimoireColors.Background)\n" +
"                .padding(horizontal = 24.dp, vertical = 20.dp),\n" +
"            contentAlignment = Alignment.Center\n" +
"        ) {\n" +
"            Box(\n" +
"                modifier = Modifier\n" +
"                    .widthIn(min = 340.dp, max = 780.dp)\n" +
"                    .fillMaxWidth(0.76f)\n" +
"                    .fillMaxHeight(0.92f)\n" +
"                    .background(\n" +
"                        color = GrimoireColors.SurfaceCard,\n" +
"                        shape = RoundedCornerShape(10.dp)\n" +
"                    )\n" +
"                    .border(\n" +
"                        width = 1.dp,\n" +
"                        color = GrimoireColors.BorderBronze,\n" +
"                        shape = RoundedCornerShape(10.dp)\n" +
"                    )\n" +
"                    .padding(horizontal = 36.dp, vertical = 24.dp)\n" +
"            ) {\n" +
"                LauncherFolioContent(\n" +
"                    projects = projects,\n" +
"                    isLoading = isLoading,\n" +
"                    error = error,\n" +
"                    onClearError = { viewModel.clearError() },\n" +
"                    onProjectSelected = onProjectSelected,\n" +
"                    onDeleteProject = { projectToDelete = it },\n" +
"                    onOpenGlobalEntities = { showGlobalEntityDialog = true },\n" +
"                    onOpenSettings = { showSettingsDialog = true },\n" +
"                    onCreateProject = { showCreateDialog = true }\n" +
"                )\n" +
"            }\n" +
"        }\n" +
"    }\n" +
"\n" +
"    // Settings Dialog\n" +
"    if (showSettingsDialog) {\n" +
"        SettingsDialog(onDismiss = { showSettingsDialog = false })\n" +
"    }\n" +
"\n";

        String folioContentComposable =
"@Composable\n" +
"private fun LauncherFolioContent(\n" +
"    projects: List<ProjectInfo>,\n" +
"    isLoading: Boolean,\n" +
"    error: String?,\n" +
"    onClearError: () -> Unit,\n" +
"    onProjectSelected: (ProjectInfo) -> Unit,\n" +
"    onDeleteProject: (ProjectInfo) -> Unit,\n" +
"    onOpenGlobalEntities: () -> Unit,\n" +
"    onOpenSettings: () -> Unit,\n" +
"    onCreateProject: () -> Unit,\n" +
"    modifier: Modifier = Modifier\n" +
") {\n" +
"    Column(\n" +
"        modifier = modifier.fillMaxSize(),\n" +
"        horizontalAlignment = Alignment.CenterHorizontally,\n" +
"    ) {\n" +
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
"        }\n" +
"\n" +
"        Spacer(Modifier.height(16.dp))\n" +
"        GrimoireDivider(\n" +
"            modifier = Modifier.fillMaxWidth(),\n" +
"            showDiamond = true,\n" +
"            color = GrimoireColors.BorderBronze\n" +
"        )\n" +
"        Spacer(Modifier.height(18.dp))\n" +
"\n" +
"        // Error banner\n" +
"        error?.let { errorMessage ->\n" +
"            Card(\n" +
"                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),\n" +
"                shape = RoundedCornerShape(6.dp),\n" +
"                border = BorderStroke(1.dp, GrimoireColors.CrimsonError),\n" +
"                colors = CardDefaults.cardColors(\n" +
"                    containerColor = GrimoireColors.ErrorContainer,\n" +
"                ),\n" +
"            ) {\n" +
"                Row(\n" +
"                    modifier = Modifier.padding(14.dp),\n" +
"                    verticalAlignment = Alignment.CenterVertically,\n" +
"                ) {\n" +
"                    Text(\n" +
"                        errorMessage,\n" +
"                        modifier = Modifier.weight(1f),\n" +
"                        color = GrimoireColors.OnErrorContainer,\n" +
"                    )\n" +
"                    TextButton(onClick = onClearError) {\n" +
"                        Text(\"Dismiss\", color = GrimoireColors.OnErrorContainer)\n" +
"                    }\n" +
"                }\n" +
"            }\n" +
"        }\n" +
"\n" +
"        if (isLoading) {\n" +
"            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {\n" +
"                CircularProgressIndicator(color = GrimoireColors.GoldPrimary)\n" +
"            }\n" +
"        } else if (projects.isEmpty()) {\n" +
"            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {\n" +
"                Column(horizontalAlignment = Alignment.CenterHorizontally) {\n" +
"                    Text(\n" +
"                        \"No campaigns yet\",\n" +
"                        style = MaterialTheme.typography.titleLarge,\n" +
"                        color = GrimoireColors.ParchmentLight,\n" +
"                    )\n" +
"                    Spacer(Modifier.height(8.dp))\n" +
"                    Text(\n" +
"                        \"Create your first campaign to begin your adventure\",\n" +
"                        style = MaterialTheme.typography.bodyMedium,\n" +
"                        color = GrimoireColors.ParchmentMuted,\n" +
"                    )\n" +
"                }\n" +
"            }\n" +
"        } else {\n" +
"            LazyColumn(\n" +
"                modifier = Modifier.weight(1f).fillMaxWidth(),\n" +
"                verticalArrangement = Arrangement.spacedBy(12.dp),\n" +
"            ) {\n" +
"                items(projects, key = { it.path }) { project ->\n" +
"                    ProjectCard(\n" +
"                        project = project,\n" +
"                        onClick = { onProjectSelected(project) },\n" +
"                        onDelete = { onDeleteProject(project) },\n" +
"                    )\n" +
"                }\n" +
"            }\n" +
"        }\n" +
"\n" +
"        Spacer(Modifier.height(16.dp))\n" +
"        Button(\n" +
"            onClick = onCreateProject,\n" +
"            colors = ButtonDefaults.buttonColors(\n" +
"                containerColor = GrimoireColors.GoldPrimary,\n" +
"                contentColor = GrimoireColors.Background\n" +
"            ),\n" +
"            shape = RoundedCornerShape(6.dp),\n" +
"            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)\n" +
"        ) {\n" +
"            Text(\n" +
"                \"+ New Campaign\",\n" +
"                style = MaterialTheme.typography.titleSmall,\n" +
"                fontWeight = FontWeight.Bold\n" +
"            )\n" +
"        }\n" +
"    }\n" +
"}\n\n";

        content = before + newLauncherFunctionAndFolio + after + "\n" + folioContentComposable;
        Files.writeString(file.toPath(), content.replace("\n", "\r\n"), StandardCharsets.UTF_8);
        System.out.println("Successfully updated ProjectLauncherScreen.kt with Battery Saver Mode support.");
    }
}
