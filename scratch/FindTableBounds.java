import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class FindTableBounds {
    public static void main(String[] args) throws Exception {
        File file = new File("C:\\Users\\Luca\\.gemini\\antigravity\\brain\\c92ecb28-f7c5-4764-9ba7-e4342d6dcab7\\tabletop_hero_1792536087171.jpg");
        BufferedImage img = ImageIO.read(file);
        int w = img.getWidth();
        int h = img.getHeight();

        int left = 0, right = w - 1, top = 0, bottom = h - 1;

        int cy = h / 2;
        for (int x = 0; x < w / 2; x++) {
            int rgb = img.getRGB(x, cy);
            int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
            if (r > 50 && r > b * 1.5) { left = x; break; }
        }
        for (int x = w - 1; x > w / 2; x--) {
            int rgb = img.getRGB(x, cy);
            int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
            if (r > 50 && r > b * 1.5) { right = x; break; }
        }

        int cx = w / 2;
        for (int y = 0; y < h / 2; y++) {
            int rgb = img.getRGB(cx, y);
            int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
            if (r > 50 && r > b * 1.5) { top = y; break; }
        }
        for (int y = h - 1; y > h / 2; y--) {
            int rgb = img.getRGB(cx, y);
            int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
            if (r > 50 && r > b * 1.5) { bottom = y; break; }
        }

        System.out.printf("Bounds of wood table: Left=%d, Right=%d, Top=%d, Bottom=%d (Width=%d, Height=%d)\n",
            left, right, top, bottom, right - left + 1, bottom - top + 1);
    }
}
