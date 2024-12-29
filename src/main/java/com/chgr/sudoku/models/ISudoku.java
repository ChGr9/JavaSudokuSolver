package com.chgr.sudoku.models;

import javafx.scene.paint.Color;

import java.util.Set;

public interface ISudoku {
    int SUDOKU_SIZE = 9;
    int SQUARE_SIZE = 3;

    enum GroupType{
        ROW,
        COLUMN,
        SQUARE
    }

    boolean initialValidation();

    void loadCandidates();

    void reRender();

    Set<ICell> getEmptyCells();

    Set<ICell> getNonEmptyCells();

    ICell getCell(int x, int y);

    Set<Integer> getRowValue(int y);

    Set<Integer> getColumnValue(int x);

    Set<Integer> getSquareValue(int squareNumber);

    Set<Integer> getSquareValue(int x, int y);

    Set<Pos> removeAffectedCandidates(int x, int y, int num);

    Set<Integer> generateCandidates(int x, int y);

    ICell[] getAllCells();

    ICell[] getRow(int y);

    ICell[] getColumn(int x);

    ICell[] getSquare(int squareNumber);

    ICell[] getSquare(int x, int y);

    ICell getCell(Pos key);

    default ICell[] getCells(GroupType type, int index) {
        return switch (type) {
            case ROW -> getRow(index);
            case COLUMN -> getColumn(index);
            case SQUARE -> getSquare(index);
        };
    }

    default void colorGroup(Pos first, Pos second, Color color) {}

    default void clearColorGroup() {}

    default void colorLine(PosCandidate first, PosCandidate second, Color color, boolean isDoubleLine) {}

    default void clearColorLine() {}
}
