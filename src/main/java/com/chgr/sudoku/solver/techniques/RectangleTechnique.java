package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import com.chgr.sudoku.models.Pos;
import com.chgr.sudoku.models.TechniqueAction;
import javafx.scene.paint.Color;
import org.apache.commons.math3.util.Combinations;
import org.apache.commons.math3.util.Pair;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.chgr.sudoku.utils.CellUtils.getPeers;
import static com.chgr.sudoku.utils.CellUtils.isPeer;

public class RectangleTechnique {

    // https://www.sudokuwiki.org/Unique_Rectangles
    // Unique Rectangles
    public static Optional<TechniqueAction> uniqueRectangle(ISudoku sudoku) {
        Map<Set<Integer>, List<ICell>> biValueMap = sudoku.getEmptyCells().stream()
                .filter(c -> c.getCandidates().size() == 2)
                .collect(Collectors.groupingBy(ICell::getCandidates));
        for(Set<Integer> candidates : biValueMap.keySet()) {
            List<ICell> cells = biValueMap.get(candidates);
            if(cells.size() >= 2) {
                Optional<TechniqueAction> techniqueAction = CheckLinearPairs(sudoku, cells);
                if (techniqueAction.isPresent())
                    return techniqueAction;
                techniqueAction = CheckDiagonalPairs(sudoku, cells);
                if (techniqueAction.isPresent())
                    return techniqueAction;
            }
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> CheckLinearPairs(ISudoku sudoku, List<ICell> cells) {
        for(int i = 0; i< cells.size() - 1; i++){
            ICell cell1 = cells.get(i);
            for(int j = i+1; j < cells.size(); j++){
                ICell cell2 = cells.get(j);
                if(cell1.getX() == cell2.getX()) {
                    Optional<TechniqueAction> techniqueAction = checkFloor(sudoku, cells, cell1, cell2, false);
                    if(techniqueAction.isPresent())
                        return techniqueAction;
                } else if (cell1.getY() == cell2.getY()) {
                    Optional<TechniqueAction> techniqueAction = checkFloor(sudoku, cells, cell1, cell2, true);
                    if(techniqueAction.isPresent())
                        return techniqueAction;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> CheckDiagonalPairs(ISudoku sudoku, List<ICell> cells){
        for(int i = 0; i< cells.size() - 1; i++){
            ICell cell1 = cells.get(i);
            for(int j = i+1; j < cells.size(); j++) {
                ICell cell2 = cells.get(j);
                if (cell1.getX() != cell2.getX() && cell1.getY() != cell2.getY()) {
                    Optional<TechniqueAction> techniqueAction = checkForDiagonal(sudoku, cell1, cell2);
                    if (techniqueAction.isPresent())
                        return techniqueAction;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> checkForDiagonal(ISudoku sudoku, ICell cell1, ICell cell2) {
        ICell cell3 = sudoku.getCell(cell1.getX(), cell2.getY());
        if(cell3.getValue() != ICell.EMPTY || !cell3.getCandidates().containsAll(cell1.getCandidates()))
            return Optional.empty();
        ICell cell4 = sudoku.getCell(cell2.getX(), cell1.getY());
        if(cell4.getValue() != ICell.EMPTY || !cell4.getCandidates().containsAll(cell1.getCandidates()))
            return Optional.empty();
        Set<Integer> extraCandidates = extractExtraCandidates(cell1.getCandidates(), cell3, cell4);
        //Type 2C
        if(extraCandidates.size() == 1){
            Set<ICell> commonPeers = getPeers(sudoku, cell3);
            commonPeers.retainAll(getPeers(sudoku, cell4));
            Map<Pos, Set<Integer>> removeCandidatesMap = commonPeers.stream().map(cell -> Map.entry(cell.getPos(), cell.getCandidates().stream().filter(extraCandidates::contains).collect(Collectors.toSet())))
                    .filter(entry -> !entry.getValue().isEmpty())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            return Optional.of(TechniqueAction.builder()
                    .name("Unique Rectangle")
                    .description(MessageFormat.format("If cell {0} or {1} is not {2} then there is no way to disambiguate the values {3} for the cells {0}, {1}, {4} and {5}, so value {2} cannot appear in the cells {6} since they see both {0} and {1}",
                            cell3.getPos(), cell4.getPos(), extraCandidates, cell1.getCandidates(), cell1.getPos(), cell2.getPos(), removeCandidatesMap.keySet()))
                    .removeCandidatesMap(removeCandidatesMap)
                    .cellColorings(List.of(
                            new TechniqueAction.CandidatesColoring(List.of(cell1.getPos(), cell2.getPos(), cell3.getPos(), cell4.getPos()), Color.YELLOW, cell1.getCandidates()),
                            new TechniqueAction.CandidatesColoring(removeCandidatesMap.keySet(), Color.RED, extraCandidates)
                    )).build());
        }
        //Type 5
        for(int candidate : cell1.getCandidates()){
            if(Arrays.stream(sudoku.getRow(cell1.getY())).filter(c -> c.getCandidates().contains(candidate)).count() == 2 &&
                    Arrays.stream(sudoku.getColumn(cell1.getX())).filter(c -> c.getCandidates().contains(candidate)).count() == 2 &&
                    Arrays.stream(sudoku.getRow(cell2.getY())).filter(c -> c.getCandidates().contains(candidate)).count() == 2 &&
                    Arrays.stream(sudoku.getColumn(cell2.getX())).filter(c -> c.getCandidates().contains(candidate)).count() == 2){
                int otherCandidate = cell1.getCandidates().stream().filter(c -> c != candidate).findFirst().orElseThrow(() -> new RuntimeException("No other candidate"));
                return Optional.of(TechniqueAction.builder()
                        .name("Unique Rectangle")
                        .description(MessageFormat.format("Cells {0} and {1} are strongly linked meaning they have the same value. They have 2 possibilities, if the value {2} is the value for {0} and {1} then cells {3} and {4} are forced to have the value {5} which is a deadly pattern since we can switch {0}, {1}, {3} and {4}",
                                cell1, cell2, otherCandidate, cell3, cell4, candidate))
                        .setValueMap(Map.of(cell1.getPos(), candidate, cell2.getPos(), candidate))
                        .cellColorings(List.of(
                                new TechniqueAction.CandidatesColoring(List.of(cell1.getPos(), cell2.getPos()), Color.GREEN, Set.of(candidate)),
                                new TechniqueAction.CandidatesColoring(List.of(cell1.getPos(), cell2.getPos()), Color.RED, Set.of(otherCandidate)),
                                new TechniqueAction.CandidatesColoring(List.of(cell3.getPos(), cell4.getPos()), Color.YELLOW, cell1.getCandidates()),
                                new TechniqueAction.LineColoring(List.of(Pair.create(cell1.getPos(), cell2.getPos())), Color.BLUE, candidate, false)
                        )).build());
            }
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> checkFloor(ISudoku sudoku, List<ICell> cells, ICell cell1, ICell cell2, boolean isRow){
        boolean sameSquare = isRow? cell1.getX()/3 == cell2.getX()/3
                : cell1.getY()/3 == cell2.getY()/3;
        if(sameSquare){
            //Type 1
            Optional<TechniqueAction> techniqueActionLShape = checkForLShape(sudoku, cells, cell1, cell2, isRow);
            if(techniqueActionLShape.isPresent())
                return techniqueActionLShape;

            //Type 2A and 3B
            Optional<TechniqueAction> techniqueActionSameSquareRoof = checkForSameSquareRoof(sudoku, cell1, cell2, isRow);
            if(techniqueActionSameSquareRoof.isPresent())
                return techniqueActionSameSquareRoof;
        } else {
            //Type 2B and 3A
            Optional<TechniqueAction> techniqueActionDifferentSquareRoof = checkForDifferentSquareRoof(sudoku, cell1, cell2, isRow);
            if(techniqueActionDifferentSquareRoof.isPresent())
                return techniqueActionDifferentSquareRoof;
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> checkForDifferentSquareRoof(ISudoku sudoku, ICell cell1, ICell cell2, boolean isRow) {
        int floorRowCol = isRow ? cell1.getY() : cell1.getX();
        int floorFirstRowColSquare = floorRowCol / 3 * 3;
        for(int i=floorFirstRowColSquare; i< floorFirstRowColSquare+3;i++){
            if(floorRowCol == i)
                continue;
            ICell cell3 = isRow ? sudoku.getCell(cell1.getX(), i) : sudoku.getCell(i, cell1.getY());
            if(cell3.getValue() != ICell.EMPTY || !cell3.getCandidates().containsAll(cell1.getCandidates()))
                continue;
            ICell cell4 = isRow ? sudoku.getCell(cell2.getX(), i) : sudoku.getCell(i, cell2.getY());
            if(cell4.getValue() != ICell.EMPTY || !cell4.getCandidates().containsAll(cell1.getCandidates()))
                continue;
            Set<Integer> extraCandidates = extractExtraCandidates(cell1.getCandidates(), cell3, cell4);
            //Type 2B
            if(extraCandidates.size() == 1){
                List<Pos> affectedPos = new ArrayList<>();
                for(ICell cell : isRow ? sudoku.getRow(i): sudoku.getColumn(i)){
                    if(cell != cell3 && cell != cell4){
                        Set<Integer> removedCandidates = cell.getCandidates().stream().filter(extraCandidates::contains).collect(Collectors.toSet());
                        if(!removedCandidates.isEmpty()){
                            affectedPos.add(cell.getPos());
                        }
                    }
                }
                if(!affectedPos.isEmpty())
                    return Optional.of(TechniqueAction.builder()
                            .name("Unique Rectangle")
                            .description(MessageFormat.format("If cell {0} or {1} is not {2} then there is no way to disambiguate the values {3} for the cells {0}, {1}, {4} and {5}, so value {2} cannot appear in the cells {6} since it sees both {0} and {1}",
                                    cell3.getPos(), cell4.getPos(), extraCandidates, cell1.getCandidates(), cell1.getPos(), cell2.getPos(), affectedPos))
                            .removeCandidatesMap(affectedPos.stream().collect(Collectors.toMap(pos -> pos, _ -> extraCandidates)))
                            .cellColorings(List.of(
                                    new TechniqueAction.CandidatesColoring(List.of(cell1.getPos(), cell2.getPos(), cell3.getPos(), cell4.getPos()), Color.YELLOW, cell1.getCandidates()),
                                    new TechniqueAction.CandidatesColoring(affectedPos, Color.RED, extraCandidates),
                                    new TechniqueAction.GroupColoring(List.of(isRow ?
                                                    Pair.create(new Pos(0, i), new Pos(8, i)) :
                                                    Pair.create(new Pos(i, 0), new Pos(i, 8)))
                                            , Color.ORANGE)
                            )).build());
            }
            List<ICell> groupCells = Arrays.stream(isRow ? sudoku.getRow(i): sudoku.getColumn(i))
                    .filter(c -> c != cell3 && c != cell4 && c.getValue() == ICell.EMPTY)
                    .toList();
            //Type 3A
            if (extraCandidates.size() == 2) {
                Pair<Pos, Map<Pos, Set<Integer>>> result = checkComplementaryCell(extraCandidates, groupCells);
                if(result != null && !result.getSecond().isEmpty()){
                    return Optional.of(TechniqueAction.builder()
                            .name("Unique Rectangle")
                            .description(MessageFormat.format("If cell {0} or {1} is not {2} then there is no way to disambiguate the values {3} for the cells {0}, {1}, {4} and {5}, so {0}, {1} and {6} are the only cells in {7} {8} that can have the values {2} and the cells {9} cannot have the values {2}",
                                    cell3.getPos(), cell4.getPos(), extraCandidates, cell1.getCandidates(), cell1.getPos(), cell2.getPos(), result.getFirst(), isRow? "row" : "column", i, result.getSecond().keySet()))
                            .removeCandidatesMap(result.getSecond())
                            .cellColorings(List.of(
                                    new TechniqueAction.CandidatesColoring(List.of(cell1.getPos(), cell2.getPos(), cell3.getPos(), cell4.getPos()), Color.YELLOW, cell1.getCandidates()),
                                    new TechniqueAction.CandidatesColoring(result.getSecond().keySet(), Color.RED, extraCandidates),
                                    new TechniqueAction.CandidatesColoring(List.of(result.getFirst()), Color.BLUE, extraCandidates),
                                    new TechniqueAction.GroupColoring(List.of(isRow ?
                                                    Pair.create(new Pos(0, i), new Pos(8, i)) :
                                                    Pair.create(new Pos(i, 0), new Pos(i, 8)))
                                            , Color.ORANGE)
                            )).build());
                }
                //Type 3 with Triple Pseudo-Cells
                Pair<List<Pos>, Map<Pos, Set<Integer>>> pairResult = checkComplementaryPair(extraCandidates, groupCells);
                if(pairResult != null && !pairResult.getSecond().isEmpty()) {
                    return Optional.of(TechniqueAction.builder()
                            .name("Unique Rectangle")
                            .description(MessageFormat.format("If cell {0} or {1} is not {2} then there is no way to disambiguate the values {3} for the cells {0}, {1}, {4} and {5}, so {0}, {1}, {6} and {7} are the only cells in {8} {9} that can have the values {2} and the cells {10} cannot have the values {2}",
                                    cell3.getPos(), cell4.getPos(), extraCandidates, cell1.getCandidates(), cell1.getPos(), cell2.getPos(), pairResult.getFirst().get(0), pairResult.getFirst().get(1), isRow ? "row" : "column", i, pairResult.getSecond().keySet()))
                            .removeCandidatesMap(pairResult.getSecond())
                            .cellColorings(List.of(
                                    new TechniqueAction.CandidatesColoring(List.of(cell1.getPos(), cell2.getPos(), cell3.getPos(), cell4.getPos()), Color.YELLOW, cell1.getCandidates()),
                                    new TechniqueAction.CandidatesColoring(pairResult.getSecond().keySet(), Color.RED, extraCandidates),
                                    new TechniqueAction.CandidatesColoring(pairResult.getFirst(), Color.BLUE, extraCandidates),
                                    new TechniqueAction.GroupColoring(List.of(isRow ?
                                                    Pair.create(new Pos(0, i), new Pos(8, i)) :
                                                    Pair.create(new Pos(i, 0), new Pos(i, 8)))
                                            , Color.ORANGE)
                            )).build());
                }
            }
            //Type 4 for different square
            for (int candidate : cell1.getCandidates()){
                if(groupCells.stream().noneMatch(c-> c.getCandidates().contains(candidate))){
                    int otherCandidate = cell1.getCandidates().stream().filter(c -> c != candidate).findFirst().orElseThrow(() -> new RuntimeException("No other candidate"));
                    return buildType4Result(cell1.getPos(), cell2.getPos(), cell3.getPos(), cell4.getPos(), candidate, otherCandidate, isRow? ISudoku.GroupType.ROW : ISudoku.GroupType.COLUMN);
                }
            }
        }
        return Optional.empty();
    }

    private static Pair<List<Pos>, Map<Pos, Set<Integer>>> checkComplementaryPair(Set<Integer> extraCandidates, List<ICell> groupCells) {
        for(int i=0; i< groupCells.size() - 1; i++){
            ICell cell1 = groupCells.get(i);
            for(int j=i+1; j < groupCells.size(); j++){
                ICell cell2 = groupCells.get(j);
                Set<Integer> groupCandidates = new HashSet<>(cell1.getCandidates());
                groupCandidates.addAll(cell2.getCandidates());
                if(groupCandidates.containsAll(extraCandidates) && groupCandidates.size() == 3){
                    Map<Pos, Set<Integer>> candidateMap = new HashMap<>();
                    for(ICell cell : groupCells){
                        if(cell != cell1 && cell != cell2){
                            for(int candidate : extraCandidates){
                                if(cell.getCandidates().contains(candidate)){
                                    candidateMap.computeIfAbsent(cell.getPos(), _ -> new HashSet<>()).add(candidate);
                                }
                            }
                        }
                    }
                    if(!candidateMap.isEmpty())
                        return Pair.create(List.of(cell1.getPos(), cell2.getPos()), candidateMap);
                }
            }
        }
        return null;
    }

    private static Optional<TechniqueAction> checkForSameSquareRoof(ISudoku sudoku, ICell cell1, ICell cell2, boolean isRow) {
        int floorRowCol = isRow ? cell1.getY() : cell1.getX();
        for(int i=0; i< ISudoku.SUDOKU_SIZE; i++){
            if(floorRowCol == i)
                continue;
            ICell cell3 = isRow ? sudoku.getCell(cell1.getX(), i) : sudoku.getCell(i, cell1.getY());
            if(cell3.getValue() != ICell.EMPTY || !cell3.getCandidates().containsAll(cell1.getCandidates()))
                continue;
            ICell cell4 = isRow ? sudoku.getCell(cell2.getX(), i) : sudoku.getCell(i, cell2.getY());
            if(cell4.getValue() != ICell.EMPTY || !cell4.getCandidates().containsAll(cell1.getCandidates()))
                continue;
            Set<Integer> extraCandidates = extractExtraCandidates(cell1.getCandidates(), cell3, cell4);
            //Type 2A
            if(extraCandidates.size() == 1){
                List<Pos> affectedPos = new ArrayList<>();
                for(ICell cell : isRow ? sudoku.getRow(i): sudoku.getColumn(i)){
                    if(cell != cell3 && cell != cell4){
                        Set<Integer> removedCandidates = cell.getCandidates().stream().filter(extraCandidates::contains).collect(Collectors.toSet());
                        if(!removedCandidates.isEmpty()){
                            affectedPos.add(cell.getPos());
                        }
                    }
                }
                for(ICell cell : sudoku.getSquare(cell3.getX(), cell3.getY())){
                    if(cell != cell3 && cell != cell4){
                        Set<Integer> removedCandidates = cell.getCandidates().stream().filter(extraCandidates::contains).collect(Collectors.toSet());
                        if(!removedCandidates.isEmpty()){
                            affectedPos.add(cell.getPos());
                        }
                    }
                }
                if(!affectedPos.isEmpty())
                    return Optional.of(TechniqueAction.builder()
                            .name("Unique Rectangle")
                            .description(MessageFormat.format("If cell {0} or {1} is not {2} then there is no way to disambiguate the values {3} for the cells {0}, {1}, {4} and {5}, so {0} and {1} form a pointing pair and a Box/Line Reduction with value {2} for the cells {6}",
                                    cell3.getPos(), cell4.getPos(), extraCandidates, cell1.getCandidates(), cell1.getPos(), cell2.getPos(), affectedPos))
                            .removeCandidatesMap(affectedPos.stream().collect(Collectors.toMap(pos -> pos, _ -> extraCandidates)))
                            .cellColorings(List.of(
                                    new TechniqueAction.CandidatesColoring(List.of(cell1.getPos(), cell2.getPos(), cell3.getPos(), cell4.getPos()), Color.YELLOW, cell1.getCandidates()),
                                    new TechniqueAction.CandidatesColoring(affectedPos, Color.RED, extraCandidates),
                                    new TechniqueAction.GroupColoring(List.of(isRow ?
                                                    Pair.create(new Pos(0, i), new Pos(8, i)) :
                                                    Pair.create(new Pos(i, 0), new Pos(i, 8)))
                                            , Color.ORANGE)
                            )).build());
            }
            List<ICell> groupCells = Arrays.stream(isRow ? sudoku.getRow(i): sudoku.getColumn(i))
                    .filter(c -> c != cell3 && c != cell4 && c.getValue() == ICell.EMPTY)
                    .toList();
            List<ICell> squareCells = Arrays.stream(sudoku.getSquare(cell3.getX(), cell3.getY()))
                    .filter(c -> c != cell3 && c != cell4 && c.getValue() == ICell.EMPTY)
                    .toList();
            //Type 3B
            if (extraCandidates.size() == 2) {
                Map<Pos, Set<Integer>> removeCandidateMap = new HashMap<>();
                Pair<Pos, Map<Pos, Set<Integer>>> groupResult = checkComplementaryCell(extraCandidates, groupCells);
                if(groupResult != null){
                    removeCandidateMap.putAll(groupResult.getSecond());
                }
                Pair<Pos, Map<Pos, Set<Integer>>> squareResult = checkComplementaryCell(extraCandidates, squareCells);
                if(squareResult != null){
                    removeCandidateMap.putAll(squareResult.getSecond());
                }
                if(!removeCandidateMap.isEmpty()){
                    List<String> descriptionEndList = new ArrayList<>();
                    List<TechniqueAction.CellColoring> colorings = new ArrayList<>();
                    colorings.add(new TechniqueAction.CandidatesColoring(List.of(cell1.getPos(), cell2.getPos(), cell3.getPos(), cell4.getPos()), Color.YELLOW, cell1.getCandidates()));
                    colorings.add(new TechniqueAction.CandidatesColoring(removeCandidateMap.keySet().stream().toList(), Color.RED, extraCandidates));
                    if(groupResult != null){
                        colorings.add(new TechniqueAction.CandidatesColoring(List.of(groupResult.getFirst()), Color.BLUE, extraCandidates));
                        colorings.add(new TechniqueAction.GroupColoring(List.of(isRow ?
                                        Pair.create(new Pos(0, i), new Pos(8, i)) :
                                        Pair.create(new Pos(i, 0), new Pos(i, 8)))
                                , Color.ORANGE));
                        descriptionEndList.add(MessageFormat.format("so cells {0} and {1} form a pointing tuple with cell {2} with values {3} for the cells {4}",
                                cell3.getPos(), cell4.getPos(), groupResult.getFirst(), extraCandidates, groupResult.getSecond().keySet()));
                    }
                    if(squareResult != null){
                        colorings.add(new TechniqueAction.CandidatesColoring(List.of(squareResult.getFirst()), Color.BLUE, extraCandidates));
                        colorings.add(new TechniqueAction.GroupColoring(List.of(Pair.create(new Pos(cell3.getX()/3*3, cell3.getY()/3*3), new Pos(cell3.getX()/3*3+2, cell3.getY()/3*3+2))), Color.ORANGE));
                        descriptionEndList.add(MessageFormat.format("so cells {0} and {1} form a Box/Line reduction with cell {2} with values {3} for the cells {4}",
                                cell3.getPos(), cell4.getPos(), squareResult.getFirst(), extraCandidates, squareResult.getSecond().keySet()));
                    }
                    return Optional.of(TechniqueAction.builder()
                            .name("Unique Rectangle")
                            .description(MessageFormat.format("If cell {0} or {1} is not {2} then there is no way to disambiguate the values {3} for the cells {0}, {1}, {4} and {5},{6}",
                                    cell3.getPos(), cell4.getPos(), extraCandidates, cell1.getCandidates(), cell1.getPos(), cell2.getPos(), String.join(" and ", descriptionEndList)))
                            .removeCandidatesMap(removeCandidateMap)
                            .cellColorings(colorings).build());
                }

                //Type 3b with Triple Pseudo-Cells
                Pair<List<Pos>, Map<Pos, Set<Integer>>> groupPairResult = checkComplementaryPair(extraCandidates, groupCells);
                if(groupPairResult != null)
                    removeCandidateMap.putAll(groupPairResult.getSecond());
                Pair<List<Pos>, Map<Pos, Set<Integer>>> squarePairResult = checkComplementaryPair(extraCandidates, squareCells);
                if(squarePairResult != null)
                    removeCandidateMap.putAll(squarePairResult.getSecond());
                if(!removeCandidateMap.isEmpty()){
                    List<String> descriptionEndList = new ArrayList<>();
                    List<TechniqueAction.CellColoring> colorings = new ArrayList<>();
                    colorings.add(new TechniqueAction.CandidatesColoring(List.of(cell1.getPos(), cell2.getPos(), cell3.getPos(), cell4.getPos()), Color.YELLOW, cell1.getCandidates()));
                    colorings.add(new TechniqueAction.CandidatesColoring(removeCandidateMap.keySet().stream().toList(), Color.RED, extraCandidates));
                    if(groupPairResult != null){
                        colorings.add(new TechniqueAction.CandidatesColoring(groupPairResult.getFirst(), Color.BLUE, extraCandidates));
                        colorings.add(new TechniqueAction.GroupColoring(List.of(isRow ?
                                        Pair.create(new Pos(0, cell3.getY()), new Pos(8, cell3.getY())) :
                                        Pair.create(new Pos(cell3.getX(), 0), new Pos(cell3.getX(), 8)))
                                , Color.ORANGE));
                        descriptionEndList.add(MessageFormat.format("so cells {0} and {1} form a pointing tuple with cells {2} and {3} with values {4} for the cells {5}",
                                cell3.getPos(), cell4.getPos(), groupPairResult.getFirst().get(0), groupPairResult.getFirst().get(1), extraCandidates, groupPairResult.getSecond().keySet()));
                    }
                    if(squarePairResult != null){
                        colorings.add(new TechniqueAction.CandidatesColoring(squarePairResult.getFirst(), Color.BLUE, extraCandidates));
                        colorings.add(new TechniqueAction.GroupColoring(List.of(Pair.create(new Pos(cell3.getX()/3*3, cell3.getY()/3*3), new Pos(cell3.getX()/3*3+2, cell3.getY()/3*3+2))), Color.ORANGE));
                        descriptionEndList.add(MessageFormat.format("so cells {0} and {1} form a Box/Line reduction with cells {2} and {3} with values {4} for the cells {5}",
                                cell3.getPos(), cell4.getPos(), squarePairResult.getFirst().get(0), squarePairResult.getFirst().get(1), extraCandidates, squarePairResult.getSecond().keySet()));
                    }
                    return Optional.of(TechniqueAction.builder()
                            .name("Unique Rectangle")
                            .description(MessageFormat.format("If cell {0} or {1} is not {2} then there is no way to disambiguate the values {3} for the cells {0}, {1}, {4} and {5},{6}",
                                    cell3.getPos(), cell4.getPos(), extraCandidates, cell1.getCandidates(), cell1.getPos(), cell2.getPos(), String.join(" and ", descriptionEndList)))
                            .removeCandidatesMap(removeCandidateMap)
                            .cellColorings(colorings).build());
                }
            }
            //Type 4 for same square
            for (int candidate : cell1.getCandidates()){
                int otherCandidate = cell1.getCandidates().stream().filter(c -> c != candidate).findFirst().orElseThrow(() -> new RuntimeException("No other candidate"));
                if(groupCells.stream().noneMatch(c-> c.getCandidates().contains(candidate))){
                    return buildType4Result(cell1.getPos(), cell2.getPos(), cell3.getPos(), cell4.getPos(), candidate, otherCandidate, isRow? ISudoku.GroupType.ROW : ISudoku.GroupType.COLUMN);
                }
                if(squareCells.stream().noneMatch(c-> c.getCandidates().contains(candidate))){
                    return buildType4Result(cell1.getPos(), cell2.getPos(), cell3.getPos(), cell4.getPos(), candidate, otherCandidate, ISudoku.GroupType.SQUARE);
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> buildType4Result(Pos pos1, Pos pos2, Pos pos3, Pos pos4, int candidate, int otherCandidate, ISudoku.GroupType groupType){
        return Optional.of(TechniqueAction.builder()
                .name("Unique Rectangle")
                .description(MessageFormat.format("Value {0} is only available in cell {1} or {2} at {3} {4}, so if value {5} is also in one of the two cells then there will be no way to disambiguate the values {0}, {5} for the cells {1}, {2}, {6} and {7}",
                        candidate, pos3, pos4, groupType.name().toLowerCase(),
                        groupType == ISudoku.GroupType.SQUARE ? pos3.y() / 3 * 3 + pos3.x() / 3 : groupType == ISudoku.GroupType.ROW ? pos3.y() : pos3.x(),
                        otherCandidate, pos1, pos2))
                .removeCandidatesMap(Map.of(pos3, Set.of(otherCandidate), pos4, Set.of(otherCandidate)))
                .cellColorings(List.of(
                        new TechniqueAction.CandidatesColoring(List.of(pos3, pos4), Color.RED, Set.of(otherCandidate)),
                        new TechniqueAction.CandidatesColoring(List.of(pos3, pos4), Color.YELLOW, Set.of(candidate)),
                        new TechniqueAction.CandidatesColoring(List.of(pos1, pos2), Color.YELLOW, Set.of(otherCandidate, candidate))
                )).build());
    }

    private static Pair<Pos, Map<Pos, Set<Integer>>> checkComplementaryCell(Set<Integer> extraCandidates, List<ICell> groupCells) {
        Map<Pos, Set<Integer>> candidatesMap = new HashMap<>();
        List<ICell> matchingCellList = groupCells.stream()
                .filter(c-> c.getCandidates().equals(extraCandidates))
                .toList();
        if(matchingCellList.size() == 1){
            ICell matchingCell = matchingCellList.getFirst();
            for (ICell cell : groupCells){
                if(cell != matchingCell){
                    for(int candidate : extraCandidates){
                        if(cell.getCandidates().contains(candidate)){
                            candidatesMap.computeIfAbsent(cell.getPos(), _ -> new HashSet<>()).add(candidate);
                        }
                    }
                }
            }
            if(!candidatesMap.isEmpty())
                return Pair.create(matchingCell.getPos(),candidatesMap);
        }
        return null;
    }

    private static Optional<TechniqueAction> checkForLShape(ISudoku sudoku, List<ICell> cells, ICell cell1, ICell cell2, boolean isRow) {
        List<ICell> cell3List = cells.stream()
                .filter(c -> (isRow ? c.getX() == cell1.getX() || c.getX() == cell2.getX()
                        : c.getY() == cell1.getY() || c.getY() == cell2.getY())
                        && c != cell1 && c != cell2)
                .toList();
        for(ICell cell3 : cell3List){
            ICell cell4;
            if(isRow){
                int otherColumn = cell3.getX() == cell1.getX()? cell2.getX() : cell1.getX();
                cell4 = sudoku.getCell(otherColumn, cell3.getY());
            } else {
                int otherRow = cell3.getY() == cell1.getY()? cell2.getY() : cell1.getY();
                cell4 = sudoku.getCell(cell3.getX(), otherRow);
            }
            if(cell4.getCandidates().stream().anyMatch(candidate -> cell1.getCandidates().contains(candidate))){
                List<Integer> otherCandidates = cell4.getCandidates().stream()
                        .filter(candidate -> !cell1.getCandidates().contains(candidate))
                        .toList();
                return Optional.of(TechniqueAction.builder()
                        .name("Unique Rectangle")
                        .description(MessageFormat.format("If cell {0} is not {1} then there is no way to disambiguate the values {2} for the cells {0}, {3}, {4} and {5}",
                                cell4.getPos(), otherCandidates, cell1.getCandidates(), cell1.getPos(), cell2.getPos(), cell3.getPos()))
                        .removeCandidatesMap(Map.of(cell4.getPos(), cell1.getCandidates()))
                        .cellColorings(List.of(
                                new TechniqueAction.CandidatesColoring(List.of(cell4.getPos()), Color.RED, cell1.getCandidates()),
                                new TechniqueAction.CandidatesColoring(List.of(cell1.getPos(), cell2.getPos(), cell3.getPos()), Color.YELLOW, cell1.getCandidates())
                        )).build());
            }
        }
        return Optional.empty();
    }

    private static Set<Integer> extractExtraCandidates(Set<Integer> ignoreCandidates, ICell... cells){
        Set<Integer> extraCandidates = new HashSet<>();
        for(ICell cell : cells){
            if(cell.getValue() == ICell.EMPTY)
                extraCandidates.addAll(cell.getCandidates().stream()
                        .filter(c -> !ignoreCandidates.contains(c))
                        .collect(Collectors.toSet()));
        }
        return extraCandidates;
    }

    // https://www.sudokuwiki.org/Rectangle_Elimination
    // Rectangle Elimination
    public static Optional<TechniqueAction> rectangleElimination(ISudoku sudoku) {
        for (int i = 0; i < ISudoku.SUDOKU_SIZE; i++) {
            Optional<TechniqueAction> result = checkRectangle(sudoku, i, ISudoku.GroupType.ROW);
            if (result.isPresent())
                return result;
            result = checkRectangle(sudoku, i, ISudoku.GroupType.COLUMN);
            if (result.isPresent())
                return result;
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> checkRectangle(ISudoku sudoku, int i, ISudoku.GroupType groupType) {
        ICell[] group = switch (groupType) {
            case ROW -> sudoku.getRow(i);
            case COLUMN -> sudoku.getColumn(i);
            case SQUARE -> throw new RuntimeException("Square not supported");
        };
        for(int num: ICell.DIGITS) {
            List<ICell> cellsWithNum = Arrays.stream(group)
                    .filter(c -> c.getCandidates().contains(num))
                    .toList();
            if (cellsWithNum.size() != 2)
                continue;

            ICell cell1 = cellsWithNum.get(0);
            ICell cell2 = cellsWithNum.get(1);
            //If in same square then skip
            if(cell1.getX()/3 == cell2.getX()/3 && cell1.getY()/3 == cell2.getY()/3)
                continue;
            Optional<TechniqueAction> result = checkRectangle(sudoku, cell1, cell2, num, groupType);
            if (result.isPresent())
                return result;
            result = checkRectangle(sudoku, cell2, cell1, num, groupType);
            if (result.isPresent())
                return result;
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> checkRectangle(ISudoku sudoku, ICell cell1, ICell cell2, int num, ISudoku.GroupType groupType) {
        List<ICell> peers = Arrays.stream(switch (groupType) {
            case ROW -> sudoku.getColumn(cell1.getX());
            case COLUMN -> sudoku.getRow(cell1.getY());
            case SQUARE -> throw new RuntimeException("Square not supported");
        }).filter(c -> c.getCandidates().contains(num) && (cell1.getX()/3 != c.getX()/3 || cell1.getY()/3 != c.getY()/3))
                .toList();

        for(ICell peer : peers) {
            int squareNumber = switch (groupType) {
                case ROW -> peer.getY() / 3 * 3 + cell2.getX() / 3;
                case COLUMN -> cell2.getY() / 3 * 3 + peer.getX() / 3;
                case SQUARE -> throw new RuntimeException("Square not supported");
            };
            List<ICell> affectedPeerSquareCells = Arrays.stream(sudoku.getSquare(squareNumber)).filter(c -> c.getCandidates().contains(num))
                    .toList();
            if(affectedPeerSquareCells.isEmpty())
                continue;
            if(affectedPeerSquareCells.stream().allMatch(c -> isPeer(c, peer) || isPeer(c, cell2))) {
                return Optional.of(TechniqueAction.builder()
                        .name("Rectangle Elimination")
                        .description(MessageFormat.format("If cell {0} is {1} then cell {2} cannot be {1} and cell {3} has to be {1}. Making it impossible to place {1} in square {4}",
                                peer.getPos(), num, cell1.getPos(), cell2.getPos(), squareNumber))
                        .removeCandidatesMap(Map.of(peer.getPos(), Set.of(num)))
                        .cellColorings(List.of(
                                new TechniqueAction.CandidatesColoring(List.of(peer.getPos()), Color.RED, List.of(num)),
                                new TechniqueAction.CandidatesColoring(List.of(cell1.getPos()), Color.GREEN, List.of(num)),
                                new TechniqueAction.CandidatesColoring(List.of(cell2.getPos()), Color.YELLOW, List.of(num)),
                                new TechniqueAction.CandidatesColoring(affectedPeerSquareCells.stream().map(ICell::getPos).toList(), Color.ORANGE, List.of(num)),
                                new TechniqueAction.LineColoring(List.of(
                                        Pair.create(cell1.getPos(), cell2.getPos()),
                                        Pair.create(cell1.getPos(), peer.getPos())
                                ), Color.BLUE, num, false)
                        ))
                        .build());
            }
        }
        return Optional.empty();
    }

    // https://www.sudokuwiki.org/Extended_Unique_Rectangles
    // Extended Unique Rectangles
    public static Optional<TechniqueAction> extendedUniqueRectangle(ISudoku sudoku) {
        for (int i = 0; i < ISudoku.SUDOKU_SIZE - 3; i++) {
            Optional<TechniqueAction> result = checkExtendedRectangle(sudoku, i, ISudoku.GroupType.ROW);
            if (result.isPresent())
                return result;
            result = checkExtendedRectangle(sudoku, i, ISudoku.GroupType.COLUMN);
            if (result.isPresent())
                return result;
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> checkExtendedRectangle(ISudoku sudoku, int index, ISudoku.GroupType groupType) {
        ICell[] group = switch (groupType) {
            case ROW -> sudoku.getRow(index);
            case COLUMN -> sudoku.getColumn(index);
            case SQUARE -> throw new RuntimeException("Square not supported");
        };
        for(int i = 0; i < 3; i++){
            int finalI = i;
            List<ICell> group1 = Arrays.stream(group)
                    .filter(c -> groupType == ISudoku.GroupType.ROW ? c.getX() / 3 == finalI : c.getY() / 3 == finalI)
                    .filter(c -> c.getValue() == ICell.EMPTY && c.getCandidates().size() <= 3)
                    .toList();
            if(group1.size() < 2)
                continue;
            Combinations combinations = new Combinations(group1.size(), 2);
            for (int[] combination : combinations) {
                ICell cell1 = group1.get(combination[0]);
                ICell cell2 = group1.get(combination[1]);
                Set<Integer> candidates = new HashSet<>(cell1.getCandidates());
                candidates.addAll(cell2.getCandidates());
                if(candidates.size() > 3)
                    continue;
                for(int index2 = (index/3+1)*3; index2 < ISudoku.SUDOKU_SIZE; index2 ++){
                    ICell cell3, cell4 = switch (groupType) {
                        case ROW -> {
                            cell3 = sudoku.getCell(cell1.getX(), index2);
                            yield sudoku.getCell(cell2.getX(), index2);
                        }
                        case COLUMN -> {
                            cell3 = sudoku.getCell(index2, cell1.getY());
                            yield sudoku.getCell(index2, cell2.getY());
                        }
                        case SQUARE -> throw new RuntimeException("Square not supported");
                    };
                    if(cell3.getValue() != ICell.EMPTY || cell4.getValue() != ICell.EMPTY)
                        continue;
                    Set<Integer> candidatesWithGroup2 = new HashSet<>(candidates);
                    candidatesWithGroup2.addAll(cell3.getCandidates());
                    candidatesWithGroup2.addAll(cell4.getCandidates());
                    if(candidatesWithGroup2.size() > 3)
                        continue;
                    Optional<TechniqueAction> result = checkExtendedRectangle(sudoku, List.of(cell1, cell2, cell3, cell4), index, index2, candidatesWithGroup2, groupType);
                    if(result.isPresent())
                        return result;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> checkExtendedRectangle(ISudoku sudoku, List<ICell> cells, int index, int index2, Set<Integer> candidates, ISudoku.GroupType groupType) {
        int baseIndex3 = IntStream.range(0, 3).filter(i -> i != index/3 && i != index2/3).findFirst().orElseThrow(() -> new RuntimeException("No base index"));
        for(int i = 0; i < 3; i ++){
            ICell cell5, cell6 = switch (groupType) {
                case ROW -> {
                    cell5 = sudoku.getCell(cells.get(0).getX(), baseIndex3*3 + i);
                    yield sudoku.getCell(cells.get(1).getX(), baseIndex3*3 + i);
                }
                case COLUMN -> {
                    cell5 = sudoku.getCell(baseIndex3*3 + i, cells.get(0).getY());
                    yield sudoku.getCell(baseIndex3*3 + i, cells.get(1).getY());
                }
                case SQUARE -> throw new RuntimeException("Square not supported");
            };
            if(cell5.getValue() != ICell.EMPTY || cell6.getValue() != ICell.EMPTY)
                continue;
            //Type 1
            Optional<TechniqueAction> result = checkForTypeOne(cells, cell5, cell6, candidates);
            if(result.isPresent())
                return result;
            result = checkForTypeOne(cells, cell6, cell5, candidates);
            if(result.isPresent())
                return result;
            //Type 2
            Set<Integer> extraCandidates = new HashSet<>(cell5.getCandidates());
            extraCandidates.addAll(cell6.getCandidates());
            extraCandidates.removeAll(candidates);
            if(extraCandidates.size() == 1){
                int extraCandidate = extraCandidates.stream().findFirst().orElseThrow(() -> new RuntimeException("No extra candidate"));
                Set<ICell> affectedCells = getPeers(sudoku, cell5);
                affectedCells.retainAll(getPeers(sudoku, cell6));
                affectedCells.removeIf(c -> c.getValue() != ICell.EMPTY || !c.getCandidates().contains(extraCandidate));
                if(!affectedCells.isEmpty()){
                    return Optional.of(TechniqueAction.builder()
                            .name("Extended Unique Rectangle")
                            .description(MessageFormat.format("If neither cell {0} and {1} are not {2} then there is no way to disambiguate the values {3} for the cells {4}, {5}, {6}, {7}, {0} and {1}",
                                    cell5.getPos(), cell6.getX(), extraCandidate, candidates, cells.get(0).getPos(), cells.get(1).getPos(), cells.get(2).getPos(), cells.get(3).getPos()))
                            .removeCandidatesMap(affectedCells.stream().collect(Collectors.toMap(ICell::getPos, _ -> extraCandidates)))
                            .cellColorings(List.of(
                                    new TechniqueAction.CandidatesColoring(affectedCells.stream().map(ICell::getPos).toList(), Color.RED, extraCandidates),
                                    new TechniqueAction.CandidatesColoring(List.of(cell5.getPos(), cell6.getPos()), Color.ORANGE, extraCandidates),
                                    new TechniqueAction.CandidatesColoring(List.of(cell5.getPos(), cell6.getPos()), Color.YELLOW, candidates),
                                    new TechniqueAction.CandidatesColoring(cells.stream().map(ICell::getPos).toList(), Color.YELLOW, candidates)
                            )).build());
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> checkForTypeOne(List<ICell> cells, ICell additionalCell, ICell extraCell, Set<Integer> candidates) {
        if(!candidates.containsAll(additionalCell.getCandidates()))
            return Optional.empty();

        Set<Integer> candidatesToBeRemoved = new HashSet<>(extraCell.getCandidates());
        candidatesToBeRemoved.retainAll(candidates);
        if(candidatesToBeRemoved.isEmpty())
            return Optional.empty();
        Set<Integer> extraCandidates = new HashSet<>(extraCell.getCandidates());
        extraCandidates.removeAll(candidatesToBeRemoved);

        return Optional.of(TechniqueAction.builder()
                .name("Extended Unique Rectangle")
                .description(MessageFormat.format("If cell {0} is not {1} then there is no way to disambiguate the values {2} for the cells {3}, {4}, {5}, {6} and {7}",
                        extraCell.getPos(), extraCandidates, candidates, cells.get(0).getPos(), cells.get(1).getPos(), cells.get(2).getPos(), cells.get(3).getPos(), additionalCell.getPos()))
                .removeCandidatesMap(Map.of(extraCell.getPos(), candidatesToBeRemoved))
                .cellColorings(List.of(
                        new TechniqueAction.CandidatesColoring(List.of(extraCell.getPos()), Color.RED, candidatesToBeRemoved),
                        new TechniqueAction.CandidatesColoring(List.of(extraCell.getPos()), Color.YELLOW, extraCandidates),
                        new TechniqueAction.CandidatesColoring(List.of(additionalCell.getPos()), Color.YELLOW, candidates),
                        new TechniqueAction.CandidatesColoring(cells.stream().map(ICell::getPos).toList(), Color.YELLOW, candidates)
                )).build());
    }

    // https://www.sudokuwiki.org/Hidden_Unique_Rectangles
    // Hidden Unique Rectangles
    public static Optional<TechniqueAction> hiddenUniqueRectangle(ISudoku sudoku) {
        for(int i = 0; i < ISudoku.SUDOKU_SIZE; i++){
            Optional<TechniqueAction> result = checkHiddenRectangle(sudoku, i, ISudoku.GroupType.ROW);
            if(result.isPresent())
                return result;
            result = checkHiddenRectangle(sudoku, i, ISudoku.GroupType.COLUMN);
            if(result.isPresent())
                return result;
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> checkHiddenRectangle(ISudoku sudoku, int index, ISudoku.GroupType groupType) {
        ICell[] group = switch (groupType) {
            case ROW -> sudoku.getRow(index);
            case COLUMN -> sudoku.getColumn(index);
            case SQUARE -> throw new RuntimeException("Square not supported");
        };
        for (int digit : ICell.DIGITS) {
            List<ICell> cellsWithDigit = Arrays.stream(group)
                    .filter(c -> c.getCandidates().contains(digit))
                    .toList();
            if (cellsWithDigit.size() != 2)
                continue;
            for (ICell mainCell : cellsWithDigit.stream().filter(c -> c.getCandidates().size() == 2).toList()) {
                ICell cell2 = cellsWithDigit.stream().filter(c -> c != mainCell).findFirst().orElseThrow(() -> new RuntimeException("No secondary cell"));
                if (!cell2.getCandidates().containsAll(mainCell.getCandidates()))
                    continue;
                ICell[] cell3Group = switch (groupType) {
                    case ROW -> sudoku.getColumn(mainCell.getX());
                    case COLUMN -> sudoku.getRow(mainCell.getY());
                    case SQUARE -> throw new RuntimeException("Square not supported");
                };
                List<ICell> cells3WithDigit = Arrays.stream(cell3Group)
                        .filter(c -> c != mainCell && c.getCandidates().contains(digit))
                        .toList();
                if (cells3WithDigit.size() != 1)
                    continue;
                ICell cell3 = cells3WithDigit.getFirst();
                if (!cell3.getCandidates().containsAll(mainCell.getCandidates()))
                    continue;
                boolean areAnyCellsInSameSquare = switch (groupType) {
                    case ROW -> cell3.getX() / 3 == mainCell.getX() / 3 || cell2.getY() / 3 == mainCell.getY() / 3;
                    case COLUMN -> cell3.getY() / 3 == mainCell.getY() / 3 || cell2.getX() / 3 == mainCell.getX() / 3;
                    case SQUARE -> throw new RuntimeException("Square not supported");
                };
                if (!areAnyCellsInSameSquare)
                    continue;
                ICell cell4 = switch (groupType) {
                    case ROW -> sudoku.getCell(cell2.getX(), cell3.getY());
                    case COLUMN -> sudoku.getCell(cell3.getX(), cell2.getY());
                    case SQUARE -> throw new RuntimeException("Square not supported");
                };
                if (!cell4.getCandidates().containsAll(mainCell.getCandidates()))
                    continue;
                //Check for type 2
                if (cell3.getCandidates().equals(mainCell.getCandidates()) || cell2.getCandidates().equals(mainCell.getCandidates()))
                    return buildHiddenUniqueRectangleResult(mainCell, cell2, cell3, cell4, digit);
                //Check for type 1
                ICell[] commonCells24 = switch (groupType) {
                    case ROW -> sudoku.getColumn(cell2.getX());
                    case COLUMN -> sudoku.getRow(cell2.getY());
                    case SQUARE -> throw new RuntimeException("Square not supported");
                };
                ICell[] commonCells34 = switch (groupType) {
                    case ROW -> sudoku.getRow(cell3.getY());
                    case COLUMN -> sudoku.getColumn(cell3.getX());
                    case SQUARE -> throw new RuntimeException("Square not supported");
                };
                if (Arrays.stream(commonCells24).noneMatch(c -> c != cell2 && c != cell4 && c.getCandidates().contains(digit))
                        && Arrays.stream(commonCells34).noneMatch(c -> c != cell3 && c != cell4 && c.getCandidates().contains(digit)))
                    return buildHiddenUniqueRectangleResult(mainCell, cell2, cell3, cell4, digit);
            }
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> buildHiddenUniqueRectangleResult(ICell mainCell, ICell cell2, ICell cell3, ICell cell4, int digit) {
        int otherCandidate = mainCell.getCandidates().stream().filter(c -> c != digit).findFirst().orElseThrow(() -> new RuntimeException("No other candidate"));
        return Optional.of(TechniqueAction.builder()
                .name("Hidden Unique Rectangle")
                .description(MessageFormat.format("If cell {0} is {1} then cells {2} and {3} are {4} and cell {5} is {1}. And this will form a deadly rectangle in the cells {0} {2} {3} {5} since by swapping the values {1} and {6} will result in a second solution",
                        cell4.getPos(), otherCandidate, cell2.getPos(), cell3.getPos(), digit, mainCell.getPos(), otherCandidate))
                .removeCandidatesMap(Map.of(cell4.getPos(), Set.of(otherCandidate)))
                .cellColorings(List.of(
                        new TechniqueAction.CandidatesColoring(List.of(cell4.getPos()), Color.RED, Set.of(otherCandidate)),
                        new TechniqueAction.GroupColoring(List.of(
                                Pair.create(cell2.getPos(), cell2.getPos()),
                                Pair.create(cell3.getPos(), cell3.getPos())
                        ), Color.ORANGE),
                        new TechniqueAction.GroupColoring(List.of(Pair.create(mainCell.getPos(), mainCell.getPos())), Color.YELLOW)
                )).build());
    }
}