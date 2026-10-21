import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class InspectImage {
    public static void main(String[] args) throws Exception {
        File dir = new File("C:\\Users\\Luca\\.gemini\\antigravity\\brain\\c92ecb28-f7c5-4764-9ba7-e4342d6dcab7");
        for (File f : dir.listFiles()) {
            if (f.getName().startsWith("prop_") && f.getName().endsWith(".jpg")) {
                BufferedImage img = ImageIO.read(f);
                int rgb0 = img.getRGB(10, 10);
                int r = (rgb0 >> 16) & 0xFF;
                int g = (rgb0 >> 8) & 0xFF;
                int b = rgb0 & 0xFF;
                System.out.printf("%s: %dx%d, corner(10,10) = RGB(%d, %d, %d)\n", f.getName(), img.getWidth(), img.getHeight(), r, g, b);
            }
        }
    }
}
