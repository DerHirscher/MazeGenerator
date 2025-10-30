package com.laxer.mazegenerator;

import java.util.HashSet;
import java.util.Set;

public class MazeSegment {
    public final Set<MazeField> mazeFields = new HashSet<>();

    public MazeSegment(MazeField initialMazeField) {
        this.mazeFields.add(initialMazeField);
    }
}
