package com.example.proj_graf05;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Locale;

public class ImageProcessingApp extends Application {

    // --- Dane obrazu (logika) ---
    private Image loadedColorImage;      // oryginalny obraz kolorowy
    private GrayscaleImage originalGray;
    private GrayscaleImage stretchedGray;
    private GrayscaleImage equalizedGray;
    private GrayscaleImage binaryGray;

    private GrayscaleImage currentGray;  // obraz w skali szarości, z którego liczony jest histogram
    private int[] currentHistogram;

    // --- UI ---
    private Stage primary;
    private Label loadedFileLabel;

    private ImageView mainImageView;      // obraz (kolorowy / szary / binarny)
    private ImageView histogramImageView; // histogram – zawsze pod obrazem

    private Label normalizationInfoLabel;
    private Label binarizationInfoLabel;

    private Slider manualThresholdSlider;
    private Label manualThresholdValueLabel;

    private Slider percentBlackSlider;
    private Label percentBlackValueLabel;

    @Override
    public void start(Stage primaryStage) {
        this.primary = primaryStage;
        Locale.setDefault(Locale.US);

        BorderPane root = new BorderPane();

        // Góra – wczytywanie obrazu
        root.setTop(createTopBar());

        // Środek – panel sterowania + obraz + histogram
        root.setCenter(createCenterPane());

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("Projekt – histogram i binaryzacja (JavaFX)");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // ---------------------------------------------------
    // GÓRNY PASEK – Wczytywanie obrazu
    // ---------------------------------------------------
    private HBox createTopBar() {
        Button loadButton = new Button("Wczytaj obraz...");
        loadButton.setOnAction(e -> loadImage());

        loadedFileLabel = new Label("Brak wczytanego obrazu.");

        HBox topBar = new HBox(15, loadButton, loadedFileLabel);
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_LEFT);
        return topBar;
    }

    // ---------------------------------------------------
    // ŚRODEK – panel sterowania + obraz + histogram
    // ---------------------------------------------------
    private HBox createCenterPane() {
        HBox rootCenter = new HBox();
        rootCenter.setSpacing(10);
        rootCenter.setPadding(new Insets(10));

        // Lewy panel sterowania (scrollowalny)
        VBox controlPanel = createControlPanel();
        ScrollPane controlScroll = new ScrollPane(controlPanel);
        controlScroll.setFitToWidth(true);
        controlScroll.setPrefWidth(420);

        // Prawy panel – obraz + histogram
        mainImageView = new ImageView();
        mainImageView.setPreserveRatio(true);
        mainImageView.setFitWidth(700);
        mainImageView.setFitHeight(500);

        histogramImageView = new ImageView();
        histogramImageView.setPreserveRatio(false);
        histogramImageView.setFitWidth(700);
        histogramImageView.setFitHeight(180);

        Label histLabel = new Label("Histogram aktualnego obrazu (poziomy szarości 0–255):");

        VBox rightBox = new VBox(10,
                mainImageView,
                histLabel,
                histogramImageView
        );
        rightBox.setAlignment(Pos.TOP_CENTER);
        rightBox.setPadding(new Insets(5));

        HBox.setHgrow(rightBox, Priority.ALWAYS);

        rootCenter.getChildren().addAll(controlScroll, rightBox);
        return rootCenter;
    }

    /**
     * Lewy panel sterowania – dwie sekcje:
     *  - Normalizacja histogramu
     *  - Binaryzacja (4 metody)
     */
    private VBox createControlPanel() {
        // --- Sekcja NORMALIZACJA HISTOGRAMU ---
        Label normTitle = new Label("Normalizacja histogramu");
        normTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Button showColorButton = new Button("Pokaż oryginał (kolor)");
        showColorButton.setMaxWidth(Double.MAX_VALUE);
        showColorButton.setOnAction(e -> showOriginalColor());

        Button showGrayButton = new Button("Pokaż oryginał (szarość)");
        showGrayButton.setMaxWidth(Double.MAX_VALUE);
        showGrayButton.setOnAction(e -> showOriginalGray());

        Button stretchButton = new Button("Rozszerz histogram");
        stretchButton.setMaxWidth(Double.MAX_VALUE);
        stretchButton.setOnAction(e -> applyHistogramStretch());

        Button equalizeButton = new Button("Wyrównaj histogram");
        equalizeButton.setMaxWidth(Double.MAX_VALUE);
        equalizeButton.setOnAction(e -> applyHistogramEqualization());

        normalizationInfoLabel = new Label("Wczytaj obraz, a następnie wybierz operację normalizacji.");
        normalizationInfoLabel.setWrapText(true);

        VBox normalizationBox = new VBox(8,
                normTitle,
                showColorButton,
                showGrayButton,
                stretchButton,
                equalizeButton,
                normalizationInfoLabel
        );
        normalizationBox.setPadding(new Insets(10));
        normalizationBox.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 4; -fx-border-width: 1;");

        // --- Sekcja BINARYZACJA ---
        Label binTitle = new Label("Binaryzacja obrazu");
        binTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // 1) RĘCZNY PRÓG
        manualThresholdSlider = new Slider(0, 255, 128);
        manualThresholdSlider.setShowTickLabels(true);
        manualThresholdSlider.setShowTickMarks(true);
        manualThresholdSlider.setMajorTickUnit(64);
        manualThresholdSlider.setMinorTickCount(4);
        manualThresholdSlider.setBlockIncrement(1);

        manualThresholdValueLabel = new Label("128");
        manualThresholdSlider.valueProperty().addListener((obs, ov, nv) ->
                manualThresholdValueLabel.setText(String.format("%.0f", nv.doubleValue()))
        );

        Button manualButton = new Button("Zastosuj próg ręczny");
        manualButton.setOnAction(e -> applyManualThreshold());

        VBox manualBox = new VBox(5,
                new Label("Próg ręczny (wartość 0–255):"),
                manualThresholdSlider,
                new HBox(10, new Label("Aktualny próg:"), manualThresholdValueLabel),
                manualButton
        );

        // 2) PERCENT BLACK SELECTION
        percentBlackSlider = new Slider(0, 100, 50);
        percentBlackSlider.setShowTickLabels(true);
        percentBlackSlider.setShowTickMarks(true);
        percentBlackSlider.setMajorTickUnit(25);
        percentBlackSlider.setMinorTickCount(4);
        percentBlackSlider.setBlockIncrement(1);

        percentBlackValueLabel = new Label("50%");
        percentBlackSlider.valueProperty().addListener((obs, ov, nv) ->
                percentBlackValueLabel.setText(String.format("%.0f%%", nv.doubleValue()))
        );

        Button pbsButton = new Button("Zastosuj selekcję procentowej czerni");
        pbsButton.setOnAction(e -> applyPercentBlackSelection());

        VBox pbsBox = new VBox(5,
                new Label("Docelowy procent pikseli czarnych (Percent Black Selection):"),
                percentBlackSlider,
                new HBox(10, new Label("Aktualna wartość:"), percentBlackValueLabel),
                pbsButton
        );

        // 3) MEAN ITERATIVE SELECTION
        Button meanButton = new Button("Zastosuj iteracyjną selekcję średniej");
        meanButton.setOnAction(e -> applyMeanIterativeSelection());

        VBox meanBox = new VBox(5,
                new Label("Automatyczny próg na podstawie średnich klas (Mean Iterative Selection):"),
                meanButton
        );

        // 4) SELEKCJA ENTROPII
        Button entropyButton = new Button("Zastosuj selekcję entropii");
        entropyButton.setOnAction(e -> applyEntropySelection());

        VBox entropyBox = new VBox(5,
                new Label("Automatyczny próg maksymalizujący sumę entropii klas (Entropy Selection):"),
                entropyButton
        );

        binarizationInfoLabel = new Label("Wczytaj obraz i wybierz jedną z metod binaryzacji.");
        binarizationInfoLabel.setWrapText(true);

        VBox binarizationBox = new VBox(10,
                binTitle,
                manualBox,
                new Separator(),
                pbsBox,
                new Separator(),
                meanBox,
                new Separator(),
                entropyBox,
                new Separator(),
                binarizationInfoLabel
        );
        binarizationBox.setPadding(new Insets(10));
        binarizationBox.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 4; -fx-border-width: 1;");

        VBox controlPanel = new VBox(15, normalizationBox, binarizationBox);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setAlignment(Pos.TOP_LEFT);

        return controlPanel;
    }

    // ---------------------------------------------------
    // Wczytywanie obrazu
    // ---------------------------------------------------
    private void loadImage() {
        FileChooser chooser = new FileChooser();

        File startDir = new File("C:/Users");
        if (startDir.exists() && startDir.isDirectory()) {
            chooser.setInitialDirectory(startDir);
        }

        chooser.setTitle("Wybierz plik obrazu");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Pliki graficzne", "*.png", "*.jpg", "*.jpeg", "*.bmp"),
                new FileChooser.ExtensionFilter("Wszystkie pliki", "*.*")
        );

        File file = chooser.showOpenDialog(primary);
        if (file != null) {
            try {
                loadedColorImage = new Image(file.toURI().toString());
                loadedFileLabel.setText("Obraz: " + file.getName());

                originalGray = GrayscaleImage.fromImage(loadedColorImage);
                stretchedGray = null;
                equalizedGray = null;
                binaryGray = null;

                mainImageView.setImage(loadedColorImage);

                currentGray = originalGray;
                updateHistogramFromGray(currentGray);

                normalizationInfoLabel.setText("Oryginalny obraz wczytany (kolor). Użyj przycisków powyżej, aby przełączyć widok/normalizację.");
                binarizationInfoLabel.setText("Obraz wczytany. Możesz teraz wykonać binaryzację (4 metody).");

            } catch (Exception ex) {
                ex.printStackTrace();
                loadedFileLabel.setText("Błąd podczas wczytywania obrazu.");
            }
        }
    }

    // ---------------------------------------------------
    // Aktualizacja histogramu na podstawie obrazu szarości
    // ---------------------------------------------------
    private void updateHistogramFromGray(GrayscaleImage img) {
        if (img == null) {
            histogramImageView.setImage(null);
            return;
        }
        currentHistogram = HistogramUtils.computeHistogram(img);
        Image histImg = HistogramUtils.createHistogramImage(currentHistogram, 700, 180);
        histogramImageView.setImage(histImg);
    }

    private boolean checkOriginalLoaded() {
        if (originalGray == null || loadedColorImage == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Brak obrazu");
            alert.setHeaderText(null);
            alert.setContentText("Najpierw wczytaj obraz z dysku.");
            alert.showAndWait();
            return false;
        }
        return true;
    }

    // ---------------------------------------------------
    // NORMALIZACJA HISTOGRAMU
    // ---------------------------------------------------
    private void showOriginalColor() {
        if (!checkOriginalLoaded()) return;

        mainImageView.setImage(loadedColorImage);
        currentGray = originalGray;
        updateHistogramFromGray(currentGray);

        int[] mm = HistogramUtils.findMinMaxGray(HistogramUtils.computeHistogram(originalGray));
        normalizationInfoLabel.setText(String.format(
                "Oryginał (kolor) – histogram liczony z wersji szarej: min = %d, max = %d.",
                mm[0], mm[1]
        ));
    }

    private void showOriginalGray() {
        if (!checkOriginalLoaded()) return;

        currentGray = originalGray;
        mainImageView.setImage(currentGray.toImage());
        updateHistogramFromGray(currentGray);

        int[] hist = HistogramUtils.computeHistogram(originalGray);
        int[] minMax = HistogramUtils.findMinMaxGray(hist);
        normalizationInfoLabel.setText(String.format(
                "Oryginał w skali szarości: min = %d, max = %d.",
                minMax[0], minMax[1]
        ));
    }

    private void applyHistogramStretch() {
        if (!checkOriginalLoaded()) return;

        if (stretchedGray == null) {
            stretchedGray = HistogramUtils.stretchHistogram(originalGray);
        }
        currentGray = stretchedGray;
        mainImageView.setImage(currentGray.toImage());
        updateHistogramFromGray(currentGray);

        int[] histOrig = HistogramUtils.computeHistogram(originalGray);
        int[] minMaxOrig = HistogramUtils.findMinMaxGray(histOrig);
        int[] minMaxStr = HistogramUtils.findMinMaxGray(currentHistogram);

        normalizationInfoLabel.setText(String.format(
                "Rozszerzenie histogramu: oryginał min=%d, max=%d; po rozszerzeniu min=%d, max=%d.",
                minMaxOrig[0], minMaxOrig[1],
                minMaxStr[0], minMaxStr[1]
        ));
    }

    private void applyHistogramEqualization() {
        if (!checkOriginalLoaded()) return;

        if (equalizedGray == null) {
            equalizedGray = HistogramUtils.equalizeHistogram(originalGray);
        }
        currentGray = equalizedGray;
        mainImageView.setImage(currentGray.toImage());
        updateHistogramFromGray(currentGray);

        normalizationInfoLabel.setText("Wyrównanie histogramu zostało wykonane (Histogram Equalization).");
    }

    // ---------------------------------------------------
    // BINARYZACJA – 4 METODY
    // ---------------------------------------------------
    private void applyManualThreshold() {
        if (!checkOriginalLoaded()) return;

        int t = (int) Math.round(manualThresholdSlider.getValue());
        binaryGray = Thresholding.applyThreshold(originalGray, t);

        currentGray = binaryGray;
        mainImageView.setImage(currentGray.toImage());
        updateHistogramFromGray(currentGray);

        binarizationInfoLabel.setText("Binaryzacja z progiem ręcznym: T = " + t + ".");
    }

    private void applyPercentBlackSelection() {
        if (!checkOriginalLoaded()) return;

        int totalPixels = originalGray.getWidth() * originalGray.getHeight();
        int[] hist = HistogramUtils.computeHistogram(originalGray);

        double percentBlack = percentBlackSlider.getValue();
        int t = Thresholding.percentBlackThreshold(hist, totalPixels, percentBlack);

        binaryGray = Thresholding.applyThreshold(originalGray, t);

        currentGray = binaryGray;
        mainImageView.setImage(currentGray.toImage());
        updateHistogramFromGray(currentGray);

        binarizationInfoLabel.setText(String.format(
                "Selekcja procentowej czerni: docelowo %.1f%% czarnych pikseli, wyznaczony próg T = %d.",
                percentBlack, t
        ));
    }

    private void applyMeanIterativeSelection() {
        if (!checkOriginalLoaded()) return;

        int totalPixels = originalGray.getWidth() * originalGray.getHeight();
        int[] hist = HistogramUtils.computeHistogram(originalGray);

        int t = Thresholding.meanIterativeThreshold(hist, totalPixels);

        binaryGray = Thresholding.applyThreshold(originalGray, t);

        currentGray = binaryGray;
        mainImageView.setImage(currentGray.toImage());
        updateHistogramFromGray(currentGray);

        binarizationInfoLabel.setText("Iteracyjna selekcja średniej: wyznaczony próg T = " + t + ".");
    }

    private void applyEntropySelection() {
        if (!checkOriginalLoaded()) return;

        int totalPixels = originalGray.getWidth() * originalGray.getHeight();
        int[] hist = HistogramUtils.computeHistogram(originalGray);

        int t = Thresholding.entropyThreshold(hist, totalPixels);

        binaryGray = Thresholding.applyThreshold(originalGray, t);

        currentGray = binaryGray;
        mainImageView.setImage(currentGray.toImage());
        updateHistogramFromGray(currentGray);

        binarizationInfoLabel.setText("Selekcja entropii (Kapur): wyznaczony próg T = " + t + ".");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
