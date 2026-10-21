import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class InspectHeroTable {
    public static void main(String[] args) throws Exception {
        File file = new File("C:\\Users\\Luca\\.gemini\\antigravity\\brain\\c92ecb28-f7c5-4764-9ba7-e4342d6dcab7\\tabletop_hero_1792536087171.jpg");
        BufferedImage img = ImageIO.read(file);
        int w = img.getWidth();
        int h = img.getHeight();
        System.out.println("Image size: " + w + "x" + h);

        // Sample along horizontal centerline from left to center
        int centerY = h / 2;
        for (int x = 0; x < w / 2; x += 10) {
            int rgb = img.getRGB(x, centerY);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            System.out.printf("x=%4d: RGB(%3d, %3d, %3d)\n", x, r, g, b);
            if (r > 60 || g > 40) { // Wood has higher red/green
                System.out.printf("--> Wood border starts around x=%d\n", x);
                break;
            }
        }
    }
}
