import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class CropHeroTable {
    public static void main(String[] args) throws Exception {
        File file = new File("C:\\Users\\Luca\\.gemini\\antigravity\\brain\\c92ecb28-f7c5-4764-9ba7-e4342d6dcab7\\tabletop_hero_1792536087171.jpg");
        BufferedImage img = ImageIO.read(file);
        
        // Exact table border coordinates:
        // Left=95, Right=1280, Top=64, Bottom=700
        int x = 95;
        int y = 64;
        int w = 1280 - 95 + 1; // 1186
        int h = 700 - 64 + 1;  // 637

        BufferedImage cropped = img.getSubimage(x, y, w, h);
        
        File outFile = new File("c:\\Users\\Luca\\Desktop\\IdeaProjects\\Dungeon-Master-Manager\\shared\\src\\commonMain\\composeResources\\drawable\\tabletop_hero.jpg");
        ImageIO.write(cropped, "JPEG", outFile);
        System.out.printf("Cropped tabletop_hero to %dx%d (pure wood, no outer stone) -> %s\n", w, h, outFile.getAbsolutePath());
    }
}
