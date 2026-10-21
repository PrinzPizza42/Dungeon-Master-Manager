import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class UpdateStoneAndTable {
    public static void main(String[] args) throws Exception {
        // 1. GrimoireEffects.kt
        File grimoireFile = new File("c:\\Users\\Luca\\Desktop\\IdeaProjects\\Dungeon-Master-Manager\\shared\\src\\commonMain\\kotlin\\de\\luca\\dungeon_master_manager\\ui\\theme\\GrimoireEffects.kt");
        String grimoire = Files.readString(grimoireFile.toPath(), StandardCharsets.UTF_8).replace("\r\n", "\n");

        String oldTiledSignature = "fun Modifier.tiledTexture(\n" +
                                   "    resource: DrawableResource,\n" +
                                   "    alpha: Float = 1.0f,\n" +
                                   "    tint: Color? = null,\n" +
                                   "    blendMode: BlendMode = BlendMode.SrcOver,\n" +
                                   "    panOffset: Offset = Offset.Zero,\n" +
                                   "    zoomScale: Float = 1.0f\n" +
                                   "): Modifier {";

        String newTiledSignature = "fun Modifier.tiledTexture(\n" +
                                   "    resource: DrawableResource,\n" +
                                   "    alpha: Float = 1.0f,\n" +
                                   "    tint: Color? = null,\n" +
                                   "    blendMode: BlendMode = BlendMode.SrcOver,\n" +
                                   "    panOffset: Offset = Offset.Zero,\n" +
                                   "    zoomScale: Float = 1.0f,\n" +
                                   "    baseTextureScale: Float = 1.0f\n" +
                                   "): Modifier {";

        if (grimoire.contains(oldTiledSignature)) {
            grimoire = grimoire.replace(oldTiledSignature, newTiledSignature);
            System.out.println("Updated tiledTexture signature.");
        } else {
            System.err.println("Could not find oldTiledSignature!");
        }

        String oldSafeScale = "val safeScale = zoomScale.coerceAtLeast(0.05f)";
        String newSafeScale = "val safeScale = (zoomScale * baseTextureScale).coerceAtLeast(0.05f)";
        if (grimoire.contains(oldSafeScale)) {
            grimoire = grimoire.replace(oldSafeScale, newSafeScale);
            System.out.println("Updated safeScale calculation.");
        } else {
            System.err.println("Could not find oldSafeScale!");
        }

        String oldChamberCall = "            .tiledTexture(\n" +
                                "                resource = Res.drawable.stone_floor_dark,\n" +
                                "                alpha = 0.88f,\n" +
                                "                panOffset = panOffset,\n" +
                                "                zoomScale = zoomScale\n" +
                                "            )";
        String newChamberCall = "            .tiledTexture(\n" +
                                "                resource = Res.drawable.stone_floor_dark,\n" +
                                "                alpha = 0.80f,\n" +
                                "                panOffset = panOffset,\n" +
                                "                zoomScale = zoomScale,\n" +
                                "                baseTextureScale = 1.7f\n" +
                                "            )";
        if (grimoire.contains(oldChamberCall)) {
            grimoire = grimoire.replace(oldChamberCall, newChamberCall);
            System.out.println("Updated stone floor tiling in GrimoireAmbientChamber.");
        } else {
            System.err.println("Could not find oldChamberCall!");
        }

        Files.writeString(grimoireFile.toPath(), grimoire.replace("\n", "\r\n"), StandardCharsets.UTF_8);

        // 2. MapViewer.kt
        File mapFile = new File("c:\\Users\\Luca\\Desktop\\IdeaProjects\\Dungeon-Master-Manager\\shared\\src\\commonMain\\kotlin\\de\\luca\\dungeon_master_manager\\ui\\MapViewer.kt");
        String map = Files.readString(mapFile.toPath(), StandardCharsets.UTF_8).replace("\r\n", "\n");

        // Update MIN_MAP_ZOOM from 0.20f to 0.48f
        String oldZoom = "private const val MIN_MAP_ZOOM = 0.20f";
        String newZoom = "private const val MIN_MAP_ZOOM = 0.48f";
        if (map.contains(oldZoom)) {
            map = map.replace(oldZoom, newZoom);
            System.out.println("Updated MIN_MAP_ZOOM to 0.48f.");
        } else {
            System.err.println("Could not find oldZoom!");
        }

        // Clean up table container shadow and image clip
        String oldTableBlock = "                    // 1. Table Elevation Shadow on Chamber Floor\n" +
                               "                    Box(\n" +
                               "                        modifier = Modifier\n" +
                               "                            .fillMaxSize()\n" +
                               "                            .graphicsLayer {\n" +
                               "                                shadowElevation = 28.dp.toPx()\n" +
                               "                                shape = RoundedCornerShape(16.dp)\n" +
                               "                                clip = false\n" +
                               "                            }\n" +
                               "                    )\n" +
                               "\n" +
                               "                    // 2. Grand War Table Surface (Non-repeating Hero Asset)\n" +
                               "                    Image(\n" +
                               "                        painter = painterResource(Res.drawable.tabletop_hero),\n" +
                               "                        contentDescription = \"Grand War Table\",\n" +
                               "                        contentScale = ContentScale.Crop,\n" +
                               "                        modifier = Modifier\n" +
                               "                            .fillMaxSize()\n" +
                               "                            .clip(RoundedCornerShape(14.dp))\n" +
                               "                            .border(\n" +
                               "                                width = 2.dp,\n" +
                               "                                brush = Brush.linearGradient(\n" +
                               "                                    listOf(Color(0x55C6A052), Color(0x203E2723), Color(0x608D6E63))\n" +
                               "                                ),\n" +
                               "                                shape = RoundedCornerShape(14.dp)\n" +
                               "                            )\n" +
                               "                    )";

        String newTableBlock = "                    // 1. Table Elevation Shadow on Chamber Stone Floor\n" +
                               "                    Box(\n" +
                               "                        modifier = Modifier\n" +
                               "                            .fillMaxSize()\n" +
                               "                            .graphicsLayer {\n" +
                               "                                shadowElevation = 24.dp.toPx()\n" +
                               "                                shape = RoundedCornerShape(10.dp)\n" +
                               "                                clip = false\n" +
                               "                            }\n" +
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
                               "                    )";

        if (map.contains(oldTableBlock)) {
            map = map.replace(oldTableBlock, newTableBlock);
            System.out.println("Cleaned up table elevation shadow and border.");
        } else {
            System.err.println("Could not find oldTableBlock!");
        }

        Files.writeString(mapFile.toPath(), map.replace("\n", "\r\n"), StandardCharsets.UTF_8);
        System.out.println("Files updated successfully!");
    }
}
