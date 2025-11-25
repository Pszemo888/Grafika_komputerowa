package com.example.proj_graf04;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public final class ImageFilters {

    private ImageFilters() {}

    private static int clamp(int v) { return v < 0 ? 0 : Math.min(255, v); }

    // bazowy splot 3x3
    public static BufferedImage convolve3x3(BufferedImage src, int[][] kernel, double divisor, double offset) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        // brzegi kopiujemy
        for (int y = 0; y < h; y++) {
            out.setRGB(0, y, src.getRGB(0, y));
            out.setRGB(w - 1, y, src.getRGB(w - 1, y));
        }
        for (int x = 0; x < w; x++) {
            out.setRGB(x, 0, src.getRGB(x, 0));
            out.setRGB(x, h - 1, src.getRGB(x, h - 1));
        }

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {

                double sumR = 0, sumG = 0, sumB = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int weight = kernel[ky + 1][kx + 1];
                        int rgb = src.getRGB(x + kx, y + ky);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = (rgb) & 0xFF;
                        sumR += weight * r;
                        sumG += weight * g;
                        sumB += weight * b;
                    }
                }

                int r = clamp((int) Math.round(sumR / divisor + offset));
                int g = clamp((int) Math.round(sumG / divisor + offset));
                int b = clamp((int) Math.round(sumB / divisor + offset));

                out.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    // Wygładzający
    public static BufferedImage mean3x3(BufferedImage src) {
        int[][] kernel = {
                {1, 1, 1},
                {1, 1, 1},
                {1, 1, 1}
        };
        return convolve3x3(src, kernel, 9.0, 0.0);
    }

    // Gauss 3x3
    public static BufferedImage gaussian3x3(BufferedImage src) {
        int[][] kernel = {
                {1, 2, 1},
                {2, 4, 2},
                {1, 2, 1}
        };
        return convolve3x3(src, kernel, 16.0, 0.0);
    }

    // Górnoprzepustowy wyostrzający
    public static BufferedImage sharpenHighPass(BufferedImage src) {
        int[][] kernel = {
                {-1, -1, -1},
                {-1,  9, -1},
                {-1, -1, -1}
        };
        return convolve3x3(src, kernel, 1.0, 0.0);
    }

    // Medianowy 3x3
    public static BufferedImage median3x3(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        // kopiujemy brzegi
        for (int y = 0; y < h; y++) {
            out.setRGB(0, y, src.getRGB(0, y));
            out.setRGB(w - 1, y, src.getRGB(w - 1, y));
        }
        for (int x = 0; x < w; x++) {
            out.setRGB(x, 0, src.getRGB(x, 0));
            out.setRGB(x, h - 1, src.getRGB(x, h - 1));
        }

        int[] windowR = new int[9];
        int[] windowG = new int[9];
        int[] windowB = new int[9];

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int idx = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int rgb = src.getRGB(x + kx, y + ky);
                        windowR[idx] = (rgb >> 16) & 0xFF;
                        windowG[idx] = (rgb >> 8) & 0xFF;
                        windowB[idx] = rgb & 0xFF;
                        idx++;
                    }
                }
                Arrays.sort(windowR);
                Arrays.sort(windowG);
                Arrays.sort(windowB);
                int r = windowR[4];
                int g = windowG[4];
                int b = windowB[4];
                out.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    // Sobel – krawędzie (wynik w skali szarości)
    // najpierw obraz w skali szarosci, dwie maski pionowe i poziome, potem obliczenie magnitudy gradientu
    public static BufferedImage sobelEdges(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();

        BufferedImage gray = ImagePointOps.toGrayLuma(src);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        int[][] gx = {
                {-1, -2, -1},
                { 0,  0,  0},
                { 1,  2,  1}
        };
        int[][] gy = {
                { 1,  0, -1},
                { 2,  0, -2},
                { 1,  0, -1}
        };

        // brzegi = czarne
        for (int y = 0; y < h; y++) {
            out.setRGB(0, y, 0);
            out.setRGB(w - 1, y, 0);
        }
        for (int x = 0; x < w; x++) {
            out.setRGB(x, 0, 0);
            out.setRGB(x, h - 1, 0);
        }

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                double sumX = 0;
                double sumY = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int rgb = gray.getRGB(x + kx, y + ky);
                        int g = rgb & 0xFF;

                        sumX += gx[ky + 1][kx + 1] * g;
                        sumY += gy[ky + 1][kx + 1] * g;
                    }
                }

                int mag = (int) Math.round(Math.sqrt(sumX * sumX + sumY * sumY));
                mag = clamp(mag);
                int rgbOut = (mag << 16) | (mag << 8) | mag;
                out.setRGB(x, y, rgbOut);
            }
        }
        return out;
    }
}
