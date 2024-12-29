package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.*;
import com.chgr.sudoku.utils.CellUtils;
import javafx.scene.paint.Color;
import org.apache.commons.math3.util.Combinations;
import org.apache.commons.math3.util.Pair;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IntersectionTechnique {

    // https://www.sudokuwiki.org/Intersection_Removal#IR
    // Section: Pointing Pairs, Pointing Triples
    public static Optional<TechniqueAction> pointingTuple(ISudoku sudoku) {

        for (int squareIndex = 0; squareIndex < ISudoku.SUDOKU_SIZE; squareIndex++) {
            ICell[] square = sudoku.getSquare(squareIndex);
            List<Set<Integer>> squareRowCandidates = new ArrayList<>();
            List<Set<Integer>> squareColumnCandidates = new ArrayList<>();

            for (int rowIndex = 0; rowIndex < ISudoku.SQUARE_SIZE; rowIndex++) {
                squareRowCandidates.add(getCandidateSetByY(square, rowIndex));
                squareColumnCandidates.add(getCandidateSetByX(square, rowIndex));
            }
            TechniqueAction techniqueAction = processCandidates(sudoku, square, squareIndex, squareRowCandidates, true);
            if (techniqueAction != null)
                return Optional.of(techniqueAction);
            techniqueAction = processCandidates(sudoku, square, squareIndex, squareColumnCandidates, false);
            if (techniqueAction != null)
                return Optional.of(techniqueAction);
        }

        return Optional.empty();
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

    private static TechniqueAction processCandidates(ISudoku sudoku, ICell[] square, int squareIndex, List<Set<Integer>> squareCandidates, boolean isRow) {
        for (int candidateIndex = 0; candidateIndex < squareCandidates.size(); candidateIndex++) {
            Set<Integer> currentCandidateSet = squareCandidates.get(candidateIndex);
            Set<Integer> distinctCandidates = getDistinctCandidates(currentCandidateSet, squareCandidates);

            if (!distinctCandidates.isEmpty()){
                TechniqueAction techniqueAction = removeCandidatesFromCells(sudoku, square, squareIndex, distinctCandidates, isRow, candidateIndex);
                if(techniqueAction != null)
                    return techniqueAction;
            }
        }

        return null;
    }

    private static TechniqueAction removeCandidatesFromCells(ISudoku sudoku, ICell[] square, int squareIndex, Set<Integer> distinctCandidates, boolean isRow, int offset) {
        int tupleAxis = (isRow ? square[0].getY() : square[0].getX()) + offset;
        ICell[] cells = isRow ? sudoku.getRow(tupleAxis) : sudoku.getColumn(tupleAxis);
        int startAxis = isRow ? square[0].getX() : square[0].getY();
        int endAxis = isRow ? square[8].getX() : square[8].getY();
        List<ICell> affectedCells = Arrays.stream(cells)
                .filter(cell -> (isRow?
                        cell.getX() < startAxis || cell.getX() > endAxis:
                        cell.getY() < startAxis || cell.getY() > endAxis)
                ).collect(Collectors.toList());

        Set<Integer> candidatesToBeRemoved = distinctCandidates.stream().filter(candidate -> affectedCells.stream().anyMatch(cell -> cell.getCandidates().contains(candidate))).collect(Collectors.toSet());

        affectedCells.removeIf(cell -> cell.getCandidates().stream().noneMatch(candidatesToBeRemoved::contains));

        List<Pos> pointingCells = Arrays.stream(square)
                .filter(cell -> cell.getCandidates().stream().anyMatch(candidatesToBeRemoved::contains))
                .map(ICell::getPos).toList();

        return affectedCells.isEmpty() ? null : TechniqueAction.builder()
                .name("Pointing tuple")
                .description("Cells " + pointingCells.stream().map(Pos::toString).collect(Collectors.joining(", ")) + " are the only cells in square " + squareIndex + " which have the candidate"+(candidatesToBeRemoved.size()==1?"":"s") + distinctCandidates.stream().map(String::valueOf).collect(Collectors.joining(", ")) + " this creates a pointing tuple in " + (isRow ? "row " : "column ") + tupleAxis + " for the candidates " + distinctCandidates.stream().map(String::valueOf).collect(Collectors.joining(", ")))
                .removeCandidatesMap(affectedCells.stream().map(ICell::getPos).collect(Collectors.toMap(pos -> pos, _ -> candidatesToBeRemoved)))
                .cellColorings(List.of(
                        new TechniqueAction.CandidatesColoring(affectedCells.stream().map(ICell::getPos).toList(), Color.RED, candidatesToBeRemoved),
                        new TechniqueAction.CandidatesColoring(pointingCells, Color.GREEN, candidatesToBeRemoved),
                        new TechniqueAction.GroupColoring(List.of(Pair.create(
                                new Pos(square[0].getX()/3*3, square[0].getY()/3*3),
                                new Pos(square[0].getX()/3*3+2, square[0].getY()/3*3+2)
                        )), Color.YELLOW)
                        ))
                .build();
    }


    private static Set<Integer> getDistinctCandidates(Set<Integer> currentCandidateSet, List<Set<Integer>> groupCandidates) {
        return currentCandidateSet.stream()
                .filter(candidate -> groupCandidates.stream()
                        .filter(set -> set != currentCandidateSet)
                        .noneMatch(set -> set.contains(candidate)))
                .collect(Collectors.toSet());
    }

    // https://www.sudokuwiki.org/Intersection_Removal#IR
    // Section: Box Line Reduction
    public static Optional<TechniqueAction> boxLineReduction(ISudoku sudoku) {
        for (int i = 0; i < Sudoku.SUDOKU_SIZE; i++) {
            TechniqueAction techniqueAction = checkBoxLineReduction(sudoku, i, true);
            if (techniqueAction != null)
                return Optional.of(techniqueAction);
            techniqueAction = checkBoxLineReduction(sudoku, i, false);
            if (techniqueAction != null)
                return Optional.of(techniqueAction);
        }
        return Optional.empty();
    }

    private static TechniqueAction checkBoxLineReduction(ISudoku sudoku, int index, boolean isRow) {
        ICell[] group = isRow ? sudoku.getRow(index) : sudoku.getColumn(index);
        List<Set<Integer>> groupSectionCandidates = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            groupSectionCandidates.add(IntStream.range(0, 3).mapToObj(num -> group[num + finalI * 3])
                    .flatMap(cell -> cell.getCandidates().stream())
                    .collect(Collectors.toSet()));
        }
        for (int i = 0; i < 3; i++) {
            Set<Integer> currentCandidateSet = groupSectionCandidates.get(i);
            Set<Integer> distinctCandidates = getDistinctCandidates(currentCandidateSet, groupSectionCandidates);
            if (!distinctCandidates.isEmpty()) {
                TechniqueAction techniqueAction = removeCandidatesFromSquare(sudoku, group, distinctCandidates, isRow, i);
                if (techniqueAction != null)
                    return techniqueAction;
            }
        }
        return null;
    }

    private static TechniqueAction removeCandidatesFromSquare(ISudoku sudoku, ICell[] group, Set<Integer> distinctCandidates, boolean isRow, int i) {
        ICell firstCell = group[i * 3];
        int index = firstCell.getY() / 3 * 3 + firstCell.getX() / 3;
        ICell[] cells = sudoku.getSquare(index);
        int limit = isRow ? firstCell.getY() : firstCell.getX();
        List<ICell> affectedCells = Arrays.stream(cells)
                .filter(cell -> isRow ?
                        cell.getY() != limit :
                        cell.getX() != limit
                ).collect(Collectors.toList());
        Set<Integer> candidatesToBeRemoved = distinctCandidates.stream().filter(candidate -> affectedCells.stream().anyMatch(cell -> cell.getCandidates().contains(candidate))).collect(Collectors.toSet());

        affectedCells.removeIf(cell -> cell.getCandidates().stream().noneMatch(candidatesToBeRemoved::contains));

        List<Pos> pointingCells = IntStream.range(0, 3).mapToObj(num -> group[num + i * 3].getPos()).toList();

        return affectedCells.isEmpty() ? null : TechniqueAction.builder()
                .name("Box line reduction")
                .description("Cells " + pointingCells.stream().map(Pos::toString).collect(Collectors.joining(", ")) + " are the only cells in " + (isRow ? "row " : "column ") + (isRow ? firstCell.getY() : firstCell.getX()) + " which have the candidate" + (candidatesToBeRemoved.size() == 1 ? "" : "s") + candidatesToBeRemoved.stream().map(String::valueOf).collect(Collectors.joining(", ")) + " this creates a box line reduction in square " + index + " for the candidates " + candidatesToBeRemoved.stream().map(String::valueOf).collect(Collectors.joining(", ")))
                .removeCandidatesMap(affectedCells.stream().map(ICell::getPos).collect(Collectors.toMap(pos -> pos, _ -> candidatesToBeRemoved)))
                .cellColorings(List.of(
                        new TechniqueAction.CandidatesColoring(affectedCells.stream().map(ICell::getPos).toList(), Color.RED, candidatesToBeRemoved),
                        new TechniqueAction.CandidatesColoring(pointingCells, Color.GREEN, candidatesToBeRemoved),
                        new TechniqueAction.GroupColoring(List.of(isRow ?
                                        Pair.create(new Pos(0, firstCell.getY()), new Pos(8, firstCell.getY())) :
                                        Pair.create(new Pos(firstCell.getX(), 0), new Pos(firstCell.getX(), 8)))
                                , Color.YELLOW)
                ))
                .build();
    }

    // https://www.sudokuwiki.org/Fireworks
    // Section: Triple Firework
    // Did not implement Quadruple Firework as it is a very rare case and implementing wouldn't be worth it since other techniques can be used to substitute it
    public static Optional<TechniqueAction> firework(ISudoku sudoku) {
        List<ICell> possibleCells = sudoku.getEmptyCells().stream()
                .filter(cell -> cell.getCandidates().size() >= 3)
                .toList();
        for (ICell cell : possibleCells){
            List<Integer> candidates = cell.getCandidates().stream().sorted().toList();
            Combinations combinations = new Combinations(cell.getCandidates().size(), 3);
            for(int[] combination : combinations){
                Set<Integer> combinationCandidates = Arrays.stream(combination).mapToObj(candidates::get).collect(Collectors.toSet());
                List<ICell> rowCells = Arrays.stream(sudoku.getRow(cell.getY())).filter(c -> c.getX()/3 != cell.getX()/3).toList();
                List<ICell> matchingRowCells = rowCells.stream().filter(c -> c.getCandidates().stream().anyMatch(combinationCandidates::contains)).toList();
                if(matchingRowCells.size() != 1)
                    continue;
                List<ICell> columnCells = Arrays.stream(sudoku.getColumn(cell.getX())).filter(c -> c.getY()/3 != cell.getY()/3).toList();
                List<ICell> matchingColumnCells = columnCells.stream().filter(c -> c.getCandidates().stream().anyMatch(combinationCandidates::contains)).toList();
                if(matchingColumnCells.size() != 1)
                    continue;

                Map<Pos, Set<Integer>> otherCandidateMap = Stream.of(cell, matchingRowCells.getFirst(), matchingColumnCells.getFirst())
                        .map(c -> Pair.create(c.getPos(), c.getCandidates().stream().filter(candidate -> !combinationCandidates.contains(candidate)).collect(Collectors.toSet())))
                        .filter(pair -> !pair.getSecond().isEmpty())
                        .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
                if(otherCandidateMap.isEmpty())
                    continue;
                List<TechniqueAction.CellColoring> colorings = new ArrayList<>();
                otherCandidateMap.forEach((pos, otherCandidates) -> colorings.add(new TechniqueAction.CandidatesColoring(List.of(pos), Color.RED, otherCandidates)));
                colorings.add(new TechniqueAction.CandidatesColoring(List.of(cell.getPos(), matchingRowCells.getFirst().getPos(), matchingColumnCells.getFirst().getPos()), Color.GREEN, combinationCandidates));
                colorings.add(new TechniqueAction.GroupColoring(
                        getConsecutiveRanges(rowCells.stream().map(ICell::getX).filter(x -> x != matchingRowCells.getFirst().getX()).toList())
                                .stream().map(pair -> Pair.create(new Pos(pair.getFirst(), cell.getY()), new Pos(pair.getSecond(), cell.getY()))).toList()
                        , Color.BLUE));
                colorings.add(new TechniqueAction.GroupColoring(
                        getConsecutiveRanges(columnCells.stream().map(ICell::getY).filter(y -> y != matchingColumnCells.getFirst().getY()).toList())
                                .stream().map(pair -> Pair.create(new Pos(cell.getX(), pair.getFirst()), new Pos(cell.getX(), pair.getSecond()))).toList()
                        , Color.BLUE));

                return Optional.of(TechniqueAction.builder()
                        .name("Firework")
                        .description(MessageFormat.format("Cells {0} and {1} are the only cells in their respective row and column outside square {2} which have the candidates {3}, this creates a firework with cell {4} which is the intersection of the row and column, the cells {0}, {1} and {4} will need to the candidates {3} and the the other candidates are eliminated",
                                matchingRowCells.getFirst().getPos(), matchingColumnCells.getFirst().getPos(),
                                cell.getX()/3 + cell.getY()/3*3,
                                combinationCandidates.stream().map(String::valueOf).collect(Collectors.joining(", ")),
                                cell.getPos()
                                ))
                        .removeCandidatesMap(otherCandidateMap)
                        .cellColorings(colorings).build());
            }
        }
        return Optional.empty();
    }

    private static List<Pair<Integer, Integer>> getConsecutiveRanges(List<Integer> numbers){
        List<Integer> sortedNumbers = numbers.stream().sorted().toList();
        List<Pair<Integer, Integer>> ranges = new ArrayList<>();
        int start = sortedNumbers.getFirst();
        int end = start;

        for(int i = 1; i < sortedNumbers.size(); i++){
            if(sortedNumbers.get(i) == end + 1){
                end = sortedNumbers.get(i);
            } else {
                ranges.add(Pair.create(start, end));
                start = sortedNumbers.get(i);
                end = start;
            }
        }
        ranges.add(Pair.create(start, end));
        return ranges;
    }

    // https://www.sudokuwiki.org/Aligned_Pair_Exclusion
    // Aligned Pair Exclusion
    // Note no tests were written for this method as it triggers for several cells and it is hard to test since each execution can have different but correct results
    public static Optional<TechniqueAction> alignedPairExclusion(ISudoku sudoku) {
        Set<ICell> emptyCells = sudoku.getEmptyCells();
        for (ICell cell : emptyCells) {
            Set<ICell> cellPeers = CellUtils.getPeers(sudoku, cell);
            for (ICell otherCell : emptyCells) {
                if (cell == otherCell)
                    continue;
                Set<ICell> commonPeers = CellUtils.getPeers(sudoku, otherCell).stream().filter(cellPeers::contains).filter(c -> c.getValue() == ICell.EMPTY).collect(Collectors.toSet());
                Set<Set<Integer>> restrictedCandidates = getRestrictedCandidates(commonPeers);
                List<Pair<Integer, Integer>> pairs = cell.getCandidates().stream()
                        .flatMap(candidate -> otherCell.getCandidates().stream().map(otherCandidate -> Pair.create(candidate, otherCandidate)))
                        .filter(pair -> {
                            if(pair.getFirst().equals(pair.getSecond()))
                                return !CellUtils.isPeer(cell, otherCell);
                            else
                                return restrictedCandidates.stream()
                                        .noneMatch(set -> set.contains(pair.getFirst()) && set.contains(pair.getSecond()));
                        })
                        .toList();
                List<Integer> candidatesToRemoveFromCell = cell.getCandidates().stream()
                        .filter(candidate -> pairs.stream().noneMatch(pair -> pair.getFirst().equals(candidate)))
                        .toList();
                if (!candidatesToRemoveFromCell.isEmpty())
                    return buildAlignedPairExclusionAction(cell, otherCell, commonPeers, candidatesToRemoveFromCell);
                List<Integer> candidatesToRemoveFromOtherCell = otherCell.getCandidates().stream()
                        .filter(candidate -> pairs.stream().noneMatch(pair -> pair.getSecond().equals(candidate)))
                        .toList();
                if (!candidatesToRemoveFromOtherCell.isEmpty())
                    return buildAlignedPairExclusionAction(otherCell, cell, commonPeers, candidatesToRemoveFromOtherCell);
            }
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> buildAlignedPairExclusionAction(ICell mainCell, ICell otherCell, Set<ICell> commonPeers, List<Integer> candidatesToRemove) {
        return Optional.of(TechniqueAction.builder()
                .name("Aligned pair exclusion")
                .description(MessageFormat.format("Cells {0} and {1} have the candidates {2} and {3} respectively, they share the peers {4} and the candidates {5} cannot be the value for cell {0} cause all combinations with cell {1} will cause a peer cell to have no value",
                        mainCell.getPos(), otherCell.getPos(),
                        mainCell.getCandidates(), otherCell.getCandidates(),
                        commonPeers.stream().map(ICell::getPos),
                        candidatesToRemove))
                .removeCandidatesMap(Map.of(mainCell.getPos(), new HashSet<>(candidatesToRemove)))
                .cellColorings(List.of(
                        new TechniqueAction.CandidatesColoring(List.of(mainCell.getPos()), Color.RED, candidatesToRemove),
                        new TechniqueAction.GroupColoring(commonPeers.stream().map(ICell::getPos).map(pos -> Pair.create(pos, pos)).toList(), Color.YELLOW),
                        new TechniqueAction.GroupColoring(List.of(Pair.create(mainCell.getPos(), mainCell.getPos()), Pair.create(otherCell.getPos(), otherCell.getPos())), Color.GREY)
                ))
                .build());
    }

    private static Set<Set<Integer>> getRestrictedCandidates(Set<ICell> commonPeers) {
        Set<Set<Integer>> restrictedCandidates = commonPeers.stream().map(ICell::getCandidates).filter(candidates -> candidates.size() == 2).collect(Collectors.toSet());
        Set<Integer> rows = commonPeers.stream().collect(Collectors.groupingBy(ICell::getY, Collectors.counting())).entrySet().stream().filter(entry -> entry.getValue() >= 2).map(Map.Entry::getKey).collect(Collectors.toSet());
        for(int row : rows){
            List<ICell> rowCells = commonPeers.stream().filter(cell -> cell.getY() == row).toList();
            restrictedCandidates.addAll(getRestrictedCandidatesFromPeerCells(rowCells));
        }
        Set<Integer> columns = commonPeers.stream().collect(Collectors.groupingBy(ICell::getX, Collectors.counting())).entrySet().stream().filter(entry -> entry.getValue() >= 2).map(Map.Entry::getKey).collect(Collectors.toSet());
        for(int column : columns){
            List<ICell> columnCells = commonPeers.stream().filter(cell -> cell.getX() == column).toList();
            restrictedCandidates.addAll(getRestrictedCandidatesFromPeerCells(columnCells));
        }
        Set<Integer> squares = commonPeers.stream().collect(Collectors.groupingBy(ICell::getSquare, Collectors.counting())).entrySet().stream().filter(entry -> entry.getValue() >= 2).map(Map.Entry::getKey).collect(Collectors.toSet());
        for(int square : squares){
            List<ICell> squareCells = commonPeers.stream().filter(cell -> cell.getSquare() == square).toList();
            restrictedCandidates.addAll(getRestrictedCandidatesFromPeerCells(squareCells));
        }
        return restrictedCandidates;
    }

    private static Collection<? extends Set<Integer>> getRestrictedCandidatesFromPeerCells(List<ICell> cells) {
        Set<Set<Integer>> restrictedCandidates = new HashSet<>();
        for(int i = 2; i <= cells.size(); i++){
            Combinations combinations = new Combinations(cells.size(), i);
            for(int[] combination : combinations){
                Set<Integer> candidates = Arrays.stream(combination).mapToObj(cells::get).flatMap(cell -> cell.getCandidates().stream()).collect(Collectors.toSet());
                if(candidates.size() == i+1)
                    restrictedCandidates.add(candidates);
            }
        }
        return restrictedCandidates;
    }
}
