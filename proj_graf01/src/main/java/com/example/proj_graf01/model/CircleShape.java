package com.example.proj_graf01.model;

import javafx.scene.canvas.GraphicsContext;

public class CircleShape extends ShapeBase {
    @Override public ShapeType type() { return ShapeType.CIRCLE; }

    @Override public void drawGeometry(GraphicsContext g) {
        double cx = ax, cy = ay, R = dist(ax,ay,bx,by);
        g.strokeOval(cx - R, cy - R, R*2, R*2);
    }

    @Override protected void drawFill(GraphicsContext g) {
        double cx = ax, cy = ay, R = dist(ax,ay,bx,by);
        g.fillOval(cx - R, cy - R, R*2, R*2);
    }

    @Override public boolean hit(double x, double y, double pickTol) {
        double R = dist(ax,ay,bx,by);
        double d = dist(x,y, ax,ay);
        if (Math.abs(d - R) <= Math.max(pickTol, strokeWidth + 2)) return true;
        return !isTransparent(fill) && d < R;
    }

    @Override public double[][] handles() {
        double cx = ax, cy = ay, R = dist(ax,ay,bx,by);
        return new double[][] {{cx+R, cy}, {cx, cy}}; // 0: promień (W), 1: środek
    }

    @Override public void applyHandleDrag(int idx, double x, double y) {
        if (idx == 0) { bx = x; by = y; } else { ax = x; ay = y; }
    }

    @Override protected void drawSelectionOutline(GraphicsContext g) {
        double cx = ax, cy = ay, R = dist(ax,ay,bx,by);
        g.strokeOval(cx - R, cy - R, R*2, R*2);
    }
}
