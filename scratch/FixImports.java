import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FixImports {
    public static void main(String[] args) throws Exception {
        File file = new File("shared/src/commonMain/kotlin/de/luca/dungeon_master_manager/ui/MapViewer.kt");
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        String dup = "import de.luca.dungeon_master_manager.data.AppSettingsRepository\nimport kotlinx.coroutines.launch\nimport androidx.compose.foundation.BorderStroke\nimport androidx.compose.foundation.shape.RoundedCornerShape";
        String fixed = "import de.luca.dungeon_master_manager.data.AppSettingsRepository\nimport kotlinx.coroutines.launch";
        if (content.contains(dup)) {
            content = content.replace(dup, fixed);
            Files.writeString(file.toPath(), content.replace("\n", "\r\n"), StandardCharsets.UTF_8);
            System.out.println("Fixed duplicate imports in MapViewer.kt");
        } else {
            System.err.println("Could not find duplicate imports");
        }
    }
}
