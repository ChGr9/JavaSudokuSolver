package com.chgr.sudoku.models;

import java.util.Set;

public interface ISudoku {
    int SUDOKU_SIZE = 9;

    boolean initialValidation();

    void loadCandidates();

    void reRender();

    Set<ICell> getEmptyCells();

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
}
