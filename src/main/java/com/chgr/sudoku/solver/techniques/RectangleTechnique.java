package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import com.chgr.sudoku.models.TechniqueAction;
import javafx.scene.paint.Color;
import org.apache.commons.math3.util.Pair;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.chgr.sudoku.utils.CellUtils.getPeers;
import static com.chgr.sudoku.utils.CellUtils.isPeer;

public class RectangleTechnique {

    public static boolean uniqueRectangle(ISudoku sudoku) {
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
                            if(checkFloor(sudoku, cells, cell1, cell2, false))
                                return true;
                        } else if (cell1.getY() == cell2.getY()) {
                            if(checkFloor(sudoku, cells, cell1, cell2, true))
                                return true;
                        }
                        else {
                            if(checkForDiagonal(sudoku, cell1, cell2))
                                return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean checkForDiagonal(ISudoku sudoku, ICell cell1, ICell cell2) {
        ICell cell3 = sudoku.getCell(cell1.getX(), cell2.getY());
        if(cell3.getValue() != ICell.EMPTY || !cell3.getCandidates().containsAll(cell1.getCandidates()))
            return false;
        ICell cell4 = sudoku.getCell(cell2.getX(), cell1.getY());
        if(cell4.getValue() != ICell.EMPTY || !cell4.getCandidates().containsAll(cell1.getCandidates()))
            return false;
        //Type 5
        for(int candidate : cell1.getCandidates()){
            if(Arrays.stream(sudoku.getRow(cell1.getY())).filter(c -> c.getCandidates().contains(candidate)).count() == 2 &&
                    Arrays.stream(sudoku.getColumn(cell1.getX())).filter(c -> c.getCandidates().contains(candidate)).count() == 2 &&
                    Arrays.stream(sudoku.getRow(cell2.getY())).filter(c -> c.getCandidates().contains(candidate)).count() == 2 &&
                    Arrays.stream(sudoku.getColumn(cell2.getX())).filter(c -> c.getCandidates().contains(candidate)).count() == 2){
                cell1.setValue(candidate);
                cell2.setValue(candidate);
                return true;
            }
        }
        Set<Integer> extraCandidates = extractExtraCandidates(cell1.getCandidates(), cell3, cell4);
        //Type 2C
        if(extraCandidates.size() == 1){
            Set<ICell> commonPeers = getPeers(sudoku, cell3);
            commonPeers.retainAll(getPeers(sudoku, cell4));
            boolean changed = false;
            for (ICell cell : commonPeers) {
                changed |= cell.removeCandidates(extraCandidates);
            }
            return changed;
        }
        return false;
    }

    private static boolean checkFloor(ISudoku sudoku, List<ICell> cells, ICell cell1, ICell cell2, boolean isRow){
        boolean sameSquare = isRow? cell1.getX()/3 == cell2.getX()/3
                : cell1.getY()/3 == cell2.getY()/3;
        if(sameSquare){
            //Type 1
            if (checkForLShape(sudoku, cells, cell1, cell2, isRow)) return true;

            //Type 2A and 3B
            return checkForSameSquareRoof(sudoku, cell1, cell2, isRow);
        } else {
            //Type 2B and 3A
            return checkForDifferentSquareRoof(sudoku, cell1, cell2, isRow);
        }
    }

    private static boolean checkForDifferentSquareRoof(ISudoku sudoku, ICell cell1, ICell cell2, boolean isRow) {
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
                boolean changed = false;
                for(ICell cell : isRow ? sudoku.getRow(i): sudoku.getColumn(i)){
                    if(cell != cell3 && cell != cell4){
                        changed |= cell.removeCandidates(extraCandidates);
                    }
                }
                if(changed)
                    return true;
            }
            List<ICell> groupCells = Arrays.stream(isRow ? sudoku.getRow(i): sudoku.getColumn(i))
                    .filter(c -> c != cell3 && c != cell4 && c.getValue() == ICell.EMPTY)
                    .toList();
            //Type 3A
            if (extraCandidates.size() == 2) {
                if(checkComplementaryCell(extraCandidates, groupCells))
                    return true;
                //Type 3 with Triple Pseudo-Cells
                return checkComplementaryPair(extraCandidates, groupCells);
            }
            //Type 4 for different square
            for (int candidate : cell1.getCandidates()){
                if(groupCells.stream().noneMatch(c-> c.getCandidates().contains(candidate))){
                    int otherCandidate = cell1.getCandidates().stream().filter(c -> c != candidate).findFirst().orElseThrow(() -> new RuntimeException("No other candidate"));
                    cell3.removeCandidate(otherCandidate);
                    cell4.removeCandidate(otherCandidate);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean checkComplementaryPair(Set<Integer> extraCandidates, List<ICell> groupCells) {
        for(int i=0; i< groupCells.size() - 1; i++){
            ICell cell1 = groupCells.get(i);
            for(int j=i+1; j < groupCells.size(); j++){
                ICell cell2 = groupCells.get(j);
                Set<Integer> groupCandidates = new HashSet<>(cell1.getCandidates());
                groupCandidates.addAll(cell2.getCandidates());
                if(groupCandidates.containsAll(extraCandidates) && groupCandidates.size() == 3){
                    boolean changed = false;
                    for(ICell cell : groupCells){
                        if(cell != cell1 && cell != cell2){
                            changed |= cell.removeCandidates(extraCandidates);
                        }
                    }
                    if(changed)
                        return true;
                }
            }
        }
        return false;
    }

    private static boolean checkForSameSquareRoof(ISudoku sudoku, ICell cell1, ICell cell2, boolean isRow) {
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
                boolean changed = false;
                for(ICell cell : isRow ? sudoku.getRow(i): sudoku.getColumn(i)){
                    if(cell != cell3 && cell != cell4){
                        changed |= cell.removeCandidates(extraCandidates);
                    }
                }
                for(ICell cell : sudoku.getSquare(cell3.getX(), cell3.getY())){
                    if(cell != cell3 && cell != cell4){
                        changed |= cell.removeCandidates(extraCandidates);
                    }
                }
                if(changed)
                    return true;
            }
            List<ICell> groupCells = Arrays.stream(isRow ? sudoku.getRow(i): sudoku.getColumn(i))
                    .filter(c -> c != cell3 && c != cell4 && c.getValue() == ICell.EMPTY)
                    .toList();
            List<ICell> squareCells = Arrays.stream(sudoku.getSquare(cell3.getX(), cell3.getY()))
                    .filter(c -> c != cell3 && c != cell4 && c.getValue() == ICell.EMPTY)
                    .toList();
            //Type 3B
            if (extraCandidates.size() == 2) {
                boolean changed = checkComplementaryCell(extraCandidates, groupCells);
                changed |= checkComplementaryCell(extraCandidates, squareCells);
                if(changed)
                    return true;

                //Type 3b with Triple Pseudo-Cells
                changed = checkComplementaryPair(extraCandidates, groupCells);
                changed |= checkComplementaryPair(extraCandidates, squareCells);
                if(changed)
                    return true;
            }
            //Type 4 for same square
            boolean changed = false;
            for (int candidate : cell1.getCandidates()){
                int otherCandidate = cell1.getCandidates().stream().filter(c -> c != candidate).findFirst().orElseThrow(() -> new RuntimeException("No other candidate"));
                if(groupCells.stream().noneMatch(c-> c.getCandidates().contains(candidate))){
                    cell3.removeCandidate(otherCandidate);
                    cell4.removeCandidate(otherCandidate);
                    changed = true;
                }
                if(squareCells.stream().noneMatch(c-> c.getCandidates().contains(candidate))){
                    cell3.removeCandidate(otherCandidate);
                    cell4.removeCandidate(otherCandidate);
                    changed = true;
                }
            }
            if(changed)
                return true;
        }
        return false;
    }

    private static boolean checkComplementaryCell(Set<Integer> extraCandidates, List<ICell> groupCells) {
        boolean changed = false;
        List<ICell> matchingCellList = groupCells.stream()
                .filter(c-> c.getCandidates().equals(extraCandidates))
                .toList();
        if(matchingCellList.size() == 1){
            ICell matchingCell = matchingCellList.get(0);
            for (ICell cell : groupCells){
                if(cell != matchingCell){
                    changed |= cell.removeCandidates(extraCandidates);
                }
            }
        }
        return changed;
    }

    private static boolean checkForLShape(ISudoku sudoku, List<ICell> cells, ICell cell1, ICell cell2, boolean isRow) {
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
            if(cell4.removeCandidates(cell1.getCandidates()))
                return true;
        }
        return false;
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
                                ), num, Color.BLUE)
                        ))
                        .build());
            }
        }
        return Optional.empty();
    }
}
