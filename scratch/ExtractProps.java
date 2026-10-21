import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Queue;
import javax.imageio.ImageIO;

public class ExtractProps {
    public static void main(String[] args) throws Exception {
        File dir = new File("C:\\Users\\Luca\\.gemini\\antigravity\\brain\\c92ecb28-f7c5-4764-9ba7-e4342d6dcab7");
        File outDir = new File("c:\\Users\\Luca\\Desktop\\IdeaProjects\\Dungeon-Master-Manager\\shared\\src\\commonMain\\composeResources\\drawable");

        String[][] props = {
            {"prop_dice", "prop_dice_1792535252144.jpg", "18"},
            {"prop_compass", "prop_compass_1792535265952.jpg", "18"},
            {"prop_seal", "prop_seal_1792535283797.jpg", "18"},
            {"prop_coins", "prop_coins_1792535299571.jpg", "18"},
            {"prop_quill", "prop_quill_1792535314970.jpg", "22"}
        };

        for (String[] prop : props) {
            String name = prop[0];
            String filename = prop[1];
            int bgThresh = Integer.parseInt(prop[2]);
            File inFile = new File(dir, filename);
            if (!inFile.exists()) {
                System.err.println("File not found: " + inFile);
                continue;
            }

            BufferedImage src = ImageIO.read(inFile);
            int w = src.getWidth();
            int h = src.getHeight();

            // 1. Flood fill from edges to identify outer background
            boolean[][] isBg = new boolean[w][h];
            Queue<int[]> q = new ArrayDeque<>();

            // Seed with borders
            for (int x = 0; x < w; x++) {
                seed(src, x, 0, bgThresh, isBg, q);
                seed(src, x, h - 1, bgThresh, isBg, q);
            }
            for (int y = 0; y < h; y++) {
                seed(src, 0, y, bgThresh, isBg, q);
                seed(src, w - 1, y, bgThresh, isBg, q);
            }

            int[] dx = {-1, 1, 0, 0};
            int[] dy = {0, 0, -1, 1};

            while (!q.isEmpty()) {
                int[] p = q.poll();
                int px = p[0];
                int py = p[1];
                for (int i = 0; i < 4; i++) {
                    int nx = px + dx[i];
                    int ny = py + dy[i];
                    if (nx >= 0 && nx < w && ny >= 0 && ny < h && !isBg[nx][ny]) {
                        int rgb = src.getRGB(nx, ny);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;
                        int maxC = Math.max(r, Math.max(g, b));
                        if (maxC <= bgThresh) {
                            isBg[nx][ny] = true;
                            q.add(new int[]{nx, ny});
                        }
                    }
                }
            }

            // 2. Compute object alpha mask with anti-aliasing
            // Foreground is !isBg
            float[][] fgAlpha = new float[w][h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (isBg[x][y]) {
                        fgAlpha[x][y] = 0f;
                    } else {
                        // Check brightness to feather edges near background
                        int rgb = src.getRGB(x, y);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;
                        int maxC = Math.max(r, Math.max(g, b));
                        if (maxC < bgThresh + 16) {
                            fgAlpha[x][y] = (float)(maxC - bgThresh) / 16f;
                            if (fgAlpha[x][y] < 0f) fgAlpha[x][y] = 0f;
                        } else {
                            fgAlpha[x][y] = 1f;
                        }
                    }
                }
            }

            // 3. Generate soft drop shadow (offset +10, +16, blurred)
            int blurR = 14;
            int shadowOffX = 8;
            int shadowOffY = 12;
            float[][] shadowAlpha = new float[w][h];

            // Box blur approximation of Gaussian for shadow
            float[][] tempBlur = new float[w][h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int sx = x - shadowOffX;
                    int sy = y - shadowOffY;
                    if (sx >= 0 && sx < w && sy >= 0 && sy < h) {
                        tempBlur[x][y] = fgAlpha[sx][sy];
                    }
                }
            }

            // Two-pass box blur
            float[][] blurredH = new float[w][h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    float sum = 0;
                    int count = 0;
                    for (int k = -blurR; k <= blurR; k++) {
                        int nx = x + k;
                        if (nx >= 0 && nx < w) {
                            sum += tempBlur[nx][y];
                            count++;
                        }
                    }
                    blurredH[x][y] = sum / count;
                }
            }
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    float sum = 0;
                    int count = 0;
                    for (int k = -blurR; k <= blurR; k++) {
                        int ny = y + k;
                        if (ny >= 0 && ny < h) {
                            sum += blurredH[x][ny];
                            count++;
                        }
                    }
                    shadowAlpha[x][y] = (sum / count) * 0.55f; // Shadow intensity 55%
                }
            }

            // 4. Find bounding box to crop tightly with breathing margin
            int minX = w, maxX = 0, minY = h, maxY = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (fgAlpha[x][y] > 0.05f || shadowAlpha[x][y] > 0.05f) {
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }

            // Margin of 20px
            minX = Math.max(0, minX - 20);
            minY = Math.max(0, minY - 20);
            maxX = Math.min(w - 1, maxX + 20);
            maxY = Math.min(h - 1, maxY + 20);

            int cropW = maxX - minX + 1;
            int cropH = maxY - minY + 1;

            BufferedImage result = new BufferedImage(cropW, cropH, BufferedImage.TYPE_INT_ARGB);

            for (int cy = 0; cy < cropH; cy++) {
                for (int cx = 0; cx < cropW; cx++) {
                    int ix = minX + cx;
                    int iy = minY + cy;

                    float fa = fgAlpha[ix][iy];
                    float sa = shadowAlpha[ix][iy];

                    int srcRgb = src.getRGB(ix, iy);
                    int fr = (srcRgb >> 16) & 0xFF;
                    int fg = (srcRgb >> 8) & 0xFF;
                    int fb = srcRgb & 0xFF;

                    // Composite foreground over dark shadow
                    // Shadow color: 10, 8, 6 (warm dark shadow on wood)
                    int sr = 10, sg = 8, sb = 6;

                    // Out alpha = fa + sa * (1 - fa)
                    float outA = fa + sa * (1f - fa);
                    if (outA <= 0.005f) {
                        result.setRGB(cx, cy, 0x00000000);
                    } else {
                        float rOut = (fr * fa + sr * sa * (1f - fa)) / outA;
                        float gOut = (fg * fa + sg * sa * (1f - fa)) / outA;
                        float bOut = (fb * fa + sb * sa * (1f - fa)) / outA;

                        int aInt = Math.min(255, Math.max(0, Math.round(outA * 255f)));
                        int rInt = Math.min(255, Math.max(0, Math.round(rOut)));
                        int gInt = Math.min(255, Math.max(0, Math.round(gOut)));
                        int bInt = Math.min(255, Math.max(0, Math.round(bOut)));

                        int argb = (aInt << 24) | (rInt << 16) | (gInt << 8) | bInt;
                        result.setRGB(cx, cy, argb);
                    }
                }
            }

            File outFile = new File(outDir, name + ".png");
            ImageIO.write(result, "PNG", outFile);
            System.out.printf("Successfully generated %s (%dx%d) -> %s\n", name, cropW, cropH, outFile.getAbsolutePath());
        }
    }

    private static void seed(BufferedImage img, int x, int y, int bgThresh, boolean[][] isBg, Queue<int[]> q) {
        if (!isBg[x][y]) {
            int rgb = img.getRGB(x, y);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            int maxC = Math.max(r, Math.max(g, b));
            if (maxC <= bgThresh) {
                isBg[x][y] = true;
                q.add(new int[]{x, y});
            }
        }
    }
}
