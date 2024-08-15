package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import com.chgr.sudoku.models.Pos;
import com.chgr.sudoku.models.TechniqueAction;
import javafx.scene.paint.Color;
import org.apache.commons.math3.util.CombinatoricsUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NakedTechnique {
    // https://www.sudokuwiki.org/Getting_Started
    // Section: The Last Possible Number
    public static Optional<TechniqueAction> nakedSingle(ISudoku sudoku) {
        Optional<ICell> oCell = Arrays.stream(sudoku.getAllCells()).filter(c -> c.getCandidates().size() == 1).findFirst();
        if (oCell.isPresent()) {
            ICell cell = oCell.get();
            int value = cell.getCandidates().stream().findFirst().orElseThrow();
            return Optional.of(
                    TechniqueAction.builder()
                            .name("Naked Single")
                            .description("Value " + cell.getValue() + " is the only available candidate at cell (" + cell.getX() + "," + cell.getY() + ")" )
                            .setValueMap(Map.of(cell.getPos(), value))
                            .colorings(List.of(
                                    TechniqueAction.CellColoring.candidatesColoring(List.of(cell.getPos()), Color.GREEN, List.of(value))
                            ))
                            .build()
            );
        }
        return Optional.empty();
    }

    // https://www.sudokuwiki.org/Naked_Candidates#NP
    // Section: Naked Pairs
    public static Optional<TechniqueAction> nakedPair(ISudoku sudoku) {
        for(int i=0;i<ISudoku.SUDOKU_SIZE; i++){
            TechniqueAction techniqueAction = checkNakedTuple(sudoku, 2, i, ISudoku.GroupType.ROW);
            if(techniqueAction != null)
                return Optional.of(techniqueAction);
            techniqueAction = checkNakedTuple(sudoku, 2, i, ISudoku.GroupType.COLUMN);
            if(techniqueAction != null)
                return Optional.of(techniqueAction);
            techniqueAction = checkNakedTuple(sudoku, 2, i, ISudoku.GroupType.SQUARE);
            if(techniqueAction != null)
                return Optional.of(techniqueAction);
        }
        return Optional.empty();
    }

    // https://www.sudokuwiki.org/Naked_Candidates#NP
    // Section: Naked Triples
    public static Optional<TechniqueAction> nakedTriple(ISudoku sudoku) {
        for(int i=0;i<ISudoku.SUDOKU_SIZE; i++){
            TechniqueAction techniqueAction = checkNakedTuple(sudoku, 3, i, ISudoku.GroupType.ROW);
            if(techniqueAction != null)
                return Optional.of(techniqueAction);
            techniqueAction = checkNakedTuple(sudoku, 3, i, ISudoku.GroupType.COLUMN);
            if(techniqueAction != null)
                return Optional.of(techniqueAction);
            techniqueAction = checkNakedTuple(sudoku, 3, i, ISudoku.GroupType.SQUARE);
            if(techniqueAction != null)
                return Optional.of(techniqueAction);
        }
        return Optional.empty();
    }

    // https://www.sudokuwiki.org/Naked_Candidates#NP
    // Section: Naked Quads
    public static Optional<TechniqueAction> nakedQuad(ISudoku sudoku) {
        for(int i=0;i<ISudoku.SUDOKU_SIZE; i++){
            TechniqueAction techniqueAction = checkNakedTuple(sudoku, 4, i, ISudoku.GroupType.ROW);
            if(techniqueAction != null)
                return Optional.of(techniqueAction);
            techniqueAction = checkNakedTuple(sudoku, 4, i, ISudoku.GroupType.COLUMN);
            if(techniqueAction != null)
                return Optional.of(techniqueAction);
            techniqueAction = checkNakedTuple(sudoku, 4, i, ISudoku.GroupType.SQUARE);
            if(techniqueAction != null)
                return Optional.of(techniqueAction);
        }
        return Optional.empty();
    }

    private static TechniqueAction checkNakedTuple(ISudoku sudoku, int num, int i, ISudoku.GroupType groupType) {
        ICell[] group = switch (groupType) {
            case ROW -> sudoku.getRow(i);
            case COLUMN -> sudoku.getColumn(i);
            case SQUARE -> sudoku.getSquare(i);
        };
        List<ICell> emptyCells = Arrays.stream(group)
                .filter(c -> c.getValue() == ICell.EMPTY)
                .toList();
        List<ICell> cellsWithNumCandidates = emptyCells.stream()
                .filter(c -> c.getCandidates().size() <= num)
                .toList();

        if(cellsWithNumCandidates.size() < num) return null;

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
            if (combinedCandidates.size() != num) continue;

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
            List<ICell> affectedCells = emptyCells.stream()
                    .filter(c -> !combination.contains(c) && c.getCandidates().stream().anyMatch(combinedCandidates::contains))
                    .toList();

            if (!affectedCells.isEmpty()) {
                String type = switch (num) {
                    case 2 -> "Pair";
                    case 3 -> "Triple";
                    case 4 -> "Quad";
                    default -> "Tuple";
                };
                return TechniqueAction.builder()
                        .name("Naked " + type)
                        .description("Cells " + combination.stream().map(ICell::getPos).map(Pos::toString).collect(Collectors.joining(", ")) + " form a naked " + type + " in" + groupType.name() + " for the candidates " + combinedCandidates.stream().map(String::valueOf).collect(Collectors.joining(", ")))
                        .removeCandidatesMap(affectedCells.stream().collect(Collectors.toMap(ICell::getPos, c -> combinedCandidates)))
                        .colorings(List.of(
                                TechniqueAction.CellColoring.candidatesColoring(combination.stream().map(ICell::getPos).toList(), Color.GREEN, combinedCandidates),
                                TechniqueAction.CellColoring.candidatesColoring(affectedCells.stream().map(ICell::getPos).toList(), Color.RED, combinedCandidates)
                        ))
                        .build();
            }
        }
        return null;
    }
}
