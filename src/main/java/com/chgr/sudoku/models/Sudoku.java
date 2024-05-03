package com.chgr.sudoku.models;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Sudoku extends Pane implements ISudoku {

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
        clearColorGroup();
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

    public Cell getCell(Pos pos){
        return cells[pos.x()][pos.y()];
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

    public Set<Integer> getSquareValue(int squareNumber) {
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

    public Set<ICell> getEmptyCells() {
        return Arrays.stream(cells).flatMap(Arrays::stream).filter(cell -> cell.getValue() == Cell.EMPTY).collect(Collectors.toSet());
    }

    public void reRender() {
        Platform.runLater(() ->{
            for (int i = 0; i < SUDOKU_SIZE; i++) {
                for (int j = 0; j < SUDOKU_SIZE; j++) {
                    cells[i][j].reRender();
                }
            }
        });
    }

    public void colorGroup(Collection<Pos> pos, Color color) {
        for(Pos p : pos){
            if(p.x() == -1 && p.y() == -1)
                continue;
            Rectangle rect;
            if(p.x() == -1){
                int y = p.y() * Cell.SIZE + Cell.SIZE / 2;
                rect = new Rectangle(0, y - Cell.SIZE / 2f, SIZE, Cell.SIZE);
            } else if(p.y() == -1){
                int x = p.x() * Cell.SIZE + Cell.SIZE / 2;
                rect = new Rectangle(x - Cell.SIZE / 2f, 0, Cell.SIZE, SIZE);
            } else {
                int x = p.x() * Cell.SIZE + Cell.SIZE / 2;
                int y = p.y() * Cell.SIZE + Cell.SIZE / 2;
                rect = new Rectangle(x - Cell.SIZE / 2f, y - Cell.SIZE / 2f, Cell.SIZE*3, Cell.SIZE*3);
            }
            rect.setFill(color);
            rect.setOpacity(0.25);
            this.getChildren().add(rect);
        }
    }

    public void clearColorGroup() {
        this.getChildren().removeIf(node -> node instanceof Rectangle);
    }

    public void colorLine(Pos first, Pos second, int candidate, Color color) {
        Point2D start = getCenter(first);
        Point2D end = getCenter(second);

        int xOffset = (candidate - 1) % 3 - 1;
        int yOffset = (candidate - 1) / 3 - 1;

        start = start.add(xOffset * Cell.SIZE * 0.6f / 2f, yOffset * Cell.SIZE * 0.6f / 2f);
        end = end.add(xOffset * Cell.SIZE * 0.6f / 2f, yOffset * Cell.SIZE * 0.6f / 2f);

        Line line = new Line(start.getX(), start.getY(), end.getX(), end.getY());
        line.setStroke(color);
        line.setStrokeWidth(2);

        this.getChildren().add(line);
    }

    private Point2D getCenter(Pos first) {
        return new Point2D(first.x() * Cell.SIZE + Cell.SIZE / 2f, first.y() * Cell.SIZE + Cell.SIZE / 2f);
    }

    @Override
    public void clearColorLine() {
        this.getChildren().removeIf(node -> node instanceof Line);
    }
}
