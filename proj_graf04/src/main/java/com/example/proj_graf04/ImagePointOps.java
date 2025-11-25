package com.example.proj_graf04;

import java.awt.image.BufferedImage;

public final class ImagePointOps {

    private ImagePointOps() {}

    private static int clamp(int v) {
        return v < 0 ? 0 : Math.min(255, v);
    }

    // Dodawanie stałej (osobno dla R,G,B)
    public static BufferedImage add(BufferedImage src, int dR, int dG, int dB) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                r = clamp(r + dR);
                g = clamp(g + dG);
                b = clamp(b + dB);

                out.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    public static BufferedImage subtract(BufferedImage src, int dR, int dG, int dB) {
        return add(src, -dR, -dG, -dB);
    }

    public static BufferedImage multiply(BufferedImage src, double fR, double fG, double fB) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                r = clamp((int) Math.round(r * fR));
                g = clamp((int) Math.round(g * fG));
                b = clamp((int) Math.round(b * fB));

                out.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    public static BufferedImage divide(BufferedImage src, double dR, double dG, double dB) {
        return multiply(src, 1.0 / dR, 1.0 / dG, 1.0 / dB);
    }

    public static BufferedImage changeBrightness(BufferedImage src, int delta) {
        return add(src, delta, delta, delta);
    }

    // Skala szarości – sposób 1: średnia arytmetyczna
    public static BufferedImage toGrayAverage(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int gray = (r + g + b) / 3;
                gray = clamp(gray);
                out.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return out;
    }

    // Skala szarości – sposób 2: luminancja
    public static BufferedImage toGrayLuma(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                //wagi luminacji
                int gray = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
                gray = clamp(gray);
                out.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return out;
    }
}
