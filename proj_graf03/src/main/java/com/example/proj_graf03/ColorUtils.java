package com.example.proj_graf03;

public class ColorUtils {

    // RGB [0..255] -> CMYK [0..1]
    public static double[] rgbToCmyk(int r, int g, int b) {
        double rd = r / 255.0;
        double gd = g / 255.0;
        double bd = b / 255.0;

        double k = 1.0 - Math.max(rd, Math.max(gd, bd));

        double c, m, y;
        if (k == 1.0) {
            // kolor czarny – wszystko 0, K = 1
            c = 0;
            m = 0;
            y = 0;
        } else {
            c = (1 - rd - k) / (1 - k);
            m = (1 - gd - k) / (1 - k);
            y = (1 - bd - k) / (1 - k);
        }
        return new double[]{c, m, y, k};
    }

    // CMYK [0..1] -> RGB [0..255]
    public static int[] cmykToRgb(double c, double m, double y, double k) {
        int r = (int) Math.round(255 * (1 - c) * (1 - k));
        int g = (int) Math.round(255 * (1 - m) * (1 - k));
        int b = (int) Math.round(255 * (1 - y) * (1 - k));

        r = clamp(r, 0, 255);
        g = clamp(g, 0, 255);
        b = clamp(b, 0, 255);
        return new int[]{r, g, b};
    }

    public static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
