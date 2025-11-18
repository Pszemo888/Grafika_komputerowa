package com.example.proj_graf03;

import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * Panel wyświetlający kostkę RGB w 3D z możliwością obracania oraz podglądem przekroju.
 */
public class RgbCubePane extends BorderPane {

    private final Rotate rotateX = new Rotate(-20, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(-30, Rotate.Y_AXIS);
    private double lastX;
    private double lastY;

    private final Canvas sliceCanvas = new Canvas(260, 260);
    private final Slider sliceSlider = new Slider(0, 1, 0.5);
    private final ChoiceBox<String> axisChoice = new ChoiceBox<>();

    public RgbCubePane() {
        setPadding(new Insets(20));

        SubScene cubeScene = createCubeScene();

        VBox controls = buildControls();
        setCenter(cubeScene);
        setRight(controls);
        setMargin(controls, new Insets(0, 0, 0, 16));

        drawSlice();
    }

    private SubScene createCubeScene() {
        double size = 250;
        Group cube = new Group();
        cube.getChildren().addAll(
                createFace(size, new Point3D(0, 0, size / 2), 0, 0, 0, FaceType.FRONT),
                createFace(size, new Point3D(0, 0, -size / 2), 0, 180, 0, FaceType.BACK),
                createFace(size, new Point3D(-size / 2, 0, 0), 0, -90, 0, FaceType.LEFT),
                createFace(size, new Point3D(size / 2, 0, 0), 0, 90, 0, FaceType.RIGHT),
                createFace(size, new Point3D(0, -size / 2, 0), 90, 0, 0, FaceType.TOP),
                createFace(size, new Point3D(0, size / 2, 0), -90, 0, 0, FaceType.BOTTOM)
        );

        Group root3d = new Group();
        root3d.getChildren().add(cube);
        cube.getTransforms().addAll(rotateX, rotateY);

        javafx.scene.PointLight light = new javafx.scene.PointLight(Color.WHITE);
        light.getTransforms().add(new Translate(-400, -300, -300));
        root3d.getChildren().add(light);

        SubScene subScene = new SubScene(root3d, 600, 600, true, javafx.scene.SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#f7f7f7"));
        subScene.setCamera(new javafx.scene.PerspectiveCamera(true));

        subScene.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onMousePressed);
        subScene.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged);
        return subScene;
    }

    private MeshView createFace(double size, Point3D translation, double rotX, double rotY, double rotZ, FaceType type) {
        TriangleMesh mesh = new TriangleMesh();

        float half = (float) (size / 2.0);
        mesh.getPoints().addAll(
                -half, -half, 0,
                half, -half, 0,
                half, half, 0,
                -half, half, 0
        );

        mesh.getTexCoords().addAll(
                0, 0,
                1, 0,
                1, 1,
                0, 1
        );

        mesh.getFaces().addAll(
                0, 0, 1, 1, 2, 2,
                2, 2, 3, 3, 0, 0
        );

        MeshView view = new MeshView(mesh);
        view.setMaterial(createMaterialForFace(type));
        view.setCullFace(javafx.scene.shape.CullFace.BACK);
        view.setDrawMode(javafx.scene.shape.DrawMode.FILL);

        view.getTransforms().addAll(new Rotate(rotX, Rotate.X_AXIS), new Rotate(rotY, Rotate.Y_AXIS), new Rotate(rotZ, Rotate.Z_AXIS),
                new Translate(translation.getX(), translation.getY(), translation.getZ()));
        return view;
    }

    private PhongMaterial createMaterialForFace(FaceType type) {
        int size = 256;
        javafx.scene.image.WritableImage texture = new javafx.scene.image.WritableImage(size, size);
        javafx.scene.image.PixelWriter writer = texture.getPixelWriter();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double u = x / (double) (size - 1);
                double v = y / (double) (size - 1);
                Color color = switch (type) {
                    case FRONT -> Color.color(u, 1 - v, 1);
                    case BACK -> Color.color(u, 1 - v, 0);
                    case LEFT -> Color.color(0, 1 - v, u);
                    case RIGHT -> Color.color(1, 1 - v, u);
                    case TOP -> Color.color(u, 1, u);
                    case BOTTOM -> Color.color(u, 0, u);
                };
                writer.setColor(x, y, color);
            }
        }
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(texture);
        return material;
    }

    private VBox buildControls() {
        VBox container = new VBox(14);
        container.setPadding(new Insets(10));
        container.setAlignment(Pos.TOP_LEFT);
        container.setPrefWidth(320);

        Label title = new Label("Przekrój kostki RGB");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        axisChoice.getItems().addAll("B (oś Z)", "G (oś Y)", "R (oś X)");
        axisChoice.getSelectionModel().selectFirst();
        axisChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> drawSlice());

        sliceSlider.setShowTickLabels(true);
        sliceSlider.setShowTickMarks(true);
        sliceSlider.setBlockIncrement(0.05);
        sliceSlider.valueProperty().addListener((obs, oldV, newV) -> drawSlice());

        HBox sliderBox = new HBox(10, new Label("Poziom:"), sliceSlider);
        HBox.setHgrow(sliceSlider, Priority.ALWAYS);

        Label hint = new Label("Przeciągaj myszą, aby obracać kostkę.\n"
                + "Zmiana suwaka pokazuje przekrój dla wybranej osi.");
        hint.setWrapText(true);

        container.getChildren().addAll(title, axisChoice, sliderBox, sliceCanvas, hint);
        return container;
    }

    private void drawSlice() {
        GraphicsContext gc = sliceCanvas.getGraphicsContext2D();
        double level = sliceSlider.getValue();
        String axis = axisChoice.getSelectionModel().getSelectedItem();
        int size = (int) sliceCanvas.getWidth();

        javafx.scene.image.PixelWriter writer = gc.getPixelWriter();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double r = x / (double) (size - 1);
                double g = 1 - y / (double) (size - 1);
                double b = level;

                if (axis.startsWith("G")) {
                    g = level;
                    b = x / (double) (size - 1);
                } else if (axis.startsWith("R")) {
                    r = level;
                    b = x / (double) (size - 1);
                    g = 1 - y / (double) (size - 1);
                }
                writer.setColor(x, y, Color.color(r, g, b));
            }
        }
        gc.setStroke(Color.DARKGRAY);
        gc.strokeRect(0.5, 0.5, size - 1, size - 1);
    }

    private void onMousePressed(MouseEvent event) {
        lastX = event.getSceneX();
        lastY = event.getSceneY();
    }

    private void onMouseDragged(MouseEvent event) {
        double deltaX = event.getSceneX() - lastX;
        double deltaY = event.getSceneY() - lastY;
        rotateY.setAngle(rotateY.getAngle() + deltaX * 0.4);
        rotateX.setAngle(rotateX.getAngle() - deltaY * 0.4);
        lastX = event.getSceneX();
        lastY = event.getSceneY();
    }

    private enum FaceType {
        FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM
    }
}
