package com.laxer.mazegenerator;

import java.util.ArrayList;

@SuppressWarnings("ClassCanBeRecord")
public class SolvingOrder {
    public final Direction[] order;

    private SolvingOrder(Direction[] order) {
        this.order = order;
    }

    public enum Direction {
        TOP(0, -1),
        RIGHT(1, 0),
        BOTTOM(0, 1),
        LEFT(-1, 0);

        public final int x, y;

        Direction(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static SolvingOrder getOrder(int curX, int curY, int endX, int endY, int mazeWidth, int mazeHeight) {
        Direction[] order = new Direction[4];
        int widthOff = mazeWidth > mazeHeight ? 1 : 0;
        if (curX < endX) {
            order[1 - widthOff] = Direction.RIGHT;
            order[2 + widthOff] = Direction.LEFT;
        } else {
            order[1 - widthOff] = Direction.LEFT;
            order[2 + widthOff] = Direction.RIGHT;
        }
        if (curY < endY) {
            order[widthOff] = Direction.BOTTOM;
            order[3 - widthOff] = Direction.TOP;
        } else {
            order[widthOff] = Direction.TOP;
            order[3 - widthOff] = Direction.BOTTOM;
        }
        return new SolvingOrder(order);
    }
}
