package com.chgr.sudoku.models;

import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;

import java.util.*;
import java.util.stream.Collectors;

public class Sudoku extends Pane {

    public static final int SUDOKU_SIZE = 9;
    private final Cell[][] cells = new Cell[SUDOKU_SIZE][SUDOKU_SIZE];
    private static final int SIZE = Cell.SIZE * SUDOKU_SIZE;

    public Sudoku(){
        this.setPrefSize(SIZE, SIZE);
        for(int i=0; i<SUDOKU_SIZE; i++){
            for(int j=0; j<SUDOKU_SIZE; j++){
                cells[i][j] = new Cell(i, j);
                this.getChildren().add(cells[i][j]);
            }
        }
    }

    public void clear(){
        for(int i=0; i<SUDOKU_SIZE; i++){
            for(int j=0; j<SUDOKU_SIZE; j++){
                cells[i][j].clear();
                cells[i][j].reRender();
            }
        }
    }

    public boolean initialValidation(){
        for(int i =0; i<SUDOKU_SIZE; i++){
            Set<Integer> row = getRowValue(i).stream().filter(value -> value != Cell.EMPTY).collect(Collectors.toSet());
            if(row.size() != row.stream().distinct().count()){
                showAlert("Duplicate found at row " + (i+1));
                return false;
            }
            Set<Integer> col = getColumnValue(i).stream().filter(value -> value != Cell.EMPTY).collect(Collectors.toSet());
            if(col.size() != col.stream().distinct().count()){
                showAlert("Duplicate found at column " + (i+1));
                return false;
            }
            Set<Integer> square = getSquareValue(i).stream().filter(value -> value != Cell.EMPTY).collect(Collectors.toSet());
            if(square.size() != square.stream().distinct().count()){
                showAlert("Duplicate found at square " + (i+1));
                return false;
            }
        }
        return true;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation error");
        alert.setContentText("Sudoku not valid\n" + message);
        alert.show();
    }

    public Cell getCell(int x, int y){
        return cells[x][y];
    }

    public Cell[] getAllCells(){
        return Arrays.stream(cells).flatMap(Arrays::stream).toArray(Cell[]::new);
    }

    public Cell[] getColumn(int x){
        return cells[x];
    }

    public Cell[] getRow(int y){
        return Arrays.stream(cells).map(row -> row[y]).toArray(Cell[]::new);
    }

    public Cell[] getSquare(int x, int y){
        //col and row are the coordinates of the top left most cell in the square that x,y is located
        int col = x / 3 * 3;
        int row = y / 3 * 3;
        Cell[] square = new Cell[SUDOKU_SIZE];
        for (int i = 0; i < 3; i++)
            System.arraycopy(cells[col + i], row, square, i * 3, 3);
        return square;
    }

    public Cell[] getSquare(int squareNumber){
        //col and row are the coordinates of the top left most cell in the square that x,y is located
        int col = squareNumber * 3 % 9;
        int row = squareNumber / 3 * 3;
        Cell[] square = new Cell[SUDOKU_SIZE];
        for (int i = 0; i < 3; i++)
            System.arraycopy(cells[col + i], row, square, i * 3, 3);
        return square;
    }

    public Set<Integer> getColumnValue(int x){
        return Arrays.stream(cells[x]).map(Cell::getValue).collect(Collectors.toSet());
    }

    public Set<Integer> getRowValue(int y){
        return Arrays.stream(cells).map(row -> row[y].getValue()).collect(Collectors.toSet());
    }

    public Set<Integer> getSquareValue(int x, int y){
        //col and row are the coordinates of the top left most cell in the square that x,y is located
        int col = x / 3 * 3;
        int row = y / 3 * 3;
        Set<Integer> square = new HashSet<>();
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                square.add(cells[col + i][row + j].getValue());
        return square;
    }

    public Set<Integer> getSquareValue(int squareNumber)
    {
        //col and row are the coordinates of the top left most cell in the square that x,y is located
        int col = squareNumber * 3 % 9;
        int row = squareNumber / 3 * 3;
        Set<Integer> square = new HashSet<>();
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                square.add(cells[col + i][row + j].getValue());
        return square;
    }

    public Set<Pos> removeAffectedCandidates(int x, int y, int value) {
        Set<Pos> affectedCells = new HashSet<>();
        for (Cell cell : getRow(y)) {
            if(cell.removeCandidate(value))
                affectedCells.add(new Pos(cell.getX(), cell.getY()));
        }
        for (Cell cell : getColumn(x)) {
            if(cell.removeCandidate(value))
                affectedCells.add(new Pos(cell.getX(), cell.getY()));
        }
        for (Cell cell : getSquare(x, y)) {
            if(cell.removeCandidate(value))
                affectedCells.add(new Pos(cell.getX(), cell.getY()));
        }
        return affectedCells;
    }

    public Set<Integer> generateCandidates(int col, int row){
        Set<Integer> candidates = new HashSet<>(Cell.DIGITS);
        candidates.removeAll(getColumnValue(col));
        candidates.removeAll(getRowValue(row));
        candidates.removeAll(getSquareValue(col, row));
        return candidates;
    }

    public void loadCandidates(){
        for(int i=0; i<SUDOKU_SIZE; i++){
            for(int j=0; j<SUDOKU_SIZE; j++){
                Cell cell = cells[i][j];
                if(cell.getValue() == Cell.EMPTY) {
                    cell.clearCandidates();
                    cell.addCandidates(generateCandidates(cell.getX(), cell.getY()));
                }
            }
        }
    }

    public Set<Cell> getEmptyCells() {
        return Arrays.stream(cells).flatMap(Arrays::stream).filter(cell -> cell.getValue() == Cell.EMPTY).collect(Collectors.toSet());
    }

    public void reRenderCandidates() {
        getEmptyCells().forEach(Cell::reRender);
    }
}
