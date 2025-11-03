package com.example.proj_graf01.controller;

import com.example.proj_graf01.model.*;
import com.example.proj_graf01.view.CanvasView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class EditorController {
    public enum Tool { SELECT, DRAW_LINE, DRAW_RECT, DRAW_CIRCLE }

    //lista narysowanych figur
    private final List<ShapeBase> shapes = new ArrayList<>();
    // referencja do widoku
    private final CanvasView view;

    //aktualnie wybrany tryb pracy
    private Tool tool = Tool.SELECT;

    //zaznaczona figura, -1 brak zaznaczenia
    private int selectedIndex = -1;

    // panel właściwości
    private TextField fx1, fy1, fx2, fy2, fStroke, fStrokeW, fFill;
    private Button bApply;

    // przeciaganie
    private boolean draggingWhole = false;


    private int draggingHandleIndex = -1;

    //ostatnia pozycja myszy do liczenia wektora  przesuniecia dx/dy
    private double lastX, lastY;
    //tolerancja trafienia
    private static final double PICK_TOL = 6;

    //obkiet gson do zapisu/odczytu JSON
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public EditorController(CanvasView view) {
        this.view = view;
        this.view.setShapesRef(shapes); // widok zna listę figur, musi je znac żeby móc je narysować
    }


    public void bindToolbar(ToggleButton tSelect, ToggleButton tLine, ToggleButton tRect, ToggleButton tCircle, Button bDelete) {
        tSelect.setOnAction(e -> setTool(Tool.SELECT));
        tLine.setOnAction(e -> setTool(Tool.DRAW_LINE));
        tRect.setOnAction(e -> setTool(Tool.DRAW_RECT));
        tCircle.setOnAction(e -> setTool(Tool.DRAW_CIRCLE));
        bDelete.setOnAction(e -> deleteSelection());
    }

    public void bindProperties(TextField fx1, TextField fy1, TextField fx2, TextField fy2,
                               TextField fStroke, TextField fStrokeW, TextField fFill,
                               Button bApply) {
        this.fx1=fx1; this.fy1=fy1; this.fx2=fx2; this.fy2=fy2;
        this.fStroke=fStroke; this.fStrokeW=fStrokeW; this.fFill=fFill;
        this.bApply=bApply;
        this.bApply.setOnAction(e -> applyFieldsToSelection());
    }

    public void installMouseAndKeys(Scene scene) {
        var canvas = view.getCanvas();


        canvas.setOnMousePressed(e -> {
            lastX = e.getX();
            lastY = e.getY();

            if (tool == Tool.SELECT) {
                int h = hitHandle(lastX, lastY);
                if (h >= 0) {
                    draggingHandleIndex = h;
                    draggingWhole = false;
                    return;
                }

                int hit = hitShape(lastX, lastY);
                if (hit >= 0) {
                    selectedIndex = hit;
                    view.selectedIndex = hit;
                    draggingWhole = true;
                    draggingHandleIndex = -1;
                    syncFieldsFromSelection();
                    view.redraw(shapes);
                } else {
                    selectedIndex = -1;
                    view.selectedIndex = -1;
                    draggingWhole = false;
                    draggingHandleIndex = -1;
                    syncFieldsFromSelection();
                    view.redraw(shapes);
                }

            } else {
                view.draft = newDraft();

                view.draft.ax = lastX;
                view.draft.ay = lastY;

                view.draft.bx = lastX;
                view.draft.by = lastY;


                view.redraw(shapes);
            }
        });


        canvas.setOnMouseDragged(e -> {
            double x = e.getX();
            double y = e.getY();

            if (tool == Tool.SELECT) {
                // Edytujemy istniejącą figurę (tylko jeśli jakaś jest zaznaczona)
                if (selectedIndex < 0) return;
                ShapeBase s = shapes.get(selectedIndex);

                if (draggingWhole) {
                    // Przesuwamy CAŁĄ figurę – liczymy różnicę pozycji
                    double dx = x - lastX;
                    double dy = y - lastY;

                    // Przesunięcie obiektu (zmienia ax/ay/bx/by w modelu)
                    s.moveBy(dx, dy);

                    // Aktualizujemy punkt odniesienia dragowania
                    lastX = x;
                    lastY = y;
                    view.redraw(shapes);

                } else if (draggingHandleIndex >= 0) {


                    s.applyHandleDrag(draggingHandleIndex, x, y);
                    view.redraw(shapes);
                }

            } else if (view.draft != null) {
                view.draft.bx = x;
                view.draft.by = y;

                view.redraw(shapes);
            }
        });

        canvas.setOnMouseReleased(e -> {
            if (tool == Tool.SELECT) {

                draggingWhole = false;
                draggingHandleIndex = -1;

            } else if (view.draft != null) {
                shapes.add(view.draft);

                selectedIndex = shapes.size()-1;
                view.selectedIndex = selectedIndex;

                view.draft = null;

                syncFieldsFromSelection();
                view.redraw(shapes);
                setTool(Tool.SELECT);
            }
        });

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE && selectedIndex >= 0) {
                deleteSelection();
            }
        });
    }

    public void setTool(Tool t) { this.tool = t; }

    public void deleteSelection() {
        if (selectedIndex >= 0) {
            shapes.remove(selectedIndex);
            selectedIndex = -1; view.selectedIndex = -1;
            view.redraw(shapes); syncFieldsFromSelection();
        }
    }

    public void refreshPropertyPanelEnabled() {
        boolean has = selectedIndex >= 0;
        if (fx1==null) return;
        fx1.setDisable(!has); fy1.setDisable(!has); fx2.setDisable(!has); fy2.setDisable(!has);
        fStroke.setDisable(!has); fStrokeW.setDisable(!has); fFill.setDisable(!has);
        bApply.setDisable(!has);
    }


    private void syncFieldsFromSelection() {
        refreshPropertyPanelEnabled();
        if (selectedIndex < 0) { clearFields(); return; }
        ShapeBase s = shapes.get(selectedIndex);
        fx1.setText(fmt(s.ax)); fy1.setText(fmt(s.ay));
        fx2.setText(fmt(s.bx)); fy2.setText(fmt(s.by));
        fStroke.setText(s.stroke);
        fStrokeW.setText(fmt(s.strokeWidth));
        fFill.setText(s.fill);
    }


    private void clearFields(){
        if (fx1!=null){fx1.clear();fy1.clear();fx2.clear();fy2.clear();}
    }


    private void applyFieldsToSelection() {
        if (selectedIndex < 0) return;
        ShapeBase s = shapes.get(selectedIndex);
        try {
            s.ax = Double.parseDouble(fx1.getText());
            s.ay = Double.parseDouble(fy1.getText());
            s.bx = Double.parseDouble(fx2.getText());
            s.by = Double.parseDouble(fy2.getText());
            s.stroke = fStroke.getText();
            s.strokeWidth = Double.parseDouble(fStrokeW.getText());
            s.fill = fFill.getText();
            view.redraw(shapes);
        } catch (NumberFormatException ignored) {}
    }


    private int hitShape(double x, double y) {
        for (int i = shapes.size()-1; i >= 0; i--) {
            if (shapes.get(i).hit(x, y, PICK_TOL)) return i;
        }
        return -1;
    }


    private int hitHandle(double x, double y) {
        if (selectedIndex < 0) return -1;
        double[][] hs = shapes.get(selectedIndex).handles();
        for (int i=0; i<hs.length; i++) {
            if (Math.abs(x - hs[i][0]) <= CanvasView.HANDLE/2 && Math.abs(y - hs[i][1]) <= CanvasView.HANDLE/2) return i;
        }
        return -1;
    }


    private ShapeBase newDraft() {
        return switch (tool) {
            case DRAW_LINE   -> new LineShape();
            case DRAW_RECT   -> new RectShape();
            case DRAW_CIRCLE -> new CircleShape();
            default -> null;
        };
    }

    // reprezentacja figury do zapisania do JSON
    private record ShapeDTO(String type, String id, double ax, double ay, double bx, double by,
                            double strokeWidth, String stroke, String fill) {}


    public void saveJson(File f) {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            List<ShapeDTO> out = new ArrayList<>();
            for (ShapeBase s : shapes) {
                out.add(new ShapeDTO(s.type().name(), s.id, s.ax, s.ay, s.bx, s.by, s.strokeWidth, s.stroke, s.fill));
            }
            gson.toJson(out, w);
        } catch (IOException ex) { showErr("Błąd zapisu: "+ex.getMessage()); }
    }


    public void loadJson(File f) {
        try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {

            ShapeDTO[] arr = gson.fromJson(r, ShapeDTO[].class);

            // Reset całego stanu edytora
            shapes.clear();
            selectedIndex = -1;
            view.selectedIndex = -1;
            view.draft = null;

            // Jeśli coś było w pliku
            if (arr != null) {
                for (ShapeDTO d : arr) {
                    // Odtwórz prawdziwy typ figury
                    ShapeBase s = switch (ShapeType.valueOf(d.type)) {
                        case LINE   -> new LineShape();
                        case RECT   -> new RectShape();
                        case CIRCLE -> new CircleShape();
                    };

                    // Przywróć wszystkie pola figury
                    s.id = (d.id != null) ? d.id : java.util.UUID.randomUUID().toString();
                    s.ax = d.ax; s.ay = d.ay;
                    s.bx = d.bx; s.by = d.by;
                    s.strokeWidth = d.strokeWidth;
                    s.stroke = d.stroke;
                    s.fill = d.fill;

                    shapes.add(s);
                }
            }


            view.redraw(shapes);
            syncFieldsFromSelection();

        } catch (IOException | IllegalArgumentException ex) {
            showErr("Błąd odczytu: " + ex.getMessage());
        }
    }


    private static void showErr(String m){ new Alert(Alert.AlertType.ERROR, m, ButtonType.OK).showAndWait(); }
    /** Formatowanie liczby do 2 miejsc po przecinku (albo "0" dla bardzo małych). */
    private static String fmt(double v){ return (Math.abs(v) < 1e-9) ? "0" : String.format(java.util.Locale.US, "%.2f", v); }
}
