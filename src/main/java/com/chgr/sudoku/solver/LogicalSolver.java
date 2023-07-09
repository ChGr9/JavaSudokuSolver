package com.chgr.sudoku.solver;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import com.chgr.sudoku.models.Sudoku;
import org.apache.commons.math3.util.CombinatoricsUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LogicalSolver {

    private static boolean isSolved(Sudoku sudoku) {
        for (int i = 0; i < ISudoku.SUDOKU_SIZE; i++) {
            if (!sudoku.getRowValue(i).containsAll(ICell.DIGITS) ||
                    !sudoku.getColumnValue(i).containsAll(ICell.DIGITS) ||
                    !sudoku.getSquareValue(i).containsAll(ICell.DIGITS))
                return false;
        }
        return true;
    }

    private static final List<Function<Sudoku, Boolean>> techniques = List.of(
            LogicalSolver::nakedSingle,
            LogicalSolver::hiddenSingle,
            LogicalSolver::nakedPair,
            LogicalSolver::nakedTriple,
            LogicalSolver::hiddenPair,
            LogicalSolver::hiddenTriple,
            LogicalSolver::nakedQuad,
            LogicalSolver::hiddenQuad,
            LogicalSolver::pointingTuple,
            LogicalSolver::boxLineReduction
    );

    public static boolean solve(Sudoku sudoku){
        boolean changed = true;
        while(changed){
            changed = false;
            for(Function<Sudoku, Boolean> technique : techniques){
                if(technique.apply(sudoku)){
                    changed = true;
                }
            }
        }
        return isSolved(sudoku);
    }

    //Naked Techniques

    //https://www.sudokuwiki.org/Getting_Started
    //Section: The Last Possible Number
    public static boolean nakedSingle(ISudoku sudoku) {
        Optional<ICell> oCell = Arrays.stream(sudoku.getAllCells()).filter(c -> c.getCandidates().size() == 1).findFirst();
        if (oCell.isPresent()) {
            ICell cell = oCell.get();
            cell.setValue(cell.getCandidates().stream().findFirst().orElseThrow());
            sudoku.removeAffectedCandidates(cell.getX(), cell.getY(), cell.getValue());
            return true;
        }
        return false;
    }

    //https://www.sudokuwiki.org/Naked_Candidates#NP
    //Section: Naked Pairs
    public static boolean nakedPair(ISudoku sudoku) {
        for(int i=0;i<ISudoku.SUDOKU_SIZE; i++){
            if(checkNakedTuple(sudoku.getRow(i), 2))
                return true;
            if(checkNakedTuple(sudoku.getColumn(i), 2))
                return true;
            if(checkNakedTuple(sudoku.getSquare(i), 2))
                return true;
        }
        return false;
    }

    //https://www.sudokuwiki.org/Naked_Candidates#NP
    //Section: Naked Triples
    public static boolean nakedTriple(ISudoku sudoku) {
        for(int i=0;i<ISudoku.SUDOKU_SIZE; i++){
            if(checkNakedTuple(sudoku.getRow(i), 3))
                return true;
            if(checkNakedTuple(sudoku.getColumn(i), 3))
                return true;
            if(checkNakedTuple(sudoku.getSquare(i), 3))
                return true;
        }
        return false;
    }

    //https://www.sudokuwiki.org/Naked_Candidates#NP
    //Section: Naked Quads
    public static Boolean nakedQuad(ISudoku sudoku) {
        for(int i=0;i<ISudoku.SUDOKU_SIZE; i++){
            if(checkNakedTuple(sudoku.getRow(i), 4))
                return true;
            if(checkNakedTuple(sudoku.getColumn(i), 4))
                return true;
            if(checkNakedTuple(sudoku.getSquare(i), 4))
                return true;
        }
        return false;
    }

    private static boolean checkNakedTuple(ICell[] group, int num){
        List<ICell> emptyCells = Arrays.stream(group)
                .filter(c -> c.getValue() == ICell.EMPTY)
                .toList();
        List<ICell> cellsWithNumCandidates = emptyCells.stream()
                .filter(c -> c.getCandidates().size() <= num)
                .toList();

        if(cellsWithNumCandidates.size() < num) return false;

        // Generate all combinations of 'num' empty cells.
        List<Integer> indices = IntStream.range(0, cellsWithNumCandidates.size()).boxed().toList();
        Iterator<int[]> combinationsIterator = CombinatoricsUtils.combinationsIterator(indices.size(), num);

        while (combinationsIterator.hasNext()) {
            int[] combinationIndices = combinationsIterator.next();

            // Get the combination of cells corresponding to the indices.
            List<ICell> combination = Arrays.stream(combinationIndices)
                    .mapToObj(indices::get)
                    .map(cellsWithNumCandidates::get)
                    .toList();

            // Collect all the candidates of the cells in this combination.
            Set<Integer> combinedCandidates = combination.stream()
                    .flatMap(cell -> cell.getCandidates().stream())
                    .collect(Collectors.toSet());

            // If the number of combined candidates is not equal to 'num', this is not a naked tuple.
            if(combinedCandidates.size() != num) continue;

            // Check if this combination is a subset of another larger combination.
            boolean isSubsetOfAnother = false;
            while (combinationsIterator.hasNext()) {
                int[] otherIndices = combinationsIterator.next();
                Set<ICell> other = Arrays.stream(otherIndices)
                        .mapToObj(indices::get)
                        .map(cellsWithNumCandidates::get)
                        .collect(Collectors.toSet());
                if (other.size() > num && other.containsAll(combination)) {
                    isSubsetOfAnother = true;
                    break;
                }
            }
            if (isSubsetOfAnother) continue;

            // For each cell in the group that is not part of the combination, remove the combined candidates.
            boolean changed = false;
            for(ICell c : emptyCells){
                if(!combination.contains(c)){
                    for(int candidate : combinedCandidates){
                        changed |= c.removeCandidate(candidate);
                    }
                }
            }

            if(changed)
                return true;
        }

        return false;
    }

    //Hidden Techniques

    //https://www.sudokuwiki.org/Getting_Started
    //Sections: Last Remaining Cell in a Box & Last Remaining Cell in a Row (or Column)
    public static boolean hiddenSingle(ISudoku sudoku){
        for(int i=0;i<ISudoku.SUDOKU_SIZE; i++){
            if(checkHiddenSingle(sudoku, sudoku.getRow(i)))
                return true;
            if(checkHiddenSingle(sudoku, sudoku.getColumn(i)))
                return true;
            if(checkHiddenSingle(sudoku, sudoku.getSquare(i)))
                return true;
        }
        return false;
    }

    private static boolean checkHiddenSingle(ISudoku sudoku, ICell[] group) {
        //foreach digit check if only one cell in the provided group can have that digit as its value
        for(int digit : ICell.DIGITS){
            List<ICell> cellsWithDigit = Arrays.stream(group).filter(c -> c.getCandidates().contains(digit)).toList();
            if(cellsWithDigit.size() == 1){
                ICell cell = cellsWithDigit.get(0);
                cell.setValue(digit);
                sudoku.removeAffectedCandidates(cell.getX(), cell.getY(), digit);
                return true;
            }
        }
        return false;
    }

    //https://www.sudokuwiki.org/Hidden_Candidates#HP
    //Section: Hidden Pairs
    public static boolean hiddenPair(ISudoku sudoku) {
        for(int i=0;i<ISudoku.SUDOKU_SIZE; i++){
            if(checkHiddenTuple(sudoku.getRow(i), 2))
                return true;
            if(checkHiddenTuple(sudoku.getColumn(i), 2))
                return true;
            if(checkHiddenTuple(sudoku.getSquare(i), 2))
                return true;
        }
        return false;
    }

    //https://www.sudokuwiki.org/Hidden_Candidates#HT
    //Section: Hidden Triples
    public static boolean hiddenTriple(ISudoku sudoku) {
        for(int i=0;i<ISudoku.SUDOKU_SIZE; i++){
            if(checkHiddenTuple(sudoku.getRow(i), 3))
                return true;
            if(checkHiddenTuple(sudoku.getColumn(i), 3))
                return true;
            if(checkHiddenTuple(sudoku.getSquare(i), 3))
                return true;
        }
        return false;
    }

    //https://www.sudokuwiki.org/Hidden_Candidates#HQ
    //Section: Hidden Quads
    public static Boolean hiddenQuad(ISudoku sudoku) {
        for(int i=0;i<ISudoku.SUDOKU_SIZE; i++){
            if(checkHiddenTuple(sudoku.getRow(i), 4))
                return true;
            if(checkHiddenTuple(sudoku.getColumn(i), 4))
                return true;
            if(checkHiddenTuple(sudoku.getSquare(i), 4))
                return true;
        }
        return false;
    }

    private static boolean checkHiddenTuple(ICell[] group, int num){
        Set<Integer> usedDigits = Arrays.stream(group)
                .map(ICell::getValue)
                .filter(value -> value != ICell.EMPTY)
                .collect(Collectors.toSet());
        List<Integer> availableDigits = ICell.DIGITS.stream()
                .filter(d -> !usedDigits.contains(d))
                .toList();
        if(availableDigits.size() < num)
            return false;
        int[] indices = IntStream.range(0, availableDigits.size()).toArray();
        Iterator<int[]> combinationsIterator = CombinatoricsUtils.combinationsIterator(indices.length, num);

        while (combinationsIterator.hasNext()){
            int[] combinationIndices = combinationsIterator.next();
            Set<Integer> combination = Arrays.stream(combinationIndices)
                    .mapToObj(availableDigits::get)
                    .collect(Collectors.toSet());
            Set<ICell> cells = Arrays.stream(group)
                    .filter(cell -> cell.getCandidates().stream().anyMatch(combination::contains))
                    .collect(Collectors.toSet());

            if(cells.size() < num){
                continue;
            }

            Set<Integer> candidates = cells.stream()
                    .flatMap(cell -> cell.getCandidates().stream())
                    .collect(Collectors.toSet());

            if(cells.size() == num && !candidates.equals(combination)){
                boolean changed = false;
                for(ICell cell : cells){
                    for(int candidate : cell.getCandidates()){
                        if(!combination.contains(candidate)){
                            changed |= cell.removeCandidate(candidate);
                        }
                    }
                }

                if(changed)
                    return true;
            }
        }
        return false;
    }

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
