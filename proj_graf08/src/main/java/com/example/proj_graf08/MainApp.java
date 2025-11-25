package com.example.proj_graf08;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class MainApp extends Application {

    private Image originalImage;

    // binarizacja z oryginału wg progu
    private boolean[][] baseBinary;

    // ostatni wynik operacji
    private boolean[][] lastResult;

    // SE ternarny: -1 (tło), 0 (ignoruj), 1 (obiekt)
    private int[][] se = new int[3][3];
    private ToggleButton[][] seButtons;

    // ===== UI =====
    private final ImageView ivOriginal = new ImageView();
    private final ImageView ivMain = new ImageView();

    private final Label statusLabel = new Label("Status: brak obrazu");
    private final Slider thresholdSlider = new Slider(0, 255, 128);
    private final Label thresholdValue = new Label("128");

    private final Spinner<Integer> seRowsSpinner = new Spinner<>(1, 51, 3);
    private final Spinner<Integer> seColsSpinner = new Spinner<>(1, 51, 3);
    private final GridPane seGrid = new GridPane();

    private final CheckBox useResultAsInput = new CheckBox("Użyj wyniku jako wejście");

    @Override
    public void start(Stage stage) {
        stage.setTitle("Morfologia (JavaFX) – Oryginał + Wynik");

        // ===== TOP BAR =====
        Button btnLoad = new Button("Wczytaj obraz");
        Button btnApplyThreshold = new Button("Zastosuj próg");
        Button btnReset = new Button("Reset (usuń wynik)");

        btnLoad.setOnAction(e -> loadImage(stage));
        btnApplyThreshold.setOnAction(e -> applyThresholdAndReset());
        btnReset.setOnAction(e -> resetResult());

        thresholdSlider.setShowTickLabels(true);
        thresholdSlider.setShowTickMarks(true);
        thresholdSlider.setMajorTickUnit(64);
        thresholdSlider.valueProperty().addListener((obs, o, n) -> thresholdValue.setText(String.valueOf(n.intValue())));

        useResultAsInput.setSelected(true);

        ToolBar topBar = new ToolBar(
                btnLoad,
                btnApplyThreshold,
                btnReset,
                new Separator(),
                new Label("Próg:"),
                thresholdSlider,
                thresholdValue,
                new Separator(),
                useResultAsInput
        );

        // ===== CENTER: SplitPane (ORYGINAŁ | PODGLĄD BINARNY/WYNIK) =====
        ivOriginal.setPreserveRatio(true);
        ivMain.setPreserveRatio(true);

        StackPane frameOriginal = new StackPane(ivOriginal);
        frameOriginal.setPadding(new Insets(10));
        frameOriginal.setStyle("""
                -fx-background-color: white;
                -fx-border-color: rgba(0,0,0,0.15);
                -fx-border-radius: 14;
                -fx-background-radius: 14;
                """);

        StackPane frameMain = new StackPane(ivMain);
        frameMain.setPadding(new Insets(10));
        frameMain.setStyle("""
                -fx-background-color: white;
                -fx-border-color: rgba(0,0,0,0.15);
                -fx-border-radius: 14;
                -fx-background-radius: 14;
                """);

        ScrollPane spOriginal = new ScrollPane(frameOriginal);
        spOriginal.setFitToWidth(true);
        spOriginal.setFitToHeight(true);
        spOriginal.setPannable(true);

        ScrollPane spMain = new ScrollPane(frameMain);
        spMain.setFitToWidth(true);
        spMain.setFitToHeight(true);
        spMain.setPannable(true);

        // skalowanie obrazów w obrębie scrollpane
        ivOriginal.fitWidthProperty().bind(spOriginal.widthProperty().subtract(40));
        ivOriginal.fitHeightProperty().bind(spOriginal.heightProperty().subtract(60));

        ivMain.fitWidthProperty().bind(spMain.widthProperty().subtract(40));
        ivMain.fitHeightProperty().bind(spMain.heightProperty().subtract(60));

        VBox leftBox = new VBox(8, new Label("Oryginał"), spOriginal);
        leftBox.setPadding(new Insets(12));
        VBox.setVgrow(spOriginal, Priority.ALWAYS);

        VBox rightBox = new VBox(8, statusLabel, spMain);
        rightBox.setPadding(new Insets(12));
        VBox.setVgrow(spMain, Priority.ALWAYS);

        SplitPane split = new SplitPane(leftBox, rightBox);
        split.setDividerPositions(0.5);

        // ===== RIGHT PANEL (SE + OPERACJE) =====
        seRowsSpinner.setEditable(true);
        seColsSpinner.setEditable(true);

        ChangeListener<Integer> seSizeListener = (obs, oldVal, newVal) -> rebuildSEGrid();
        seRowsSpinner.valueProperty().addListener(seSizeListener);
        seColsSpinner.valueProperty().addListener(seSizeListener);

        seGrid.setHgap(2);
        seGrid.setVgap(2);
        seGrid.setPadding(new Insets(8));
        seGrid.setStyle("-fx-background-color: rgba(0,0,0,0.06); -fx-background-radius: 12;");

        Label seHint = new Label("Klikaj komórki SE: -1 → 0 → 1\n( -1 ma sens głównie w Hit-or-miss )");
        seHint.setWrapText(true);

        Button btnClearSE = new Button("Wyczyść (0)");
        Button btnFill1 = new Button("Wypełnij (1)");
        Button btnCross = new Button("Krzyż (1)");
        Button btnCenter1 = new Button("Tylko środek (1)");

        btnClearSE.setOnAction(e -> fillSE(0));
        btnFill1.setOnAction(e -> fillSE(1));
        btnCross.setOnAction(e -> makeCross());
        btnCenter1.setOnAction(e -> makeCenterOnly());

        Button btnDilate = new Button("Dylatacja");
        Button btnErode = new Button("Erozja");
        Button btnOpen = new Button("Otwarcie");
        Button btnClose = new Button("Domknięcie");
        Button btnHmt = new Button("Hit-or-miss");

        for (Button b : new Button[]{btnDilate, btnErode, btnOpen, btnClose, btnHmt}) {
            b.setMaxWidth(Double.MAX_VALUE);
        }

        btnDilate.setOnAction(e -> applyOp("dilate"));
        btnErode.setOnAction(e -> applyOp("erode"));
        btnOpen.setOnAction(e -> applyOp("open"));
        btnClose.setOnAction(e -> applyOp("close"));
        btnHmt.setOnAction(e -> applyOp("hmt"));

        VBox rightPanel = new VBox(10,
                new Label("Element strukturyzujący (SE)"),
                new HBox(8, new Label("Wiersze:"), seRowsSpinner, new Label("Kolumny:"), seColsSpinner),
                seHint,
                seGrid,
                new HBox(8, btnClearSE, btnFill1),
                new HBox(8, btnCross, btnCenter1),
                new Separator(),
                new Label("Operacje morfologiczne"),
                btnDilate, btnErode, btnOpen, btnClose, btnHmt
        );
        rightPanel.setPadding(new Insets(12));
        rightPanel.setFillWidth(true);

        ScrollPane rightScroll = new ScrollPane(rightPanel);
        rightScroll.setFitToWidth(true);
        rightScroll.setPannable(true);
        rightScroll.setPrefViewportWidth(360);

        // ===== ROOT =====
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(split);
        root.setRight(rightScroll);

        rebuildSEGrid(); // startowy SE 3x3
        refreshViews();

        Scene scene = new Scene(root, 980, 620);
        stage.setScene(scene);
        stage.show();
    }

    // ===================== LOGIKA UI =====================

    private void loadImage(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Wybierz obraz");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Obrazy", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"),
                new FileChooser.ExtensionFilter("Wszystkie pliki", "*.*")
        );

        File f = fc.showOpenDialog(stage);
        if (f == null) return;

        originalImage = new Image(f.toURI().toString());
        applyThresholdAndReset();
    }

    private void applyThresholdAndReset() {
        if (originalImage == null) {
            alert("Najpierw wczytaj obraz.");
            return;
        }
        int thr = (int) Math.round(thresholdSlider.getValue());
        baseBinary = Morphology.thresholdToBinary(originalImage, thr);
        lastResult = null;
        refreshViews();
    }

    private void resetResult() {
        if (originalImage == null || baseBinary == null) return;
        lastResult = null;
        refreshViews();
    }

    private boolean[][] currentInput() {
        if (baseBinary == null) return null;
        if (useResultAsInput.isSelected() && lastResult != null) return lastResult;
        return baseBinary;
    }

    private void applyOp(String op) {
        if (originalImage == null || baseBinary == null) {
            alert("Najpierw wczytaj obraz i wykonaj binarizację (Zastosuj próg).");
            return;
        }

        boolean[][] in = currentInput();
        boolean[][] out;

        switch (op) {
            case "dilate" -> out = Morphology.dilate(in, se);
            case "erode" -> out = Morphology.erode(in, se);
            case "open" -> out = Morphology.open(in, se);
            case "close" -> out = Morphology.close(in, se);
            case "hmt" -> out = Morphology.hitOrMiss(in, se);
            default -> throw new IllegalArgumentException("Nieznana operacja: " + op);
        }

        lastResult = out;
        refreshViews();
    }

    private void refreshViews() {
        // lewy zawsze: oryginał
        if (originalImage != null) {
            ivOriginal.setImage(originalImage);
        } else {
            ivOriginal.setImage(null);
        }

        // prawy: jeśli jest wynik -> wynik, jeśli nie -> binarny po progu, jeśli nie ma progu -> pusty
        if (originalImage == null) {
            ivMain.setImage(null);
            statusLabel.setText("Status: brak obrazu");
            return;
        }

        if (baseBinary == null) {
            ivMain.setImage(null);
            statusLabel.setText("Status: brak binarizacji (kliknij: Zastosuj próg)");
            return;
        }

        if (lastResult != null) {
            ivMain.setImage(Morphology.binaryToImage(lastResult));
            statusLabel.setText("Status: WYNIK operacji (binarny)");
        } else {
            ivMain.setImage(Morphology.binaryToImage(baseBinary));
            statusLabel.setText("Status: BINARNY po progu (brak wyniku)");
        }
    }

    // ===================== SE GRID =====================

    private void rebuildSEGrid() {
        int rows = seRowsSpinner.getValue();
        int cols = seColsSpinner.getValue();

        se = new int[rows][cols]; // domyślnie 0
        seButtons = new ToggleButton[rows][cols];

        seGrid.getChildren().clear();
        seGrid.getColumnConstraints().clear();
        seGrid.getRowConstraints().clear();

        for (int c = 0; c < cols; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPrefWidth(28);
            seGrid.getColumnConstraints().add(cc);
        }
        for (int r = 0; r < rows; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setPrefHeight(28);
            seGrid.getRowConstraints().add(rc);
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                ToggleButton b = new ToggleButton("0");
                b.setMinSize(28, 28);
                b.setMaxSize(28, 28);

                int rr = r, cc = c;
                b.setOnAction(e -> {
                    int v = se[rr][cc];
                    v = (v == -1) ? 0 : (v == 0) ? 1 : -1;
                    se[rr][cc] = v;
                    paintSEButton(b, v);
                });

                seButtons[r][c] = b;
                paintSEButton(b, 0);
                seGrid.add(b, c, r);
            }
        }

        // sensowny start: środek = 1
        int cr = rows / 2, cc = cols / 2;
        se[cr][cc] = 1;
        paintSEButton(seButtons[cr][cc], 1);
    }

    private void fillSE(int value) {
        for (int r = 0; r < se.length; r++) {
            for (int c = 0; c < se[0].length; c++) {
                se[r][c] = value;
                paintSEButton(seButtons[r][c], value);
            }
        }
    }

    private void makeCenterOnly() {
        fillSE(0);
        int cr = se.length / 2;
        int cc = se[0].length / 2;
        se[cr][cc] = 1;
        paintSEButton(seButtons[cr][cc], 1);
    }

    private void makeCross() {
        fillSE(0);
        int cr = se.length / 2;
        int cc = se[0].length / 2;

        for (int r = 0; r < se.length; r++) {
            se[r][cc] = 1;
            paintSEButton(seButtons[r][cc], 1);
        }
        for (int c = 0; c < se[0].length; c++) {
            se[cr][c] = 1;
            paintSEButton(seButtons[cr][c], 1);
        }
    }

    private void paintSEButton(ToggleButton b, int v) {
        b.setText(String.valueOf(v));
        String style = switch (v) {
            case -1 -> "-fx-background-color: rgba(255,80,80,0.55); -fx-font-weight: bold;";
            case  0 -> "-fx-background-color: rgba(180,180,180,0.45);";
            case  1 -> "-fx-background-color: rgba(80,220,130,0.55); -fx-font-weight: bold;";
            default -> "";
        };
        b.setStyle(style + " -fx-background-radius: 7;");
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
