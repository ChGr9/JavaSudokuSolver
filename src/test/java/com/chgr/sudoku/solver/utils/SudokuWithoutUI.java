package com.chgr.sudoku.solver.utils;

import com.chgr.sudoku.models.Cell;
import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import com.chgr.sudoku.models.Pos;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SudokuWithoutUI implements ISudoku {

    private final CellWithoutUI[][] cells = new CellWithoutUI[SUDOKU_SIZE][SUDOKU_SIZE];

    public SudokuWithoutUI(){
        for (int i = 0; i < SUDOKU_SIZE; i++) {
            for (int j = 0; j < SUDOKU_SIZE; j++) {
                cells[i][j] = new CellWithoutUI(i, j);
            }
        }
    }

    @Override
    public boolean initialValidation() {
        for(int i =0; i<SUDOKU_SIZE; i++){
            Set<Integer> row = getRowValue(i).stream().filter(value -> value != Cell.EMPTY).collect(Collectors.toSet());
            if(row.size() != row.stream().distinct().count()){
                return false;
            }
            Set<Integer> col = getColumnValue(i).stream().filter(value -> value != Cell.EMPTY).collect(Collectors.toSet());
            if(col.size() != col.stream().distinct().count()){
                return false;
            }
            Set<Integer> square = getSquareValue(i).stream().filter(value -> value != Cell.EMPTY).collect(Collectors.toSet());
            if(square.size() != square.stream().distinct().count()){
                return false;
            }
        }
        return true;
    }

    @Override
    public void loadCandidates() {
        for(int i=0; i<SUDOKU_SIZE; i++){
            for(int j=0; j<SUDOKU_SIZE; j++){
                CellWithoutUI cell = cells[i][j];
                if(cell.getValue() == Cell.EMPTY) {
                    cell.clearCandidates();
                    cell.addCandidates(generateCandidates(cell.getX(), cell.getY()));
                }
            }
        }
    }

    @Override
    public void reRender() {
    }

    @Override
    public Set<ICell> getEmptyCells() {
        return Arrays.stream(cells).flatMap(Arrays::stream).filter(cell -> cell.getValue() == Cell.EMPTY).collect(Collectors.toSet());
    }

    @Override
    public ICell getCell(int x, int y) {
        return cells[x][y];
    }

    @Override
    public Set<Integer> getRowValue(int y) {
        return Arrays.stream(cells).map(row -> row[y].getValue()).collect(Collectors.toSet());
    }

    @Override
    public Set<Integer> getColumnValue(int x) {
        return Arrays.stream(cells[x]).map(CellWithoutUI::getValue).collect(Collectors.toSet());
    }

    @Override
    public Set<Integer> getSquareValue(int x, int y) {
        //col and row are the coordinates of the top left most cell in the square that x,y is located
        int col = x / 3 * 3;
        int row = y / 3 * 3;
        Set<Integer> square = new HashSet<>();
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                square.add(cells[col + i][row + j].getValue());
        return square;
    }

    private Set<Integer> getSquareValue(int squareNumber) {
        //col and row are the coordinates of the top left most cell in the square that x,y is located
        int col = squareNumber * 3 % 9;
        int row = squareNumber / 3 * 3;
        Set<Integer> square = new HashSet<>();
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                square.add(cells[col + i][row + j].getValue());
        return square;
    }

    @Override
    public Set<Pos> removeAffectedCandidates(int x, int y, int value) {
        Set<Pos> affectedCells = new HashSet<>();
        for (CellWithoutUI cell : getRow(y)) {
            if(cell.removeCandidate(value))
                affectedCells.add(new Pos(cell.getX(), cell.getY()));
        }
        for (CellWithoutUI cell : getColumn(x)) {
            if(cell.removeCandidate(value))
                affectedCells.add(new Pos(cell.getX(), cell.getY()));
        }
        for (CellWithoutUI cell : getSquare(x, y)) {
            if(cell.removeCandidate(value))
                affectedCells.add(new Pos(cell.getX(), cell.getY()));
        }
        return affectedCells;
    }

    @Override
    public Set<Integer> generateCandidates(int x, int y) {
        Set<Integer> candidates = new HashSet<>(Cell.DIGITS);
        candidates.removeAll(getColumnValue(x));
        candidates.removeAll(getRowValue(y));
        candidates.removeAll(getSquareValue(x, y));
        return candidates;
    }

    public CellWithoutUI[] getColumn(int x){
        return cells[x];
    }

    public CellWithoutUI[] getRow(int y){
        return Arrays.stream(cells).map(row -> row[y]).toArray(CellWithoutUI[]::new);
    }

    public CellWithoutUI[] getSquare(int x, int y){
        //col and row are the coordinates of the top left most cell in the square that x,y is located
        int col = x / 3 * 3;
        int row = y / 3 * 3;
        CellWithoutUI[] square = new CellWithoutUI[SUDOKU_SIZE];
        for (int i = 0; i < 3; i++)
            System.arraycopy(cells[col + i], row, square, i * 3, 3);
        return square;
    }

    public List<List<Integer>> toIntList() {
        return IntStream.range(0, SUDOKU_SIZE)
                .mapToObj(i -> Arrays.stream(cells)
                        .map(row -> row[i].getValue())
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }
}
