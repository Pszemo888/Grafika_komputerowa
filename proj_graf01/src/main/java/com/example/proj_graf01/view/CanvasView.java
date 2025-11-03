package com.example.proj_graf01.view;

import com.example.proj_graf01.model.ShapeBase;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class CanvasView {
    public static final double HANDLE = 8;
    public int selectedIndex = -1;
    public ShapeBase draft = null;

    private final Canvas canvas = new Canvas(1000, 700);
    private final GraphicsContext g = canvas.getGraphicsContext2D();
    private List<ShapeBase> shapesRef = new ArrayList<>();

    public Canvas getCanvas(){ return canvas; }

    public void setShapesRef(List<ShapeBase> shapes){
        this.shapesRef = shapes != null ? shapes : new ArrayList<>();
    }

    public void redraw(List<ShapeBase> shapes){
        setShapesRef(shapes);
        redraw();
    }

    public void redraw(){
        g.setFill(Color.WHITE);
        g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
        for (int i=0;i<shapesRef.size();i++){
            shapesRef.get(i).draw(g, i==selectedIndex);
        }
        if (draft != null) {
            g.setLineDashes(6,6);
            draft.drawGeometry(g);
            g.setLineDashes(null);
        }
    }
}
