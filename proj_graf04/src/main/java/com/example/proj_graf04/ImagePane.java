package com.example.proj_graf04;

import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;

import java.awt.image.BufferedImage;

public class ImagePane extends StackPane {

    private final ImageView imageView = new ImageView();
    private BufferedImage bufferedImage;

    public ImagePane() {
        getChildren().add(imageView);
        imageView.setPreserveRatio(true);
        imageView.fitWidthProperty().bind(widthProperty());
        imageView.fitHeightProperty().bind(heightProperty());
    }

    public void setImage(BufferedImage img) {
        this.bufferedImage = img;
        if (img != null) {
            int w = img.getWidth();
            int h = img.getHeight();

            // RĘCZNA KONWERSJA BufferedImage -> JavaFX WritableImage
            WritableImage fxImage = new WritableImage(w, h);
            PixelWriter pw = fxImage.getPixelWriter();

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = img.getRGB(x, y); // ARGB od BufferedImage
                    pw.setArgb(x, y, argb);      // bezpośrednio do WritableImage
                }
            }

            imageView.setImage(fxImage);
        } else {
            imageView.setImage(null);
        }
    }

    public BufferedImage getImage() {
        return bufferedImage;
    }

    public boolean hasImage() {
        return bufferedImage != null;
    }
}
