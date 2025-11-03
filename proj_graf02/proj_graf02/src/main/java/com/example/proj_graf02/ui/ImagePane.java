package com.example.proj_graf02.ui;

import com.example.proj_graf02.util.ImageUtils;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;

/** Canvas z rysowaniem obrazu, zoom/pan i overlayem RGB przy dużym powiększeniu. */
public class ImagePane {
    private final StackPane root = new StackPane();
    private final Canvas canvas = new Canvas(100, 100);

    private BufferedImage source;   // 8-bit RGB
    private WritableImage fxImage;

    private final DoubleProperty zoom = new SimpleDoubleProperty(1.0);
    private final DoubleProperty brightness = new SimpleDoubleProperty(1.0);

    private double offsetX = 0, offsetY = 0;
    private double dragStartX, dragStartY;
    private double startOffX, startOffY;

    public ImagePane() {
        root.getChildren().add(canvas);
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());

        // Zoom kółkiem myszy z Ctrl
        root.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {
                double factor = e.getDeltaY() > 0 ? 1.1 : 1/1.1;
                setZoom(clamp(zoom.get() * factor, 0.25, 64));
                e.consume();
            }
        });

        // Pan (przesuwanie) przy zoom > 1
        root.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (zoom.get() > 1.0) {
                dragStartX = e.getX(); dragStartY = e.getY();
                startOffX = offsetX;    startOffY = offsetY;
            }
        });
        root.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (zoom.get() > 1.0) {
                offsetX = startOffX + (e.getX() - dragStartX);
                offsetY = startOffY + (e.getY() - dragStartY);
                redraw();
            }
        });

        zoom.addListener((o, ov, nv) -> redraw());
        brightness.addListener((o, ov, nv) -> applyBrightness());
        canvas.widthProperty().addListener((o, ov, nv) -> redraw());
        canvas.heightProperty().addListener((o, ov, nv) -> redraw());
    }

    public Node getNode() { return root; }

    public void setImage(BufferedImage image) {
        this.source = ImageUtils.toRGB8(image);
        this.fxImage = ImageUtils.toWritable(this.source);
        this.offsetX = this.offsetY = 0;
        redraw();
    }

    public BufferedImage getImage() { return source; }

    public void setZoom(double z) { this.zoom.set(z); }
    public void resetZoom() { setZoom(1.0); offsetX = offsetY = 0; }
    public void setBrightness(double b) { this.brightness.set(b); }

    private void applyBrightness() {
        if (source == null) return;
        BufferedImage scaled = ImageUtils.scaleBrightness(source, brightness.get());
        fxImage = ImageUtils.toWritable(scaled);
        redraw();
    }

    private void redraw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.web("#1e1e1e"));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (fxImage == null) return;

        double iw = fxImage.getWidth();
        double ih = fxImage.getHeight();
        double zw = iw * zoom.get();
        double zh = ih * zoom.get();

        double cx = canvas.getWidth() / 2.0;
        double cy = canvas.getHeight() / 2.0;

        double x = cx - zw / 2.0 + offsetX;
        double y = cy - zh / 2.0 + offsetY;

        g.drawImage(fxImage, x, y, zw, zh);

        // Overlay: siatka + R,G,B przy dużym zoomie
        if (zoom.get() >= 8.0 && source != null) {
            int startX = (int) Math.floor((0 - (x)) / zoom.get());
            int startY = (int) Math.floor((0 - (y)) / zoom.get());
            int endX = (int) Math.ceil((canvas.getWidth() - x) / zoom.get());
            int endY = (int) Math.ceil((canvas.getHeight() - y) / zoom.get());

            startX = Math.max(0, startX); startY = Math.max(0, startY);
            endX = Math.min((int) iw, endX); endY = Math.min((int) ih, endY);

            g.setLineWidth(1);
            g.setStroke(Color.color(1, 1, 1, 0.25));
            g.setFill(Color.color(0, 0, 0, 0.6));
            for (int py = startY; py < endY; py++) {
                for (int px = startX; px < endX; px++) {
                    double rx = x + px * zoom.get();
                    double ry = y + py * zoom.get();
                    g.strokeRect(rx, ry, zoom.get(), zoom.get());

                    int rgb = source.getRGB(px, py);
                    int r = (rgb >> 16) & 0xFF;
                    int gr = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    g.fillText(r + "," + gr + "," + b, rx + 2, ry + 12);
                }
            }
        }
    }

    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
}
