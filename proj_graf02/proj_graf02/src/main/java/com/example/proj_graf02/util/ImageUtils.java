package com.example.proj_graf02.util;


import javafx.scene.image.WritableImage;

import java.awt.*;
import java.awt.image.BufferedImage;

public final class ImageUtils {
    private ImageUtils() {}

    /** Upewnia się, że obraz jest w 8-bitowym RGB (TYPE_INT_RGB). */
    public static BufferedImage toRGB8(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    public static WritableImage toWritable(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        WritableImage wi = new WritableImage(w, h);
        var pw = wi.getPixelWriter();
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            src.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) {
                int rgb = row[x];
                int a = 0xFF, r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                pw.setArgb(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return wi;
    }

    /** Liniowe skalowanie jasności (mnożnik). */
    public static BufferedImage scaleBrightness(BufferedImage src, double factor) {
        if (Math.abs(factor - 1.0) < 1e-6) return src;
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                r = clamp((int)Math.round(r * factor));
                g = clamp((int)Math.round(g * factor));
                b = clamp((int)Math.round(b * factor));
                out.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    /** Normalizacja min–max do zakresu 0..255. */
    public static BufferedImage normalizeToByte(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        int min = 255, max = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                min = Math.min(min, Math.min(r, Math.min(g, b)));
                max = Math.max(max, Math.max(r, Math.max(g, b)));
            }
        }
        if (max == min) return src;
        double scale = 255.0 / (max - min);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                r = clamp((int)Math.round((r - min) * scale));
                g = clamp((int)Math.round((g - min) * scale));
                b = clamp((int)Math.round((b - min) * scale));
                out.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    private static int clamp(int v) { return v < 0 ? 0 : Math.min(255, v); }
}
