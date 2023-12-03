package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import com.chgr.sudoku.models.TechniqueAction;
import org.apache.commons.math3.util.CombinatoricsUtils;

import java.util.*;
import java.util.stream.Collectors;

public class WingTechnique {

    private static class PossibleWing {
        int rowOrColIndex;
        List<Integer> cellIndices;
    }

    //https://www.sudokuwiki.org/X_Wing_Strategy
    //Section: X-Wing
    public static Optional<TechniqueAction> xWing(ISudoku sudoku) {
        return Optional.ofNullable(checkFinStructure(sudoku, 2));
    }

    //https://www.sudokuwiki.org/Sword_Fish_Strategy
    //Section: Swordfish
    public static boolean swordfish(ISudoku sudoku) {
        return checkFinStructureOld(sudoku, 3);
    }

    //https://www.sudokuwiki.org/Jelly_Fish_Strategy
    //Section: Jellyfish
    public static boolean jellyfish(ISudoku sudoku) {
        return checkFinStructureOld(sudoku, 4);
    }

    private static TechniqueAction checkFinStructure(ISudoku sudoku, int combSize) {
        TechniqueAction techniqueAction = checkFin(sudoku, true, combSize);
        if (techniqueAction != null)
            return techniqueAction;
        return checkFin(sudoku, false, combSize);
    }

    private static boolean checkFinStructureOld(ISudoku sudoku, int combSize) {
        return checkFinOld(sudoku, true, combSize) || checkFinOld(sudoku, false, combSize);
    }

    private static TechniqueAction checkFin(ISudoku sudoku, boolean isRow, int combSize) {
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
                if (cellsContainingCandidate.size() >= 2 && cellsContainingCandidate.size() <= combSize) {
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

            if (possibleWings.size() >= combSize) {
                List<List<PossibleWing>> combinations = generateCombinations(possibleWings, combSize);

                for (List<PossibleWing> combination : combinations) {
                    Set<Integer> groupIndices = new HashSet<>();
                    for (PossibleWing wing : combination) {
                        groupIndices.addAll(wing.cellIndices);
                    }

                    if (groupIndices.size() == combSize) {
                        boolean changed = false;
                        for (Integer groupIndex : groupIndices) {
                            ICell[] group = isRow ? sudoku.getColumn(groupIndex) : sudoku.getRow(groupIndex);
                            for (ICell cell : group) {
                                int index = isRow ? cell.getY() : cell.getX();
                                if (combination.stream().noneMatch(wing -> wing.rowOrColIndex == index)) {
                                    changed |= cell.removeCandidate(entry.getKey());
                                }
                            }
                        }
                        if (changed) {
                            return true;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean checkFinOld(ISudoku sudoku, boolean isRow, int combSize) {
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
                if (cellsContainingCandidate.size() >= 2 && cellsContainingCandidate.size() <= combSize) {
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

            if (possibleWings.size() >= combSize) {
                List<List<PossibleWing>> combinations = generateCombinations(possibleWings, combSize);

                for (List<PossibleWing> combination : combinations) {
                    Set<Integer> groupIndices = new HashSet<>();
                    for (PossibleWing wing : combination) {
                        groupIndices.addAll(wing.cellIndices);
                    }

                    if (groupIndices.size() == combSize) {
                        boolean changed = false;
                        for (Integer groupIndex : groupIndices) {
                            ICell[] group = isRow ? sudoku.getColumn(groupIndex) : sudoku.getRow(groupIndex);
                            for (ICell cell : group) {
                                int index = isRow ? cell.getY() : cell.getX();
                                if (combination.stream().noneMatch(wing -> wing.rowOrColIndex == index)) {
                                    changed |= cell.removeCandidate(entry.getKey());
                                }
                            }
                        }
                        if (changed) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static List<List<PossibleWing>> generateCombinations(List<PossibleWing> wings, int combSize) {
        List<List<PossibleWing>> allCombinations = new ArrayList<>();

        Iterator<int[]> iterator = CombinatoricsUtils.combinationsIterator(wings.size(), combSize);
        while (iterator.hasNext()) {
            int[] combinationIndices = iterator.next();
            allCombinations.add(Arrays.stream(combinationIndices).mapToObj(wings::get).toList());
        }

        return allCombinations;
    }


    //https://www.sudokuwiki.org/Y_Wing_Strategy
    //Section: Y-Wing
    public static boolean yWing(ISudoku sudoku) {
        Map<ICell, Set<ICell>> peerMap = new HashMap<>();

        for (ICell cell : sudoku.getEmptyCells()) {
            peerMap.put(cell, getPeers(sudoku, cell.getY(), cell.getX()));
        }

        for (ICell pivot : sudoku.getEmptyCells()) {
            if (pivot.getCandidates().size() == 2) {
                List<Integer> pivotCandidates = pivot.getCandidates().stream().toList();
                Integer A = pivotCandidates.get(0);
                Integer B = pivotCandidates.get(1);

                List<ICell> possibleWings = peerMap.get(pivot).stream()
                        .filter(cell -> cell.getCandidates().size() == 2)
                        .toList();

                for (ICell wing1 : possibleWings) {
                    if (wing1.getCandidates().contains(A) && !wing1.getCandidates().contains(B)) {
                        Integer C = wing1.getCandidates().stream().filter(candidate -> !candidate.equals(A)).findFirst().orElse(null);
                        if (C != null) {
                            for (ICell wing2 : possibleWings) {
                                if (!wing1.equals(wing2) && !isPeer(wing1.getX(), wing1.getY(), wing2.getX(), wing2.getY())
                                        && wing2.getCandidates().contains(B) && wing2.getCandidates().contains(C)) {
                                    Set<ICell> commonPeers = new HashSet<>(peerMap.get(wing1));
                                    commonPeers.retainAll(peerMap.get(wing2));

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


    //https://www.sudokuwiki.org/XYZ_Wing
    //Section: XYZ-Wing
    public static boolean xyzWing(ISudoku sudoku){
        Map<ICell, Set<ICell>> peerMap = new HashMap<>();

        for (ICell cell : sudoku.getEmptyCells()) {
            peerMap.put(cell, getPeers(sudoku, cell.getY(), cell.getX()));
        }

        for (ICell pivot : sudoku.getEmptyCells()) {
            if (pivot.getCandidates().size() == 3) {
                List<ICell> possibleWings = peerMap.get(pivot).stream()
                        .filter(cell -> cell.getCandidates().size() == 2)
                        .filter(wing -> pivot.getCandidates().containsAll(wing.getCandidates()))
                        .toList();

                for (int i = 0; i < possibleWings.size(); i++) {
                    for (int j = i + 1; j < possibleWings.size(); j++) {
                        ICell wing1 = possibleWings.get(i);
                        ICell wing2 = possibleWings.get(j);
                        if (!isPeer(wing1.getX(), wing1.getY(), wing2.getX(), wing2.getY())) {
                            Set<Integer> allCommonCandidates = new HashSet<>(pivot.getCandidates());
                            allCommonCandidates.retainAll(wing1.getCandidates());
                            allCommonCandidates.retainAll(wing2.getCandidates());

                            if (allCommonCandidates.size() == 1) {
                                Set<ICell> commonPeers = new HashSet<>(peerMap.get(pivot));
                                commonPeers.retainAll(peerMap.get(wing1));
                                commonPeers.retainAll(peerMap.get(wing2));

                                Integer candidateToRemove = allCommonCandidates.iterator().next();
                                boolean changed = false;
                                for (ICell peer : commonPeers) {
                                    changed |= peer.removeCandidate(candidateToRemove);
                                }
                                if (changed)
                                    return true;
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
