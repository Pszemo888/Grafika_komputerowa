package com.example.proj_graf06;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Main extends Application {

    // Główny obszar rysowania
    private Pane drawingPane;

    // Polilinia łącząca punkty kontrolne (wielokąt kontrolny)
    private Polyline controlPolygon;

    // Polilinia przybliżająca krzywą Béziera
    private Polyline bezierCurve;

    // Panel z polami tekstowymi do edycji punktów
    private VBox pointsBox;

    // Etykieta pokazująca stopień krzywej
    private Label degreeLabel;

    // Spinner z liczbą punktów na krzywej (k)
    private Spinner<Integer> samplesSpinner;

    // Lista punktów kontrolnych
    private final List<ControlPoint> controlPoints = new ArrayList<>();

    public static void main(String[] args) {
        // Na wszelki wypadek wymuszamy kropkę jako separator dziesiętny
        Locale.setDefault(Locale.US);
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        // --- ŚRODEK: obszar rysowania ---
        drawingPane = new Pane();
        drawingPane.setPrefSize(800, 600);
        drawingPane.setStyle("-fx-background-color: white; -fx-border-color: lightgray;");

        controlPolygon = new Polyline();
        controlPolygon.setStroke(Color.GRAY);
        controlPolygon.setStrokeWidth(1.0);
        controlPolygon.getStrokeDashArray().addAll(5.0, 5.0);
        controlPolygon.setFill(null);

        bezierCurve = new Polyline();
        bezierCurve.setStroke(Color.RED);
        bezierCurve.setStrokeWidth(2.0);
        bezierCurve.setFill(null);

        drawingPane.getChildren().addAll(controlPolygon, bezierCurve);

        // Dodawanie nowych punktów kontrolnych kliknięciem w pusty obszar
        drawingPane.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getTarget() == drawingPane) {
                addControlPoint(event.getX(), event.getY());
            }
        });

        root.setCenter(drawingPane);

        // --- PRAWY PANEL: lista punktów kontrolnych i ich pola tekstowe ---
        pointsBox = new VBox(5);
        pointsBox.setPadding(new Insets(5));

        Label pointsLabel = new Label("Punkty kontrolne (Pi):");
        pointsLabel.setStyle("-fx-font-weight: bold;");

        Button addPointButton = new Button("Dodaj punkt");
        addPointButton.setOnAction(e -> {
            // Dodaje punkt w środku obszaru rysowania
            double x = drawingPane.getWidth() / 2.0;
            double y = drawingPane.getHeight() / 2.0;
            addControlPoint(x, y);
        });

        VBox rightContent = new VBox(10, pointsLabel, addPointButton, pointsBox);
        rightContent.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(rightContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefWidth(320);

        root.setRight(scrollPane);

        // --- GÓRA: sterowanie krzywą (stopień, k, reset) ---
        degreeLabel = new Label("Stopień n = 0 (liczba punktów = 0)");

        Label kLabel = new Label("Liczba punktów na krzywej (k):");
        samplesSpinner = new Spinner<>();
        samplesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 2000, 200));
        samplesSpinner.setEditable(true);
        samplesSpinner.valueProperty().addListener((obs, oldV, newV) -> redraw());

        Button clearButton = new Button("Wyczyść");
        clearButton.setOnAction(e -> {
            controlPoints.clear();
            pointsBox.getChildren().clear();
            refreshPointLabels();
            redraw();
        });

        HBox topBar = new HBox(10, degreeLabel, kLabel, samplesSpinner, clearButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10));
        root.setTop(topBar);

        // --- Opcjonalnie: 3 domyślne punkty, żeby od razu coś zobaczyć ---
        addControlPoint(150, 450);
        addControlPoint(400, 100);
        addControlPoint(650, 450);

        Scene scene = new Scene(root);
        primaryStage.setTitle("Krzywa Béziera – JavaFX");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Po pokazaniu sceny możemy poprawić początkowe rysowanie
        redraw();
    }

    /**
     * Dodaje nowy punkt kontrolny do listy + UI.
     */
    private void addControlPoint(double x, double y) {
        ControlPoint cp = new ControlPoint(x, y);
        controlPoints.add(cp);

        pointsBox.getChildren().add(cp.getRow());
        drawingPane.getChildren().add(cp.getCircle());

        refreshPointLabels();
        redraw();
    }

    /**
     * Usuwa punkt kontrolny z listy + UI.
     */
    private void removeControlPoint(ControlPoint cp) {
        controlPoints.remove(cp);
        pointsBox.getChildren().remove(cp.getRow());
        drawingPane.getChildren().remove(cp.getCircle());

        refreshPointLabels();
        redraw();
    }

    /**
     * Aktualizacja etykiet P0, P1, ..., oraz stopnia n.
     */
    private void refreshPointLabels() {
        for (int i = 0; i < controlPoints.size(); i++) {
            controlPoints.get(i).setIndex(i);
        }
        int n = controlPoints.isEmpty() ? 0 : controlPoints.size() - 1;
        degreeLabel.setText("Stopień n = " + n + " (liczba punktów = " + controlPoints.size() + ")");
    }

    /**
     * Rysuje wielokąt kontrolny i krzywą Béziera.
     * Wyliczanie punktów B_j wg wzoru:
     *
     * B(t) = Σ_{i=0..n} C(n,i) (1 - t)^(n-i) t^i P_i
     * gdzie t = j/(k-1), j=0..k-1
     */
    private void redraw() {
        // Najpierw wielokąt kontrolny
        controlPolygon.getPoints().clear();
        for (ControlPoint cp : controlPoints) {
            controlPolygon.getPoints().addAll(cp.getX(), cp.getY());
        }

        // Krzywa Béziera
        bezierCurve.getPoints().clear();

        if (controlPoints.size() < 2) {
            // Za mało punktów, żeby narysować sensowną krzywą
            return;
        }

        int n = controlPoints.size() - 1;
        int k = samplesSpinner.getValue();
        if (k < 2) {
            k = 2;
        }

        double[] binom = computeBinomialCoefficients(n);

        for (int j = 0; j < k; j++) {
            double t = (double) j / (k - 1);
            double oneMinusT = 1.0 - t;

            double x = 0.0;
            double y = 0.0;

            for (int i = 0; i <= n; i++) {
                double basis = binom[i] * Math.pow(oneMinusT, n - i) * Math.pow(t, i);
                x += basis * controlPoints.get(i).getX();
                y += basis * controlPoints.get(i).getY();
            }

            bezierCurve.getPoints().addAll(x, y);
        }
    }

    /**
     * Oblicza współczynniki dwumianowe C(n, i) dla i=0..n.
     */
    private double[] computeBinomialCoefficients(int n) {
        double[] c = new double[n + 1];
        c[0] = 1.0;
        for (int i = 1; i <= n; i++) {
            c[i] = c[i - 1] * (n - (i - 1)) / i;
        }
        return c;
    }

    /**
     * Bezpieczny parser double: obsługuje polskie przecinki.
     */
    private Double parseDoubleSafe(String text, Double fallback) {
        if (text == null || text.trim().isEmpty()) {
            return fallback;
        }
        try {
            text = text.trim().replace(',', '.');
            return Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    /**
     * Klasa reprezentująca pojedynczy punkt kontrolny Pi:
     * - Circle na płótnie,
     * - wiersz z polami tekstowymi x, y i przyciskiem Usuń.
     */
    private class ControlPoint {
        private double x;
        private double y;

        private final Circle circle;
        private final TextField xField;
        private final TextField yField;
        private final Label nameLabel;
        private final HBox row;

        ControlPoint(double x, double y) {
            this.x = x;
            this.y = y;

            circle = new Circle(x, y, 5);
            circle.setFill(Color.DODGERBLUE);
            circle.setStroke(Color.BLACK);

            xField = new TextField(String.format(Locale.US, "%.1f", x));
            xField.setPrefColumnCount(5);

            yField = new TextField(String.format(Locale.US, "%.1f", y));
            yField.setPrefColumnCount(5);

            nameLabel = new Label("P?");
            nameLabel.setMinWidth(30);

            Button deleteButton = new Button("Usuń");
            deleteButton.setOnAction(e -> removeControlPoint(this));

            row = new HBox(5,
                    nameLabel,
                    new Label("x:"), xField,
                    new Label("y:"), yField,
                    deleteButton
            );
            row.setAlignment(Pos.CENTER_LEFT);

            // Zmiana pozycji z pól tekstowych
            xField.setOnAction(e -> updateFromTextFields());
            yField.setOnAction(e -> updateFromTextFields());

            xField.focusedProperty().addListener((obs, oldV, newV) -> {
                if (!newV) updateFromTextFields();
            });
            yField.focusedProperty().addListener((obs, oldV, newV) -> {
                if (!newV) updateFromTextFields();
            });

            // Szybsze odświeżanie przy wpisywaniu, gdy wartość jest poprawna
            xField.setOnKeyReleased(e -> updateFromTextFieldsQuick());
            yField.setOnKeyReleased(e -> updateFromTextFieldsQuick());

            // Przeciąganie myszą – aktualizacja w czasie rzeczywistym
            circle.setOnMousePressed(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    circle.toFront();
                }
            });

            circle.setOnMouseDragged(e -> {
                if (!e.isPrimaryButtonDown()) return;

                Point2D p = drawingPane.sceneToLocal(e.getSceneX(), e.getSceneY());
                setPosition(p.getX(), p.getY());
                redraw();
            });
        }

        HBox getRow() {
            return row;
        }

        Circle getCircle() {
            return circle;
        }

        double getX() {
            return x;
        }

        double getY() {
            return y;
        }

        void setIndex(int index) {
            nameLabel.setText("P" + index);
        }

        void setPosition(double newX, double newY) {
            this.x = newX;
            this.y = newY;

            circle.setCenterX(newX);
            circle.setCenterY(newY);

            // Nie nadpisuj tekstu, gdy użytkownik aktualnie pisze
            if (!xField.isFocused()) {
                xField.setText(String.format(Locale.US, "%.1f", newX));
            }
            if (!yField.isFocused()) {
                yField.setText(String.format(Locale.US, "%.1f", newY));
            }
        }

        private void updateFromTextFields() {
            Double newX = parseDoubleSafe(xField.getText(), x);
            Double newY = parseDoubleSafe(yField.getText(), y);
            setPosition(newX, newY);
            redraw();
        }

        private void updateFromTextFieldsQuick() {
            Double newX = parseDoubleSafe(xField.getText(), null);
            Double newY = parseDoubleSafe(yField.getText(), null);
            if (newX != null && newY != null) {
                setPosition(newX, newY);
                redraw();
            }
        }
    }
}
