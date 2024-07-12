package com.chgr.sudoku.models;

public record Pos(int x, int y) {

    @Override
    public String toString() {
        return String.format("( %s, %s)", x, y);
    }

    public boolean isOutOfBound(){
        return x < 0 || y < 0 || x >= ISudoku.SUDOKU_SIZE || y >= ISudoku.SUDOKU_SIZE;
    }
}
