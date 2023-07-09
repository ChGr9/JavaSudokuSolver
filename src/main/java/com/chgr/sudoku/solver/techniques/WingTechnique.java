package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class WingTechnique {

    private static class PossibleWing{
        int rowOrColIndex;
        List<Integer> cellIndices;
    }

    //https://www.sudokuwiki.org/X_Wing_Strategy
    //Section: X-Wing
    public static boolean xWing(ISudoku sudoku) {
        return checkXWing(sudoku, true) || checkXWing(sudoku, false);
    }

    private static boolean checkXWing(ISudoku sudoku, boolean isRow){
        Map<Integer, List<PossibleWing>> map = new HashMap<>();

        for(int i=0;i<ISudoku.SUDOKU_SIZE;i++){
            List<ICell> group = Arrays.stream((isRow ? sudoku.getRow(i) : sudoku.getColumn(i)))
                    .filter(cell -> cell.getValue() == ICell.EMPTY)
                    .toList();
            for(int j=1;j<=ISudoku.SUDOKU_SIZE; j++){
                int finalJ = j;
                List<ICell> cellsContainingCandidate = group.stream()
                        .filter(cell -> cell.getCandidates().contains(finalJ))
                        .toList();
                if(cellsContainingCandidate.size() == 2){
                    PossibleWing possibleWing = new PossibleWing();
                    possibleWing.rowOrColIndex = i;
                    possibleWing.cellIndices = cellsContainingCandidate.stream()
                            .map(cell -> isRow ? cell.getX() : cell.getY())
                            .collect(Collectors.toList());
                    map.computeIfAbsent(j, k -> new ArrayList<>()).add(possibleWing);
                }
            }
        }

        for (Map.Entry<Integer, List<PossibleWing>> entry : map.entrySet()) {
            List<PossibleWing> possibleWings = entry.getValue();
            if(possibleWings.size() >= 2) {
                for (int i = 0; i < possibleWings.size(); i++) {
                    for (int j = i + 1; j < possibleWings.size(); j++) {
                        PossibleWing possibleWing1 = possibleWings.get(i);
                        PossibleWing possibleWing2 = possibleWings.get(j);
                        if (CollectionUtils.isEqualCollection(possibleWing1.cellIndices, possibleWing2.cellIndices)) {
                            ICell[] group1 = isRow ? sudoku.getColumn(possibleWing1.cellIndices.get(0)) : sudoku.getRow(possibleWing1.cellIndices.get(0));
                            ICell[] group2 = isRow ? sudoku.getColumn(possibleWing1.cellIndices.get(1)) : sudoku.getRow(possibleWing1.cellIndices.get(1));
                            boolean changed = false;
                            for (ICell cell : group1) {
                                int index = isRow ? cell.getY() : cell.getX();
                                if (possibleWing1.rowOrColIndex != index && possibleWing2.rowOrColIndex != index) {
                                    changed |= cell.removeCandidate(entry.getKey());
                                }
                            }
                            for (ICell cell : group2) {
                                int index = isRow ? cell.getY() : cell.getX();
                                if (possibleWing1.rowOrColIndex != index && possibleWing2.rowOrColIndex != index) {
                                    changed |= cell.removeCandidate(entry.getKey());
                                }
                            }
                            if (changed)
                                return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
