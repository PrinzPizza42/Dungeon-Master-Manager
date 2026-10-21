import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class UpdatePeltSizing {
    public static void main(String[] args) throws Exception {
        File file = new File("shared/src/commonMain/kotlin/de/luca/dungeon_master_manager/ui/theme/GrimoireEffects.kt");
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8).replace("\r\n", "\n");

        String target = "fun TabletopFurPelt(\n" +
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

        String replacement = "fun TabletopFurPelt(\n" +
                             "    modifier: Modifier = Modifier,\n" +
                             "    tableWidth: Dp,\n" +
                             "    tableHeight: Dp,\n" +
                             "    peltWidth: Dp = 520.dp,\n" +
                             "    peltHeight: Dp = 570.dp,\n" +
                             "    peekX: Dp = 160.dp,\n" +
                             "    peekY: Dp = 150.dp,\n" +
                             "    rotation: Float = -14f\n" +
                             ") {\n" +
                             "    val peltBitmap = imageResource(Res.drawable.prop_fur_pelt)\n" +
                             "    Image(\n" +
                             "        bitmap = peltBitmap,\n" +
                             "        contentDescription = \"Fur Pelt Rug\",\n" +
                             "        modifier = modifier\n" +
                             "            .offset(x = -peekX, y = tableHeight - peltHeight + peekY)\n" +
                             "            .requiredSize(peltWidth, peltHeight)\n" +
                             "            .graphicsLayer {\n" +
                             "                rotationZ = rotation\n" +
                             "                alpha = 0.98f\n" +
                             "            }\n" +
                             "    )\n" +
                             "}";

        if (content.contains(target)) {
            content = content.replace(target, replacement);
            Files.writeString(file.toPath(), content.replace("\n", "\r\n"), StandardCharsets.UTF_8);
            System.out.println("Successfully updated TabletopFurPelt in GrimoireEffects.kt");
        } else {
            System.err.println("Could not find target in GrimoireEffects.kt!");
        }
    }
}
