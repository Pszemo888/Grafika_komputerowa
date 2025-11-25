package com.example.proj_graf03;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        TabPane tabs = new TabPane();

        Tab converterTab = new Tab("Konwersja RGB / CMYK", new ColorConverterPane());
        converterTab.setClosable(false);

        Tab cubeTab = new Tab("Kostka RGB 3D", new RgbCubePane());
        cubeTab.setClosable(false);

        tabs.getTabs().addAll(converterTab, cubeTab);

        Scene scene = new Scene(tabs, 1100, 650);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Grafika komputerowa – RGB / CMYK i kostka RGB");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
