package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import com.chgr.sudoku.models.Pos;
import com.chgr.sudoku.models.TechniqueAction;
import com.chgr.sudoku.utils.CellUtils;
import javafx.scene.paint.Color;
import lombok.Getter;
import org.apache.commons.math3.util.Combinations;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WingTechnique {

    @Getter
    private static class PossibleWing {
        int rowOrColIndex;
        List<Integer> cellIndices;
    }

    // https://www.sudokuwiki.org/X_Wing_Strategy
    // X-Wing
    public static Optional<TechniqueAction> xWing(ISudoku sudoku) {
        return checkFishStructure(sudoku, 2);
    }

    // https://www.sudokuwiki.org/Sword_Fish_Strategy
    // Swordfish
    public static Optional<TechniqueAction> swordfish(ISudoku sudoku) {
        return checkFishStructure(sudoku, 3);
    }

    // https://www.sudokuwiki.org/Jelly_Fish_Strategy
    // Jellyfish
    public static Optional<TechniqueAction> jellyfish(ISudoku sudoku) {
        return checkFishStructure(sudoku, 4);
    }

    private static Optional<TechniqueAction> checkFishStructure(ISudoku sudoku, int combSize) {
        Optional<TechniqueAction> techniqueAction = checkFish(sudoku, true, combSize);
        if (techniqueAction.isPresent())
            return techniqueAction;
        return checkFish(sudoku, false, combSize);
    }

    private static Optional<TechniqueAction> checkFish(ISudoku sudoku, boolean isRow, int combSize) {
        Map<Integer, List<PossibleWing>> map = generatePossibleWings(sudoku, isRow, combSize);

        for (Map.Entry<Integer, List<PossibleWing>> entry : map.entrySet()) {
            List<PossibleWing> possibleWings = entry.getValue();

            if (possibleWings.size() >= combSize) {
                Combinations combinations = new Combinations(possibleWings.size(), combSize);

                for (int[] comb : combinations) {
                    List<PossibleWing> combination = Arrays.stream(comb)
                            .mapToObj(possibleWings::get)
                            .toList();
                    Set<Integer> groupIndices = combination.stream()
                            .map(PossibleWing::getCellIndices)
                            .flatMap(List::stream)
                            .collect(Collectors.toSet());

                    if (groupIndices.size() == combSize) {
                        Set<Pos> affectedPos = new HashSet<>();
                        Set<Integer> combinationIndices = combination.stream()
                                .map(PossibleWing::getRowOrColIndex)
                                .collect(Collectors.toSet());

                        for (Integer groupIndex : groupIndices) {
                            ICell[] group = isRow ? sudoku.getColumn(groupIndex) : sudoku.getRow(groupIndex);
                            for (ICell cell : group) {
                                int index = isRow ? cell.getY() : cell.getX();
                                if (!combinationIndices.contains(index)) {
                                    Set<Integer> candidates = cell.getCandidates();
                                    Pos position = cell.getPos();
                                    if(candidates.contains(entry.getKey())){
                                        affectedPos.add(position);
                                    }
                                }
                            }
                        }
                        if(!affectedPos.isEmpty()){
                            String name = switch (combSize) {
                                case 2 -> "X-Wing";
                                case 3 -> "Swordfish";
                                case 4 -> "Jellyfish";
                                default -> "Fish";
                            };

                            Set<Pos> fishCells = groupIndices.stream()
                                    .flatMap(index -> combination.stream().map(combin -> isRow ?
                                            new Pos(index, combin.rowOrColIndex) :
                                            new Pos(combin.rowOrColIndex, index)
                                    )).collect(Collectors.toSet());

                            return Optional.of(TechniqueAction.builder()
                                    .name(name)
                                    .description("Cells: " + fishCells.stream().map(Pos::toString).collect(Collectors.joining(", "))
                                            + " form a " + name + " on " + (isRow? "rows " : "columns ")
                                            + combinationIndices.stream().map(String::valueOf).collect(Collectors.joining(", ")))
                                    .removeCandidatesMap(affectedPos.stream().collect(Collectors.toMap(pos -> pos, _ -> Set.of(entry.getKey()))))
                                    .cellColorings(List.of(
                                            new TechniqueAction.CandidatesColoring(fishCells, Color.ORANGE, Set.of(entry.getKey())),
                                            new TechniqueAction.GroupColoring(combinationIndices.stream().map(index -> isRow ?
                                                    Pair.create(new Pos(0, index), new Pos(8, index)) :
                                                    Pair.create(new Pos(index, 0), new Pos(index, 8))
                                            ).toList(), Color.YELLOW),
                                            new TechniqueAction.CandidatesColoring(affectedPos, Color.RED, Set.of(entry.getKey()))
                                    ))
                                    .build());
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Map<Integer, List<PossibleWing>> generatePossibleWings(ISudoku sudoku, boolean isRow, int combSize) {
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
                    map.computeIfAbsent(j, _ -> new ArrayList<>()).add(possibleWing);
                }
            }
        }
        return map;
    }

    // https://www.sudokuwiki.org/Y_Wing_Strategy
    // Y-Wing
    public static Optional<TechniqueAction> yWing(ISudoku sudoku) {
        Map<ICell, Set<ICell>> peerMap = new HashMap<>();

        for (ICell cell : sudoku.getEmptyCells()) {
            peerMap.put(cell, CellUtils.getPeers(sudoku, cell));
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
                                if (!wing1.equals(wing2) && isNotPeer(wing1.getX(), wing1.getY(), wing2.getX(), wing2.getY())
                                        && wing2.getCandidates().contains(B) && wing2.getCandidates().contains(C)) {
                                    Set<ICell> commonPeers = new HashSet<>(peerMap.get(wing1));
                                    commonPeers.retainAll(peerMap.get(wing2));

                                    List<ICell> toRemove = commonPeers.stream()
                                            .filter(cell -> cell != pivot && cell.getCandidates().contains(C))
                                            .toList();
                                    if(!toRemove.isEmpty()){
                                        Set<Pos> affectedPos = toRemove.stream().map(ICell::getPos).collect(Collectors.toSet());
                                        return Optional.of(TechniqueAction.builder()
                                                .name("Y-Wing")
                                                .description("Cells: " + pivot.getPos() + ", " + wing1.getPos() + ", " + wing2.getPos()
                                                        + " form a Y-Wing on candidates " + A + ", " + B + ", " + C)
                                                .removeCandidatesMap(affectedPos.stream().collect(Collectors.toMap(pos -> pos, _ -> Set.of(C))))
                                                .cellColorings(List.of(
                                                        new TechniqueAction.CandidatesColoring(Set.of(pivot.getPos(), wing1.getPos(), wing2.getPos()), Color.GREEN, Set.of(A, B, C)),
                                                        new TechniqueAction.CandidatesColoring(affectedPos, Color.RED, Set.of(C))
                                                ))
                                                .build());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }


    // https://www.sudokuwiki.org/XYZ_Wing
    // XYZ-Wing
    public static Optional<TechniqueAction> xyzWing(ISudoku sudoku){
        Map<ICell, Set<ICell>> peerMap = new HashMap<>();

        for (ICell cell : sudoku.getEmptyCells()) {
            peerMap.put(cell, CellUtils.getPeers(sudoku, cell));
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
                        if (isNotPeer(wing1.getX(), wing1.getY(), wing2.getX(), wing2.getY())) {
                            Set<Integer> allCommonCandidates = new HashSet<>(pivot.getCandidates());
                            allCommonCandidates.retainAll(wing1.getCandidates());
                            allCommonCandidates.retainAll(wing2.getCandidates());

                            if (allCommonCandidates.size() == 1) {
                                Set<ICell> commonPeers = new HashSet<>(peerMap.get(pivot));
                                commonPeers.retainAll(peerMap.get(wing1));
                                commonPeers.retainAll(peerMap.get(wing2));

                                Integer candidateToRemove = allCommonCandidates.iterator().next();
                                Set<Pos> toRemove = commonPeers.stream()
                                        .filter(cell -> cell.getCandidates().contains(candidateToRemove))
                                        .map(ICell::getPos)
                                        .collect(Collectors.toSet());
                                if(!toRemove.isEmpty()) {
                                    return Optional.of(TechniqueAction.builder()
                                            .name("XYZ-Wing")
                                            .description("Cells: " + pivot.getPos() + ", " + wing1.getPos() + ", " + wing2.getPos()
                                                    + " form a XYZ-Wing on candidates " + allCommonCandidates.iterator().next())
                                            .removeCandidatesMap(toRemove.stream().collect(Collectors.toMap(pos -> pos, _ -> Set.of(candidateToRemove))))
                                            .cellColorings(List.of(
                                                    new TechniqueAction.CandidatesColoring(Set.of(pivot.getPos()), Color.YELLOW, allCommonCandidates),
                                                    new TechniqueAction.CandidatesColoring(Set.of(wing1.getPos(), wing2.getPos()), Color.GREEN, allCommonCandidates),
                                                    new TechniqueAction.CandidatesColoring(toRemove, Color.RED, Set.of(candidateToRemove))
                                            )).build());
                                }
                            }
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    // https://www.sudokuwiki.org/WXYZ_Wing
    // WXYZ-Wing
    public static Optional<TechniqueAction> wxyzWing(ISudoku sudoku) {
        for(int i = 0; i<ISudoku.SUDOKU_SIZE; i++){
            Optional<TechniqueAction> techniqueAction = checkwxyzWing(sudoku, i, ISudoku.GroupType.ROW);
            if(techniqueAction.isPresent())
                return techniqueAction;
            techniqueAction = checkwxyzWing(sudoku, i, ISudoku.GroupType.COLUMN);
            if(techniqueAction.isPresent())
                return techniqueAction;
            techniqueAction = checkwxyzWing(sudoku, i, ISudoku.GroupType.SQUARE);
            if(techniqueAction.isPresent())
                return techniqueAction;
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> checkwxyzWing(ISudoku sudoku, int index, ISudoku.GroupType groupType) {
        List<ICell> group = Arrays.stream(switch (groupType) {
                    case ROW -> sudoku.getRow(index);
                    case COLUMN -> sudoku.getColumn(index);
                    case SQUARE -> sudoku.getSquare(index);
                })
                .filter(cell -> cell.getValue() == ICell.EMPTY)
                .toList();
        if (group.size() < 3)
            return Optional.empty();
        Combinations combinations = new Combinations(group.size(), 3);
        for (int[] combination : combinations) {
            ICell cell1 = group.get(combination[0]);
            ICell cell2 = group.get(combination[1]);
            ICell cell3 = group.get(combination[2]);

            Set<Integer> candidates = new HashSet<>(cell1.getCandidates());
            candidates.addAll(cell2.getCandidates());
            candidates.addAll(cell3.getCandidates());

            if (candidates.size() == 4) {
                Pair<List<ICell>, List<ICell>> groups = groupCells(cell1, cell2, cell3, groupType);
                if (groups == null)
                    continue;
                Optional<TechniqueAction> techniqueAction = checkwxyzWing(sudoku, groups.getFirst(), groups.getSecond(), candidates, groupType);
                if (techniqueAction.isPresent())
                    return techniqueAction;
                techniqueAction = checkwxyzWing(sudoku, groups.getSecond(), groups.getFirst(), candidates, groupType);
                if (techniqueAction.isPresent())
                    return techniqueAction;
            }
        }
        return Optional.empty();
    }

    private static Pair<List<ICell>, List<ICell>> groupCells(ICell cell1, ICell cell2, ICell cell3, ISudoku.GroupType groupType) {
        switch (groupType) {
            case ROW:
                return groupCells(cell1, cell2, cell3, true);
            case COLUMN:
                return groupCells(cell1, cell2, cell3, false);
            case SQUARE:
                Pair<List<ICell>, List<ICell>> groups = groupCells(cell1, cell2, cell3, true, 1);
                if (groups != null)
                    return groups;
                return groupCells(cell1, cell2, cell3, false, 1);
            default:
                throw new RuntimeException("Invalid group type");
        }
    }

    private static Pair<List<ICell>, List<ICell>> groupCells(ICell cell1, ICell cell2, ICell cell3, boolean isRow){
        return groupCells(cell1, cell2, cell3, isRow, 3);
    }

    private static Pair<List<ICell>, List<ICell>> groupCells(ICell cell1, ICell cell2, ICell cell3, boolean isRow, int divisor){
        List<Integer> squareIndices = Stream.of(cell1, cell2, cell3)
                .map(cell -> isRow ? cell.getX() / divisor : cell.getY() / divisor)
                .distinct()
                .toList();
        if (squareIndices.size() != 2)
            return null;
        List<ICell> square1 = Stream.of(cell1, cell2, cell3)
                .filter(cell -> isRow ? cell.getX() / divisor == squareIndices.getFirst() : cell.getY() / divisor == squareIndices.getFirst())
                .toList();
        List<ICell> square2 = Stream.of(cell1, cell2, cell3)
                .filter(cell -> isRow ? cell.getX() / divisor == squareIndices.getLast() : cell.getY() / divisor == squareIndices.getLast())
                .toList();
        return Pair.create(square1, square2);
    }

    private static Optional<TechniqueAction> checkwxyzWing(ISudoku sudoku, List<ICell> pivotGroup, List<ICell> wingGroup, Set<Integer> candidates, ISudoku.GroupType groupType) {
        if(groupType == ISudoku.GroupType.SQUARE && pivotGroup.size() == 2)
            //If we try and use a pivot group of size 2, for a square search, we will end up with a WXYZ-Wing with a WXYZ-Wing which would have found via the row or column search (which is slightly easier to understand and implement)
            return Optional.empty();
        Set<Integer> uniqueCandidates = pivotGroup.stream()
                .flatMap(cell -> cell.getCandidates().stream())
                .filter(candidate -> wingGroup.stream().noneMatch(cell -> cell.getCandidates().contains(candidate)))
                .collect(Collectors.toSet());
        for (int uniqueCandidate : uniqueCandidates) {
            Stream<ICell> groupStream = switch (groupType) {
                case ROW, COLUMN -> Arrays.stream(sudoku.getSquare(pivotGroup.getFirst().getSquare()));
                case SQUARE -> Stream.concat(Arrays.stream(sudoku.getRow(pivotGroup.getFirst().getY())), Arrays.stream(sudoku.getColumn(pivotGroup.getFirst().getX())))
                        .filter(cell -> cell.getSquare() != pivotGroup.getFirst().getSquare());
            };
            List<ICell> possibleCell4 = groupStream
                    .filter(iCell -> !pivotGroup.contains(iCell))
                    .filter(cell -> cell.getCandidates().contains(uniqueCandidate) && cell.getCandidates().size() == 2 && candidates.containsAll(cell.getCandidates()))
                    .toList();
            if (possibleCell4.isEmpty())
                continue;
            for (ICell cell4 : possibleCell4) {
                Integer candidateToRemove = cell4.getCandidates().stream().filter(candidate -> uniqueCandidate != candidate).findFirst().orElseThrow(() -> new RuntimeException("Invalid state"));
                Set<ICell> allWingCells = Stream.concat(Stream.concat(pivotGroup.stream(), wingGroup.stream()), Stream.of(cell4)).collect(Collectors.toSet());
                Set<ICell> cellsWithCandidateToRemove = allWingCells.stream()
                        .filter(cell -> cell.getCandidates().contains(candidateToRemove))
                        .collect(Collectors.toSet());
                Iterator<ICell> iterator = cellsWithCandidateToRemove.iterator();
                Set<ICell> affectedCells = CellUtils.getPeers(sudoku, iterator.next());
                while (iterator.hasNext()) {
                    affectedCells.retainAll(CellUtils.getPeers(sudoku, iterator.next()));
                }
                affectedCells.removeIf(cell -> !cell.getCandidates().contains(candidateToRemove));
                if (affectedCells.isEmpty())
                    continue;
                return Optional.of(TechniqueAction.builder()
                        .name("WXYZ-Wing")
                        .description("Cells: " + allWingCells.stream().map(ICell::getPos).map(Pos::toString).collect(Collectors.joining(", "))
                                + " form a WXYZ-Wing on candidates " + candidateToRemove)
                        .removeCandidatesMap(affectedCells.stream().map(ICell::getPos).collect(Collectors.toMap(pos -> pos, _ -> Set.of(candidateToRemove))))
                        .cellColorings(List.of(
                                new TechniqueAction.CandidatesColoring(allWingCells.stream().map(ICell::getPos).collect(Collectors.toSet()), Color.YELLOW, candidates),
                                new TechniqueAction.CandidatesColoring(affectedCells.stream().map(ICell::getPos).collect(Collectors.toSet()), Color.RED, Set.of(candidateToRemove))
                        ))
                        .build());
            }
        }
        return Optional.empty();
    }

    // https://www.sudokuwiki.org/Finned_X_Wing
    // Finned X-Wing
    public static Optional<TechniqueAction> finnedXWing(ISudoku sudoku) {
        return checkFinStructure(sudoku, 2);
    }

    // https://www.sudokuwiki.org/Finned_Swordfish
    // Finned Swordfish
    public static Optional<TechniqueAction> finnedSwordfish(ISudoku sudoku) {
        return checkFinStructure(sudoku, 3);
    }

    private static Optional<TechniqueAction> checkFinStructure(ISudoku sudoku, int combSize) {
        Optional<TechniqueAction> techniqueAction = checkFin(sudoku, true, combSize);
        if (techniqueAction.isPresent())
            return techniqueAction;
        return checkFin(sudoku, false, combSize);
    }

    private static Optional<TechniqueAction> checkFin(ISudoku sudoku, boolean isRow, int combSize) {
        Map<Integer, List<PossibleWing>> map = generatePossibleWings(sudoku, isRow, combSize);

        for (Map.Entry<Integer, List<PossibleWing>> entry : map.entrySet()){
            List<PossibleWing> possibleWings = entry.getValue();

            if(possibleWings.size() >= combSize - 1){
                Combinations combinations = new Combinations(possibleWings.size(), combSize - 1);

                for (int[] comb : combinations) {
                    List<PossibleWing> combination = Arrays.stream(comb)
                            .mapToObj(possibleWings::get)
                            .toList();
                    Set<Integer> groupIndices = combination.stream()
                            .map(PossibleWing::getCellIndices)
                            .flatMap(List::stream)
                            .collect(Collectors.toSet());
                    Set<Integer> rowOrColIndices = combination.stream()
                            .map(PossibleWing::getRowOrColIndex)
                            .collect(Collectors.toSet());

                    if(groupIndices.size() == combSize){
                        for(int i = 0; i < ISudoku.SUDOKU_SIZE; i++){
                            List<ICell> group = Arrays.stream((isRow ? sudoku.getRow(i) : sudoku.getColumn(i)))
                                    .filter(cell -> cell.getValue() != ICell.EMPTY || cell.getCandidates().contains(entry.getKey()))
                                    .toList();

                            Set<ICell> fishCells = group.stream()
                                    .filter(cell -> groupIndices.contains(isRow ? cell.getX() : cell.getY()))
                                    .collect(Collectors.toSet());
                            if(fishCells.size() == combSize && fishCells.stream().anyMatch(cell -> cell.getCandidates().contains(entry.getKey()))){
                                List<ICell> finCells = group.stream()
                                        .filter(cell -> !groupIndices.contains(isRow ? cell.getX() : cell.getY()) && cell.getValue() == ICell.EMPTY)
                                        .toList();
                                if(finCells.stream().map(ICell::getSquare).distinct().count() != 1)
                                    continue;
                                int finalI = i;
                                Set<Pos> affectedPos = Arrays.stream(sudoku.getSquare(finCells.getFirst().getSquare()))
                                        .filter(cell -> groupIndices.contains(isRow ? cell.getX() : cell.getY()) && !rowOrColIndices.contains(isRow ? cell.getY() : cell.getX()) && finalI != (isRow ? cell.getY() : cell.getX()))
                                        .filter(cell -> cell.getValue() == ICell.EMPTY && cell.getCandidates().contains(entry.getKey()))
                                        .map(ICell::getPos)
                                        .collect(Collectors.toSet());

                                if(!affectedPos.isEmpty()) {
                                    String name = switch (combSize) {
                                        case 2 -> "Finned X-Wing";
                                        case 3 -> "Finned Swordfish";
                                        default -> "Finned Fish";
                                    };

                                    Set<Pos> fishPos = groupIndices.stream()
                                            .flatMap(index -> rowOrColIndices.stream().map(rowOrColIndex -> isRow ?
                                                    new Pos(index, rowOrColIndex) :
                                                    new Pos(rowOrColIndex, index)
                                            )).collect(Collectors.toSet());
                                    fishPos.addAll(fishCells.stream().map(ICell::getPos).collect(Collectors.toSet()));

                                    return Optional.of(TechniqueAction.builder()
                                            .name(name)
                                            .description("Cells: " + groupIndices.stream().map(String::valueOf).collect(Collectors.joining(", "))
                                                    + " form a Finned X-Wing on " + (isRow ? "rows " : "columns ") + rowOrColIndices.stream().map(String::valueOf).collect(Collectors.joining(", ")))
                                            .removeCandidatesMap(affectedPos.stream().collect(Collectors.toMap(pos -> pos, _ -> Set.of(entry.getKey()))))
                                            .cellColorings(List.of(
                                                    new TechniqueAction.CandidatesColoring(fishPos, Color.YELLOW, Set.of(entry.getKey())),
                                                    new TechniqueAction.CandidatesColoring(finCells.stream().map(ICell::getPos).collect(Collectors.toSet()), Color.ORANGE, Set.of(entry.getKey())),
                                                    new TechniqueAction.CandidatesColoring(affectedPos, Color.RED, Set.of(entry.getKey()))
                                            ))
                                            .build());
                                }
                            }
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isNotPeer(int x1, int y1, int x2, int y2) {
        return x1 != x2 && y1 != y2 && (x1 / 3 != x2 / 3 || y1 / 3 != y2 / 3);
    }
}
