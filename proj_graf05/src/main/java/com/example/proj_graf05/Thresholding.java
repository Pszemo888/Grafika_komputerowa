package com.example.proj_graf05;

/**
 * Klasa z metodami binaryzacji (tylko logika – żadnego UI).
 * Zaimplementowane:
 *  1) Ręczny próg
 *  2) Selekcja procentowej czerni (Percent Black Selection)
 *  3) Iteracyjna selekcja średniej (Mean Iterative Selection)
 *  4) Selekcja entropii (Entropy Selection / Kapur)
 */
public class Thresholding {

    /**
     * Prosta binaryzacja: g <= threshold -> 0 (czarny), g > threshold -> 255 (biały).
     */
    public static GrayscaleImage applyThreshold(GrayscaleImage src, int threshold) {
        int[] srcData = src.getData();
        int len = srcData.length;
        int[] dstData = new int[len];

        for (int i = 0; i < len; i++) {
            int g = srcData[i];
            dstData[i] = (g <= threshold) ? 0 : 255;
        }

        return new GrayscaleImage(src.getWidth(), src.getHeight(), dstData);
    }

    /**
     * Selekcja procentowej czerni (Percent Black Selection):
     * szukamy takiego progu, aby zadany procent pikseli był czarny.
     */
    public static int percentBlackThreshold(int[] hist, int totalPixels, double percentBlack) {
        if (totalPixels <= 0) return 0;
        if (percentBlack <= 0) return 0;
        if (percentBlack >= 100) return 255;

        long targetBlack = Math.round(totalPixels * (percentBlack / 100.0));
        long cum = 0;
        for (int t = 0; t < 256; t++) {
            cum += hist[t];
            if (cum >= targetBlack) {
                return t;
            }
        }
        return 255;
    }

    /**
     * Iteracyjna selekcja średniej (Mean Iterative Selection):
     *
     * 1. Początkowy próg T = średnia globalna,
     * 2. Dzielimy na dwie klasy [0..T] oraz [T+1..255],
     * 3. Liczymy średnie jasności obu klas m1, m2,
     * 4. Nowy próg T' = (m1 + m2) / 2,
     * 5. Powtarzamy aż |T' - T| < epsilon lub do limitu iteracji.
     */
    public static int meanIterativeThreshold(int[] hist, int totalPixels) {
        if (totalPixels <= 0) return 0;

        double sum = 0.0;
        for (int i = 0; i < 256; i++) {
            sum += i * hist[i];
        }
        double T = sum / totalPixels;

        for (int iter = 0; iter < 100; iter++) {
            int tInt = (int) Math.round(T);

            double sum1 = 0, sum2 = 0;
            int count1 = 0, count2 = 0;

            for (int i = 0; i <= tInt; i++) {
                sum1 += i * hist[i];
                count1 += hist[i];
            }
            for (int i = tInt + 1; i < 256; i++) {
                sum2 += i * hist[i];
                count2 += hist[i];
            }

            if (count1 == 0 || count2 == 0) {
                break; // jedna z klas pusta – dalej nie ma sensu
            }

            double mean1 = sum1 / count1;
            double mean2 = sum2 / count2;
            double newT = (mean1 + mean2) / 2.0;

            if (Math.abs(newT - T) < 0.5) {
                T = newT;
                break;
            }
            T = newT;
        }

        int result = (int) Math.round(T);
        if (result < 0) result = 0;
        if (result > 255) result = 255;
        return result;
    }

    /**
     * Selekcja entropii (Entropy Selection / metoda Kapura):
     * szukamy takiego progu T, dla którego suma entropii klas tła i obiektu
     * (0..T oraz T+1..255) jest maksymalna.
     */
    public static int entropyThreshold(int[] hist, int totalPixels) {
        if (totalPixels <= 0) return 0;

        // Prawdopodobieństwa p(i) dla poziomu szarości i
        double[] p = new double[256];
        for (int i = 0; i < 256; i++) {
            p[i] = hist[i] / (double) totalPixels;
        }

        int bestT = 0;
        double bestEntropy = Double.NEGATIVE_INFINITY;

        // Próbujemy wszystkie możliwe progi 0..254
        for (int t = 0; t < 255; t++) {
            double pBackground = 0.0;
            for (int i = 0; i <= t; i++) {
                pBackground += p[i];
            }
            double pForeground = 1.0 - pBackground;

            if (pBackground <= 0.0 || pForeground <= 0.0) {
                continue; // jedna z klas pusta – brak sensownej entropii
            }

            // Entropia tła
            double hB = 0.0;
            for (int i = 0; i <= t; i++) {
                if (p[i] > 0) {
                    double pi = p[i] / pBackground; // znormalizowane w klasie
                    hB -= pi * Math.log(pi);
                }
            }

            // Entropia obiektu
            double hF = 0.0;
            for (int i = t + 1; i < 256; i++) {
                if (p[i] > 0) {
                    double pi = p[i] / pForeground;
                    hF -= pi * Math.log(pi);
                }
            }

            double H = hB + hF; // łączna entropia
            if (H > bestEntropy) {
                bestEntropy = H;
                bestT = t;
            }
        }

        return bestT;
    }
}
