package com.laxer.mazegenerator;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class MazeGenerator extends Application {
    private final CopyOnWriteArrayList<MazeWall> walls = new CopyOnWriteArrayList<>();
    private final ArrayList<MazeField> path = new ArrayList<>();

    private int width = 10;
    private int height = 10;
    private double fieldSize = 0;
    private double x0 = 0;
    private double y0 = 0;
    private int segmentCount = 0;
    private boolean generatorStarted = false;
    private boolean finished = false;

    @Override
    public void start(@NotNull Stage stage) {
        Group root = new Group();
        Scene scene = new Scene(root);
        stage.setTitle("Maze Generator");
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.show();
        Platform.runLater(() -> stage.setFullScreen(true));

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.N && !generatorStarted) {
                generatorStarted = true;
                walls.clear();
                path.clear();
                finished = false;
                width += 5;
                height += 5;
                new Thread(this::createMaze).start();
            }
            if (event.getCode() == KeyCode.BACK_SPACE && !(path.size() == 1)) {
                path.removeLast();
            }
        });

        scene.setOnMouseDragged(event -> {
            if (!finished
                    && event.getButton() == MouseButton.PRIMARY
                    && event.getX() >= x0
                    && event.getX() <= x0 + width * fieldSize
                    && event.getY() >= y0
                    && event.getY() <= y0 + height * fieldSize
                    && segmentCount == 1) {

                int fieldX = (int) Math.floor((event.getX() - x0) / fieldSize);
                int fieldY = (int) Math.floor((event.getY() - y0) / fieldSize);

                Iterator<MazeField> iterator = path.iterator();
                boolean removeNext = false;
                while (iterator.hasNext()) {
                    MazeField field = iterator.next();
                    if (!removeNext) {
                        if (fieldX == field.x() && fieldY == field.y()) {
                            removeNext = true;
                        }
                    } else {
                        iterator.remove();
                    }
                }

                if ((path.getLast().x() != fieldX
                        || path.getLast().y() != fieldY)
                        && (((fieldX == path.getLast().x() - 1
                        || fieldX == path.getLast().x() + 1)
                        && fieldY == path.getLast().y())
                        ^ ((fieldY == path.getLast().y() - 1
                        || fieldY == path.getLast().y() + 1)
                        && fieldX == path.getLast().x()))) {

                    if (isPassable(fieldX, fieldY, path.getLast().x(), path.getLast().y())) {
                        path.add(new MazeField(fieldX, fieldY));

                        if (fieldX == width - 1 && fieldY == height - 1) {
                            finished = true;
                        }
                    }
                }

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
        generatorStarted = true;
        MazeSegment[][] mazeSegments = new MazeSegment[width][height];
        segmentCount = width * height;
        MazeField[] lastRow = new MazeField[width];
        path.add(new MazeField(0, 0));

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
        while (segmentCount > 1 && !walls.isEmpty()) {
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
        generatorStarted = false;
    }

    private void drawMaze(@NotNull Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();

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
        gc.setLineWidth(1);
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

        if (finished) gc.setStroke(Color.rgb(0, 255, 0));
        else gc.setStroke(Color.BLUE);

        gc.setLineWidth(10);
        MazeField lastField = null;
        for (MazeField field : path) {
            if (lastField != null) {
                gc.strokeLine(x0 + (lastField.x() + 0.5) * fieldSize, y0 + (lastField.y() + 0.5) * fieldSize, x0 + (field.x() + 0.5) * fieldSize, y0 + (field.y() + 0.5) * fieldSize);
            }
            lastField = field;
        }
    }

    private boolean isPassable(int x1, int y1, int x2, int y2) {
        for (MazeWall w : walls) {
            if ((w.mazeField1().x() == x1 && w.mazeField1().y() == y1 && w.mazeField2().x() == x2 && w.mazeField2().y() == y2)
                    || (w.mazeField1().x() == x2 && w.mazeField1().y() == y2 && w.mazeField2().x() == x1 && w.mazeField2().y() == y1)) {
                return false;
            }
        }
        return true;
    }
}
