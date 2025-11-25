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
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// Główny panel z kostką RGB – zawiera Canvas (gdzie rysujemy)
// oraz dolny panel sterowania (przekrój X/Y/Z, suwak).
public class RgbCubePane extends BorderPane {

    public RgbCubePane() {
        int voxelSteps = 50; // ile "kroków" na osi – 50^3 punktów w kostce

        // Płótno, na którym rysujemy 3D kostkę
        CubeCanvas cubeCanvas = new CubeCanvas(voxelSteps, 800, 600);

        // Dolny panel z kontrolkami (label + radio + slider)
        HBox controlPanel = new HBox(10);
        controlPanel.setPadding(new Insets(5, 10, 5, 10));

        Label label = new Label("Przekrój:");

        // Radio buttony do wyboru osi przekroju
        RadioButton rbNone = new RadioButton("Brak");
        RadioButton rbX = new RadioButton("X");
        RadioButton rbY = new RadioButton("Y");
        RadioButton rbZ = new RadioButton("Z");

        // Grupa przycisków, żeby tylko jeden był zaznaczony
        ToggleGroup axisGroup = new ToggleGroup();
        rbNone.setToggleGroup(axisGroup);
        rbX.setToggleGroup(axisGroup);
        rbY.setToggleGroup(axisGroup);
        rbZ.setToggleGroup(axisGroup);
        rbNone.setSelected(true); // domyślnie: bez przekroju

        // Po kliknięciu zmieniamy oś przekroju w CubeCanvas
        rbNone.setOnAction(e -> cubeCanvas.setSliceAxis('N'));
        rbX.setOnAction(e -> cubeCanvas.setSliceAxis('X'));
        rbY.setOnAction(e -> cubeCanvas.setSliceAxis('Y'));
        rbZ.setOnAction(e -> cubeCanvas.setSliceAxis('Z'));

        // Suwak określający "głębokość" przekroju – indeks od 0 do voxelSteps-1
        Slider sliceSlider = new Slider(0, voxelSteps - 1, voxelSteps - 1);
        sliceSlider.setPrefWidth(200);
        sliceSlider.valueProperty().addListener((obs, oldV, newV) ->
                cubeCanvas.setSliceValue(newV.intValue())
        );

        // Dodajemy kontrolki do dolnego panelu
        controlPanel.getChildren().addAll(label, rbNone, rbX, rbY, rbZ, sliceSlider);

        // Centrum – czarny background z nałożonym Canvasem
        StackPane center = new StackPane(cubeCanvas);
        center.setStyle("-fx-background-color: black;");

        setCenter(center);
        setBottom(controlPanel);
    }

    // ------------------ to jest odpowiednik CubePanel ------------------

    // Wewnętrzna klasa odpowiedzialna za rysowanie kostki na Canvasie.
    private static class CubeCanvas extends Canvas {

        // Lista wszystkich punktów (voxel'i) w kostce
        private final List<Point3D> voxels = new ArrayList<>();
        private final int voxelSteps; // ile kroków na osi (np. 50)

        // Kąty obrotu kostki w 3D (w radianach)
        private double rotX = 0.4; // obrót góra/dół (oś X)
        private double rotY = 0.6; // obrót lewo/prawo (oś Y)

        // Ostatnia pozycja myszy – do obliczenia przesunięcia
        private double lastMouseX, lastMouseY;

        // Wybrana oś przekroju: N (none), X, Y, Z
        private char sliceAxis = 'N';
        // Wartość przekroju – indeks "kroku" wzdłuż osi (0..voxelSteps-1)
        private int sliceValue;

        public CubeCanvas(int steps, double width, double height) {
            super(width, height);
            this.voxelSteps = steps;
            this.sliceValue = steps - 1; // domyślnie pokazujemy całą kostkę

            // Tworzymy punkty (x,y,z) w przestrzeni [0..1]^3
            initializeVoxels();

            // Podpinamy obsługę myszy (obracanie)
            initMouseHandlers();

            // Pierwsze narysowanie kostki
            drawCube();

            // Przerysowujemy kostkę przy zmianie rozmiaru Canvasu
            widthProperty().addListener((o, ov, nv) -> drawCube());
            heightProperty().addListener((o, ov, nv) -> drawCube());
        }

        // Tworzy wszystkie voxel'e – równomierna siatka 3D w [0..1]^3
        private void initializeVoxels() {
            voxels.clear();
            for (int i = 0; i < voxelSteps; i++) {
                for (int j = 0; j < voxelSteps; j++) {
                    for (int k = 0; k < voxelSteps; k++) {
                        // Normalizacja indeksu (0..steps-1) na (0..1)
                        double x = (double) i / (voxelSteps - 1);
                        double y = (double) j / (voxelSteps - 1);
                        double z = (double) k / (voxelSteps - 1);
                        voxels.add(new Point3D(x, y, z));
                    }
                }
            }
        }

        // Ustawia wybraną oś przekroju i od razu przerysowuje
        public void setSliceAxis(char axis) {
            this.sliceAxis = axis;
            drawCube();
        }

        // Ustawia aktualną "głębokość" przekroju (indeks) i przerysowuje
        public void setSliceValue(int value) {
            this.sliceValue = value;
            drawCube();
        }

        // Obsługa myszy – zapamiętujemy gdzie kliknięto, a przy przeciąganiu
        // zmieniamy kąty rotacji i rysujemy kostkę na nowo.
        private void initMouseHandlers() {
            addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            });

            addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
                double dx = e.getX() - lastMouseX;
                double dy = e.getY() - lastMouseY;

                // Skalujemy ruch myszy do małych zmian kątów
                rotY += dx * 0.01;
                rotX += dy * 0.01;

                lastMouseX = e.getX();
                lastMouseY = e.getY();

                drawCube();
            });
        }

        // Główna metoda rysująca kostkę RGB na Canvasie.
        private void drawCube() {
            GraphicsContext g2d = getGraphicsContext2D();

            double w = getWidth();
            double h = getHeight();

            // Wypełniamy tło na czarno
            g2d.setFill(Color.BLACK);
            g2d.fillRect(0, 0, w, h);

            // Środek ekranu – tu będzie środek kostki
            double centerX = w / 2.0;
            double centerY = h / 2.0;

            // Skalowanie, żeby kostka mieściła się w oknie
            double scale = Math.min(w, h) * 0.6;

            // Rozmiar pojedynczego "kwadracika" (rzutu voxela)
            int voxelSize = (int) Math.max(2, (scale / voxelSteps * 0.8));

            // Preliczone cos/sin dla aktualnych kątów obrotu
            double cosX = Math.cos(rotX);
            double sinX = Math.sin(rotX);
            double cosY = Math.cos(rotY);
            double sinY = Math.sin(rotY);

            // Lista voxel'i gotowych do rysowania po projekcji 3D->2D
            List<RenderableVoxel> renderList = new ArrayList<>();

            // Przechodzimy po wszystkich punktach z modelu 3D
            for (Point3D p : voxels) {

                // Przeliczenie [0..1] na indeks 0..voxelSteps-1
                int px = (int) Math.round(p.x * (voxelSteps - 1));
                int py = (int) Math.round(p.y * (voxelSteps - 1));
                int pz = (int) Math.round(p.z * (voxelSteps - 1));

                // Filtrowanie według przekroju:
                // jeśli oś X – przepuszczamy tylko te o px <= sliceValue itd.
                if (sliceAxis == 'X' && px > sliceValue) continue;
                if (sliceAxis == 'Y' && py > sliceValue) continue;
                if (sliceAxis == 'Z' && pz > sliceValue) continue;

                // Przesuwamy kostkę tak, by jej środek był w (0,0,0)
                double x = p.x - 0.5;
                double y = p.y - 0.5;
                double z = p.z - 0.5;

                // Obrót wokół osi X
                double y_rot = y * cosX - z * sinX;
                double z_rot = y * sinX + z * cosX;

                // Obrót wokół osi Y (używamy już z_rot)
                double x_rot = x * cosY - z_rot * sinY;
                double z_final = x * sinY + z_rot * cosY;

                // Rzutowanie 3D → 2D: mnożymy przez skalę i przesuwamy na środek ekranu
                double sx = x_rot * scale + centerX;
                double sy = y_rot * scale + centerY;

                // Kolor voxela – bezpośrednio z wartości x,y,z jako R,G,B
                Color color = new Color(p.x, p.y, p.z, 1.0);

                // Dodajemy do listy z informacją o pozycji na ekranie i "głębokości" z
                renderList.add(new RenderableVoxel(sx, sy, z_final, color));
            }

            // Sortujemy voxel'e po z (głębokości), żeby te z tyłu rysować jako pierwsze
            Collections.sort(renderList, Comparator.comparingDouble(v -> v.z));

            // Rysujemy każdy voxel jako mały kwadrat
            for (RenderableVoxel v : renderList) {
                g2d.setFill(v.color);
                g2d.fillRect(
                        v.sx - voxelSize / 2.0,
                        v.sy - voxelSize / 2.0,
                        voxelSize,
                        voxelSize
                );
            }
        }
    }

    // Prosta klasa reprezentująca punkt w przestrzeni 3D (model kostki)
    private static class Point3D {
        final double x, y, z;
        Point3D(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    // Klasa pomocnicza do rysowania – punkt już po projekcji na 2D
    // z zapamiętanym kolorem i "głębokością" z (do sortowania).
    private static class RenderableVoxel {
        final double sx, sy; // pozycja na ekranie 2D
        final double z;      // głębokość po obrocie (do sortowania)
        final Color color;   // kolor voxela

        RenderableVoxel(double sx, double sy, double z, Color color) {
            this.sx = sx;
            this.sy = sy;
            this.z = z;
            this.color = color;
        }
    }
}
