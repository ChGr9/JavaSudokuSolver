package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import org.apache.commons.math3.util.CombinatoricsUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NakedTechnique {
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
}
