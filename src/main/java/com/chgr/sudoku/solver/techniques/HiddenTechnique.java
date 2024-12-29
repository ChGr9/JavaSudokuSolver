package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import com.chgr.sudoku.models.Pos;
import com.chgr.sudoku.models.TechniqueAction;
import javafx.scene.paint.Color;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HiddenTechnique {

    // https://www.sudokuwiki.org/Getting_Started
    // Sections: Last Remaining Cell in a Box & Last Remaining Cell in a Row (or Column)
    public static Optional<TechniqueAction> hiddenSingle(ISudoku sudoku){
        for(int i=0;i<ISudoku.SUDOKU_SIZE; i++){
            TechniqueAction techniqueAction = checkHiddenSingle(sudoku, i, ISudoku.GroupType.ROW);
            if(techniqueAction != null){
                return Optional.of(techniqueAction);
            }
            techniqueAction = checkHiddenSingle(sudoku, i, ISudoku.GroupType.COLUMN);
            if(techniqueAction != null){
                return Optional.of(techniqueAction);
            }
            techniqueAction = checkHiddenSingle(sudoku, i, ISudoku.GroupType.SQUARE);
            if(techniqueAction != null){
                return Optional.of(techniqueAction);
            }
        }
        return Optional.empty();
    }

    private static TechniqueAction checkHiddenSingle(ISudoku sudoku, int i, ISudoku.GroupType groupType) {
        //foreach digit check if only one cell in the provided group can have that digit as its value
        ICell[] group = switch (groupType) {
            case ROW -> sudoku.getRow(i);
            case COLUMN -> sudoku.getColumn(i);
            case SQUARE -> sudoku.getSquare(i);
        };
        for(int digit : ICell.DIGITS){
            List<ICell> cellsWithDigit = Arrays.stream(group).filter(c -> c.getCandidates().contains(digit)).toList();
            if(cellsWithDigit.size() == 1){
                ICell cell = cellsWithDigit.getFirst();

                return TechniqueAction.builder()
                        .name("Hidden Single")
                        .setValueMap(Map.of(cell.getPos(), digit))
                        .cellColorings(List.of(
                                new TechniqueAction.CandidatesColoring(List.of(cell.getPos()), Color.GREEN, List.of(digit)),
                                new TechniqueAction.GroupColoring(List.of(
                                        switch (groupType) {
                                            case ROW -> Pair.create(new Pos(0, i), new Pos(8, i));
                                            case COLUMN -> Pair.create(new Pos(i, 0), new Pos(i, 8));
                                            case SQUARE -> Pair.create(new Pos(i%3*3, i/3*3), new Pos(i%3*3+2, i/3*3+2));
                                        }),
                                        Color.YELLOW)
                                ))
                        .build();
            }
        }
        return null;
    }

    // https://www.sudokuwiki.org/Hidden_Candidates#HP
    // Section: Hidden Pairs
    public static Optional<TechniqueAction> hiddenPair(ISudoku sudoku) {
        for(int i=0;i<ISudoku.SUDOKU_SIZE; i++){
            TechniqueAction techniqueAction = checkHiddenTuple(sudoku, 2, i, ISudoku.GroupType.ROW);
            if(techniqueAction != null)
                return Optional.of(techniqueAction);
            techniqueAction = checkHiddenTuple(sudoku, 2, i, ISudoku.GroupType.COLUMN);
            if(techniqueAction != null)
                return Optional.of(techniqueAction);
            techniqueAction = checkHiddenTuple(sudoku, 2, i, ISudoku.GroupType.SQUARE);
            if(techniqueAction != null)
                return Optional.of(techniqueAction);
        }
        return Optional.empty();
    }

    // https://www.sudokuwiki.org/Hidden_Candidates#HT
    // Section: Hidden Triples
    public static Optional<TechniqueAction> hiddenTriple(ISudoku sudoku) {
        for(int i=0;i<ISudoku.SUDOKU_SIZE; i++){
            TechniqueAction techniqueAction = checkHiddenTuple(sudoku, 3, i, ISudoku.GroupType.ROW);
            if(techniqueAction != null)
                return Optional.of(techniqueAction);
            techniqueAction = checkHiddenTuple(sudoku, 3, i, ISudoku.GroupType.COLUMN);
            if(techniqueAction != null)
                return Optional.of(techniqueAction);
            techniqueAction = checkHiddenTuple(sudoku, 3, i, ISudoku.GroupType.SQUARE);
            if(techniqueAction != null)
                return Optional.of(techniqueAction);
        }
        return Optional.empty();
    }

    // https://www.sudokuwiki.org/Hidden_Candidates#HQ
    // Section: Hidden Quads
    public static Optional<TechniqueAction> hiddenQuad(ISudoku sudoku) {
        for(int i=0;i<ISudoku.SUDOKU_SIZE; i++){
            TechniqueAction techniqueAction = checkHiddenTuple(sudoku, 4, i, ISudoku.GroupType.ROW);
            if(techniqueAction != null)
                return Optional.of(techniqueAction);
            techniqueAction = checkHiddenTuple(sudoku, 4, i, ISudoku.GroupType.COLUMN);
            if(techniqueAction != null)
                return Optional.of(techniqueAction);
            techniqueAction = checkHiddenTuple(sudoku, 4, i, ISudoku.GroupType.SQUARE);
            if(techniqueAction != null)
                return Optional.of(techniqueAction);
        }
        return Optional.empty();
    }

    private static TechniqueAction checkHiddenTuple(ISudoku sudoku, int num, int i, ISudoku.GroupType groupType) {
        ICell[] group = switch (groupType) {
            case ROW -> sudoku.getRow(i);
            case COLUMN -> sudoku.getColumn(i);
            case SQUARE -> sudoku.getSquare(i);
        };
        Set<Integer> usedDigits = Arrays.stream(group)
                .map(ICell::getValue)
                .filter(value -> value != ICell.EMPTY)
                .collect(Collectors.toSet());
        List<Integer> availableDigits = ICell.DIGITS.stream()
                .filter(d -> !usedDigits.contains(d))
                .toList();
        if(availableDigits.size() < num)
            return null;
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

            if(cells.size() != num){
                continue;
            }

            Set<Integer> candidates = cells.stream()
                    .flatMap(cell -> cell.getCandidates().stream())
                    .collect(Collectors.toSet());

            if(!candidates.equals(combination)){
                Set<Integer> candidatesToRemove = candidates.stream()
                        .filter(c -> !combination.contains(c))
                        .collect(Collectors.toSet());

                if(!candidatesToRemove.isEmpty()) {
                    String type = switch (num) {
                        case 2 -> "Pair";
                        case 3 -> "Triple";
                        case 4 -> "Quad";
                        default -> "Tuple";
                    };
                    return TechniqueAction.builder()
                            .name("Hidden " + type)
                            .description("Cells " + cells.stream().map(ICell::getPos).map(Pos::toString).collect(Collectors.joining(", ")) + " form a hidden " + type + " in" + groupType.name() + " for the candidates " + candidates.stream().map(String::valueOf).collect(Collectors.joining(", ")))
                            .removeCandidatesMap(cells.stream().collect(Collectors.toMap(ICell::getPos, _ -> candidatesToRemove)))
                            .cellColorings(List.of(
                                    new TechniqueAction.CandidatesColoring(cells.stream().map(ICell::getPos).toList(), Color.GREEN, combination),
                                    new TechniqueAction.CandidatesColoring(cells.stream().map(ICell::getPos).toList(), Color.RED, candidatesToRemove),
                                    new TechniqueAction.GroupColoring(List.of(
                                            switch (groupType) {
                                                case ROW -> Pair.create(new Pos(0, i), new Pos(8, i));
                                                case COLUMN -> Pair.create(new Pos(i, 0), new Pos(i, 8));
                                                case SQUARE -> Pair.create(new Pos(i%3*3, i/3*3), new Pos(i%3*3+2, i/3*3+2));
                                            }),
                                            Color.YELLOW)
                            ))
                            .build();
                }
            }
        }
        return null;
    }
}
