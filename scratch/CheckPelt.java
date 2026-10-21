import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class CheckPelt {
    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File("shared/src/commonMain/composeResources/drawable/prop_fur_pelt.png"));
        System.out.println("Dimensions: " + img.getWidth() + "x" + img.getHeight());
        int minX = img.getWidth(), maxX = 0, minY = img.getHeight(), maxY = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int alpha = (img.getRGB(x, y) >> 24) & 0xFF;
                if (alpha > 20) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        System.out.println("Non-transparent bounds: x=[" + minX + "," + maxX + "] y=[" + minY + "," + maxY + "]");
        System.out.println("Tight content size: " + (maxX - minX) + "x" + (maxY - minY));
    }
}
