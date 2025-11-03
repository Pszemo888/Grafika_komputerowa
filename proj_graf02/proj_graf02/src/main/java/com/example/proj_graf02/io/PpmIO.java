package com.example.proj_graf02.io;

import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Ręczny parser PPM:
 *  - P3 (ASCII) i P6 (binarny),
 *  - komentarze '#' i dowolne białe znaki,
 *  - maxval do 65535 (Netpbm), liniowe mapowanie do 0..255,
 *  - P6 czytany BLOKOWO (readNBytes), nie bajt-po-bajcie.
 * Zabronione są zewnętrzne biblioteki PPM – tutaj nie używamy żadnych.
 */
public final class PpmIO {
    private PpmIO() {}

    public static BufferedImage read(InputStream rawIn) throws IOException {
        InputStream in = rawIn instanceof BufferedInputStream ? rawIn : new BufferedInputStream(rawIn, 1 << 16);

        String magic = readToken(in);
        if (!"P3".equals(magic) && !"P6".equals(magic)) {
            throw new IOException("Nieobsługiwany nagłówek PPM: " + magic);
        }
        int width  = parseInt(readToken(in), "szerokość");
        int height = parseInt(readToken(in), "wysokość");
        int maxval = parseInt(readToken(in), "maxval");
        if (width <= 0 || height <= 0) throw new IOException("Nieprawidłowe wymiary: " + width + "x" + height);
        if (maxval <= 0 || maxval > 65535) throw new IOException("Nieobsługiwany maxval: " + maxval);

        return "P3".equals(magic) ? readP3(in, width, height, maxval) : readP6(in, width, height, maxval);
    }

    /* ===== P3 (ASCII) ===== */
    private static BufferedImage readP3(InputStream in, int w, int h, int maxval) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        final double scale = 255.0 / maxval;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = parseInt(readToken(in), "R");
                int g = parseInt(readToken(in), "G");
                int b = parseInt(readToken(in), "B");
                int rr = clamp((int)Math.round(r * scale));
                int gg = clamp((int)Math.round(g * scale));
                int bb = clamp((int)Math.round(b * scale));
                img.setRGB(x, y, (rr << 16) | (gg << 8) | bb);
            }
        }
        return img;
    }

    /* ===== P6 (binarny, blokowo) ===== */
    private static BufferedImage readP6(InputStream in, int w, int h, int maxval) throws IOException {
        // Po maxval dozwolny jeden/więcej białych znaków przed danymi – zignoruj je:
        int c;
        do { c = in.read(); if (c == -1) throw new EOFException("Brak danych po nagłówku P6"); }
        while (isWhitespace((byte)c));

        // Oddaj nie-biały znak do strumienia, jeżeli taki był:
        PushbackInputStream pin = new PushbackInputStream(in, 1 << 16);
        if (c != -1 && !isWhitespace((byte)c)) pin.unread(c);

        final boolean twoBytes = maxval > 255;
        final int bytesPerSample = twoBytes ? 2 : 1;
        final long expected = (long) w * h * 3L * bytesPerSample;

        byte[] buf = pin.readNBytes((int) expected);   // BLOKOWO
        if (buf.length < expected) throw new EOFException("Za mało danych P6: " + buf.length + "/" + expected);

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        final double scale = 255.0 / maxval;

        int idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r, g, b;
                if (twoBytes) {
                    r = ((buf[idx] & 0xFF) << 8) | (buf[idx + 1] & 0xFF);
                    g = ((buf[idx + 2] & 0xFF) << 8) | (buf[idx + 3] & 0xFF);
                    b = ((buf[idx + 4] & 0xFF) << 8) | (buf[idx + 5] & 0xFF);
                    idx += 6;
                } else {
                    r = buf[idx++] & 0xFF;
                    g = buf[idx++] & 0xFF;
                    b = buf[idx++] & 0xFF;
                }
                int rr = clamp((int)Math.round(r * scale));
                int gg = clamp((int)Math.round(g * scale));
                int bb = clamp((int)Math.round(b * scale));
                img.setRGB(x, y, (rr << 16) | (gg << 8) | bb);
            }
        }
        return img;
    }

    /* ===== Tokenizer dla nagłówków i danych P3 ===== */
    private static String readToken(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;

        // pomiń białe i komentarze
        while (true) {
            in.mark(1);
            c = in.read();
            if (c == -1) throw new EOFException("Nieoczekiwany koniec pliku");
            if (isWhitespace((byte)c)) continue;
            if (c == '#') { skipLine(in); continue; }
            in.reset();
            break;
        }

        // czytaj do białego/komentarza
        while (true) {
            in.mark(1);
            c = in.read();
            if (c == -1) break;
            if (isWhitespace((byte)c)) break;
            if (c == '#') { in.reset(); break; }
            sb.append((char)c);
        }
        return sb.toString();
    }

    private static void skipLine(InputStream in) throws IOException {
        int c;
        while ((c = in.read()) != -1) if (c == '\n') break;
    }

    private static boolean isWhitespace(byte b) {
        return b == ' ' || b == '\n' || b == '\r' || b == '\t' || b == '\f';
    }

    private static int parseInt(String s, String what) throws IOException {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException ex) { throw new IOException("Nieprawidłowy token " + what + ": '" + s + "'"); }
    }

    private static int clamp(int v) { return v < 0 ? 0 : Math.min(255, v); }
}
