package com.example.proj_graf07;

import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class EditorLogic {

    // ====== Modele danych (serializowalne) ======
    public static class Point2 implements Serializable {
        public double x, y;
        public Point2() {}
        public Point2(double x, double y) { this.x = x; this.y = y; }
    }

    public static class Poly implements Serializable {
        public String id;
        public List<Point2> pts = new ArrayList<>();
        public String fillHex;
        public String strokeHex;
    }

    public static class State implements Serializable {
        public List<Poly> polys = new ArrayList<>();
        public String selectedId = null;
        public double pivotX = 200;
        public double pivotY = 200;
    }

    // ====== Macierze jednorodne 3x3 ======
    public static class Mat3 {
        private final double[][] m = new double[3][3];

        public static Mat3 identity() {
            Mat3 r = new Mat3();
            r.m[0][0] = 1; r.m[1][1] = 1; r.m[2][2] = 1;
            return r;
        }

        public static Mat3 translation(double dx, double dy) {
            Mat3 r = identity();
            r.m[0][2] = dx;
            r.m[1][2] = dy;
            return r;
        }

        public static Mat3 rotationDegAround(double angleDeg, double px, double py) {
            double a = Math.toRadians(angleDeg);
            double c = Math.cos(a);
            double s = Math.sin(a);

            // T(p) * R * T(-p)
            Mat3 t1 = translation(-px, -py);
            Mat3 r = identity();
            r.m[0][0] = c;  r.m[0][1] = -s;
            r.m[1][0] = s;  r.m[1][1] = c;
            Mat3 t2 = translation(px, py);

            return t2.mul(r).mul(t1);
        }

        public static Mat3 scaleAround(double k, double px, double py) {
            // T(p) * S * T(-p)
            Mat3 t1 = translation(-px, -py);
            Mat3 s = identity();
            s.m[0][0] = k;
            s.m[1][1] = k;
            Mat3 t2 = translation(px, py);

            return t2.mul(s).mul(t1);
        }

        public Mat3 mul(Mat3 b) {
            Mat3 r = new Mat3();
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    double sum = 0;
                    for (int k = 0; k < 3; k++) sum += this.m[i][k] * b.m[k][j];
                    r.m[i][j] = sum;
                }
            }
            return r;
        }

        public Point2 apply(Point2 p) {
            double x = p.x, y = p.y;
            double nx = m[0][0] * x + m[0][1] * y + m[0][2];
            double ny = m[1][0] * x + m[1][1] * y + m[1][2];
            double nw = m[2][0] * x + m[2][1] * y + m[2][2];
            if (Math.abs(nw) < 1e-12) nw = 1.0;
            return new Point2(nx / nw, ny / nw);
        }
    }

    // ====== Stan logiki ======
    private State state = new State();

    public State getState() { return state; }
    public List<Poly> polys() { return state.polys; }

    public void clear() { state = new State(); }

    public void select(String id) { state.selectedId = id; }
    public String selectedId() { return state.selectedId; }
    public Poly selected() { return findById(state.selectedId); }

    public void setPivot(double x, double y) { state.pivotX = x; state.pivotY = y; }
    public double pivotX() { return state.pivotX; }
    public double pivotY() { return state.pivotY; }

    public Poly findById(String id) {
        if (id == null) return null;
        for (Poly p : state.polys) if (id.equals(p.id)) return p;
        return null;
    }

    public String addPolygon(List<Point2> pts) {
        if (pts == null || pts.size() < 3) throw new IllegalArgumentException("min 3 punkty");
        Poly p = new Poly();
        p.id = "P" + (state.polys.size() + 1);
        p.pts.addAll(pts);
        p.fillHex = randomSoftHex();
        p.strokeHex = "#e2e8f0";

        state.polys.add(p);
        state.selectedId = p.id;
        return p.id;
    }

    public void applyToSelected(Mat3 t) {
        Poly p = selected();
        if (p == null) return;
        for (int i = 0; i < p.pts.size(); i++) p.pts.set(i, t.apply(p.pts.get(i)));
    }

    // ====== Zapis / wczyt ======
    public void save(File f) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f)))) {
            oos.writeObject(state);
        }
    }

    public void load(File f) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)))) {
            Object obj = ois.readObject();
            if (!(obj instanceof State s)) throw new IOException("zły format pliku");
            state = s;
        }
    }

    // ====== Parsowanie punktów "x,y x,y ..." ======
    public static List<Point2> parsePoints(String text) {
        String t = text.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("empty");

        String[] parts = t.split("\\s+");
        List<Point2> pts = new ArrayList<>();
        for (String part : parts) {
            String[] xy = part.split(",");
            if (xy.length != 2) throw new IllegalArgumentException("bad point");
            double x = parseDouble(xy[0]);
            double y = parseDouble(xy[1]);
            pts.add(new Point2(x, y));
        }
        if (pts.size() < 3) throw new IllegalArgumentException("need 3");
        return pts;
    }

    private static double parseDouble(String s) {
        return Double.parseDouble(s.trim().replace(',', '.'));
    }

    private static String randomSoftHex() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int rr = (int) Math.round((0.35 + r.nextDouble() * 0.5) * 255);
        int gg = (int) Math.round((0.35 + r.nextDouble() * 0.5) * 255);
        int bb = (int) Math.round((0.35 + r.nextDouble() * 0.5) * 255);
        return String.format("#%02x%02x%02x", rr, gg, bb);
    }
}
