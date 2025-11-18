package com.example.proj_graf03;

import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * JavaFX wersja kostki RGB z przykładu Swing.
 * Rysuje gęstą siatkę voxeli w 3D, umożliwia obrót myszą oraz podgląd przekroju X/Y/Z.
 */
public class RgbCubePane extends BorderPane {

    private final int voxelSteps = 40;
    private final List<Point3D> voxels = new ArrayList<>();

    private final Canvas canvas = new Canvas(800, 650);
    private final Slider sliceSlider = new Slider(0, voxelSteps - 1, voxelSteps - 1);

    private char sliceAxis = 'N';
    private double rotX = 0.4;
    private double rotY = 0.6;
    private double lastMouseX;
    private double lastMouseY;

    public RgbCubePane() {
        setPadding(new Insets(16));
        initializeVoxels();

        StackPane canvasHolder = new StackPane(canvas);
        canvasHolder.setStyle("-fx-background-color: black; -fx-border-color: #444;");
        canvas.widthProperty().bind(canvasHolder.widthProperty());
        canvas.heightProperty().bind(canvasHolder.heightProperty());
        canvasHolder.widthProperty().addListener((obs, o, n) -> render());
        canvasHolder.heightProperty().addListener((obs, o, n) -> render());

        setCenter(canvasHolder);
        setBottom(buildControls());

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onMousePressed);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged);

        render();
    }

    private void initializeVoxels() {
        voxels.clear();
        for (int i = 0; i < voxelSteps; i++) {
            for (int j = 0; j < voxelSteps; j++) {
                for (int k = 0; k < voxelSteps; k++) {
                    double x = i / (double) (voxelSteps - 1);
                    double y = j / (double) (voxelSteps - 1);
                    double z = k / (double) (voxelSteps - 1);
                    voxels.add(new Point3D(x, y, z));
                }
            }
        }
    }

    private HBox buildControls() {
        Label label = new Label("Przekrój:");

        RadioButton none = new RadioButton("Brak");
        RadioButton axisX = new RadioButton("X");
        RadioButton axisY = new RadioButton("Y");
        RadioButton axisZ = new RadioButton("Z");
        none.setSelected(true);

        ToggleGroup group = new ToggleGroup();
        none.setToggleGroup(group);
        axisX.setToggleGroup(group);
        axisY.setToggleGroup(group);
        axisZ.setToggleGroup(group);

        sliceSlider.setPrefWidth(220);
        sliceSlider.setDisable(true);
        sliceSlider.valueProperty().addListener((obs, oldV, newV) -> {
            render();
        });

        group.selectedToggleProperty().addListener((obs, old, toggle) -> {
            if (toggle == axisX) {
                sliceAxis = 'X';
            } else if (toggle == axisY) {
                sliceAxis = 'Y';
            } else if (toggle == axisZ) {
                sliceAxis = 'Z';
            } else {
                sliceAxis = 'N';
            }
            sliceSlider.setDisable(sliceAxis == 'N');
            render();
        });

        HBox box = new HBox(12, label, none, axisX, axisY, axisZ, sliceSlider);
        box.setPadding(new Insets(10, 0, 0, 0));
        HBox.setHgrow(sliceSlider, Priority.ALWAYS);
        return box;
    }

    private void onMousePressed(MouseEvent event) {
        lastMouseX = event.getX();
        lastMouseY = event.getY();
    }

    private void onMouseDragged(MouseEvent event) {
        double dx = event.getX() - lastMouseX;
        double dy = event.getY() - lastMouseY;
        rotY += dx * 0.01;
        rotX += dy * 0.01;
        lastMouseX = event.getX();
        lastMouseY = event.getY();
        render();
    }

    private void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();

        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, width, height);

        double centerX = width / 2.0;
        double centerY = height / 2.0;
        double scale = Math.min(width, height) * 0.65;
        int voxelSize = Math.max(2, (int) (scale / voxelSteps * 0.8));

        double cosX = Math.cos(rotX);
        double sinX = Math.sin(rotX);
        double cosY = Math.cos(rotY);
        double sinY = Math.sin(rotY);

        List<RenderableVoxel> renderList = new ArrayList<>();
        int sliceValue = (int) Math.round(sliceSlider.getValue());

        for (Point3D p : voxels) {
            int px = (int) Math.round(p.x * (voxelSteps - 1));
            int py = (int) Math.round(p.y * (voxelSteps - 1));
            int pz = (int) Math.round(p.z * (voxelSteps - 1));

            if (sliceAxis == 'X' && px > sliceValue) {
                continue;
            }
            if (sliceAxis == 'Y' && py > sliceValue) {
                continue;
            }
            if (sliceAxis == 'Z' && pz > sliceValue) {
                continue;
            }

            double x = p.x - 0.5;
            double y = p.y - 0.5;
            double z = p.z - 0.5;

            double yRot = y * cosX - z * sinX;
            double zRot = y * sinX + z * cosX;

            double xRot = x * cosY - zRot * sinY;
            double zFinal = x * sinY + zRot * cosY;

            double sx = xRot * scale + centerX;
            double sy = yRot * scale + centerY;

            Color color = Color.color(p.x, p.y, p.z);
            renderList.add(new RenderableVoxel(sx, sy, zFinal, color));
        }

        renderList.sort(Comparator.comparingDouble(v -> v.depth));

        for (RenderableVoxel v : renderList) {
            gc.setFill(v.color);
            gc.fillRect(v.sx - voxelSize / 2.0, v.sy - voxelSize / 2.0, voxelSize, voxelSize);
        }

        gc.setStroke(Color.DARKGRAY);
        gc.strokeRect(0.5, 0.5, width - 1, height - 1);
    }

    private record Point3D(double x, double y, double z) {
    }

    private record RenderableVoxel(double sx, double sy, double depth, Color color) {
    }
}
