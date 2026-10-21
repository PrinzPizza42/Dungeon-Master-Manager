import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class UpdateMapViewerCleanParchment {
    public static void main(String[] args) throws Exception {
        File file = new File("c:\\Users\\Luca\\Desktop\\IdeaProjects\\Dungeon-Master-Manager\\shared\\src\\commonMain\\kotlin\\de\\luca\\dungeon_master_manager\\ui\\MapViewer.kt");
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);

        boolean hadCr = content.contains("\r");
        String normalized = content.replace("\r\n", "\n");

        // 1. Ambient chamber stone floor with pan & zoom sync
        String oldChamber = "        // The Dungeon Master's ambient chamber room floor surrounding the war table\n" +
                            "        GrimoireAmbientChamber(modifier = Modifier.fillMaxSize())";
        String newChamber = "        // The Dungeon Master's ambient chamber stone floor surrounding the war table\n" +
                            "        GrimoireAmbientChamber(\n" +
                            "            modifier = Modifier.fillMaxSize(),\n" +
                            "            panOffset = androidx.compose.ui.geometry.Offset(animatedOffsetX.value, animatedOffsetY.value),\n" +
                            "            zoomScale = animatedScale.value\n" +
                            "        )";

        if (normalized.contains(oldChamber)) {
            normalized = normalized.replace(oldChamber, newChamber);
            System.out.println("1. Updated GrimoireAmbientChamber with stone floor pan/zoom.");
        } else {
            System.err.println("Could not find oldChamber!");
        }

        // 2. Parchment drop shadow shape
        String oldShadow = "shadowElevation = 18.dp.toPx()\n" +
                           "                                shape = RoundedCornerShape(12.dp)";
        String newShadow = "shadowElevation = 18.dp.toPx()\n" +
                           "                                shape = RoundedCornerShape(8.dp)";
        if (normalized.contains(oldShadow)) {
            normalized = normalized.replace(oldShadow, newShadow);
            System.out.println("2. Updated drop shadow shape to 8.dp.");
        } else {
            System.err.println("Could not find oldShadow!");
        }

        // 3. Remove 32px DstIn fade now that fog is removed
        String oldImageModifier = "                    // 2. Alpha-Masked Map Image (frayed/feathered edges so hard rectangular border is dissolved)\n" +
                                  "                    Image(\n" +
                                  "                        bitmap = imageBitmap!!, \n" +
                                  "                        contentDescription = \"Map\",\n" +
                                  "                        modifier = Modifier\n" +
                                  "                            .fillMaxSize()\n" +
                                  "                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)\n" +
                                  "                            .drawWithContent {\n" +
                                  "                                drawContent()\n" +
                                  "                                val fade = 32.dp.toPx().coerceAtMost(size.minDimension * 0.08f)\n" +
                                  "                                // Top edge fade\n" +
                                  "                                drawRect(\n" +
                                  "                                    brush = Brush.verticalGradient(\n" +
                                  "                                        colors = listOf(Color.Transparent, Color.Black),\n" +
                                  "                                        startY = 0f,\n" +
                                  "                                        endY = fade\n" +
                                  "                                    ),\n" +
                                  "                                    blendMode = BlendMode.DstIn\n" +
                                  "                                )\n" +
                                  "                                // Bottom edge fade\n" +
                                  "                                drawRect(\n" +
                                  "                                    brush = Brush.verticalGradient(\n" +
                                  "                                        colors = listOf(Color.Black, Color.Transparent),\n" +
                                  "                                        startY = size.height - fade,\n" +
                                  "                                        endY = size.height\n" +
                                  "                                    ),\n" +
                                  "                                    blendMode = BlendMode.DstIn\n" +
                                  "                                )\n" +
                                  "                                // Left edge fade\n" +
                                  "                                drawRect(\n" +
                                  "                                    brush = Brush.horizontalGradient(\n" +
                                  "                                        colors = listOf(Color.Transparent, Color.Black),\n" +
                                  "                                        startX = 0f,\n" +
                                  "                                        endX = fade\n" +
                                  "                                    ),\n" +
                                  "                                    blendMode = BlendMode.DstIn\n" +
                                  "                                )\n" +
                                  "                                // Right edge fade\n" +
                                  "                                drawRect(\n" +
                                  "                                    brush = Brush.horizontalGradient(\n" +
                                  "                                        colors = listOf(Color.Black, Color.Transparent),\n" +
                                  "                                        startX = size.width - fade,\n" +
                                  "                                        endX = size.width\n" +
                                  "                                    ),\n" +
                                  "                                    blendMode = BlendMode.DstIn\n" +
                                  "                                )\n" +
                                  "                            }\n" +
                                  "                            .pointerInput(addPinMode) {";

        String newImageModifier = "                    // 2. Crisp Parchment Map Image\n" +
                                  "                    Image(\n" +
                                  "                        bitmap = imageBitmap!!, \n" +
                                  "                        contentDescription = \"Map\",\n" +
                                  "                        modifier = Modifier\n" +
                                  "                            .fillMaxSize()\n" +
                                  "                            .clip(RoundedCornerShape(8.dp))\n" +
                                  "                            .pointerInput(addPinMode) {";

        if (normalized.contains(oldImageModifier)) {
            normalized = normalized.replace(oldImageModifier, newImageModifier);
            System.out.println("3. Replaced 32px DstIn fade with clean rounded parchment clip.");
        } else {
            System.err.println("Could not find oldImageModifier!");
        }

        // 4. Remove Fog of War overlay
        String oldFog = "                    // Atmospheric Boundary Fog of War\n" +
                        "                    MapBoundaryFogOverlay(mapSeed = mapData.imagePath.hashCode())\n" +
                        "                    \n";
        if (normalized.contains(oldFog)) {
            normalized = normalized.replace(oldFog, "");
            System.out.println("4. Removed MapBoundaryFogOverlay completely.");
        } else {
            // Check without blank line
            String oldFogAlt = "                    // Atmospheric Boundary Fog of War\n" +
                               "                    MapBoundaryFogOverlay(mapSeed = mapData.imagePath.hashCode())\n";
            if (normalized.contains(oldFogAlt)) {
                normalized = normalized.replace(oldFogAlt, "");
                System.out.println("4. Removed MapBoundaryFogOverlay (alt) completely.");
            } else {
                System.err.println("Could not find oldFog!");
            }
        }

        if (hadCr) {
            normalized = normalized.replace("\n", "\r\n");
        }

        Files.writeString(file.toPath(), normalized, StandardCharsets.UTF_8);
        System.out.println("MapViewer.kt updated successfully!");
    }
}
