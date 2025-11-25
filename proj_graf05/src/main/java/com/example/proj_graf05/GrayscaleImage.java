package com.example.proj_graf05;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * Klasa pomocnicza – reprezentacja obrazu w odcieniach szarości.
 * Przechowuje tablicę wartości 0..255 oraz rozmiar obrazu.
 * ZERO elementów UI – tylko logika.
 */
public class GrayscaleImage {

    private final int width;
    private final int height;
    private final int[] data; // odcienie szarości 0..255, length = width * height

    public GrayscaleImage(int width, int height, int[] data) {
        if (data.length != width * height) {
            throw new IllegalArgumentException("Nieprawidłowy rozmiar tablicy danych szarości");
        }
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int[] getData() {
        return data;
    }

    /**
     * Tworzy GrayscaleImage z obrazu JavaFX (Image), używając luminancji:
     * Y = 0.299 R + 0.587 G + 0.114 B.
     */
    public static GrayscaleImage fromImage(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int size = width * height;
        int[] grayData = new int[size];

        PixelReader pr = image.getPixelReader();
        if (pr == null) {
            throw new IllegalStateException("Brak PixelReader w obrazie");
        }

        int[] argb = new int[size];
        pr.getPixels(0, 0, width, height,
                PixelFormat.getIntArgbInstance(), argb, 0, width);

        for (int i = 0; i < size; i++) {
            int pixel = argb[i];
            int a = (pixel >>> 24) & 0xFF;
            if (a == 0) {
                grayData[i] = 0;
                continue;
            }
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;

            int gray = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
            if (gray < 0) gray = 0;
            if (gray > 255) gray = 255;
            grayData[i] = gray;
        }

        return new GrayscaleImage(width, height, grayData);
    }

    /**
     * Zamiana obrazu szarości (0..255) na obraz JavaFX (ARGB).
     */
    public Image toImage() {
        WritableImage wimg = new WritableImage(width, height);
        PixelWriter pw = wimg.getPixelWriter();

        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int g = data[idx++];
                int argb = (0xFF << 24) | (g << 16) | (g << 8) | g;
                pw.setArgb(x, y, argb);
            }
        }
        return wimg;
    }
}
