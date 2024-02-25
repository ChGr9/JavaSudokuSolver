package com.chgr.sudoku.models;

public record Pos(int x, int y) {

    @Override
    public String toString() {
        return String.format("( %s, %s)", x, y);
    }
}
