package com.example.proj_graf03;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ColorConverterPane extends BorderPane {

    private final TextField hexField = new TextField("#808080");
    private final Slider rSlider = new Slider(0, 255, 128);
    private final Slider gSlider = new Slider(0, 255, 128);
    private final Slider bSlider = new Slider(0, 255, 128);

    private final TextField rField = new TextField("128");
    private final TextField gField = new TextField("128");
    private final TextField bField = new TextField("128");

    private final Slider cSlider = new Slider(0, 1, 0.5);
    private final Slider mSlider = new Slider(0, 1, 0.5);
    private final Slider ySlider = new Slider(0, 1, 0.5);
    private final Slider kSlider = new Slider(0, 1, 0.5);

    private final TextField cField = new TextField("0.5");
    private final TextField mField = new TextField("0.5");
    private final TextField yField = new TextField("0.5");
    private final TextField kField = new TextField("0.5");

    private final Rectangle previewRect = new Rectangle(150, 150);

    private boolean updating = false; // flaga, żeby nie robić pętli przy aktualizacjach

    public ColorConverterPane() {
        setPadding(new Insets(15));

        hexField.setEditable(false);
        hexField.setStyle("-fx-font-family: monospace");

        previewRect.setStroke(Color.GRAY);

        VBox rgbBox = buildRgbBox();
        VBox cmykBox = buildCmykBox();

        HBox center = new HBox(20, rgbBox, cmykBox, buildPreviewBox());
        center.setAlignment(Pos.TOP_CENTER);
        setCenter(center);

        initListeners();

        // ustawienie początkowe
        syncFromRGB();
    }

    private VBox buildRgbBox() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5; -fx-border-width: 1;");
        Label title = new Label("RGB");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);

        setupIntSlider(rSlider);
        setupIntSlider(gSlider);
        setupIntSlider(bSlider);

        grid.addRow(0, new Label("R:"), rSlider, rField);
        grid.addRow(1, new Label("G:"), gSlider, gField);
        grid.addRow(2, new Label("B:"), bSlider, bField);

        box.getChildren().addAll(title, grid);
        return box;
    }

    private VBox buildCmykBox() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5; -fx-border-width: 1;");
        Label title = new Label("CMYK");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);

        setupDoubleSlider(cSlider);
        setupDoubleSlider(mSlider);
        setupDoubleSlider(ySlider);
        setupDoubleSlider(kSlider);

        grid.addRow(0, new Label("C:"), cSlider, cField);
        grid.addRow(1, new Label("M:"), mSlider, mField);
        grid.addRow(2, new Label("Y:"), ySlider, yField);
        grid.addRow(3, new Label("K:"), kSlider, kField);

        box.getChildren().addAll(title, grid);
        return box;
    }

    private VBox buildPreviewBox() {
        Label title = new Label("Podgląd koloru");
        Label hexLabel = new Label("HEX:");

        HBox hexBox = new HBox(5, hexLabel, hexField);

        VBox box = new VBox(10, title, previewRect, hexBox);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(10));
        return box;
    }

    private void setupIntSlider(Slider s) {
        s.setShowTickLabels(true);
        s.setShowTickMarks(true);
        s.setMajorTickUnit(64);
        s.setMinorTickCount(4);
        s.setBlockIncrement(1);
    }

    private void setupDoubleSlider(Slider s) {
        s.setShowTickLabels(true);
        s.setShowTickMarks(true);
        s.setMajorTickUnit(0.25);
        s.setMinorTickCount(4);
        s.setBlockIncrement(0.01);
    }

    private void initListeners() {
        // RGB -> aktualizacja CMYK
        rSlider.valueProperty().addListener((obs, o, n) -> onRgbChangedFromSliders());
        gSlider.valueProperty().addListener((obs, o, n) -> onRgbChangedFromSliders());
        bSlider.valueProperty().addListener((obs, o, n) -> onRgbChangedFromSliders());

        rField.setOnAction(e -> onRgbChangedFromFields());
        gField.setOnAction(e -> onRgbChangedFromFields());
        bField.setOnAction(e -> onRgbChangedFromFields());

        rField.focusedProperty().addListener((obs, o, n) -> { if (!n) onRgbChangedFromFields(); });
        gField.focusedProperty().addListener((obs, o, n) -> { if (!n) onRgbChangedFromFields(); });
        bField.focusedProperty().addListener((obs, o, n) -> { if (!n) onRgbChangedFromFields(); });

        // CMYK -> aktualizacja RGB
        cSlider.valueProperty().addListener((obs, o, n) -> onCmykChangedFromSliders());
        mSlider.valueProperty().addListener((obs, o, n) -> onCmykChangedFromSliders());
        ySlider.valueProperty().addListener((obs, o, n) -> onCmykChangedFromSliders());
        kSlider.valueProperty().addListener((obs, o, n) -> onCmykChangedFromSliders());

        cField.setOnAction(e -> onCmykChangedFromFields());
        mField.setOnAction(e -> onCmykChangedFromFields());
        yField.setOnAction(e -> onCmykChangedFromFields());
        kField.setOnAction(e -> onCmykChangedFromFields());

        cField.focusedProperty().addListener((obs, o, n) -> { if (!n) onCmykChangedFromFields(); });
        mField.focusedProperty().addListener((obs, o, n) -> { if (!n) onCmykChangedFromFields(); });
        yField.focusedProperty().addListener((obs, o, n) -> { if (!n) onCmykChangedFromFields(); });
        kField.focusedProperty().addListener((obs, o, n) -> { if (!n) onCmykChangedFromFields(); });
    }

    private void onRgbChangedFromSliders() {
        if (updating) return;
        updating = true;
        int r = (int) Math.round(rSlider.getValue());
        int g = (int) Math.round(gSlider.getValue());
        int b = (int) Math.round(bSlider.getValue());

        rField.setText(String.valueOf(r));
        gField.setText(String.valueOf(g));
        bField.setText(String.valueOf(b));

        double[] cmyk = ColorUtils.rgbToCmyk(r, g, b);
        updateCmykControls(cmyk[0], cmyk[1], cmyk[2], cmyk[3]);

        updatePreview(r, g, b);
        updating = false;
    }

    private void onRgbChangedFromFields() {
        if (updating) return;
        updating = true;
        int r = parseInt(rField.getText(), 0, 255, (int) rSlider.getValue());
        int g = parseInt(gField.getText(), 0, 255, (int) gSlider.getValue());
        int b = parseInt(bField.getText(), 0, 255, (int) bSlider.getValue());

        rSlider.setValue(r);
        gSlider.setValue(g);
        bSlider.setValue(b);

        double[] cmyk = ColorUtils.rgbToCmyk(r, g, b);
        updateCmykControls(cmyk[0], cmyk[1], cmyk[2], cmyk[3]);

        updatePreview(r, g, b);
        updating = false;
    }

    private void onCmykChangedFromSliders() {
        if (updating) return;
        updating = true;

        double c = cSlider.getValue();
        double m = mSlider.getValue();
        double y = ySlider.getValue();
        double k = kSlider.getValue();

        cField.setText(String.format("%.3f", c));
        mField.setText(String.format("%.3f", m));
        yField.setText(String.format("%.3f", y));
        kField.setText(String.format("%.3f", k));

        int[] rgb = ColorUtils.cmykToRgb(c, m, y, k);
        updateRgbControls(rgb[0], rgb[1], rgb[2]);
        updatePreview(rgb[0], rgb[1], rgb[2]);

        updating = false;
    }

    private void onCmykChangedFromFields() {
        if (updating) return;
        updating = true;

        double c = parseDouble(cField.getText(), 0, 1, cSlider.getValue());
        double m = parseDouble(mField.getText(), 0, 1, mSlider.getValue());
        double y = parseDouble(yField.getText(), 0, 1, ySlider.getValue());
        double k = parseDouble(kField.getText(), 0, 1, kSlider.getValue());

        cSlider.setValue(c);
        mSlider.setValue(m);
        ySlider.setValue(y);
        kSlider.setValue(k);

        int[] rgb = ColorUtils.cmykToRgb(c, m, y, k);
        updateRgbControls(rgb[0], rgb[1], rgb[2]);
        updatePreview(rgb[0], rgb[1], rgb[2]);

        updating = false;
    }

    private void updateCmykControls(double c, double m, double y, double k) {
        c = ColorUtils.clamp01(c);
        m = ColorUtils.clamp01(m);
        y = ColorUtils.clamp01(y);
        k = ColorUtils.clamp01(k);

        cSlider.setValue(c);
        mSlider.setValue(m);
        ySlider.setValue(y);
        kSlider.setValue(k);

        cField.setText(String.format("%.3f", c));
        mField.setText(String.format("%.3f", m));
        yField.setText(String.format("%.3f", y));
        kField.setText(String.format("%.3f", k));
    }

    private void updateRgbControls(int r, int g, int b) {
        rSlider.setValue(r);
        gSlider.setValue(g);
        bSlider.setValue(b);

        rField.setText(String.valueOf(r));
        gField.setText(String.valueOf(g));
        bField.setText(String.valueOf(b));
    }

    private void updatePreview(int r, int g, int b) {
        previewRect.setFill(Color.rgb(r, g, b));
        String hex = String.format("#%02X%02X%02X", r, g, b);
        hexField.setText(hex);
    }

    private int parseInt(String text, int min, int max, int fallback) {
        try {
            int v = Integer.parseInt(text.trim());
            if (v < min || v > max) return fallback;
            return v;
        } catch (Exception e) {
            return fallback;
        }
    }

    private double parseDouble(String text, double min, double max, double fallback) {
        try {
            double v = Double.parseDouble(text.trim().replace(",", "."));
            if (v < min || v > max) return fallback;
            return v;
        } catch (Exception e) {
            return fallback;
        }
    }

    private void syncFromRGB() {
        onRgbChangedFromSliders();
    }
}
