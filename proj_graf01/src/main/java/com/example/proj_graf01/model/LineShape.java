package com.example.proj_graf01.model;

import javafx.scene.canvas.GraphicsContext;



// A(ax,ay) = początek linii,
//B(bx,by) = koniec linii.

public class LineShape extends ShapeBase {

    // Zwraca typ figury (dla serializacji / switchy itp.).
    @Override public ShapeType type() { return ShapeType.LINE; }


    // Rysowanie KONTURU linii aktualnym "piórem" GraphicsContext.
    // Ustawienia pióra (kolor, grubość) są podane wcześniej w ShapeBase.draw(...).
    @Override public void drawGeometry(GraphicsContext g) {
        // Narysuj odcinek od A(ax,ay) do B(bx,by)
        g.strokeLine(ax, ay, bx, by);
    }

    // roznica x,y miedzy a i b punktem
    @Override public boolean hit(double x, double y, double pickTol) {
        double d = pointToSegment(x, y, ax, ay, bx, by);      // odległość punkt–odcinek
        return d <= Math.max(pickTol, strokeWidth + 2);       // "klik blisko linii?"
    }

    /** Pozycje uchwytów (punkty, za które można złapać i przeciągnąć figurę). */
    @Override public double[][] handles() {
        return new double[][] { {ax, ay}, {bx, by} };
    }



     // Przeciąganie uchwytu o indeksie idx do nowego położenia (x,y).
    @Override public void applyHandleDrag(int idx, double x, double y) {
        if (idx == 0) { ax = x; ay = y; }     // zmiana punktu A
        else          { bx = x; by = y; }     // zmiana punktu B
    }


    @Override protected void drawSelectionOutline(GraphicsContext g) {
        g.strokeLine(ax, ay, bx, by);
    }
}
