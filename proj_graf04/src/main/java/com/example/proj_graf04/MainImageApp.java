package com.example.proj_graf04;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class MainImageApp extends Application {

    private ImagePane imagePane;
    private BufferedImage originalImage; // <- TU trzymamy obraz po wczytaniu (do resetu)

    @Override
    public void start(Stage stage) {
        imagePane = new ImagePane();

        MenuBar menuBar = buildMenuBar(stage); // tylko Plik: Otwórz/Zapisz/Zamknij
        VBox sidePanel = buildSidePanel();     // tu będą przyciski operacji + RESET

        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(imagePane);
        root.setRight(sidePanel);

        Scene scene = new Scene(root, 1100, 700);
        stage.setScene(scene);
        stage.setTitle("Przekształcenia punktowe i filtry");
        stage.show();
    }

    // ------------------------------ MENU PLIK ------------------------------

    private MenuBar buildMenuBar(Stage stage) {
        MenuBar menuBar = new MenuBar();

        Menu mFile = new Menu("Plik");

        MenuItem miOpen = new MenuItem("Otwórz...");
        miOpen.setOnAction(e -> openImage(stage));

        MenuItem miSave = new MenuItem("Zapisz jako...");
        miSave.setOnAction(e -> saveImage(stage));

        MenuItem miExit = new MenuItem("Zamknij");
        miExit.setOnAction(e -> stage.close());

        mFile.getItems().addAll(miOpen, miSave, new SeparatorMenuItem(), miExit);
        menuBar.getMenus().add(mFile);

        return menuBar;
    }

    // ------------------------------ PANEL BOCZNY ------------------------------

    private VBox buildSidePanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new javafx.geometry.Insets(10));
        panel.setPrefWidth(260);
        panel.setStyle("-fx-background-color: #f5f5f5;");

        // PRZYCISK RESET
        Button resetButton = new Button("Reset obrazu");
        resetButton.setMaxWidth(Double.MAX_VALUE);
        resetButton.setOnAction(e -> resetImage());

        // Sekcja: OPERACJE PUNKTOWE
        VBox pointOpsBox = new VBox(5);
        pointOpsBox.setPadding(new javafx.geometry.Insets(5));

        Button btnAdd = new Button("Dodawanie");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setOnAction(e -> {
            if (!imagePane.hasImage()) return;
            int val = askInt("Dodawanie", "Podaj wartość dodawaną (np. 20):", 20);
            applyAndShow(img -> ImagePointOps.add(img, val, val, val));
        });

        Button btnSub = new Button("Odejmowanie");
        btnSub.setMaxWidth(Double.MAX_VALUE);
        btnSub.setOnAction(e -> {
            if (!imagePane.hasImage()) return;
            int val = askInt("Odejmowanie", "Podaj wartość odejmowaną (np. 20):", 20);
            applyAndShow(img -> ImagePointOps.subtract(img, val, val, val));
        });

        Button btnMul = new Button("Mnożenie");
        btnMul.setMaxWidth(Double.MAX_VALUE);
        btnMul.setOnAction(e -> {
            if (!imagePane.hasImage()) return;
            double val = askDouble("Mnożenie", "Podaj współczynnik (np. 1.2):", 1.2);
            applyAndShow(img -> ImagePointOps.multiply(img, val, val, val));
        });

        Button btnDiv = new Button("Dzielenie");
        btnDiv.setMaxWidth(Double.MAX_VALUE);
        btnDiv.setOnAction(e -> {
            if (!imagePane.hasImage()) return;
            double val = askDouble("Dzielenie", "Podaj dzielnik (np. 1.2):", 1.2);
            applyAndShow(img -> ImagePointOps.divide(img, val, val, val));
        });

        Button btnBright = new Button("Zmiana jasności");
        btnBright.setMaxWidth(Double.MAX_VALUE);
        btnBright.setOnAction(e -> {
            if (!imagePane.hasImage()) return;
            int val = askInt("Jasność", "Podaj Δjasności (np. 30 lub -30):", 30);
            applyAndShow(img -> ImagePointOps.changeBrightness(img, val));
        });

        Button btnGrayAvg = new Button("Skala szarości – średnia");
        btnGrayAvg.setMaxWidth(Double.MAX_VALUE);
        btnGrayAvg.setOnAction(e -> {
            if (!imagePane.hasImage()) return;
            applyAndShow(ImagePointOps::toGrayAverage);
        });

        Button btnGrayLuma = new Button("Skala szarości – luminancja");
        btnGrayLuma.setMaxWidth(Double.MAX_VALUE);
        btnGrayLuma.setOnAction(e -> {
            if (!imagePane.hasImage()) return;
            applyAndShow(ImagePointOps::toGrayLuma);
        });

        pointOpsBox.getChildren().addAll(
                btnAdd, btnSub, btnMul, btnDiv,
                new Separator(),
                btnBright,
                new Separator(),
                btnGrayAvg, btnGrayLuma
        );

        TitledPane pointOpsPane = new TitledPane("Operacje punktowe", pointOpsBox);
        pointOpsPane.setExpanded(true);

        // Sekcja: FILTRY
        VBox filtersBox = new VBox(5);
        filtersBox.setPadding(new javafx.geometry.Insets(5));

        Button btnMean = new Button("Wygładzający (uśredniający)");
        btnMean.setMaxWidth(Double.MAX_VALUE);
        btnMean.setOnAction(e -> {
            if (!imagePane.hasImage()) return;
            applyAndShow(ImageFilters::mean3x3);
        });

        Button btnMedian = new Button("Medianowy 3x3");
        btnMedian.setMaxWidth(Double.MAX_VALUE);
        btnMedian.setOnAction(e -> {
            if (!imagePane.hasImage()) return;
            applyAndShow(ImageFilters::median3x3);
        });

        Button btnSobel = new Button("Sobel – krawędzie");
        btnSobel.setMaxWidth(Double.MAX_VALUE);
        btnSobel.setOnAction(e -> {
            if (!imagePane.hasImage()) return;
            applyAndShow(ImageFilters::sobelEdges);
        });

        Button btnHighPass = new Button("Górnoprzepustowy wyostrzający");
        btnHighPass.setMaxWidth(Double.MAX_VALUE);
        btnHighPass.setOnAction(e -> {
            if (!imagePane.hasImage()) return;
            applyAndShow(ImageFilters::sharpenHighPass);
        });

        Button btnGauss = new Button("Rozmycie Gaussa 3x3");
        btnGauss.setMaxWidth(Double.MAX_VALUE);
        btnGauss.setOnAction(e -> {
            if (!imagePane.hasImage()) return;
            applyAndShow(ImageFilters::gaussian3x3);
        });

        filtersBox.getChildren().addAll(
                btnMean, btnMedian, btnSobel, btnHighPass, btnGauss
        );

        TitledPane filtersPane = new TitledPane("Filtry", filtersBox);
        filtersPane.setExpanded(false);

        // Accordion – ładne „rozwijane” sekcje
        Accordion accordion = new Accordion(pointOpsPane, filtersPane);

        panel.getChildren().addAll(resetButton, new Separator(), accordion);
        return panel;
    }

    // ------------------------------ RESET ------------------------------

    private void resetImage() {
        if (originalImage != null) {
            // jeśli chcesz kopię: imagePane.setImage(deepCopy(originalImage));
            imagePane.setImage(originalImage);
        }
    }

    // (opcjonalnie) Gdybyś chciał kopiować:
    /*
    private BufferedImage deepCopy(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                copy.setRGB(x, y, src.getRGB(x, y));
            }
        }
        return copy;
    }
    */

    // ------------------------------ OPERACJE NA OBRAZIE ------------------------------

    private void applyAndShow(java.util.function.Function<BufferedImage, BufferedImage> op) {
        BufferedImage src = imagePane.getImage();
        if (src == null) return;
        BufferedImage result = op.apply(src);
        imagePane.setImage(result);
    }

    private void openImage(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Otwórz obraz");

        File startDir = new File("C:/Users/przem/Desktop/SEMESTR 7/Grafika/OneDrive_1_16.11.2025");
        if (startDir.exists() && startDir.isDirectory()) {
            fc.setInitialDirectory(startDir);
        }

        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Obrazy", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif")
        );
        File file = fc.showOpenDialog(stage);
        if (file == null) return;
        try {
            BufferedImage img = ImageIO.read(file);
            if (img != null) {
                originalImage = img;      // zapamiętujemy oryginał do RESET
                imagePane.setImage(img);  // wyświetlamy
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            showError("Błąd wczytywania obrazu: " + ex.getMessage());
        }
    }

    private void saveImage(Stage stage) {
        if (!imagePane.hasImage()) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Zapisz obraz jako");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG", "*.png")
        );
        File file = fc.showSaveDialog(stage);
        if (file == null) return;
        try {
            BufferedImage img = imagePane.getImage();
            ImageIO.write(img, "png", file);
        } catch (IOException ex) {
            ex.printStackTrace();
            showError("Błąd zapisu obrazu: " + ex.getMessage());
        }
    }

    // ------------------------------ DIALOGI POMOCNICZE ------------------------------

    private int askInt(String title, String header, int def) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(def));
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        return Integer.parseInt(dialog.showAndWait().orElse(String.valueOf(def)));
    }

    private double askDouble(String title, String header, double def) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(def));
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        return Double.parseDouble(dialog.showAndWait().orElse(String.valueOf(def)).replace(",", "."));
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Błąd");
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
