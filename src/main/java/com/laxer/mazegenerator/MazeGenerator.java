package com.laxer.mazegenerator;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class MazeGenerator extends Application {
    private final CopyOnWriteArrayList<MazeWall> walls = new CopyOnWriteArrayList<>();
    private final int width = 200;
    private final int height = 200;
    private int segmentCount = 0;
    @Override
    public void start(Stage stage) {
        Group root = new Group();
        Scene scene = new Scene(root);
        stage.setTitle("Maze Generator");
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.show();
        Platform.runLater(() -> stage.setFullScreen(true));

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.N) {
                walls.clear();
                new Thread(this::createMaze).start();
            }
        });

        Canvas canvas = new Canvas(scene.getWidth(), scene.getHeight());
        root.getChildren().add(canvas);
        canvas.widthProperty().bind(scene.widthProperty());
        canvas.heightProperty().bind(scene.heightProperty());

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                drawMaze(canvas);
            }
        };
        timer.start();

        new Thread(this::createMaze).start();
    }

    private void createMaze() {
        MazeSegment[][] mazeSegments = new MazeSegment[width][height];
        segmentCount = width * height;
        MazeField[] lastRow = new MazeField[width];

        for (int h = 0; h < height; h++) {
            MazeField lastField = null;
            for (int w = 0; w < width; w++) {
                MazeField newField = new MazeField(w, h);
                mazeSegments[w][h] = new MazeSegment(newField);
                if (lastField != null) {
                    walls.add(new MazeWall(lastField, newField));
                }
                if (lastRow[w] != null) {
                    walls.add(new MazeWall(lastRow[w], newField));
                }
                lastField = lastRow[w] = newField;
            }
        }

        Random rand = new Random();
        while (segmentCount > 1) {
            MazeWall wall = walls.get(rand.nextInt(walls.size()));
            MazeSegment segment1 = mazeSegments[wall.mazeField1().x()][wall.mazeField1().y()];
            MazeSegment segment2 = mazeSegments[wall.mazeField2().x()][wall.mazeField2().y()];
            if (segment1 != segment2) {
                for (MazeField mazeField : segment2.mazeFields) {
                    mazeSegments[mazeField.x()][mazeField.y()] = segment1;
                }
                segment1.mazeFields.addAll(segment2.mazeFields);
                walls.remove(wall);
                segmentCount--;
            }
        }
    }

    private void drawMaze(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double x0;
        double y0;

        double fieldSize;
        if (canvas.getWidth() > canvas.getHeight()) {
            fieldSize = canvas.getHeight() / height;
            x0 = (canvas.getWidth() - width * fieldSize) / 2;
            y0 = 0;
        } else  {
            fieldSize = canvas.getWidth() / width;
            x0 = 0;
            y0 = (canvas.getHeight() - height * fieldSize) / 2;
        }

        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());


        final int c = (int) (255 * ((double) segmentCount / (width * height)));
        gc.setStroke(Color.rgb(c, 255 - c, 0));
        gc.strokeRect(x0, y0, width * fieldSize, height * fieldSize);

        walls.forEach(w -> {
            double x = x0 + w.mazeField1().x() * fieldSize;
            double y = y0 + w.mazeField1().y() * fieldSize;

            if (w.mazeField1().y() == w.mazeField2().y()) {
                if (w.mazeField1().x() < w.mazeField2().x()) {
                    gc.strokeLine(x + fieldSize, y, x + fieldSize, y + fieldSize);
                } else {
                    gc.strokeLine(x, y, x, y + fieldSize);
                }
            } else {
                if (w.mazeField1().y() < w.mazeField2().y()) {
                    gc.strokeLine(x, y + fieldSize, x + fieldSize, y + fieldSize);
                } else {
                    gc.strokeLine(x, y, x + fieldSize, y);
                }
            }
        });
    }
}
