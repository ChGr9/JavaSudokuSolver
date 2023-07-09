package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import org.apache.commons.math3.util.CombinatoricsUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HiddenTechnique {

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
}
