package com.example.proj_graf03;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        TabPane tabs = new TabPane();

        Tab converterTab = new Tab("Konwersja RGB/CMYK", new ColorConverterPane());
        converterTab.setClosable(false);

        Tab cubeTab = new Tab("Kostka RGB 3D", new RgbCubePane());
        cubeTab.setClosable(false);

        tabs.getTabs().addAll(converterTab, cubeTab);

        stage.setTitle("Grafika komputerowa – konwersja i kostka RGB");
        stage.setScene(new Scene(tabs, 1200, 750));
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
