import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ApplyAtmosphereUpdates {
    public static void main(String[] args) throws Exception {
        // 1. Update GrimoireEffects.kt
        File grimoireFile = new File("c:\\Users\\Luca\\Desktop\\IdeaProjects\\Dungeon-Master-Manager\\shared\\src\\commonMain\\kotlin\\de\\luca\\dungeon_master_manager\\ui\\theme\\GrimoireEffects.kt");
        String grimoire = Files.readString(grimoireFile.toPath(), StandardCharsets.UTF_8).replace("\r\n", "\n");

        // Add imports
        String oldImports = "import dungeon_master_manager.shared.generated.resources.prop_seal";
        String newImports = "import dungeon_master_manager.shared.generated.resources.prop_seal\n" +
                            "import dungeon_master_manager.shared.generated.resources.prop_fur_pelt\n" +
                            "import androidx.compose.animation.core.*\n" +
                            "import androidx.compose.runtime.getValue\n" +
                            "import androidx.compose.ui.draw.blur";

        if (grimoire.contains(oldImports)) {
            grimoire = grimoire.replace(oldImports, newImports);
            System.out.println("Added imports to GrimoireEffects.kt");
        } else {
            System.err.println("Could not find oldImports!");
        }

        // Update GrimoireAmbientChamber with living candlelight breathing
        String oldChamber = "fun GrimoireAmbientChamber(\n" +
                            "    modifier: Modifier = Modifier,\n" +
                            "    panOffset: Offset = Offset.Zero,\n" +
                            "    zoomScale: Float = 1.0f,\n" +
                            "    content: @Composable BoxScope.() -> Unit = {}\n" +
                            ") {\n" +
                            "    Box(\n" +
                            "        modifier = modifier\n" +
                            "            .fillMaxSize()\n" +
                            "            .background(Color(0xFF141210))\n" +
                            "            .tiledTexture(\n" +
                            "                resource = Res.drawable.stone_floor_dark,\n" +
                            "                alpha = 0.80f,\n" +
                            "                panOffset = panOffset,\n" +
                            "                zoomScale = zoomScale,\n" +
                            "                baseTextureScale = 1.7f\n" +
                            "            )\n" +
                            "            .drawWithContent {\n" +
                            "                drawContent()\n" +
                            "                val centerOffset = Offset(size.width / 2f, size.height / 2f)\n" +
                            "                val radius = (size.maxDimension * 0.85f).coerceAtLeast(500f)\n" +
                            "                drawRect(\n" +
                            "                    brush = Brush.radialGradient(\n" +
                            "                        colorStops = arrayOf(\n" +
                            "                            0.0f to Color.Transparent,\n" +
                            "                            0.45f to Color(0x200A0807),\n" +
                            "                            0.75f to Color(0x65060504),\n" +
                            "                            1.0f to Color(0xB8040303)\n" +
                            "                        ),\n" +
                            "                        center = centerOffset,\n" +
                            "                        radius = radius\n" +
                            "                    )\n" +
                            "                )\n" +
                            "            },\n" +
                            "        content = content\n" +
                            "    )\n" +
                            "}";

        String newChamber = "fun GrimoireAmbientChamber(\n" +
                            "    modifier: Modifier = Modifier,\n" +
                            "    panOffset: Offset = Offset.Zero,\n" +
                            "    zoomScale: Float = 1.0f,\n" +
                            "    content: @Composable BoxScope.() -> Unit = {}\n" +
                            ") {\n" +
                            "    val infiniteTransition = rememberInfiniteTransition()\n" +
                            "    val candlelightBreathing by infiniteTransition.animateFloat(\n" +
                            "        initialValue = 0.96f,\n" +
                            "        targetValue = 1.04f,\n" +
                            "        animationSpec = infiniteRepeatable(\n" +
                            "            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),\n" +
                            "            repeatMode = RepeatMode.Reverse\n" +
                            "        )\n" +
                            "    )\n" +
                            "\n" +
                            "    Box(\n" +
                            "        modifier = modifier\n" +
                            "            .fillMaxSize()\n" +
                            "            .background(Color(0xFF12100E))\n" +
                            "            .tiledTexture(\n" +
                            "                resource = Res.drawable.stone_floor_dark,\n" +
                            "                alpha = 0.78f,\n" +
                            "                panOffset = panOffset,\n" +
                            "                zoomScale = zoomScale,\n" +
                            "                baseTextureScale = 1.7f\n" +
                            "            )\n" +
                            "            .drawWithContent {\n" +
                            "                drawContent()\n" +
                            "\n" +
                            "                val centerOffset = Offset(size.width / 2f, size.height / 2f)\n" +
                            "\n" +
                            "                // 1. Warm Candlelight Pool with living breathing oscillation\n" +
                            "                val candleRadius = (size.minDimension * 0.75f * candlelightBreathing).coerceAtLeast(350f)\n" +
                            "                drawRect(\n" +
                            "                    brush = Brush.radialGradient(\n" +
                            "                        colorStops = arrayOf(\n" +
                            "                            0.0f to Color(0x32FFB74D),\n" +
                            "                            0.35f to Color(0x15FF9800),\n" +
                            "                            0.70f to Color.Transparent\n" +
                            "                        ),\n" +
                            "                        center = centerOffset,\n" +
                            "                        radius = candleRadius\n" +
                            "                    ),\n" +
                            "                    blendMode = BlendMode.Screen\n" +
                            "                )\n" +
                            "\n" +
                            "                // 2. Chamber Perimeter Vignette\n" +
                            "                val roomRadius = (size.maxDimension * 0.85f).coerceAtLeast(500f)\n" +
                            "                drawRect(\n" +
                            "                    brush = Brush.radialGradient(\n" +
                            "                        colorStops = arrayOf(\n" +
                            "                            0.0f to Color.Transparent,\n" +
                            "                            0.45f to Color(0x250A0807),\n" +
                            "                            0.75f to Color(0x70060504),\n" +
                            "                            1.0f to Color(0xC8040303)\n" +
                            "                        ),\n" +
                            "                        center = centerOffset,\n" +
                            "                        radius = roomRadius\n" +
                            "                    )\n" +
                            "                )\n" +
                            "            },\n" +
                            "        content = content\n" +
                            "    )\n" +
                            "}\n" +
                            "\n" +
                            "/**\n" +
                            " * Deep, multi-layered realistic contact drop shadow cast by the heavy oak war table onto the stone floor.\n" +
                            " */\n" +
                            "@Composable\n" +
                            "fun BoxScope.RealisticTableDropShadow(\n" +
                            "    modifier: Modifier = Modifier,\n" +
                            "    shape: Shape = RoundedCornerShape(12.dp)\n" +
                            ") {\n" +
                            "    // Direct contact shadow\n" +
                            "    Box(\n" +
                            "        modifier = modifier\n" +
                            "            .matchParentSize()\n" +
                            "            .offset(x = 6.dp, y = 14.dp)\n" +
                            "            .background(Color(0xCC050403), shape)\n" +
                            "            .blur(16.dp)\n" +
                            "    )\n" +
                            "    // Diffuse ambient occlusion shadow\n" +
                            "    Box(\n" +
                            "        modifier = modifier\n" +
                            "            .matchParentSize()\n" +
                            "            .offset(x = 10.dp, y = 26.dp)\n" +
                            "            .background(Color(0x88040302), shape)\n" +
                            "            .blur(34.dp)\n" +
                            "    )\n" +
                            "}\n" +
                            "\n" +
                            "/**\n" +
                            " * Rustic medieval bear fur pelt rug resting on the stone floor, peeking out from under a corner of the table.\n" +
                            " */\n" +
                            "@Composable\n" +
                            "fun TabletopFurPelt(\n" +
                            "    modifier: Modifier = Modifier,\n" +
                            "    tableWidth: Dp,\n" +
                            "    tableHeight: Dp\n" +
                            ") {\n" +
                            "    val peltBitmap = imageResource(Res.drawable.prop_fur_pelt)\n" +
                            "    Image(\n" +
                            "        bitmap = peltBitmap,\n" +
                            "        contentDescription = \"Fur Pelt Rug\",\n" +
                            "        modifier = modifier\n" +
                            "            .offset(x = (-120).dp, y = tableHeight - 220.dp)\n" +
                            "            .requiredSize(280.dp, 310.dp)\n" +
                            "            .graphicsLayer {\n" +
                            "                rotationZ = -18f\n" +
                            "                alpha = 0.95f\n" +
                            "            }\n" +
                            "    )\n" +
                            "}";

        if (grimoire.contains(oldChamber)) {
            grimoire = grimoire.replace(oldChamber, newChamber);
            System.out.println("Updated GrimoireAmbientChamber and added RealisticTableDropShadow & TabletopFurPelt.");
        } else {
            System.err.println("Could not find oldChamber!");
        }

        Files.writeString(grimoireFile.toPath(), grimoire.replace("\n", "\r\n"), StandardCharsets.UTF_8);
    }
}
