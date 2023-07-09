package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import com.chgr.sudoku.models.Sudoku;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IntersectionTechnique {

    //https://www.sudokuwiki.org/Intersection_Removal#IR
    //Section: Pointing Pairs, Pointing Triples
    public static boolean pointingTuple(ISudoku sudoku) {
        final int SQUARE_SIZE = 3;

        for (int squareIndex = 0; squareIndex < ISudoku.SUDOKU_SIZE; squareIndex++) {
            ICell[] square = sudoku.getSquare(squareIndex);
            List<Set<Integer>> squareRowCandidates = new ArrayList<>();
            List<Set<Integer>> squareColumnCandidates = new ArrayList<>();

            for (int rowIndex = 0; rowIndex < SQUARE_SIZE; rowIndex++) {
                squareRowCandidates.add(getCandidateSetByY(square, rowIndex));
                squareColumnCandidates.add(getCandidateSetByX(square, rowIndex));
            }

            if (processCandidates(sudoku, square, squareRowCandidates, true)
                    || processCandidates(sudoku, square, squareColumnCandidates, false)) {
                return true;
            }
        }

        return false;
    }

    private static Set<Integer> getCandidateSetByY(ICell[] square, int finalRowIndex) {
        return Arrays.stream(square)
                .filter(cell -> cell.getY() % 3 == finalRowIndex)
                .flatMap(cell -> cell.getCandidates().stream())
                .collect(Collectors.toSet());
    }

    private static Set<Integer> getCandidateSetByX(ICell[] square, int finalRowIndex) {
        return Arrays.stream(square)
                .filter(cell -> cell.getX() % 3 == finalRowIndex)
                .flatMap(cell -> cell.getCandidates().stream())
                .collect(Collectors.toSet());
    }

    private static boolean processCandidates(ISudoku sudoku, ICell[] square, List<Set<Integer>> squareCandidates, boolean isRow) {
        for (int candidateIndex = 0; candidateIndex < squareCandidates.size(); candidateIndex++) {
            Set<Integer> currentCandidateSet = squareCandidates.get(candidateIndex);
            Set<Integer> distinctCandidates = getDistinctCandidates(currentCandidateSet, squareCandidates);

            if (!distinctCandidates.isEmpty() && removeCandidatesFromCells(sudoku, square, distinctCandidates, isRow, candidateIndex)) {
                return true;
            }
        }

        return false;
    }

    private static boolean removeCandidatesFromCells(ISudoku sudoku, ICell[] square, Set<Integer> distinctCandidates, boolean isRow, int offset) {
        boolean changed = false;

        ICell[] cells = isRow ? sudoku.getRow(square[0].getY() + offset) : sudoku.getColumn(square[0].getX() + offset);
        int startAxis = isRow ? square[0].getX() : square[0].getY();
        int endAxis = isRow ? square[8].getX() : square[8].getY();

        for (ICell cell : cells) {
            int axis = isRow ? cell.getX() : cell.getY();
            if (axis < startAxis || axis > endAxis) {
                for (int candidate : distinctCandidates) {
                    changed |= cell.removeCandidate(candidate);
                }
            }
        }

        return changed;
    }


    private static Set<Integer> getDistinctCandidates(Set<Integer> currentCandidateSet, List<Set<Integer>> groupCandidates) {
        return currentCandidateSet.stream()
                .filter(candidate -> groupCandidates.stream()
                        .filter(set -> set != currentCandidateSet)
                        .noneMatch(set -> set.contains(candidate)))
                .collect(Collectors.toSet());
    }

    //https://www.sudokuwiki.org/Intersection_Removal#IR
    //Section: Box Line Reduction
    public static boolean boxLineReduction(ISudoku sudoku) {
        for (int i = 0; i < Sudoku.SUDOKU_SIZE; i++) {
            if (checkBoxLineReduction(sudoku, sudoku.getRow(i), true))
                return true;
            if (checkBoxLineReduction(sudoku, sudoku.getColumn(i), false))
                return true;
        }
        return false;
    }

    private static boolean checkBoxLineReduction(ISudoku sudoku, ICell[] group, boolean isRow) {
        List<Set<Integer>> groupSectionCandidates = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            groupSectionCandidates.add(IntStream.range(0, 3).mapToObj(index -> group[index + finalI * 3])
                    .flatMap(cell -> cell.getCandidates().stream())
                    .collect(Collectors.toSet()));
        }
        for (int i = 0; i < 3; i++) {
            Set<Integer> currentCandidateSet = groupSectionCandidates.get(i);
            Set<Integer> distinctCandidates = getDistinctCandidates(currentCandidateSet, groupSectionCandidates);
            if (!distinctCandidates.isEmpty() && removeCandidatesFromSquare(sudoku, group, distinctCandidates, isRow, i))
                return true;
        }
        return false;
    }

    private static boolean removeCandidatesFromSquare(ISudoku sudoku, ICell[] group, Set<Integer> distinctCandidates, boolean isRow, int i) {
        boolean changed = false;
        ICell cell = group[i * 3];
        int index = cell.getY() / 3 * 3 + cell.getX() / 3;
        ICell[] cells = sudoku.getSquare(index);
        int limit = isRow ? cell.getY() : cell.getX();
        for (ICell cell1 : cells) {
            int axis = isRow ? cell1.getY() : cell1.getX();
            if (axis != limit && cell1.removeCandidates(distinctCandidates)) {
                changed = true;
            }
        }
        return changed;
    }
}
