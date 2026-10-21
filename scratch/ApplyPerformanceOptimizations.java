import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ApplyPerformanceOptimizations {
    public static void main(String[] args) throws Exception {
        File file = new File("shared/src/commonMain/kotlin/de/luca/dungeon_master_manager/ui/theme/GrimoireEffects.kt");
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8).replace("\r\n", "\n");

        // 1. Optimize tiledTexture: memoize brush
        String oldTiled = "    val imageBitmap = imageResource(resource)\n" +
                          "    return this.then(\n" +
                          "        Modifier.drawWithContent {\n" +
                          "            val shader = ImageShader(imageBitmap, TileMode.Repeated, TileMode.Repeated)\n" +
                          "            val brush = ShaderBrush(shader)";

        String newTiled = "    val imageBitmap = imageResource(resource)\n" +
                          "    val brush = remember(imageBitmap) {\n" +
                          "        val shader = ImageShader(imageBitmap, TileMode.Repeated, TileMode.Repeated)\n" +
                          "        ShaderBrush(shader)\n" +
                          "    }\n" +
                          "    return this.then(\n" +
                          "        Modifier.drawWithContent {";

        if (content.contains(oldTiled)) {
            content = content.replace(oldTiled, newTiled);
            System.out.println("Optimized tiledTexture with memoized brush.");
        } else {
            System.err.println("Could not find oldTiled in GrimoireEffects.kt!");
        }

        // 2. Make GrimoireAmbientChamber completely static (remove rememberInfiniteTransition)
        String oldChamber = "fun GrimoireAmbientChamber(\n" +
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
                            "                val candleRadius = (size.minDimension * 0.75f * candlelightBreathing).coerceAtLeast(350f)";

        String newChamber = "fun GrimoireAmbientChamber(\n" +
                            "    modifier: Modifier = Modifier,\n" +
                            "    panOffset: Offset = Offset.Zero,\n" +
                            "    zoomScale: Float = 1.0f,\n" +
                            "    content: @Composable BoxScope.() -> Unit = {}\n" +
                            ") {\n" +
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
                            "                // 1. Warm Candlelight Pool (Static atmospheric light, 0% idle GPU load)\n" +
                            "                val candleRadius = (size.minDimension * 0.78f).coerceAtLeast(350f)";

        if (content.contains(oldChamber)) {
            content = content.replace(oldChamber, newChamber);
            System.out.println("Optimized GrimoireAmbientChamber to static lighting.");
        } else {
            System.err.println("Could not find oldChamber in GrimoireEffects.kt!");
        }

        Files.writeString(file.toPath(), content.replace("\n", "\r\n"), StandardCharsets.UTF_8);
    }
}
