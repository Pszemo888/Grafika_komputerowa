package com.example.proj_graf01.model;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public abstract class ShapeBase {
    public String id = java.util.UUID.randomUUID().toString();
    public double ax, ay, bx, by;
    public double strokeWidth = 2.0;
    public String stroke = "#222222"; // kolor konturu
    public String fill = "#00000000"; // Line zignoruje fill

    public abstract ShapeType type();

    // Rysowanie podstawowej geometrii (kontur)
    public abstract void drawGeometry(GraphicsContext g);

    // Czy klik trafia w figurę (krawędź/wypełnienie)
    public abstract boolean hit(double x, double y, double pickTol);

    // Uchwyt(y) do modyfikacji
    public abstract double[][] handles();

    // Przeciąganie uchwytu
    public abstract void applyHandleDrag(int handleIndex, double x, double y);

    // Przesunięcie całości
    public void moveBy(double dx, double dy) { ax+=dx; ay+=dy; bx+=dx; by+=dy; }

    // Rysowanie całości (fill + stroke)
    public void draw(GraphicsContext g, boolean selected) {
        // Fill (tylko tam gdzie ma sens)
        if (type() != ShapeType.LINE && !isTransparent(fill)) {
            g.setFill(parseColor(fill));
            drawFill(g);
        }
        // Stroke (kontur)
        g.setStroke(parseColor(stroke)); //kolor
        g.setLineWidth(strokeWidth); //grubosc konturu
        drawGeometry(g);

        if (selected) drawSelection(g);
    }

    protected void drawFill(GraphicsContext g) { /* implementują tylko RECT/CIRCLE */ }

    /**
     * Rysowanie „oprawy zaznaczenia”:
     * - przerywany niebieski obrys (kształt-specyficzny),
     * - kwadratowe uchwyty w miejscach podanych przez handles().
     */
    protected void drawSelection(GraphicsContext g) {
        g.setStroke(Color.web("#0078D7"));
        g.setLineWidth(1);
        g.setLineDashes(4,4);
        drawSelectionOutline(g);
        g.setLineDashes(null);

        // uchwyty (małe kwadraciki)
        double[][] hs = handles();
        g.setFill(Color.web("#0078D7"));
        for (double[] h : hs) g.fillRect(h[0]-4, h[1]-4, 8, 8);
    }
    protected abstract void drawSelectionOutline(GraphicsContext g);

    /**
     * Parsuje kolor z HEX (#RRGGBB lub #AARRGGBB).
     * W razie błędu zwraca Color.BLACK (bezpieczny fallback).
     */
    protected static Color parseColor(String hex){
        try{
            if (hex == null || !hex.startsWith("#")) return Color.BLACK;
            String h = hex.substring(1);
            if (h.length()==6) {
                int r = Integer.parseInt(h.substring(0,2),16);
                int g = Integer.parseInt(h.substring(2,4),16);
                int b = Integer.parseInt(h.substring(4,6),16);
                return Color.rgb(r,g,b);
            } else if (h.length()==8) {
                int a = Integer.parseInt(h.substring(0,2),16);
                int r = Integer.parseInt(h.substring(2,4),16);
                int g = Integer.parseInt(h.substring(4,6),16);
                int b = Integer.parseInt(h.substring(6,8),16);
                return Color.rgb(r,g,b, a/255.0);
            }
        } catch(Exception ignored){}
        return Color.BLACK;
    }
    /**
     * Sprawdza, czy HEX oznacza w pełni przezroczysty kolor (alfa == 0).
     * Jeśli nie da się sparsować — bezpiecznie uznajemy, że „transparent”.
     */
    protected static boolean isTransparent(String hex){
        try { return parseColor(hex).getOpacity() == 0.0; } catch (Exception e) { return true; }
    }

    /**
     * Z dwóch punktów A(ax,ay) i B(bx,by) buduje „normalną” ramkę prostokąta [x,y,w,h]
     * (niezależnie od kierunku przeciągania — normalizuje lewy/górny oraz szer./wys.).
     */
    protected static double[] rectFromAB(double ax, double ay, double bx, double by) {
        double x = Math.min(ax,bx), y = Math.min(ay,by);
        double w = Math.abs(bx-ax), h = Math.abs(by-ay);
        return new double[] {x,y,w,h};
    }

    /** Odległość euklidesowa między dwoma punktami (przydatne np. dla promienia koła). */
    protected static double dist(double x1,double y1,double x2,double y2){
        return Math.hypot(x2-x1, y2-y1);
    }

    /**
     * Odległość punkt–odcinek: zwraca minimalną odległość od P(px,py) do odcinka A(x1,y1)–B(x2,y2).
     * Używane w hit-test linii, żeby sprawdzić „klik blisko linii”.
     */
    protected static double pointToSegment(double px, double py, double x1, double y1, double x2, double y2){
        double vx = x2 - x1, vy = y2 - y1; // wektor AB
        double wx = px - x1, wy = py - y1; // wektor AP

        double c1 = vx*wx + vy*wy;         // rzut skalar AP na AB
        if (c1 <= 0) return dist(px,py,x1,y1); // najbliżej końca A

        double c2 = vx*vx + vy*vy;         // |AB|^2
        if (c2 <= c1) return dist(px,py,x2,y2); // najbliżej końca B

        double b = c1 / c2;                // pozycja rzutu na odcinku <0..1>
        double bx = x1 + b*vx, by = y1 + b*vy; // współrzędne rzutu
        return dist(px,py,bx,by);
    }
}
