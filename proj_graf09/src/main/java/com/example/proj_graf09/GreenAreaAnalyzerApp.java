package com.example.proj_graf09;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.IntBuffer;
import java.util.Locale;

public class GreenAreaAnalyzerApp extends Application {

    // --- PARAMETRY "WYDANOŚCI" / DETEKCJI ---
    // Co ile pikseli próbkujemy (1 = każdy piksel, 2 = co drugi, 3 = co trzeci, itd.)
    private static final int SAMPLE_STEP = 2;

    // Minimalne nasycenie i jasność dla zaliczenia piksela jako "kolor"
    private static final double MIN_SAT = 0.2;   // 0..1
    private static final double MIN_VAL = 0.2;   // 0..1

    private ImageView imageView;
    private Image loadedImage;

    private Label resultLabel;
    private ColorPicker colorPicker;
    private Slider toleranceSlider;
    private Label toleranceValueLabel;

    @Override
    public void start(Stage primaryStage) {
        // ---------- GÓRNY PANEL KONTROLI ----------
        Button loadButton = new Button("Wczytaj obraz");
        loadButton.setOnAction(e -> loadImage(primaryStage));

        colorPicker = new ColorPicker(Color.rgb(0, 200, 0)); // Domyślnie "zieleń"

        // Tutaj tolerancja będzie interpretowana jako tolerancja H (w stopniach)
        toleranceSlider = new Slider(0, 180, 30);
        toleranceSlider.setShowTickMarks(true);
        toleranceSlider.setShowTickLabels(true);
        toleranceSlider.setMajorTickUnit(30);
        toleranceSlider.setMinorTickCount(5);
        toleranceSlider.setBlockIncrement(5);

        toleranceValueLabel = new Label(String.format(Locale.US, "%.0f°", toleranceSlider.getValue()));
        toleranceSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                toleranceValueLabel.setText(String.format(Locale.US, "%.0f°", newVal.doubleValue()))
        );

        Button computeButton = new Button("Oblicz procent");
        computeButton.setOnAction(e -> computePercentage());

        HBox controls = new HBox(10);
        controls.setPadding(new Insets(10));
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getChildren().addAll(
                loadButton,
                new Label("Kolor docelowy:"),
                colorPicker,
                new Label("Tolerancja H (°):"),
                toleranceSlider,
                toleranceValueLabel,
                computeButton
        );

        // ---------- ŚRODKOWA CZĘŚĆ: PODGLĄD OBRAZU ----------
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(800);
        imageView.setFitHeight(600);

        ScrollPane scrollPane = new ScrollPane(imageView);
        scrollPane.setPannable(true);

        // ---------- DOLNY PANEL: WYNIKI ----------
        resultLabel = new Label("Wczytaj obraz i kliknij \"Oblicz procent\".");
        resultLabel.setPadding(new Insets(10));

        // ---------- GŁÓWNY LAYOUT ----------
        BorderPane root = new BorderPane();
        root.setTop(controls);
        root.setCenter(scrollPane);
        root.setBottom(resultLabel);

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setTitle("Analizator terenów zielonych / koloru (HSV + próbkowanie)");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Wczytanie obrazu z dysku za pomocą FileChooser.
     */
    private void loadImage(Stage stage) {
        FileChooser chooser = new FileChooser();

        File startDir = new File("C:/Users/przem/Desktop/SEMESTR 7/Grafika/OneDrive_1_16.11.2025");
        if (startDir.exists() && startDir.isDirectory()) {
            chooser.setInitialDirectory(startDir);
        }

        chooser.setTitle("Wybierz obraz");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Pliki graficzne", "*.png", "*.jpg", "*.jpeg", "*.bmp"),
                new FileChooser.ExtensionFilter("Wszystkie pliki", "*.*")
        );

        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            try {
                loadedImage = new Image(file.toURI().toString());
                imageView.setImage(loadedImage);
                resultLabel.setText("Obraz wczytany: " + file.getName());
            } catch (Exception ex) {
                ex.printStackTrace();
                resultLabel.setText("Błąd podczas wczytywania obrazu.");
            }
        }
    }

    /**
     * Główna logika – obliczenie procentu pikseli "podobnych" do wybranego koloru
     * w przestrzeni HSV, z uwzględnieniem próbkowania.
     */
    private void computePercentage() {
        if (loadedImage == null) {
            resultLabel.setText("Najpierw wczytaj obraz.");
            return;
        }

        long startTime = System.nanoTime();

        PixelReader pixelReader = loadedImage.getPixelReader();
        if (pixelReader == null) {
            resultLabel.setText("Błąd: nie można odczytać pikseli z obrazu.");
            return;
        }

        int width = (int) loadedImage.getWidth();
        int height = (int) loadedImage.getHeight();

        int[] pixels = new int[width * height];
        WritablePixelFormat<IntBuffer> format = PixelFormat.getIntArgbInstance();
        pixelReader.getPixels(0, 0, width, height, format, pixels, 0, width);

        // Kolor docelowy z ColorPicker → H,S,V
        Color fxColor = colorPicker.getValue();
        double targetH = fxColor.getHue();         // 0..360
        double targetS = fxColor.getSaturation();  // 0..1
        double targetV = fxColor.getBrightness();  // 0..1

        // Tolerancja w stopniach na H
        double hueTolerance = toleranceSlider.getValue();

        long totalSamples = 0;
        long matching = 0;

        // Bufor na HSV, żeby nie tworzyć nowych obiektów w pętli
        double[] hsv = new double[3];

        for (int y = 0; y < height; y += SAMPLE_STEP) {
            int rowIndex = y * width;
            for (int x = 0; x < width; x += SAMPLE_STEP) {
                int argb = pixels[rowIndex + x];

                int a = (argb >>> 24) & 0xFF;
                if (a < 10) {
                    continue; // pomijamy prawie przezroczyste
                }

                totalSamples++;

                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                rgbToHsv(r, g, b, hsv);
                double h = hsv[0];
                double s = hsv[1];
                double v = hsv[2];

                // odległość na kole barw (0..180)
                double dh = Math.abs(h - targetH);
                if (dh > 180.0) {
                    dh = 360.0 - dh;
                }

                // warunek dopasowania: podobna barwa + wystarczające nasycenie i jasność
                if (dh <= hueTolerance && s >= MIN_SAT && v >= MIN_VAL) {
                    matching++;
                }
            }
        }

        double percent = (totalSamples == 0) ? 0.0 : (matching * 100.0 / totalSamples);
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        resultLabel.setText(String.format(
                Locale.US,
                "Próbkowanie co %d px. Dopasowane próbki: %d z %d (%.2f%%). Czas: %d ms",
                SAMPLE_STEP, matching, totalSamples, percent, elapsedMs
        ));
    }

    /**
     * Ręczna konwersja z RGB (0..255) do HSV:
     *  hsv[0] = H w stopniach 0..360
     *  hsv[1] = S w 0..1
     *  hsv[2] = V w 0..1
     */
    private static void rgbToHsv(int r, int g, int b, double[] hsv) {
        double rf = r / 255.0;
        double gf = g / 255.0;
        double bf = b / 255.0;

        double max = Math.max(rf, Math.max(gf, bf));
        double min = Math.min(rf, Math.min(gf, bf));
        double delta = max - min;

        double h;
        if (delta == 0) {
            h = 0;
        } else if (max == rf) {
            h = 60.0 * (((gf - bf) / delta) % 6.0);
        } else if (max == gf) {
            h = 60.0 * (((bf - rf) / delta) + 2.0);
        } else {
            h = 60.0 * (((rf - gf) / delta) + 4.0);
        }

        if (h < 0) {
            h += 360.0;
        }

        double s = (max == 0) ? 0.0 : (delta / max);
        double v = max;

        hsv[0] = h;
        hsv[1] = s;
        hsv[2] = v;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
