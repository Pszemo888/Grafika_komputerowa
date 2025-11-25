package com.example.proj_graf02;

import com.example.proj_graf02.io.PpmIO;
import com.example.proj_graf02.ui.ImagePane;
import com.example.proj_graf02.util.ImageUtils;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

public class App extends Application {

    private final ImagePane imagePane = new ImagePane();

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setTop(createMenu(stage));
        root.setCenter(imagePane.getNode());
        root.setBottom(createBottomBar());

        Scene scene = new Scene(root, 1200, 800);
        stage.setTitle("PPM–JPEG Viewer (JavaFX)");
        stage.setScene(scene);
        stage.show();
    }

    private MenuBar createMenu(Stage stage) {
        // Plik
        Menu mFile = new Menu("Plik");

        MenuItem miOpen = new MenuItem("Otwórz…");
        miOpen.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
        miOpen.setOnAction(e -> onOpen(stage));

        MenuItem miSaveJpeg = new MenuItem("Zapisz jako JPEG…");
        miSaveJpeg.setAccelerator(KeyCombination.keyCombination("Ctrl+S"));
        miSaveJpeg.setOnAction(e -> onSaveAsJpeg(stage));

        MenuItem miExit = new MenuItem("Zakończ");
        miExit.setOnAction(e -> stage.close());

        mFile.getItems().addAll(miOpen, miSaveJpeg, new SeparatorMenuItem(), miExit);

        // Widok
        Menu mView = new Menu("Widok");
        MenuItem miReset = new MenuItem("Reset powiększenia");
        miReset.setOnAction(e -> imagePane.resetZoom());
        mView.getItems().add(miReset);

        // Kolor
        Menu mColor = new Menu("Kolor");
        MenuItem miNormalize = new MenuItem("Znormalizuj (0–255)");
        miNormalize.setOnAction(e -> {
            if (imagePane.getImage() != null) {
                imagePane.setImage(ImageUtils.normalizeToByte(imagePane.getImage()));
            }
        });
        mColor.getItems().add(miNormalize);

        // Pomoc
        Menu mHelp = new Menu("Pomoc");
        MenuItem miAbout = new MenuItem("Informacje");
        miAbout.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION,
                "PPM–JPEG Viewer\nPPM P3/P6 (ręcznie, blokowo), JPEG (ImageIO)\nZoom, pan, overlay RGB przy dużym powiększeniu.\n© com.example.proj_graf02")
                .showAndWait());
        mHelp.getItems().add(miAbout);

        return new MenuBar(mFile, mView, mColor, mHelp);
    }

    private HBox createBottomBar() {
        HBox box = new HBox(12);
        box.setPadding(new Insets(6));

        Label lZoom = new Label("Zoom:");
        Slider sZoom = new Slider(0.25, 32, 1);
        sZoom.setBlockIncrement(0.25);
        sZoom.valueProperty().addListener((o, ov, nv) -> imagePane.setZoom(nv.doubleValue()));

        Label lBright = new Label("Jasność:");
        Slider sBright = new Slider(0.1, 3.0, 1.0);
        sBright.setBlockIncrement(0.1);
        sBright.valueProperty().addListener((o, ov, nv) -> imagePane.setBrightness(nv.doubleValue()));

        box.getChildren().addAll(lZoom, sZoom, new Separator(), lBright, sBright);
        return box;
    }

    private void onOpen(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Otwórz obraz");
        File defaultDir = new File("C:/Users/przem/Desktop/SEMESTR 7/Grafika/OneDrive_1_16.11.2025");
        if (defaultDir.exists() && defaultDir.isDirectory()) {
            fc.setInitialDirectory(defaultDir);
        }

        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PPM / JPEG", "*.ppm", "*.pnm", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("PPM", "*.ppm", "*.pnm"),
                new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg")
        );
        File f = fc.showOpenDialog(stage);
        if (f == null) return;

        try {
            BufferedImage bi;
            String name = f.getName().toLowerCase();
            if (name.endsWith(".ppm") || name.endsWith(".pnm")) {
                try (InputStream in = new BufferedInputStream(new FileInputStream(f))) {
                    bi = PpmIO.read(in);                        // WŁASNY parser P3/P6 (blokowo)
                }
            } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                bi = ImageIO.read(f);                            // Dozwolone: biblioteka JDK
                if (bi == null) throw new IOException("Nieobsługiwany/zepsuty JPEG");
            } else {
                throw new IOException("Nieobsługiwany format pliku: " + name);
            }
            imagePane.setImage(bi);
        } catch (Exception ex) {
            showError("Błąd wczytywania", ex);
        }
    }

    private void onSaveAsJpeg(Stage stage) {
        if (imagePane.getImage() == null) {
            new Alert(Alert.AlertType.WARNING, "Najpierw otwórz obraz.").showAndWait();
            return;
        }

        // Dialog z wyborem jakości
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Jakość JPEG");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Slider s = new Slider(0.1, 1.0, 0.9);
        s.setShowTickMarks(true); s.setShowTickLabels(true); s.setMajorTickUnit(0.1);
        dialog.getDialogPane().setContent(new VBox(8, new Label("Wybierz jakość (0.1–1.0):"), s));
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? s.getValue() : null);
        Double quality = dialog.showAndWait().orElse(null);
        if (quality == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Zapisz jako JPEG");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg"));
        File out = fc.showSaveDialog(stage);
        if (out == null) return;

        try {
            BufferedImage bi = imagePane.getImage();

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) throw new IOException("Brak kodera JPEG w ImageIO");
            ImageWriter writer = writers.next();

            try (FileImageOutputStream fos = new FileImageOutputStream(out)) {
                writer.setOutput(fos);
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(quality.floatValue()); // 0..1
                }
                writer.write(null, new IIOImage(bi, null, null), param);
            } finally {
                writer.dispose();
            }
        } catch (Exception ex) {
            showError("Błąd zapisu JPEG", ex);
        }
    }

    private void showError(String title, Exception ex) {
        String msg = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setHeaderText(title);
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }
}
