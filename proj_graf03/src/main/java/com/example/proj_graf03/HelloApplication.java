package com.example.proj_graf03;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) {
        TabPane tabPane = new TabPane();

        Tab conversionTab = new Tab("Konwersja kolorów", new ColorConverterPane());
        conversionTab.setClosable(false);

        Tab cubeTab = new Tab("Kostka RGB", new RgbCubePane());
        cubeTab.setClosable(false);

        tabPane.getTabs().addAll(conversionTab, cubeTab);

        Scene scene = new Scene(tabPane, 1100, 700);
        stage.setTitle("Konwersja barw i kostka RGB");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
