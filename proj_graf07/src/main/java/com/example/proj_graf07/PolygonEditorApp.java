package com.example.proj_graf07;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;

public class PolygonEditorApp extends Application {

    enum Mode { SELECT, DRAW, MOVE, ROTATE, SCALE, SET_PIVOT }

    private final EditorLogic logic = new EditorLogic();
    private Mode mode = Mode.SELECT;

    // UI
    private Pane canvas;
    private Label status;

    private TextField tfPoints, tfDx, tfDy, tfPivotX, tfPivotY, tfAngle, tfScale;

    // rysowanie
    private final List<EditorLogic.Point2> drawingPts = new ArrayList<>();
    private final Polyline drawingPreview = new Polyline();

    // pivot marker
    private final Circle pivotMarker = new Circle(5, Color.ORANGE);

    // map id -> node
    private final Map<String, Polygon> nodes = new HashMap<>();

    // drag state
    private boolean dragging = false;
    private double lastX, lastY;
    private double lastAngleRad;
    private double lastDist;

    @Override
    public void start(Stage stage) {
        canvas = new Pane();
        canvas.setPrefSize(900, 650);
        canvas.setStyle("-fx-background-color:#0f172a;");

        drawingPreview.setStroke(Color.web("#94a3b8"));
        drawingPreview.getStrokeDashArray().addAll(8.0, 6.0);
        drawingPreview.setMouseTransparent(true);

        pivotMarker.setMouseTransparent(true);

        BorderPane root = new BorderPane();
        root.setTop(buildMenu(stage));
        root.setLeft(buildLeftPanel(stage));
        root.setCenter(canvas);

        status = new Label("Tryb: SELECT");
        status.setPadding(new Insets(6));
        status.setStyle("-fx-text-fill:#e2e8f0;-fx-background-color:#111827;");
        root.setBottom(status);

        Scene scene = new Scene(root, 1280, 760);
        installHandlers(scene);

        stage.setTitle("Edytor wielokątów (UI + Logic) - JavaFX");
        stage.setScene(scene);
        stage.show();

        rerenderAll();
        setMode(Mode.SELECT);
    }

    // ===== UI building =====

    private Node buildMenu(Stage stage) {
        Menu file = new Menu("Plik");
        MenuItem miNew = new MenuItem("Nowy");
        MenuItem miSave = new MenuItem("Zapisz...");
        MenuItem miLoad = new MenuItem("Wczytaj...");
        MenuItem miExit = new MenuItem("Wyjście");

        miNew.setOnAction(e -> { logic.clear(); rerenderAll(); setStatus("Nowa scena."); });
        miSave.setOnAction(e -> doSave(stage));
        miLoad.setOnAction(e -> doLoad(stage));
        miExit.setOnAction(e -> stage.close());

        file.getItems().addAll(miNew, new SeparatorMenuItem(), miSave, miLoad, new SeparatorMenuItem(), miExit);
        return new MenuBar(file);
    }

    private Node buildLeftPanel(Stage stage) {
        ToggleGroup tg = new ToggleGroup();
        ToggleButton bSel   = modeBtn("SELECT", tg, Mode.SELECT);
        ToggleButton bDraw  = modeBtn("DRAW", tg, Mode.DRAW);
        ToggleButton bMove  = modeBtn("MOVE", tg, Mode.MOVE);
        ToggleButton bRot   = modeBtn("ROTATE", tg, Mode.ROTATE);
        ToggleButton bScale = modeBtn("SCALE", tg, Mode.SCALE);
        ToggleButton bPivot = modeBtn("SET PIVOT", tg, Mode.SET_PIVOT);
        bSel.setSelected(true);

        VBox modeBox = panel("Tryb:",
                new HBox(8, bSel, bDraw),
                new HBox(8, bMove, bRot),
                new HBox(8, bScale, bPivot)
        );

        tfPoints = tf("100,100 220,120 280,240 160,280");
        Button addPoly = btn("Dodaj wielokąt", () -> {
            try {
                String id = logic.addPolygon(EditorLogic.parsePoints(tfPoints.getText()));
                rerenderAll();
                select(id);
                setStatus("Dodano " + id);
            } catch (Exception ex) {
                setStatus("Błędny format punktów: x,y x,y x,y ... (min 3)");
            }
        });
        VBox addBox = panel("Nowa figura (z tekstu):", tfPoints, addPoly);

        tfDx = tf("30"); tfDy = tf("10");
        Button applyMove = btn("Przesuń", () -> {
            if (logic.selected() == null) { setStatus("Zaznacz figurę."); return; }
            try {
                double dx = parseD(tfDx.getText());
                double dy = parseD(tfDy.getText());
                logic.applyToSelected(EditorLogic.Mat3.translation(dx, dy));
                refreshSelected();
                setStatus("Przesunięto: dx=" + fmt(dx) + ", dy=" + fmt(dy));
            } catch (Exception ex) { setStatus("Błędne dx/dy."); }
        });
        VBox moveBox = panel("Przesunięcie (pola):",
                new HBox(8, labeled("dx", tfDx), labeled("dy", tfDy)),
                applyMove
        );

        tfPivotX = tf(String.valueOf((int) logic.pivotX()));
        tfPivotY = tf(String.valueOf((int) logic.pivotY()));
        Button setPivot = btn("Ustaw pivot (pola)", () -> {
            try {
                logic.setPivot(parseD(tfPivotX.getText()), parseD(tfPivotY.getText()));
                updatePivotMarker();
                setStatus("Pivot ustawiony z pól.");
            } catch (Exception ex) { setStatus("Błędne px/py."); }
        });

        tfAngle = tf("15");
        Button applyRot = btn("Obróć", () -> {
            if (logic.selected() == null) { setStatus("Zaznacz figurę."); return; }
            try {
                logic.setPivot(parseD(tfPivotX.getText()), parseD(tfPivotY.getText()));
                updatePivotMarker();
                double ang = parseD(tfAngle.getText());
                logic.applyToSelected(EditorLogic.Mat3.rotationDegAround(ang, logic.pivotX(), logic.pivotY()));
                refreshSelected();
                setStatus("Obrót: " + fmt(ang) + "°");
            } catch (Exception ex) { setStatus("Błędny pivot/kąt."); }
        });

        tfScale = tf("1.1");
        Button applyScale = btn("Skaluj", () -> {
            if (logic.selected() == null) { setStatus("Zaznacz figurę."); return; }
            try {
                logic.setPivot(parseD(tfPivotX.getText()), parseD(tfPivotY.getText()));
                updatePivotMarker();
                double k = parseD(tfScale.getText());
                logic.applyToSelected(EditorLogic.Mat3.scaleAround(k, logic.pivotX(), logic.pivotY()));
                refreshSelected();
                setStatus("Skalowanie: k=" + fmt(k));
            } catch (Exception ex) { setStatus("Błędny pivot/k."); }
        });

        VBox trsBox = panel("Pivot / Obrót / Skala:",
                new HBox(8, labeled("px", tfPivotX), labeled("py", tfPivotY)),
                setPivot,
                new Separator(),
                labeled("kąt (°)", tfAngle), applyRot,
                new Separator(),
                labeled("k", tfScale), applyScale
        );

        Button saveBtn = btn("Zapisz...", () -> doSave(stage));
        Button loadBtn = btn("Wczytaj...", () -> doLoad(stage));
        Button clearBtn = btn("Wyczyść (Nowy)", () -> { logic.clear(); rerenderAll(); setStatus("Nowa scena."); });

        VBox fileBox = panel("Zapis / Wczyt:", saveBtn, loadBtn, clearBtn);

        VBox left = new VBox(10, modeBox, addBox, moveBox, trsBox, fileBox);
        left.setPadding(new Insets(10));
        left.setPrefWidth(360);
        left.setStyle("-fx-background-color:#0b1220;");
        for (Node n : left.lookupAll(".label")) if (n instanceof Label l) l.setTextFill(Color.web("#e2e8f0"));
        return left;
    }

    private ToggleButton modeBtn(String text, ToggleGroup tg, Mode m) {
        ToggleButton b = new ToggleButton(text);
        b.setToggleGroup(tg);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color:#111827;-fx-text-fill:#e2e8f0;-fx-background-radius:10;");
        b.setOnAction(e -> setMode(m));
        return b;
    }

    private VBox panel(String title, Node... content) {
        Label l = new Label(title);
        l.setTextFill(Color.web("#e2e8f0"));
        VBox box = new VBox(8);
        box.getChildren().add(l);
        box.getChildren().addAll(content);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color:#0b1324;-fx-background-radius:14;-fx-border-radius:14;" +
                "-fx-border-color:rgba(148,163,184,0.25);-fx-border-width:1;");
        return box;
    }

    private Button btn(String text, Runnable r) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> r.run());
        b.setStyle("-fx-background-radius:12;-fx-background-color:#1f2937;-fx-text-fill:#e2e8f0;");
        return b;
    }

    private TextField tf(String initial) {
        TextField t = new TextField(initial);
        t.setStyle("-fx-background-color:#0f172a;-fx-text-fill:#e2e8f0;-fx-background-radius:10;" +
                "-fx-border-radius:10;-fx-border-color:rgba(148,163,184,0.25);");
        return t;
    }

    private Node labeled(String lbl, TextField tf) {
        Label l = new Label(lbl);
        l.setTextFill(Color.web("#e2e8f0"));
        return new VBox(4, l, tf);
    }

    // ===== Handlers =====

    private void installHandlers(Scene scene) {
        canvas.setOnMouseMoved(e -> {
            if (mode == Mode.DRAW && !drawingPts.isEmpty()) {
                updateDrawingPreview(e.getX(), e.getY());
            }
        });

        canvas.setOnMouseClicked(e -> {
            String pickedId = pickId(e.getPickResult().getIntersectedNode());

            if (mode == Mode.DRAW) {
                if (e.getButton() == MouseButton.PRIMARY) {
                    drawingPts.add(new EditorLogic.Point2(e.getX(), e.getY()));
                    updateDrawingPreview(e.getX(), e.getY());
                    if (e.getClickCount() == 2) finishDrawing();
                } else if (e.getButton() == MouseButton.SECONDARY) {
                    finishDrawing();
                }
                e.consume();
                return;
            }

            if (mode == Mode.SET_PIVOT) {
                logic.setPivot(e.getX(), e.getY());
                tfPivotX.setText(fmt(logic.pivotX()));
                tfPivotY.setText(fmt(logic.pivotY()));
                updatePivotMarker();
                setStatus("Pivot ustawiony myszą.");
                e.consume();
                return;
            }

            // select
            select(pickedId);
        });

        canvas.setOnMousePressed(e -> {
            String pickedId = pickId(e.getPickResult().getIntersectedNode());
            if (pickedId != null) select(pickedId);

            if (logic.selected() == null) return;

            if (mode == Mode.MOVE) {
                dragging = true;
                lastX = e.getX(); lastY = e.getY();
                canvas.setCursor(Cursor.MOVE);
            } else if (mode == Mode.ROTATE) {
                dragging = true;
                lastAngleRad = Math.atan2(e.getY() - logic.pivotY(), e.getX() - logic.pivotX());
                canvas.setCursor(Cursor.CROSSHAIR);
            } else if (mode == Mode.SCALE) {
                dragging = true;
                lastDist = dist(e.getX(), e.getY(), logic.pivotX(), logic.pivotY());
                if (lastDist < 1e-6) lastDist = 1e-6;
                canvas.setCursor(Cursor.NE_RESIZE);
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (!dragging || logic.selected() == null) return;

            if (mode == Mode.MOVE) {
                double dx = e.getX() - lastX;
                double dy = e.getY() - lastY;
                lastX = e.getX(); lastY = e.getY();

                logic.applyToSelected(EditorLogic.Mat3.translation(dx, dy));
                refreshSelected();
                setStatus("MOVE myszą: dx=" + fmt(dx) + ", dy=" + fmt(dy));
            } else if (mode == Mode.ROTATE) {
                double ang = Math.atan2(e.getY() - logic.pivotY(), e.getX() - logic.pivotX());
                double delta = ang - lastAngleRad;
                lastAngleRad = ang;

                double deltaDeg = Math.toDegrees(delta);
                logic.applyToSelected(EditorLogic.Mat3.rotationDegAround(deltaDeg, logic.pivotX(), logic.pivotY()));
                refreshSelected();
                setStatus("ROTATE myszą: Δ=" + fmt(deltaDeg) + "°");
            } else if (mode == Mode.SCALE) {
                double d = dist(e.getX(), e.getY(), logic.pivotX(), logic.pivotY());
                if (d < 1e-6) d = 1e-6;

                double k = d / lastDist;
                lastDist = d;

                logic.applyToSelected(EditorLogic.Mat3.scaleAround(k, logic.pivotX(), logic.pivotY()));
                refreshSelected();
                setStatus("SCALE myszą: k=" + fmt(k));
            }
        });

        canvas.setOnMouseReleased(e -> {
            dragging = false;
            canvas.setCursor(Cursor.DEFAULT);
        });

        scene.setOnKeyPressed(e -> {
            if (mode == Mode.DRAW) {
                if (e.getCode() == KeyCode.ENTER) finishDrawing();
                if (e.getCode() == KeyCode.ESCAPE) cancelDrawing();
            }
        });
    }

    // ===== Rysowanie / render =====

    private void rerenderAll() {
        nodes.clear();
        canvas.getChildren().clear();

        for (EditorLogic.Poly p : logic.polys()) {
            Polygon node = new Polygon();
            node.setUserData(p.id);
            applyModelToNode(p, node);
            nodes.put(p.id, node);
            canvas.getChildren().add(node);
        }

        // pivot + preview na wierzchu
        updatePivotMarker();
        canvas.getChildren().addAll(pivotMarker, drawingPreview);
        highlightSelection();
    }

    private void refreshSelected() {
        EditorLogic.Poly p = logic.selected();
        if (p == null) return;
        Polygon node = nodes.get(p.id);
        if (node == null) return;
        applyModelToNode(p, node);
        highlightSelection();
    }

    private void applyModelToNode(EditorLogic.Poly m, Polygon node) {
        List<Double> pts = new ArrayList<>(m.pts.size() * 2);
        for (EditorLogic.Point2 p : m.pts) { pts.add(p.x); pts.add(p.y); }
        node.getPoints().setAll(pts);

        node.setFill(Color.web(m.fillHex).deriveColor(0, 1, 1, 0.35));
        node.setStroke(Color.web(m.strokeHex));
        node.setStrokeWidth(2.0);
    }

    private void highlightSelection() {
        for (Polygon p : nodes.values()) {
            p.setStroke(Color.web("#e2e8f0"));
            p.setStrokeWidth(2.0);
        }
        if (logic.selectedId() != null) {
            Polygon sel = nodes.get(logic.selectedId());
            if (sel != null) {
                sel.setStroke(Color.web("#22c55e"));
                sel.setStrokeWidth(4.0);
            }
        }
    }

    private void updatePivotMarker() {
        pivotMarker.setCenterX(logic.pivotX());
        pivotMarker.setCenterY(logic.pivotY());
        pivotMarker.toFront();
        drawingPreview.toFront();
    }

    private void updateDrawingPreview(double mx, double my) {
        drawingPreview.getPoints().clear();
        for (EditorLogic.Point2 p : drawingPts) drawingPreview.getPoints().addAll(p.x, p.y);
        drawingPreview.getPoints().addAll(mx, my);
        drawingPreview.toFront();
    }

    private void finishDrawing() {
        if (drawingPts.size() < 3) { setStatus("DRAW: min 3 punkty."); return; }
        try {
            String id = logic.addPolygon(new ArrayList<>(drawingPts));
            drawingPts.clear();
            drawingPreview.getPoints().clear();
            rerenderAll();
            select(id);
            setStatus("Dodano " + id + " (myszą).");
        } catch (Exception ex) {
            setStatus("Nie udało się dodać wielokąta.");
        }
    }

    private void cancelDrawing() {
        drawingPts.clear();
        drawingPreview.getPoints().clear();
        setStatus("DRAW: anulowano.");
    }

    private void select(String id) {
        logic.select(id);
        highlightSelection();
        setStatus(id == null ? "Brak zaznaczenia." : ("Zaznaczono: " + id));
    }

    // ===== Pliki =====

    private void doSave(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Zapisz scenę");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Polygon Scene (*.polys)", "*.polys"));
        File f = fc.showSaveDialog(stage);
        if (f == null) return;

        try {
            logic.save(f);
            setStatus("Zapisano: " + f.getName());
        } catch (Exception ex) {
            setStatus("Błąd zapisu: " + ex.getMessage());
        }
    }

    private void doLoad(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Wczytaj scenę");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Polygon Scene (*.polys)", "*.polys"));
        File f = fc.showOpenDialog(stage);
        if (f == null) return;

        try {
            logic.load(f);
            tfPivotX.setText(fmt(logic.pivotX()));
            tfPivotY.setText(fmt(logic.pivotY()));
            drawingPts.clear();
            drawingPreview.getPoints().clear();
            rerenderAll();
            setStatus("Wczytano: " + f.getName());
        } catch (Exception ex) {
            setStatus("Błąd wczytu: " + ex.getMessage());
        }
    }

    // ===== Utils =====

    private void setMode(Mode m) {
        mode = m;
        if (mode != Mode.DRAW) { drawingPts.clear(); drawingPreview.getPoints().clear(); }

        String hint = switch (mode) {
            case SELECT -> "kliknij figurę.";
            case DRAW -> "klikaj punkty; PPM/double/Enter kończy; ESC anuluje.";
            case MOVE -> "kliknij figurę i przeciągnij (lub dx/dy).";
            case ROTATE -> "ustaw pivot i przeciągnij (lub kąt w polu).";
            case SCALE -> "ustaw pivot i przeciągnij (lub k w polu).";
            case SET_PIVOT -> "kliknij na płótnie, aby ustawić pivot.";
        };
        setStatus("Tryb: " + mode + " — " + hint);
    }

    private String pickId(Node n) {
        if (n instanceof Polygon p) {
            Object ud = p.getUserData();
            return (ud instanceof String s) ? s : null;
        }
        return null;
    }

    private static double parseD(String s) { return Double.parseDouble(s.trim().replace(',', '.')); }
    private static String fmt(double v) { return String.format(Locale.US, "%.2f", v); }

    private static double dist(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2, dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private void setStatus(String text) {
        status.setText(text);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
