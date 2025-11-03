package com.example.proj_graf01;

import com.example.proj_graf01.controller.EditorController;
import com.example.proj_graf01.view.CanvasView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        // View + Controller (BEZ DocumentModel)
        CanvasView view = new CanvasView();
        EditorController controller = new EditorController(view);

        // Toolbar
        ToggleButton tSelect = new ToggleButton("Select");
        ToggleButton tLine   = new ToggleButton("Line");
        ToggleButton tRect   = new ToggleButton("Rect");
        ToggleButton tCircle = new ToggleButton("Circle");
        ToggleGroup tg = new ToggleGroup();
        tSelect.setToggleGroup(tg); tLine.setToggleGroup(tg);
        tRect.setToggleGroup(tg);  tCircle.setToggleGroup(tg);
        tSelect.setSelected(true);

        Button bDelete = new Button("Delete");
        Button bSave   = new Button("Save JSON");
        Button bLoad   = new Button("Load JSON");
        ToolBar tb = new ToolBar(tSelect, tLine, tRect, tCircle, new Separator(), bDelete, new Separator(), bSave, bLoad);

        // Panel właściwości
        TextField fx1 = new TextField();
        TextField fy1 = new TextField();
        TextField fx2 = new TextField();
        TextField fy2 = new TextField();
        TextField fStroke = new TextField("#222222");
        TextField fStrokeW = new TextField("2.0");
        TextField fFill = new TextField("#00000000");
        Button bApply = new Button("Zastosuj");
        bApply.setMaxWidth(Double.MAX_VALUE);

        GridPane props = new GridPane();
        props.setHgap(6); props.setVgap(6);
        props.add(new Label("x1 / cx"),0,0); props.add(fx1,1,0);
        props.add(new Label("y1 / cy"),0,1); props.add(fy1,1,1);
        props.add(new Label("x2 / r"), 0,2); props.add(fx2,1,2);
        props.add(new Label("y2"),     0,3); props.add(fy2,1,3);
        props.add(new Label("stroke"), 0,4); props.add(fStroke,1,4);
        props.add(new Label("strokeW"),0,5); props.add(fStrokeW,1,5);
        props.add(new Label("fill"),   0,6); props.add(fFill,1,6);
        props.add(bApply, 0, 7, 2, 1);
        ColumnConstraints c0 = new ColumnConstraints(); c0.setPercentWidth(40);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(60);
        props.getColumnConstraints().addAll(c0,c1);

        VBox right = new VBox(8, new Label("Właściwości"), props);
        right.setAlignment(Pos.TOP_LEFT);
        right.setFillWidth(true);
        right.setPadding(new Insets(10));
        right.setPrefWidth(260);

        BorderPane root = new BorderPane(new StackPane(view.getCanvas()), tb, null, null, right);

        // Powiązania z kontrolerem
        controller.bindToolbar(tSelect, tLine, tRect, tCircle, bDelete);
        controller.bindProperties(fx1, fy1, fx2, fy2, fStroke, fStrokeW, fFill, bApply);

        // Save/Load
        bSave.setOnAction(e -> {
            FileChooser ch = new FileChooser();
            ch.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            File f = ch.showSaveDialog(stage);
            if (f != null) controller.saveJson(f);
        });
        bLoad.setOnAction(e -> {
            FileChooser ch = new FileChooser();
            ch.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            File f = ch.showOpenDialog(stage);
            if (f != null) controller.loadJson(f);
        });

        // Scena i start
        Scene scene = new Scene(root, 1200, 750);
        stage.setTitle("Vector Editor");
        stage.setScene(scene);
        stage.show();

        controller.installMouseAndKeys(scene); // po show()
        view.redraw();                         // pusta scena
        controller.refreshPropertyPanelEnabled();
    }

    public static void main(String[] args) { launch(args); }
}
