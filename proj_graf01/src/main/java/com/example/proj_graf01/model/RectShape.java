package com.example.proj_graf01.model;

import javafx.scene.canvas.GraphicsContext;

public class RectShape extends ShapeBase {
    @Override public ShapeType type() { return ShapeType.RECT; }

    @Override public void drawGeometry(GraphicsContext g) {
        double[] r = rectFromAB(ax,ay,bx,by); // → [x, y, w, h]
        g.strokeRect(r[0], r[1], r[2], r[3]);
    }

    @Override protected void drawFill(GraphicsContext g) {
        double[] r = rectFromAB(ax,ay,bx,by);
        g.fillRect(r[0], r[1], r[2], r[3]);
    }

    @Override public boolean hit(double x, double y, double pickTol) {
        double[] r = rectFromAB(ax,ay,bx,by);
        boolean inside = x>=r[0] && y>=r[1] && x<=r[0]+r[2] && y<=r[1]+r[3];
        if (!inside) return false;
        double near = Math.min(Math.min(Math.abs(x - r[0]), Math.abs(x - (r[0]+r[2]))),
                Math.min(Math.abs(y - r[1]), Math.abs(y - (r[1]+r[3]))));
        return near <= Math.max(pickTol, strokeWidth + 2) || !isTransparent(fill);
    }

    @Override public double[][] handles() {
        double[] r = rectFromAB(ax,ay,bx,by);
        double L=r[0], T=r[1], W=r[2], H=r[3];
        return new double[][] {{L,T},{L+W,T},{L,T+H},{L+W,T+H}};
    }

    @Override public void applyHandleDrag(int idx, double x, double y) {
        double[] r = rectFromAB(ax,ay,bx,by);
        double L=r[0], T=r[1], W=r[2], H=r[3];
        double R=L+W, B=T+H;
        switch (idx) {
            case 0 -> { L = x; T = y; }
            case 1 -> { R = x; T = y; }
            case 2 -> { L = x; B = y; }
            case 3 -> { R = x; B = y; }
        }
        ax = L; ay = T; bx = R; by = B;
    }

    @Override protected void drawSelectionOutline(GraphicsContext g) {
        double[] r = rectFromAB(ax,ay,bx,by);
        g.strokeRect(r[0], r[1], r[2], r[3]);
    }
}
