import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class InspectScreenshot {
    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File("C:/Users/Luca/.gemini/antigravity/brain/c92ecb28-f7c5-4764-9ba7-e4342d6dcab7/.user_uploaded/media_1792566929352_493b8253.png"));
        int w = img.getWidth();
        int h = img.getHeight();

        // Find table bounds (wood is brownish, border is carved wood)
        // Table center is around w/2 = 512, h/2 = 214
        int tableTop = -1, tableBottom = -1, tableLeft = -1, tableRight = -1;
        // Wood color around x=300, y=200
        for (int y = 0; y < h; y++) {
            int rgb = img.getRGB(512, y);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            // Stone floor is grey: r~g~b ~ 40-70. Wood is warmer or dark frame:
            if (r > 60 && r > b + 15) { // warm wood
                if (tableTop == -1) tableTop = y;
                tableBottom = y;
            }
        }
        for (int x = 0; x < w; x++) {
            int rgb = img.getRGB(x, 214);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            if (r > 60 && r > b + 15) {
                if (tableLeft == -1) tableLeft = x;
                tableRight = x;
            }
        }

        System.out.println("Table bounds approximate: x=[" + tableLeft + "," + tableRight + "] y=[" + tableTop + "," + tableBottom + "]");
        System.out.println("Table width=" + (tableRight - tableLeft) + ", height=" + (tableBottom - tableTop));

        // Now search around bottom-left of table for pelt (x: 200-350, y: tableBottom - 50 to tableBottom + 100)
        int peltMinX = w, peltMaxX = 0, peltMinY = h, peltMaxY = 0;
        for (int y = tableBottom - 60; y < Math.min(h, tableBottom + 120); y++) {
            for (int x = tableLeft - 100; x < tableLeft + 150; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                // Pelt has lighter hair strands / brownish fur texture
                // Let's sample difference from standard stone floor
                if (x < tableLeft && y > tableBottom - 30) {
                    if (r > 65 && g > 55 && b > 45 && r > b) {
                        if (x < peltMinX) peltMinX = x;
                        if (x > peltMaxX) peltMaxX = x;
                        if (y < peltMinY) peltMinY = y;
                        if (y > peltMaxY) peltMaxY = y;
                    }
                }
            }
        }
        System.out.println("Pelt detected bounds: x=[" + peltMinX + "," + peltMaxX + "] y=[" + peltMinY + "," + peltMaxY + "]");
        System.out.println("Pelt size: " + (peltMaxX - peltMinX) + "x" + (peltMaxY - peltMinY));
    }
}
