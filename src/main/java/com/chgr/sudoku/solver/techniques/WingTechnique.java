package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class WingTechnique {

    private static class PossibleWing {
        int rowOrColIndex;
        List<Integer> cellIndices;
    }

    //https://www.sudokuwiki.org/X_Wing_Strategy
    //Section: X-Wing
    public static boolean xWing(ISudoku sudoku) {
        return checkXWing(sudoku, true) || checkXWing(sudoku, false);
    }

    private static boolean checkXWing(ISudoku sudoku, boolean isRow) {
        Map<Integer, List<PossibleWing>> map = new HashMap<>();

        for (int i = 0; i < ISudoku.SUDOKU_SIZE; i++) {
            List<ICell> group = Arrays.stream((isRow ? sudoku.getRow(i) : sudoku.getColumn(i)))
                    .filter(cell -> cell.getValue() == ICell.EMPTY)
                    .toList();
            for (int j = 1; j <= ISudoku.SUDOKU_SIZE; j++) {
                int finalJ = j;
                List<ICell> cellsContainingCandidate = group.stream()
                        .filter(cell -> cell.getCandidates().contains(finalJ))
                        .toList();
                if (cellsContainingCandidate.size() == 2) {
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
            if (possibleWings.size() >= 2) {
                for (int i = 0; i < possibleWings.size() - 1; i++) {
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

    //https://www.sudokuwiki.org/Y_Wing_Strategy
    //Section: Y-Wing
    public static boolean yWing(ISudoku sudoku) {
        for (ICell pivot : sudoku.getEmptyCells()) {

            // Find pivot cells with exactly two candidates
            if (pivot.getCandidates().size() == 2) {
                List<Integer> pivotCandidates = pivot.getCandidates().stream().toList();
                Integer A = pivotCandidates.get(0);
                Integer B = pivotCandidates.get(1);

                // Collect cells that are peers to the pivot and have only two candidates
                List<ICell> possibleWings = getPeers(sudoku, pivot.getY(), pivot.getX()).stream()
                        .filter(cell -> cell.getCandidates().size() == 2)
                        .toList();

                for (ICell wing1 : possibleWings) {
                    if (wing1.getCandidates().contains(A) && !wing1.getCandidates().contains(B)) {
                        // wing1 is AC type. Search for a BC wing.
                        for (ICell wing2 : possibleWings) {
                            if (!wing1.equals(wing2) && !isPeer(wing1.getX(), wing1.getY(), wing2.getX(), wing2.getY())
                                    && wing2.getCandidates().contains(B) && !wing2.getCandidates().contains(A)) {
                                Integer C = wing1.getCandidates().stream().filter(candidate -> !candidate.equals(A)).findFirst().orElse(null);
                                if (C != null && wing2.getCandidates().contains(C)) {
                                    // Identify common peers of wing1 and wing2
                                    Set<ICell> commonPeers = new LinkedHashSet<>(getPeers(sudoku, wing1.getY(), wing1.getX()));
                                    commonPeers.retainAll(getPeers(sudoku, wing2.getY(), wing2.getX()));

                                    // Remove candidate C from common peers
                                    boolean changed = false;
                                    for (ICell peer : commonPeers) {
                                        if (peer != pivot) {
                                            changed |= peer.removeCandidate(C);
                                        }
                                    }
                                    if (changed)
                                        return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static Set<ICell> getPeers(ISudoku sudoku, int row, int col) {
        Set<ICell> peers = new LinkedHashSet<>();
        peers.addAll(Set.of(sudoku.getColumn(col)));
        peers.addAll(Set.of(sudoku.getRow(row)));
        peers.addAll(Set.of(sudoku.getSquare(col, row)));
        peers.removeIf(cell -> cell.getX() == col && cell.getY() == row);
        return peers;
    }

    private static boolean isPeer(int x1, int y1, int x2, int y2) {
        return x1 == x2 || y1 == y2 || (x1 / 3 == x2 / 3 && y1 / 3 == y2 / 3);
    }
}
