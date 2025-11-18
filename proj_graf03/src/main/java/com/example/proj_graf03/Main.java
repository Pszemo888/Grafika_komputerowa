package com.example.proj_graf03;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.scene.PointLight;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        TabPane tabs = new TabPane();

        tabs.getTabs().add(new Tab("RGB ↔ CMYK", buildConverter()));
        tabs.getTabs().add(new Tab("RGB Cube 3D", buildCube3D()));

        stage.setScene(new Scene(tabs, 900, 600));
        stage.show();
    }

    // ==================================================================================
    // KONWERTER RGB/CMYK — stabilny
    // ==================================================================================
    private Pane buildConverter() {

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        TextField r = new TextField();
        TextField g = new TextField();
        TextField b = new TextField();

        TextField c = new TextField();
        TextField m = new TextField();
        TextField y = new TextField();
        TextField k = new TextField();

        Pane preview = new Pane();
        preview.setPrefSize(120, 120);
        preview.setStyle("-fx-border-color:black");

        Button toCmyk = new Button("RGB → CMYK");
        Button toRgb  = new Button("CMYK → RGB");

        toCmyk.setOnAction(e -> {
            try {
                double R = Integer.parseInt(r.getText()) / 255.0;
                double G = Integer.parseInt(g.getText()) / 255.0;
                double B = Integer.parseInt(b.getText()) / 255.0;

                double K = 1 - Math.max(R, Math.max(G, B));
                double Cc = (1 - R - K) / (1 - K + 1e-9);
                double Mm = (1 - G - K) / (1 - K + 1e-9);
                double Yy = (1 - B - K) / (1 - K + 1e-9);

                c.setText(String.format("%.3f", Cc));
                m.setText(String.format("%.3f", Mm));
                y.setText(String.format("%.3f", Yy));
                k.setText(String.format("%.3f", K));

                preview.setBackground(new Background(new BackgroundFill(
                        Color.rgb((int)(R*255),(int)(G*255),(int)(B*255)),
                        null,null)));
            } catch (Exception ignored) {}
        });

        toRgb.setOnAction(e -> {
            try {
                double Cc = Double.parseDouble(c.getText());
                double Mm = Double.parseDouble(m.getText());
                double Yy = Double.parseDouble(y.getText());
                double Kk = Double.parseDouble(k.getText());

                int R = (int)(255*(1-Cc)*(1-Kk));
                int G = (int)(255*(1-Mm)*(1-Kk));
                int B = (int)(255*(1-Yy)*(1-Kk));

                r.setText(""+R);
                g.setText(""+G);
                b.setText(""+B);

                preview.setBackground(new Background(new BackgroundFill(Color.rgb(R,G,B),null,null)));
            } catch (Exception ignored) {}
        });

        GridPane g1 = new GridPane();
        g1.setHgap(10); g1.setVgap(10);

        g1.addRow(0,new Label("R:"),r);
        g1.addRow(1,new Label("G:"),g);
        g1.addRow(2,new Label("B:"),b);
        g1.add(toCmyk,0,3);

        g1.addRow(4,new Label("C:"),c);
        g1.addRow(5,new Label("M:"),m);
        g1.addRow(6,new Label("Y:"),y);
        g1.addRow(7,new Label("K:"),k);
        g1.add(toRgb,0,8);

        root.getChildren().addAll(g1,preview);
        return root;
    }

    // ==================================================================================
    // *** W PEŁNI WIDOCZNA, STABILNA KOSTKA RGB 3D ***
    // ==================================================================================
    private Pane buildCube3D() {

        Pane container = new Pane();
        container.setPrefSize(800, 600);

        Group root3D = new Group();

        // ŚWIATŁO (kluczowe!)
        PointLight light = new PointLight(Color.WHITE);
        light.setTranslateX(300);
        light.setTranslateY(-300);
        light.setTranslateZ(-300);
        root3D.getChildren().add(light);

        // Voxele RGB
        int step = 60;     // mniejsza ilość = szybsze działanie
        int size = 30;

        for(int R=0;R<=255;R+=step){
            for(int G=0;G<=255;G+=step){
                for(int B=0;B<=255;B+=step){

                    Box box = new Box(size,size,size);
                    box.setMaterial(new PhongMaterial(Color.rgb(R,G,B)));

                    // Pozycjonowanie skoncentrowane
                    box.setTranslateX(R - 128);
                    box.setTranslateY(G - 128);
                    box.setTranslateZ(B - 128);

                    root3D.getChildren().add(box);
                }
            }
        }

        SubScene sub = new SubScene(root3D, 800,600,true,SceneAntialiasing.BALANCED);
        sub.setFill(Color.gray(0.15));  // tło aby było widać kostkę

        PerspectiveCamera cam = new PerspectiveCamera(true);
        cam.setNearClip(0.1);
        cam.setFarClip(5000);
        cam.setTranslateZ(-700);     // najważniejsze!!
        sub.setCamera(cam);

        // Rotacja
        Rotate rotX = new Rotate(20, Rotate.X_AXIS);
        Rotate rotY = new Rotate(30, Rotate.Y_AXIS);
        root3D.getTransforms().addAll(rotX, rotY);

        final double[] last = new double[2];

        sub.setOnMousePressed(e -> {
            last[0]=e.getSceneX();
            last[1]=e.getSceneY();
        });

        sub.setOnMouseDragged(e -> {
            rotY.setAngle(rotY.getAngle() + (e.getSceneX()-last[0])*0.5);
            rotX.setAngle(rotX.getAngle() - (e.getSceneY()-last[1])*0.5);
            last[0]=e.getSceneX();
            last[1]=e.getSceneY();
        });

        sub.widthProperty().bind(container.widthProperty());
        sub.heightProperty().bind(container.heightProperty());
        container.getChildren().add(sub);

        return container;
    }

    public static void main(String[] args) {
        launch();
    }
}
