package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import com.chgr.sudoku.models.Pos;
import com.chgr.sudoku.models.TechniqueAction;
import javafx.scene.paint.Color;
import org.apache.commons.math3.util.Pair;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.chgr.sudoku.utils.CellUtils.getPeers;
import static com.chgr.sudoku.utils.CellUtils.isPeer;

public class RectangleTechnique {

    public static Optional<TechniqueAction> uniqueRectangle(ISudoku sudoku) {
        Map<Set<Integer>, List<ICell>> biValueMap = sudoku.getEmptyCells().stream()
                .filter(c -> c.getCandidates().size() == 2)
                .collect(Collectors.groupingBy(ICell::getCandidates));
        for(Set<Integer> candidates : biValueMap.keySet()) {
            List<ICell> cells = biValueMap.get(candidates);
            if(cells.size() >= 2) {
                for(int i=0; i< cells.size() - 1; i++){
                    ICell cell1 = cells.get(i);
                    for(int j=i+1; j < cells.size(); j++){
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
                        else {
                            Optional<TechniqueAction> techniqueAction = checkForDiagonal(sudoku, cell1, cell2);
                            if(techniqueAction.isPresent())
                                return techniqueAction;
                        }
                    }
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
                        .colorings(List.of(
                                TechniqueAction.CellColoring.candidatesColoring(List.of(cell1.getPos(), cell2.getPos()), Color.GREEN, Set.of(candidate)),
                                TechniqueAction.CellColoring.candidatesColoring(List.of(cell1.getPos(), cell2.getPos()), Color.RED, Set.of(otherCandidate)),
                                TechniqueAction.CellColoring.candidatesColoring(List.of(cell3.getPos(), cell4.getPos()), Color.YELLOW, cell1.getCandidates()),
                                TechniqueAction.CellColoring.lineColoring(List.of(Pair.create(cell1.getPos(), cell2.getPos())), Color.BLUE, candidate)
                        )).build());
            }
        }
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
                    .colorings(List.of(
                            TechniqueAction.CellColoring.candidatesColoring(List.of(cell1.getPos(), cell2.getPos(), cell3.getPos(), cell4.getPos()), Color.YELLOW, cell1.getCandidates()),
                            TechniqueAction.CellColoring.candidatesColoring(removeCandidatesMap.keySet(), Color.RED, extraCandidates)
                    )).build());
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
                            .removeCandidatesMap(affectedPos.stream().collect(Collectors.toMap(pos -> pos, pos -> extraCandidates)))
                            .colorings(List.of(
                                    TechniqueAction.CellColoring.candidatesColoring(List.of(cell1.getPos(), cell2.getPos(), cell3.getPos(), cell4.getPos()), Color.YELLOW, cell1.getCandidates()),
                                    TechniqueAction.CellColoring.candidatesColoring(affectedPos, Color.RED, extraCandidates),
                                    TechniqueAction.CellColoring.groupColoring(List.of(isRow ?
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
                            .colorings(List.of(
                                    TechniqueAction.CellColoring.candidatesColoring(List.of(cell1.getPos(), cell2.getPos(), cell3.getPos(), cell4.getPos()), Color.YELLOW, cell1.getCandidates()),
                                    TechniqueAction.CellColoring.candidatesColoring(result.getSecond().keySet(), Color.RED, extraCandidates),
                                    TechniqueAction.CellColoring.candidatesColoring(List.of(result.getFirst()), Color.BLUE, extraCandidates),
                                    TechniqueAction.CellColoring.groupColoring(List.of(isRow ?
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
                            .colorings(List.of(
                                    TechniqueAction.CellColoring.candidatesColoring(List.of(cell1.getPos(), cell2.getPos(), cell3.getPos(), cell4.getPos()), Color.YELLOW, cell1.getCandidates()),
                                    TechniqueAction.CellColoring.candidatesColoring(pairResult.getSecond().keySet(), Color.RED, extraCandidates),
                                    TechniqueAction.CellColoring.candidatesColoring(pairResult.getFirst(), Color.BLUE, extraCandidates),
                                    TechniqueAction.CellColoring.groupColoring(List.of(isRow ?
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
                                    candidateMap.computeIfAbsent(cell.getPos(), pos -> new HashSet<>()).add(candidate);
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
                            .removeCandidatesMap(affectedPos.stream().collect(Collectors.toMap(pos -> pos, pos -> extraCandidates)))
                            .colorings(List.of(
                                    TechniqueAction.CellColoring.candidatesColoring(List.of(cell1.getPos(), cell2.getPos(), cell3.getPos(), cell4.getPos()), Color.YELLOW, cell1.getCandidates()),
                                    TechniqueAction.CellColoring.candidatesColoring(affectedPos, Color.RED, extraCandidates),
                                    TechniqueAction.CellColoring.groupColoring(List.of(isRow ?
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
                    colorings.add(TechniqueAction.CellColoring.candidatesColoring(List.of(cell1.getPos(), cell2.getPos(), cell3.getPos(), cell4.getPos()), Color.YELLOW, cell1.getCandidates()));
                    colorings.add(TechniqueAction.CellColoring.candidatesColoring(removeCandidateMap.keySet().stream().toList(), Color.RED, extraCandidates));
                    if(groupResult != null){
                        colorings.add(TechniqueAction.CellColoring.candidatesColoring(List.of(groupResult.getFirst()), Color.BLUE, extraCandidates));
                        colorings.add(TechniqueAction.CellColoring.groupColoring(List.of(isRow ?
                                        Pair.create(new Pos(0, i), new Pos(8, i)) :
                                        Pair.create(new Pos(i, 0), new Pos(i, 8)))
                                , Color.ORANGE));
                        descriptionEndList.add(MessageFormat.format("so cells {0} and {1} form a pointing tuple with cell {2} with values {3} for the cells {4}",
                                cell3.getPos(), cell4.getPos(), groupResult.getFirst(), extraCandidates, groupResult.getSecond().keySet()));
                    }
                    if(squareResult != null){
                        colorings.add(TechniqueAction.CellColoring.candidatesColoring(List.of(squareResult.getFirst()), Color.BLUE, extraCandidates));
                        colorings.add(TechniqueAction.CellColoring.groupColoring(List.of(Pair.create(new Pos(cell3.getX()/3*3, cell3.getY()/3*3), new Pos(cell3.getX()/3*3+2, cell3.getY()/3*3+2))), Color.ORANGE));
                        descriptionEndList.add(MessageFormat.format("so cells {0} and {1} form a Box/Line reduction with cell {2} with values {3} for the cells {4}",
                                cell3.getPos(), cell4.getPos(), squareResult.getFirst(), extraCandidates, squareResult.getSecond().keySet()));
                    }
                    return Optional.of(TechniqueAction.builder()
                            .name("Unique Rectangle")
                            .description(MessageFormat.format("If cell {0} or {1} is not {2} then there is no way to disambiguate the values {3} for the cells {0}, {1}, {4} and {5},{6}",
                                    cell3.getPos(), cell4.getPos(), extraCandidates, cell1.getCandidates(), cell1.getPos(), cell2.getPos(), String.join(" and ", descriptionEndList)))
                            .removeCandidatesMap(removeCandidateMap)
                            .colorings(colorings).build());
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
                    colorings.add(TechniqueAction.CellColoring.candidatesColoring(List.of(cell1.getPos(), cell2.getPos(), cell3.getPos(), cell4.getPos()), Color.YELLOW, cell1.getCandidates()));
                    colorings.add(TechniqueAction.CellColoring.candidatesColoring(removeCandidateMap.keySet().stream().toList(), Color.RED, extraCandidates));
                    if(groupPairResult != null){
                        colorings.add(TechniqueAction.CellColoring.candidatesColoring(groupPairResult.getFirst(), Color.BLUE, extraCandidates));
                        colorings.add(TechniqueAction.CellColoring.groupColoring(List.of(isRow ?
                                        Pair.create(new Pos(0, cell3.getY()), new Pos(8, cell3.getY())) :
                                        Pair.create(new Pos(cell3.getX(), 0), new Pos(cell3.getX(), 8)))
                                , Color.ORANGE));
                        descriptionEndList.add(MessageFormat.format("so cells {0} and {1} form a pointing tuple with cells {2} and {3} with values {4} for the cells {5}",
                                cell3.getPos(), cell4.getPos(), groupPairResult.getFirst().get(0), groupPairResult.getFirst().get(1), extraCandidates, groupPairResult.getSecond().keySet()));
                    }
                    if(squarePairResult != null){
                        colorings.add(TechniqueAction.CellColoring.candidatesColoring(squarePairResult.getFirst(), Color.BLUE, extraCandidates));
                        colorings.add(TechniqueAction.CellColoring.groupColoring(List.of(Pair.create(new Pos(cell3.getX()/3*3, cell3.getY()/3*3), new Pos(cell3.getX()/3*3+2, cell3.getY()/3*3+2))), Color.ORANGE));
                        descriptionEndList.add(MessageFormat.format("so cells {0} and {1} form a Box/Line reduction with cells {2} and {3} with values {4} for the cells {5}",
                                cell3.getPos(), cell4.getPos(), squarePairResult.getFirst().get(0), squarePairResult.getFirst().get(1), extraCandidates, squarePairResult.getSecond().keySet()));
                    }
                    return Optional.of(TechniqueAction.builder()
                            .name("Unique Rectangle")
                            .description(MessageFormat.format("If cell {0} or {1} is not {2} then there is no way to disambiguate the values {3} for the cells {0}, {1}, {4} and {5},{6}",
                                    cell3.getPos(), cell4.getPos(), extraCandidates, cell1.getCandidates(), cell1.getPos(), cell2.getPos(), String.join(" and ", descriptionEndList)))
                            .removeCandidatesMap(removeCandidateMap)
                            .colorings(colorings).build());
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
                .colorings(List.of(
                        TechniqueAction.CellColoring.candidatesColoring(List.of(pos3, pos4), Color.RED, Set.of(otherCandidate)),
                        TechniqueAction.CellColoring.candidatesColoring(List.of(pos3, pos4), Color.YELLOW, Set.of(candidate)),
                        TechniqueAction.CellColoring.candidatesColoring(List.of(pos1, pos2), Color.YELLOW, Set.of(otherCandidate, candidate))
                )).build());
    }

    private static Pair<Pos, Map<Pos, Set<Integer>>> checkComplementaryCell(Set<Integer> extraCandidates, List<ICell> groupCells) {
        Map<Pos, Set<Integer>> candidatesMap = new HashMap<>();
        List<ICell> matchingCellList = groupCells.stream()
                .filter(c-> c.getCandidates().equals(extraCandidates))
                .toList();
        if(matchingCellList.size() == 1){
            ICell matchingCell = matchingCellList.get(0);
            for (ICell cell : groupCells){
                if(cell != matchingCell){
                    for(int candidate : extraCandidates){
                        if(cell.getCandidates().contains(candidate)){
                            candidatesMap.computeIfAbsent(cell.getPos(), pos -> new HashSet<>()).add(candidate);
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
                        .colorings(List.of(
                                TechniqueAction.CellColoring.candidatesColoring(List.of(cell4.getPos()), Color.RED, cell1.getCandidates()),
                                TechniqueAction.CellColoring.candidatesColoring(List.of(cell1.getPos(), cell2.getPos(), cell3.getPos()), Color.YELLOW, cell1.getCandidates())
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
                        .colorings(List.of(
                                TechniqueAction.CellColoring.candidatesColoring(List.of(peer.getPos()), Color.RED, List.of(num)),
                                TechniqueAction.CellColoring.candidatesColoring(List.of(cell1.getPos()), Color.GREEN, List.of(num)),
                                TechniqueAction.CellColoring.candidatesColoring(List.of(cell2.getPos()), Color.YELLOW, List.of(num)),
                                TechniqueAction.CellColoring.candidatesColoring(affectedPeerSquareCells.stream().map(ICell::getPos).toList(), Color.ORANGE, List.of(num)),
                                TechniqueAction.CellColoring.lineColoring(List.of(
                                        Pair.create(cell1.getPos(), cell2.getPos()),
                                        Pair.create(cell1.getPos(), peer.getPos())
                                ), Color.BLUE, num)
                        ))
                        .build());
            }
        }
        return Optional.empty();
    }
}
