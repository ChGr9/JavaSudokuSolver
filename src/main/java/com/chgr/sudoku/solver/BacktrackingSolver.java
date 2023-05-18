package com.chgr.sudoku.solver;

import com.chgr.sudoku.models.*;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.Comparator;
import java.util.Set;

public class BacktrackingSolver extends Task<Boolean> {

    @Override
    protected Boolean call() {
        return solve();
    }

    private final ISudoku sudoku;

    public BacktrackingSolver(ISudoku sudoku) {
        this.sudoku = sudoku;
    }

    public boolean solve() {
        if (sudoku.initialValidation()) {
            sudoku.loadCandidates();
            return scan();
        }
        return false;
    }

    private boolean scan() {
        Platform.runLater(sudoku::reRender);
        ICell cell = sudoku.getEmptyCells().stream().min(Comparator.comparingInt(c -> c.getCandidates().size())).orElse(null);

        // if no empty cell
        if (cell == null)
            return true;


        for (int candidate : cell.getCandidates()) {
            Set<Pos> affectedCells = assignValue(cell, candidate);

            if (scan()) {
                return true;
            }

            unassignValue(cell, candidate, affectedCells);
        }
        return false;
    }

    private Set<Pos> assignValue(ICell cell, int num) {
        // remove this cell from possibleValues as it is filled
        cell.clearCandidates();

        // remove num from possible values of all peers
        Set<Pos> affectedPositions = sudoku.removeAffectedCandidates(cell.getX(), cell.getY(), num);

        // fill the cell
        cell.setValue(num);

        return affectedPositions;
    }

    private void unassignValue(ICell cell, int num, Set<Pos> affectedPositions) {
        // unfill the cell
        cell.clear();

        // add num back to possible values of all affected cells
        for (Pos affectedPos : affectedPositions) {
            ICell affectedCell = sudoku.getCell(affectedPos.x(), affectedPos.y());
            affectedCell.addCandidate(num);
        }

        // clear the list of affected cells
        affectedPositions.clear();

        // Repopulate the possible values for the unassigned cell
        Set<Integer> candidates = sudoku.generateCandidates(cell.getX(), cell.getY());
        cell.addCandidates(candidates);
    }
}
