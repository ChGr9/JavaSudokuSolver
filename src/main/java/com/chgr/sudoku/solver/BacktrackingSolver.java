package com.chgr.sudoku.solver;

import com.chgr.sudoku.models.Cell;
import com.chgr.sudoku.models.Pos;
import com.chgr.sudoku.models.Sudoku;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.Comparator;
import java.util.Set;

public class BacktrackingSolver extends Task<Boolean> {

    @Override
    protected Boolean call() {
        return solve();
    }

    private final Sudoku sudoku;

    public BacktrackingSolver(Sudoku sudoku) {
        this.sudoku = sudoku;
    }

    public boolean solve() {
        if (sudoku.initialValidation()) {
            sudoku.loadCandidates();
            Platform.runLater(sudoku::reRenderCandidates);
            try {
                Thread.sleep(100);
                return scan();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    private boolean scan() {
        Cell cell = sudoku.getEmptyCells().stream().min(Comparator.comparingInt(c -> c.getCandidates().size())).orElse(null);

        // if no empty cell
        if (cell == null)
            return true;


        for (int num : cell.getCandidates()) {
            if (isSafe(cell.getX(), cell.getY(), num)) {
                Set<Pos> affectedCells = assignValue(cell, num);

                if (scan()) {
                    return true;
                }

                unassignValue(cell, num, affectedCells);
            }
        }

        return false;
    }

    private boolean isSafe(int x, int y, int num) {
        // if cell is not empty
        if (!sudoku.getCell(x, y).isEmpty())
            return false;

        // check row
        if (sudoku.getRowValue(y).contains(num))
            return false;

        // check column
        if (sudoku.getColumnValue(x).contains(num))
            return false;

        // check 3*3 box
        if (sudoku.getSquareValue(x, y).contains(num))
            return false;

        return true;
    }

    private Set<Pos> assignValue(Cell cell, int num) {
        // remove this cell from possibleValues as it is filled
        cell.clearCandidates();

        // remove num from possible values of all peers
        Set<Pos> affectedPositions = sudoku.removeAffectedCandidates(cell.getX(), cell.getY(), num);
        Platform.runLater(() -> {
            for (Pos affectedPos : affectedPositions) {
                sudoku.getCell(affectedPos.x(), affectedPos.y()).reRender();
            }
        });

        // fill the cell
        cell.setValue(num);
        Platform.runLater(cell::reRender);

        return affectedPositions;
    }

    private void unassignValue(Cell cell, int num, Set<Pos> affectedPositions) {
        // unfill the cell
        cell.clear();

        // add num back to possible values of all affected cells
        for (Pos affectedPos : affectedPositions) {
            Cell affectedCell = sudoku.getCell(affectedPos.x(), affectedPos.y());
            affectedCell.addCandidate(num);
            Platform.runLater(affectedCell::reRender);
        }

        // clear the list of affected cells
        affectedPositions.clear();

        // Repopulate the possible values for the unassigned cell
        Set<Integer> candidates = sudoku.generateCandidates(cell.getX(), cell.getY());
        cell.addCandidates(candidates);
        Platform.runLater(cell::reRender);
    }
}
