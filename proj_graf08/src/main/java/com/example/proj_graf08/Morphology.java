package com.example.proj_graf08;

import javafx.scene.image.*;
import javafx.scene.paint.Color;

public class Morphology {

    private Morphology() {}

    /** Zamiana obrazu na binarny (true=obiekt/biały) wg progu 0..255 */
    public static boolean[][] thresholdToBinary(Image img, int threshold) {
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();

        boolean[][] out = new boolean[h][w];
        PixelReader pr = img.getPixelReader();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = pr.getColor(x, y);
                int gray = (int) Math.round(255.0 * (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()));
                out[y][x] = gray >= threshold;
            }
        }
        return out;
    }

    /** Wizualizacja boolean[][] jako obraz czarno-biały */
    public static WritableImage binaryToImage(boolean[][] bin) {
        int h = bin.length;
        int w = bin[0].length;

        WritableImage out = new WritableImage(w, h);
        PixelWriter pw = out.getPixelWriter();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pw.setColor(x, y, bin[y][x] ? Color.WHITE : Color.BLACK);
            }
        }
        return out;
    }

    /** Dylatacja: białe się "rozszerza". SE: liczą się tylko pola == 1 */
    public static boolean[][] dilate(boolean[][] in, int[][] se) {
        int h = in.length;
        int w = in[0].length;

        int sh = se.length;
        int sw = se[0].length;
        int oy = sh / 2;
        int ox = sw / 2;

        boolean[][] out = new boolean[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                boolean hit = false;

                outer:
                for (int j = 0; j < sh; j++) {
                    for (int i = 0; i < sw; i++) {
                        if (se[j][i] != 1) continue;

                        int yy = y + (j - oy);
                        int xx = x + (i - ox);

                        if (yy >= 0 && yy < h && xx >= 0 && xx < w) {
                            if (in[yy][xx]) {
                                hit = true;
                                break outer;
                            }
                        }
                    }
                }

                out[y][x] = hit;
            }
        }
        return out;
    }

    /** Erozja: białe się "kurczy". SE: wszystkie pola == 1 muszą trafić w true */
    public static boolean[][] erode(boolean[][] in, int[][] se) {
        int h = in.length;
        int w = in[0].length;

        int sh = se.length;
        int sw = se[0].length;
        int oy = sh / 2;
        int ox = sw / 2;

        boolean[][] out = new boolean[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                boolean ok = true;

                outer:
                for (int j = 0; j < sh; j++) {
                    for (int i = 0; i < sw; i++) {
                        if (se[j][i] != 1) continue;

                        int yy = y + (j - oy);
                        int xx = x + (i - ox);

                        // poza obrazem = tło => erozja fail
                        if (yy < 0 || yy >= h || xx < 0 || xx >= w || !in[yy][xx]) {
                            ok = false;
                            break outer;
                        }
                    }
                }

                out[y][x] = ok;
            }
        }
        return out;
    }

    /** Otwarcie = erozja -> dylatacja */
    public static boolean[][] open(boolean[][] in, int[][] se) {
        return dilate(erode(in, se), se);
    }

    /** Domknięcie = dylatacja -> erozja */
    public static boolean[][] close(boolean[][] in, int[][] se) {
        return erode(dilate(in, se), se);
    }

    /**
     * Hit-or-miss: detekcja wzorca
     *  1  => musi być true (biały)
     * -1  => musi być false (czarny)
     *  0  => ignoruj
     */
    public static boolean[][] hitOrMiss(boolean[][] in, int[][] seTernary) {
        int h = in.length;
        int w = in[0].length;

        int sh = seTernary.length;
        int sw = seTernary[0].length;
        int oy = sh / 2;
        int ox = sw / 2;

        boolean[][] out = new boolean[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                boolean match = true;

                outer:
                for (int j = 0; j < sh; j++) {
                    for (int i = 0; i < sw; i++) {
                        int req = seTernary[j][i];
                        if (req == 0) continue;

                        int yy = y + (j - oy);
                        int xx = x + (i - ox);

                        boolean pix = false; // poza obrazem traktujemy jako tło
                        if (yy >= 0 && yy < h && xx >= 0 && xx < w) {
                            pix = in[yy][xx];
                        }

                        if (req == 1 && !pix) { match = false; break outer; }
                        if (req == -1 && pix) { match = false; break outer; }
                    }
                }

                out[y][x] = match;
            }
        }

        return out;
    }
}
