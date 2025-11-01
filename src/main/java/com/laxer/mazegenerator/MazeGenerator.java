package com.laxer.mazegenerator;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Cursor;
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
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

public class MazeGenerator extends Application {
    private final CopyOnWriteArrayList<MazeWall> walls = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<MazeField> path = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArraySet<MazeField> segmentLessFields = new CopyOnWriteArraySet<>();

    private final int allowedLoops = 0;
    private final int width = 200;
    private final int height = 200;
    private MazeField start = new MazeField(0, 0);
    private MazeField end = new MazeField(width / 2, height / 2);
    private final Predicate<MazeField> slfPredicate  = field -> false;

    private double fieldSize = 0;
    private double x0 = 0;
    private double y0 = 0;
    private int segmentCount = 0;
    private boolean generatorStarted = false;
    private boolean finished = false;
    private boolean startPicked = true;
    private boolean endPicked = true;

    @Override
    public void start(@NotNull Stage stage) {
        Group root = new Group();
        Scene scene = new Scene(root);
        stage.setTitle("Maze Generator");
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.show();
//        Platform.runLater(() -> stage.setFullScreen(true));

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.N && !generatorStarted && startPicked && endPicked) {
                generatorStarted = true;
                walls.clear();
                finished = false;
                new Thread(this::createMaze).start();
            }
            if (event.getCode() == KeyCode.BACK_SPACE && !(path.size() == 1) && startPicked && endPicked) {
                path.removeLast();
            }
            if (event.getCode() == KeyCode.S && !generatorStarted && startPicked && endPicked) {
                new Thread(this::solve).start();
            }
            if (event.getCode() == KeyCode.R && !generatorStarted) {
                resetPath();
                finished = false;
            }
            if (event.getCode() == KeyCode.P && !generatorStarted && !finished) {
                scene.setCursor(Cursor.HAND);
                startPicked = false;
            }
        });

        scene.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (!startPicked) {
                    start = new MazeField((int) ((event.getX() - x0) / fieldSize), (int) ((event.getY() - y0) / fieldSize));
                    startPicked = true;
                    endPicked = false;
                } else if (!endPicked) {
                    end = new MazeField((int) ((event.getX() - x0) / fieldSize), (int) ((event.getY() - y0) / fieldSize));
                    endPicked = true;
                    scene.setCursor(Cursor.DEFAULT);
                    resetPath();
                }
            }
        });

        scene.setOnMouseDragged(event -> {
            if (!finished
                    && startPicked
                    && endPicked
                    && event.getButton() == MouseButton.PRIMARY
                    && event.getX() >= x0
                    && event.getX() <= x0 + width * fieldSize
                    && event.getY() >= y0
                    && event.getY() <= y0 + height * fieldSize
                    && segmentCount == 1) {

                int fieldX = (int) Math.floor((event.getX() - x0) / fieldSize);
                int fieldY = (int) Math.floor((event.getY() - y0) / fieldSize);

                boolean found = false;
                List<MazeField> toRemove = new ArrayList<>();
                for (MazeField field : path) {
                    if (!found) {
                        if (fieldX == field.x() && fieldY == field.y()) {
                            found = true;
                        }
                    } else {
                        toRemove.add(field);
                    }
                }
                if (!toRemove.isEmpty()) {
                    path.removeAll(toRemove);
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

                        if (fieldX == end.x() && fieldY == end.y()) {
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
        resetPath();
        int loopsLeft = allowedLoops;
        Random rand = new Random();

        for (int h = 0; h < height; h++) {
            MazeField lastField = null;
            for (int w = 0; w < width; w++) {
                MazeField newField = new MazeField(w, h);
                if (!slfPredicate.test(newField)) {
                    mazeSegments[w][h] = new MazeSegment(newField);
                } else segmentLessFields.add(newField);

                if (lastField != null && !(segmentLessFields.contains(lastField) && segmentLessFields.contains(newField))) {
                    walls.add(new MazeWall(lastField, newField));
                }
                if (lastRow[w] != null && !(segmentLessFields.contains(lastRow[w]) && segmentLessFields.contains(newField))) {
                    walls.add(new MazeWall(lastRow[w], newField));
                }
                lastField = lastRow[w] = newField;
            }
        }

//        for (MazeField segmentLessField : segmentLessFields) {
//            mazeSegments[segmentLessField.x()][segmentLessField.y()] = null;
//        }
        segmentCount -= segmentLessFields.size();

        while (segmentCount > 1 && !walls.isEmpty()) {
            MazeWall wall = walls.get(rand.nextInt(walls.size()));
            MazeSegment segment1 = mazeSegments[wall.mazeField1().x()][wall.mazeField1().y()];
            MazeSegment segment2 = mazeSegments[wall.mazeField2().x()][wall.mazeField2().y()];

            if (segment1 == null || segment2 == null) continue;

            if (segment1 != segment2) {
                for (MazeField mazeField : segment2.mazeFields) {
                    mazeSegments[mazeField.x()][mazeField.y()] = segment1;
                }
                segment1.mazeFields.addAll(segment2.mazeFields);
                walls.remove(wall);
                segmentCount--;
            } else if (loopsLeft > 0) {
                walls.remove(wall);
                loopsLeft--;
            }
        }
        generatorStarted = false;
    }

    private void solve() {
        resetPath();

        Set<MazeField> visited = new HashSet<>();
        visited.add(path.getFirst());

        while (!(path.getLast().x() == end.x() && path.getLast().y() == end.y())) {
            int lastX = path.getLast().x();
            int lastY = path.getLast().y();

            SolvingOrder order = SolvingOrder.getOrder(lastX, lastY, end.x(), end.y(), width, height);

            MazeField next = null;

            for (SolvingOrder.Direction direction : order.order) {
                int nextX = lastX + direction.x;
                int nextY = lastY + direction.y;

                if (nextX >= 0 && nextX < width && nextY >= 0 && nextY < height && isPassable(lastX, lastY, nextX, nextY) && !visited.contains(new MazeField(nextX, nextY))) {
                    next = new MazeField(nextX, nextY);
                    break;
                }
            }
            if (next != null) {
                path.add(next);
                visited.add(next);
            } else {
                path.removeLast();
            }
        }
        finished = true;
    }

    private void drawMaze(@NotNull Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        if ((double) width / height < canvas.getWidth() / canvas.getHeight()) {
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


//        final int c = (int) (255 * ((double) segmentCount / (width * height)));
//        gc.setStroke(Color.rgb(c, 255 - c, 0));

        gc.setStroke(Color.web("#37ffa3"));
        gc.setLineWidth(fieldSize / 10);
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

        if (finished) gc.setStroke(Color.web("#37ffa3"));
        else gc.setStroke(Color.WHITE);

        gc.setLineWidth(fieldSize / 3.5);
        MazeField lastField = null;
        for (MazeField field : path) {
            if (lastField != null) {
                gc.strokeLine(x0 + (lastField.x() + 0.5) * fieldSize, y0 + (lastField.y() + 0.5) * fieldSize, x0 + (field.x() + 0.5) * fieldSize, y0 + (field.y() + 0.5) * fieldSize);
            }
            lastField = field;
        }

        gc.setFill(Color.web("#37ffa3"));
        for (MazeField segmentLessField : segmentLessFields) {
            double x = x0 + segmentLessField.x() * fieldSize;
            double y = y0 + segmentLessField.y() * fieldSize;
            double f = 0.2f;

            gc.fillPolygon(new double[]{x, x + f * fieldSize, x}, new double[]{y, y, y + f * fieldSize}, 3);
            gc.fillPolygon(new double[]{x + fieldSize, x + fieldSize - f * fieldSize, x + fieldSize}, new double[]{y + fieldSize, y + fieldSize, y + fieldSize - f * fieldSize}, 3);
            gc.fillPolygon(new double[]{
                    x,
                    x,
                    x + fieldSize - f * fieldSize,
                    x + fieldSize,
                    x + fieldSize,
                    x + f * fieldSize
            }, new double[]{
                    y + fieldSize,
                    y + fieldSize - f * fieldSize,
                    y,
                    y,
                    y + f * fieldSize,
                    y + fieldSize
            }, 6);
        }
        gc.fillRect(x0 + (start.x() + 0.4) * fieldSize, y0 + (start.y() + 0.4) * fieldSize, 0.2 * fieldSize, 0.2 * fieldSize);
        gc.fillRect(x0 + (end.x() + 0.2) * fieldSize, y0 + (end.y() + 0.2) * fieldSize, 0.6 * fieldSize, 0.6 * fieldSize);
        gc.setFill(Color.BLACK);
        gc.fillRect(x0 + (end.x() + 0.4) * fieldSize, y0 + (end.y() + 0.4) * fieldSize, 0.2 * fieldSize, 0.2 * fieldSize);

        if (generatorStarted) {
            int w = 400;
            int h = 50;
            int off = 5;

            double x = x0 + (width * fieldSize - w) / 2;
            double y = y0 + (height * fieldSize - h) / 2;

            gc.setLineWidth(1);
            gc.setStroke(Color.web("#37ffa3"));
            gc.strokeRect(x, y, w, h);
            gc.setFill(Color.BLACK);
            gc.fillRect(x, y, w, h);
            gc.setFill(Color.web("#37ffa3"));
            gc.fillRect(x + off, y + off, (w - 2 * off) - ((w - 2 * off) * ((double) segmentCount / (width * height))), h - 2 * off);
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

    private void resetPath() {
        path.clear();
        path.add(start);
    }
}