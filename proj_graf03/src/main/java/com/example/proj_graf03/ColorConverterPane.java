package com.example.proj_graf03;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Panel obsługujący konwersję przestrzeni barw między RGB oraz CMYK w czasie rzeczywistym.
 */
public class ColorConverterPane extends BorderPane {

    private final Slider redSlider = createSlider(0, 255);
    private final Slider greenSlider = createSlider(0, 255);
    private final Slider blueSlider = createSlider(0, 255);

    private final TextField redField = createNumberField("0", 3);
    private final TextField greenField = createNumberField("0", 3);
    private final TextField blueField = createNumberField("0", 3);

    private final Slider cyanSlider = createSlider(0, 1);
    private final Slider magentaSlider = createSlider(0, 1);
    private final Slider yellowSlider = createSlider(0, 1);
    private final Slider blackSlider = createSlider(0, 1);

    private final TextField cyanField = createNumberField("0.0", 4);
    private final TextField magentaField = createNumberField("0.0", 4);
    private final TextField yellowField = createNumberField("0.0", 4);
    private final TextField blackField = createNumberField("0.0", 4);

    private final Rectangle preview = new Rectangle(260, 120);
    private final DecimalFormat cmykFormat = new DecimalFormat("0.00");

    private boolean updating = false;

    public ColorConverterPane() {
        setPadding(new Insets(20));
        setPrefWidth(500);

        VBox root = new VBox(16);
        root.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Konwersja przestrzeni barw");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        HBox modeBox = new HBox(12);
        modeBox.setAlignment(Pos.CENTER_LEFT);
        ToggleGroup toggleGroup = new ToggleGroup();
        RadioButton rgbInput = new RadioButton("Wprowadzam RGB");
        RadioButton cmykInput = new RadioButton("Wprowadzam CMYK");
        rgbInput.setToggleGroup(toggleGroup);
        cmykInput.setToggleGroup(toggleGroup);
        rgbInput.setSelected(true);
        modeBox.getChildren().addAll(new Label("Tryb wejściowy:"), rgbInput, cmykInput);

        GridPane rgbPane = buildRgbPane();
        GridPane cmykPane = buildCmykPane();

        HBox previewBox = new HBox(12);
        previewBox.setAlignment(Pos.CENTER_LEFT);
        preview.setStroke(Color.GRAY);
        previewBox.getChildren().addAll(new Label("Podgląd koloru:"), preview);

        root.getChildren().addAll(title, modeBox, rgbPane, cmykPane, previewBox);
        setCenter(root);

        setupListeners(rgbInput, cmykInput);
        updateFromRgb(0, 0, 0);
    }

    private GridPane buildRgbPane() {
        GridPane grid = createGrid();
        addRow(grid, 0, "R", redSlider, redField, 255);
        addRow(grid, 1, "G", greenSlider, greenField, 255);
        addRow(grid, 2, "B", blueSlider, blueField, 255);
        grid.setPadding(new Insets(10, 0, 6, 0));
        grid.setStyle("-fx-border-color: #d0d0d0; -fx-border-radius: 6; -fx-padding: 12;");
        Label title = new Label("RGB (0-255)");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        grid.add(title, 0, 0, 4, 1);
        return grid;
    }

    private GridPane buildCmykPane() {
        GridPane grid = createGrid();
        addRow(grid, 0, "C", cyanSlider, cyanField, 1);
        addRow(grid, 1, "M", magentaSlider, magentaField, 1);
        addRow(grid, 2, "Y", yellowSlider, yellowField, 1);
        addRow(grid, 3, "K", blackSlider, blackField, 1);
        grid.setPadding(new Insets(10, 0, 6, 0));
        grid.setStyle("-fx-border-color: #d0d0d0; -fx-border-radius: 6; -fx-padding: 12;");
        Label title = new Label("CMYK (0-1)");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        grid.add(title, 0, 0, 4, 1);
        return grid;
    }

    private void setupListeners(RadioButton rgbInput, RadioButton cmykInput) {
        redSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (rgbInput.isSelected()) {
                updateFromRgb(redSlider.getValue(), greenSlider.getValue(), blueSlider.getValue());
            }
        });
        greenSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (rgbInput.isSelected()) {
                updateFromRgb(redSlider.getValue(), greenSlider.getValue(), blueSlider.getValue());
            }
        });
        blueSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (rgbInput.isSelected()) {
                updateFromRgb(redSlider.getValue(), greenSlider.getValue(), blueSlider.getValue());
            }
        });

        cyanSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (cmykInput.isSelected()) {
                updateFromCmyk(cyanSlider.getValue(), magentaSlider.getValue(), yellowSlider.getValue(), blackSlider.getValue());
            }
        });
        magentaSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (cmykInput.isSelected()) {
                updateFromCmyk(cyanSlider.getValue(), magentaSlider.getValue(), yellowSlider.getValue(), blackSlider.getValue());
            }
        });
        yellowSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (cmykInput.isSelected()) {
                updateFromCmyk(cyanSlider.getValue(), magentaSlider.getValue(), yellowSlider.getValue(), blackSlider.getValue());
            }
        });
        blackSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (cmykInput.isSelected()) {
                updateFromCmyk(cyanSlider.getValue(), magentaSlider.getValue(), yellowSlider.getValue(), blackSlider.getValue());
            }
        });

        redField.setOnAction(e -> handleRgbFieldChange());
        greenField.setOnAction(e -> handleRgbFieldChange());
        blueField.setOnAction(e -> handleRgbFieldChange());

        cyanField.setOnAction(e -> handleCmykFieldChange());
        magentaField.setOnAction(e -> handleCmykFieldChange());
        yellowField.setOnAction(e -> handleCmykFieldChange());
        blackField.setOnAction(e -> handleCmykFieldChange());

        rgbInput.selectedProperty().addListener((obs, oldV, newV) -> {
            if (newV) {
                updateFromRgb(redSlider.getValue(), greenSlider.getValue(), blueSlider.getValue());
            }
        });
        cmykInput.selectedProperty().addListener((obs, oldV, newV) -> {
            if (newV) {
                updateFromCmyk(cyanSlider.getValue(), magentaSlider.getValue(), yellowSlider.getValue(), blackSlider.getValue());
            }
        });
    }

    private void handleRgbFieldChange() {
        try {
            double r = clamp(Double.parseDouble(redField.getText()), 0, 255);
            double g = clamp(Double.parseDouble(greenField.getText()), 0, 255);
            double b = clamp(Double.parseDouble(blueField.getText()), 0, 255);
            updateFromRgb(r, g, b);
        } catch (NumberFormatException ignored) {
        }
    }

    private void handleCmykFieldChange() {
        try {
            double c = clamp(parseLocaleNumber(cyanField.getText()), 0, 1);
            double m = clamp(parseLocaleNumber(magentaField.getText()), 0, 1);
            double y = clamp(parseLocaleNumber(yellowField.getText()), 0, 1);
            double k = clamp(parseLocaleNumber(blackField.getText()), 0, 1);
            updateFromCmyk(c, m, y, k);
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateFromRgb(double r, double g, double b) {
        if (updating) return;
        updating = true;

        redSlider.setValue(r);
        greenSlider.setValue(g);
        blueSlider.setValue(b);

        redField.setText(String.valueOf((int) Math.round(r)));
        greenField.setText(String.valueOf((int) Math.round(g)));
        blueField.setText(String.valueOf((int) Math.round(b)));

        double rNorm = r / 255.0;
        double gNorm = g / 255.0;
        double bNorm = b / 255.0;
        double k = 1 - Math.max(rNorm, Math.max(gNorm, bNorm));

        double c = (1 - rNorm - k) / (1 - k + 1e-6);
        double m = (1 - gNorm - k) / (1 - k + 1e-6);
        double y = (1 - bNorm - k) / (1 - k + 1e-6);

        updateCmykControls(c, m, y, k);
        preview.setFill(Color.rgb((int) r, (int) g, (int) b));
        updating = false;
    }

    private void updateFromCmyk(double c, double m, double y, double k) {
        if (updating) return;
        updating = true;
        updateCmykControls(c, m, y, k);

        double r = 255 * (1 - c) * (1 - k);
        double g = 255 * (1 - m) * (1 - k);
        double b = 255 * (1 - y) * (1 - k);

        redSlider.setValue(r);
        greenSlider.setValue(g);
        blueSlider.setValue(b);

        redField.setText(String.valueOf((int) Math.round(r)));
        greenField.setText(String.valueOf((int) Math.round(g)));
        blueField.setText(String.valueOf((int) Math.round(b)));

        preview.setFill(Color.rgb((int) r, (int) g, (int) b));
        updating = false;
    }

    private void updateCmykControls(double c, double m, double y, double k) {
        cyanSlider.setValue(c);
        magentaSlider.setValue(m);
        yellowSlider.setValue(y);
        blackSlider.setValue(k);

        cyanField.setText(cmykFormat.format(c));
        magentaField.setText(cmykFormat.format(m));
        yellowField.setText(cmykFormat.format(y));
        blackField.setText(cmykFormat.format(k));
    }

    private double parseLocaleNumber(String text) {
        String normalized = text.replace(',', '.');
        return Double.parseDouble(normalized);
    }

    private Slider createSlider(double min, double max) {
        Slider slider = new Slider(min, max, min);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setMajorTickUnit((max - min) / 4.0);
        slider.setBlockIncrement((max - min) / 100.0);
        slider.setSnapToTicks(false);
        return slider;
    }

    private TextField createNumberField(String initial, int width) {
        TextField field = new TextField(initial);
        field.setPrefColumnCount(width);
        return field;
    }

    private GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setPercentWidth(10);
        ColumnConstraints sliderCol = new ColumnConstraints();
        sliderCol.setPercentWidth(70);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setPercentWidth(20);
        grid.getColumnConstraints().addAll(labelCol, sliderCol, fieldCol);
        return grid;
    }

    private void addRow(GridPane grid, int row, String labelText, Slider slider, TextField field, double max) {
        Label label = new Label(labelText + ":");
        HBox sliderBox = new HBox(slider);
        HBox.setHgrow(slider, Priority.ALWAYS);
        Label maxLabel = new Label(String.format(Locale.US, " / %.0f", max));

        grid.add(label, 0, row + 1);
        grid.add(sliderBox, 1, row + 1);
        grid.add(field, 2, row + 1);
        grid.add(maxLabel, 3, row + 1);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
