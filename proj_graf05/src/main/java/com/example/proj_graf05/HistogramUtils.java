package com.example.proj_graf05;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * Funkcje związane z histogramem:
 * - liczenie histogramu
 * - rozszerzenie histogramu
 * - wyrównanie histogramu
 * - rysowanie histogramu jako obrazka
 */
public class HistogramUtils {

    /**
     * Liczy histogram (256 elementów) dla obrazu w skali szarości.
     */
    public static int[] computeHistogram(GrayscaleImage image) {
        int[] hist = new int[256];
        int[] data = image.getData();
        for (int g : data) {
            hist[g]++;
        }
        return hist;
    }

    /**
     * Znajduje minimalny i maksymalny poziom szarości, który faktycznie występuje.
     * Zwraca {min, max}.
     */
    public static int[] findMinMaxGray(int[] hist) {
        int min = 0;
        int max = 255;

        for (int i = 0; i < 256; i++) {
            if (hist[i] > 0) {
                min = i;
                break;
            }
        }
        for (int i = 255; i >= 0; i--) {
            if (hist[i] > 0) {
                max = i;
                break;
            }
        }
        return new int[]{min, max};
    }

    /**
     * Rozszerzenie histogramu (histogram stretching):
     * g' = (g - min) * 255 / (max - min)
     */
    public static GrayscaleImage stretchHistogram(GrayscaleImage src) {
        int[] srcData = src.getData();
        int len = srcData.length;
        int[] dstData = new int[len];

        int[] hist = computeHistogram(src);
        int[] minMax = findMinMaxGray(hist);
        int min = minMax[0];
        int max = minMax[1];

        if (max == min) {
            System.arraycopy(srcData, 0, dstData, 0, len);
            return new GrayscaleImage(src.getWidth(), src.getHeight(), dstData);
        }

        for (int i = 0; i < len; i++) {
            int g = srcData[i];
            int newG = (g - min) * 255 / (max - min);
            if (newG < 0) newG = 0;
            if (newG > 255) newG = 255;
            dstData[i] = newG;
        }

        return new GrayscaleImage(src.getWidth(), src.getHeight(), dstData);
    }

    /**
     * Wyrównanie histogramu (histogram equalization) z użyciem CDF.
     */
    public static GrayscaleImage equalizeHistogram(GrayscaleImage src) {
        int[] srcData = src.getData();
        int len = srcData.length;
        int[] dstData = new int[len];

        int[] hist = computeHistogram(src);

        int[] cdf = new int[256];
        int cum = 0;
        for (int i = 0; i < 256; i++) {
            cum += hist[i];
            cdf[i] = cum;
        }

        int total = len;
        int cdfMin = 0;
        for (int i = 0; i < 256; i++) {
            if (cdf[i] > 0) {
                cdfMin = cdf[i];
                break;
            }
        }

        if (cdfMin == 0 || cdf[255] == cdfMin) {
            System.arraycopy(srcData, 0, dstData, 0, len);
            return new GrayscaleImage(src.getWidth(), src.getHeight(), dstData);
        }

        double denom = total - cdfMin;

        for (int i = 0; i < len; i++) {
            int g = srcData[i];
            int mapped = (int) Math.round((cdf[g] - cdfMin) * 255.0 / denom);
            if (mapped < 0) mapped = 0;
            if (mapped > 255) mapped = 255;
            dstData[i] = mapped;
        }

        return new GrayscaleImage(src.getWidth(), src.getHeight(), dstData);
    }

    /**
     * Rysuje histogram jako obraz  (słupki białe na czarnym tle).
     */
    public static Image createHistogramImage(int[] hist, int width, int height) {
        int maxCount = 0;
        for (int h : hist) {
            if (h > maxCount) maxCount = h;
        }
        if (maxCount == 0) maxCount = 1;

        WritableImage wimg = new WritableImage(width, height);
        PixelWriter pw = wimg.getPixelWriter();

        for (int x = 0; x < width; x++) {
            int histIndex = (int) Math.round((x / (double) (width - 1)) * 255);
            if (histIndex < 0) histIndex = 0;
            if (histIndex > 255) histIndex = 255;

            int count = hist[histIndex];
            int barHeight = (int) Math.round((count / (double) maxCount) * (height - 1));

            for (int y = 0; y < height; y++) {
                int yy = height - 1 - y;
                int argb;
                if (yy <= barHeight) {
                    argb = 0xFFFFFFFF; // słupek
                } else {
                    argb = 0xFF000000; // tło
                }
                pw.setArgb(x, y, argb);
            }
        }

        return wimg;
    }
}
