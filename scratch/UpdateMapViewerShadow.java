import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class UpdateMapViewerShadow {
    public static void main(String[] args) throws Exception {
        File file = new File("c:\\Users\\Luca\\Desktop\\IdeaProjects\\Dungeon-Master-Manager\\shared\\src\\commonMain\\kotlin\\de\\luca\\dungeon_master_manager\\ui\\MapViewer.kt");
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8).replace("\r\n", "\n");

        String oldShadowBlock = "                    // 1. Table Elevation Shadow on Chamber Stone Floor\n" +
                                "                    Box(\n" +
                                "                        modifier = Modifier\n" +
                                "                            .fillMaxSize()\n" +
                                "                            .graphicsLayer {\n" +
                                "                                shadowElevation = 24.dp.toPx()\n" +
                                "                                shape = RoundedCornerShape(10.dp)\n" +
                                "                                clip = false\n" +
                                "                            }\n" +
                                "                    )";

        String newShadowBlock = "                    // 0. Fur Pelt Rug on stone floor peeking out from under the table\n" +
                                "                    TabletopFurPelt(\n" +
                                "                        tableWidth = tableWidthDp,\n" +
                                "                        tableHeight = tableHeightDp\n" +
                                "                    )\n" +
                                "\n" +
                                "                    // 1. Realistic Deep Table Drop Shadow on Chamber Stone Floor\n" +
                                "                    RealisticTableDropShadow(\n" +
                                "                        shape = RoundedCornerShape(10.dp)\n" +
                                "                    )";

        if (content.contains(oldShadowBlock)) {
            content = content.replace(oldShadowBlock, newShadowBlock);
            System.out.println("Replaced shadow with Fur Pelt and RealisticTableDropShadow in MapViewer.kt.");
        } else {
            System.err.println("Could not find oldShadowBlock in MapViewer.kt!");
        }

        Files.writeString(file.toPath(), content.replace("\n", "\r\n"), StandardCharsets.UTF_8);
    }
}
