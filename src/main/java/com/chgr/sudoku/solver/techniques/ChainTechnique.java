package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import com.chgr.sudoku.models.Pos;
import com.chgr.sudoku.models.TechniqueAction;
import com.chgr.sudoku.utils.CellUtils;
import javafx.scene.paint.Color;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ChainTechnique {

    private enum LinkType {
        STRONG,
        WEAK
    }

    private record Link (ICell start, ICell end, LinkType type){}

    private record GroupLink(GroupCell start, GroupCell end, LinkType type){}

    private record GroupCell(List<ICell> cells, List<Pair<ISudoku.GroupType, Integer>> allowedConnections){}

    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode
    protected static class CellNumPair {
        private ICell cell;
        private int num;
    }

    // https://www.sudokuwiki.org/Singles_Chains
    // Simple Coloring or Singles Chains
    public static Optional<TechniqueAction> simpleColoring(ISudoku sudoku) {
        for (int num = 1; num <= ISudoku.SUDOKU_SIZE; num++) {
            Set<ICell> emptyCells = sudoku.getEmptyCells();
            Map<ICell, Set<ICell>> linkMap = new HashMap<>();

            // Find chains for each number
            for (ICell cell : emptyCells) {
                if (cell.getCandidates().contains(num)) {
                    Set<ICell> links = findLinks(sudoku, cell, num);
                    if(!links.isEmpty())
                        linkMap.put(cell, links);
                }
            }

            List<Pair<Pos, Pos>> linkList = new ArrayList<>();

            // Color chains alternately
            List<ICell> keys = new ArrayList<>(linkMap.keySet());
            while(!keys.isEmpty()) {
                ICell current = keys.getFirst();
                keys.remove(current);
                List<ICell> openCells = new ArrayList<>();
                openCells.add(current);
                Map<ICell, Integer> coloring = new HashMap<>();
                coloring.put(current, 0);
                while (!openCells.isEmpty()) {
                    ICell cell = openCells.getFirst();
                    openCells.remove(cell);
                    for (ICell link : linkMap.get(cell)) {
                        if (!coloring.containsKey(link)) {
                            coloring.put(link, 1 - coloring.get(cell));
                            linkList.add(Pair.create(cell.getPos(), link.getPos()));
                            if (!openCells.contains(link))
                                openCells.add(link);
                            keys.remove(link);
                        } else if (Objects.equals(coloring.get(link), coloring.get(cell))) {
                            throw new RuntimeException("Two colors on same cell found");
                        }
                    }
                }

                Set<ICell> colorGroup0 = coloring.entrySet().stream().filter(e -> e.getValue() == 0).map(Map.Entry::getKey).collect(Collectors.toSet());
                Set<ICell> colorGroup1 = coloring.entrySet().stream().filter(e -> e.getValue() == 1).map(Map.Entry::getKey).collect(Collectors.toSet());
                List<TechniqueAction.CellColoring> techniqueCellColoring = new ArrayList<>();
                techniqueCellColoring.add(
                        new TechniqueAction.CandidatesColoring(
                                colorGroup0.stream().map(ICell::getPos).collect(Collectors.toSet()),
                                Color.YELLOW, Set.of(num))
                );
                techniqueCellColoring.add(
                        new TechniqueAction.CandidatesColoring(
                                colorGroup1.stream().map(ICell::getPos).collect(Collectors.toSet()),
                                Color.GREEN, Set.of(num))
                );
                techniqueCellColoring.add(
                        new TechniqueAction.LineColoring(linkList, Color.BLUE, num, false)
                );
                Optional<TechniqueAction> techniqueAction = hasDuplicateInUnit(colorGroup0, colorGroup1, num, techniqueCellColoring, "Simple Coloring");
                if (techniqueAction.isPresent())
                    return techniqueAction;
                techniqueAction = eliminateFromOutsideNeighboringCells(emptyCells, num, colorGroup0, colorGroup1, coloring.keySet(), techniqueCellColoring, "Simple Coloring");
                if (techniqueAction.isPresent())
                    return techniqueAction;
            }
        }

        return Optional.empty();
    }

    // https://www.sudokuwiki.org/3D_Medusa
    // 3D Medusa
    public static Optional<TechniqueAction> medusa3D(ISudoku sudoku) {
        Set<ICell> emptyCells = sudoku.getEmptyCells();
        Map<CellNumPair, Set<CellNumPair>> linkMap = new HashMap<>();
        for(ICell cell : emptyCells){
            for(int num : cell.getCandidates()){
                Set<ICell> links = findLinks(sudoku, cell, num);
                if(!links.isEmpty()){
                    linkMap.put(new CellNumPair(cell, num),
                            links.stream().map(link -> new CellNumPair(link, num)).collect(Collectors.toSet()));
                }
                if(cell.getCandidates().size() == 2){
                    int otherNum = cell.getCandidates().stream().filter(n -> n != num).findFirst().orElseThrow(() -> new RuntimeException("Expected two candidates in cell, but found only one"));
                    linkMap.computeIfAbsent(new CellNumPair(cell, num), _ -> new HashSet<>()).add(new CellNumPair(cell, otherNum));
                }
            }
        }

        List<Pair<Pair<Pos, Pos>, Integer>> linkList = new ArrayList<>();

        // Color chains alternately
        List<CellNumPair> keys = new ArrayList<>(linkMap.keySet());
        while (!keys.isEmpty()) {
            CellNumPair current = keys.getFirst();
            keys.remove(current);
            List<CellNumPair> openCells = new ArrayList<>();
            openCells.add(current);
            Map<CellNumPair, Integer> coloring = new HashMap<>();
            coloring.put(current, 0);
            while (!openCells.isEmpty()) {
                CellNumPair cell = openCells.getFirst();
                openCells.remove(cell);
                for (CellNumPair link : linkMap.get(cell)) {
                    if (!coloring.containsKey(link)) {
                        coloring.put(link, 1 - coloring.get(cell));
                        if(link.num == cell.num)
                            linkList.add(Pair.create(Pair.create(cell.getCell().getPos(), link.getCell().getPos()), cell.getNum()));
                        if (!openCells.contains(link))
                            openCells.add(link);
                        keys.remove(link);
                    } else if (Objects.equals(coloring.get(link), coloring.get(cell))) {
                        throw new RuntimeException("Two colors on same cell and number found");
                    }
                }
            }

            List<Integer> nums = coloring.keySet().stream().map(CellNumPair::getNum).distinct().toList();
            List<TechniqueAction.CellColoring> techniqueCellColoring = new ArrayList<>();
            techniqueCellColoring.addAll(
                    coloring.entrySet().stream()
                            .filter(e -> e.getValue() == 0)
                            .map(entry -> new TechniqueAction.CandidatesColoring(Set.of(entry.getKey().getCell().getPos()), Color.YELLOW, Set.of(entry.getKey().getNum()))
                            ).toList());
            techniqueCellColoring.addAll(
                    coloring.entrySet().stream()
                            .filter(e -> e.getValue() == 1)
                            .map(entry -> new TechniqueAction.CandidatesColoring(Set.of(entry.getKey().getCell().getPos()), Color.GREEN, Set.of(entry.getKey().getNum()))
                            ).toList());
            techniqueCellColoring.addAll(
                    linkList.stream()
                            .filter(pair -> coloring.keySet().stream().anyMatch(cellNumPair -> cellNumPair.getCell().getPos().equals(pair.getFirst().getFirst()) && cellNumPair.getNum() == pair.getSecond()))
                            .map(pair -> new TechniqueAction.LineColoring(List.of(pair.getFirst()), Color.BLUE, pair.getSecond(), false)
                            ).toList());
            for (int num : nums) {
                Set<ICell> colorGroup0 = coloring.entrySet().stream()
                        .filter(e -> e.getValue() == 0)
                        .map(Map.Entry::getKey).filter(pair -> pair.getNum() == num)
                        .map(CellNumPair::getCell)
                        .collect(Collectors.toSet());
                Set<ICell> colorGroup1 = coloring.entrySet().stream()
                        .filter(e -> e.getValue() == 1)
                        .map(Map.Entry::getKey).filter(pair -> pair.getNum() == num)
                        .map(CellNumPair::getCell)
                        .collect(Collectors.toSet());
                Set<ICell> group = coloring.keySet().stream()
                        .filter(pair -> pair.getNum() == num)
                        .map(CellNumPair::getCell)
                        .collect(Collectors.toSet());
                Optional<TechniqueAction> techniqueAction = hasDuplicateInUnit(colorGroup0, colorGroup1, num, techniqueCellColoring, "3D Medusa");
                if(techniqueAction.isPresent())
                    return techniqueAction;
                techniqueAction = eliminateFromOutsideNeighboringCells(emptyCells, num, colorGroup0, colorGroup1, group, techniqueCellColoring, "3D Medusa");
                if(techniqueAction.isPresent())
                    return techniqueAction;
            }
            Set<ICell> cells = coloring.keySet().stream().map(CellNumPair::getCell).collect(Collectors.toSet());
            Optional<TechniqueAction> techniqueAction = hasColorConflictInCell(coloring, cells, techniqueCellColoring);
            if (techniqueAction.isPresent()) return techniqueAction;
            techniqueAction = eliminateExtraCandidatesFromBicolorCells(coloring, cells, techniqueCellColoring);
            if (techniqueAction.isPresent()) return techniqueAction;
            techniqueAction = removeUncoloredDueToUnitAndCellConflict(coloring, cells, techniqueCellColoring);
            if (techniqueAction.isPresent()) return techniqueAction;
            techniqueAction = removeUncoloredDueToAllCandidatesSeeSameColor(sudoku, coloring, cells, techniqueCellColoring);
            if (techniqueAction.isPresent()) return techniqueAction;
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> hasColorConflictInCell(Map<CellNumPair, Integer> coloring, Set<ICell> cells, List<TechniqueAction.CellColoring> techniqueCellColoring) {
        // Rule 1
        // Check for same color appearing twice or more in a cell
        for(int i=0; i<= 1; i++) {
            int finalI = i;
            List<CellNumPair> color = coloring.entrySet().stream()
                    .filter(e -> e.getValue() == finalI)
                    .map(Map.Entry::getKey)
                    .toList();
            for(ICell cell : cells) {
                List<CellNumPair> colorPerCell = color.stream().filter(pair -> pair.getCell() == cell).toList();
                if(colorPerCell.size() > 1){
                    techniqueCellColoring.addAll(
                            colorPerCell.stream()
                                    .collect(Collectors.groupingBy(CellNumPair::getNum))
                                    .entrySet().stream()
                                    .map(keyValue -> new TechniqueAction.CandidatesColoring(keyValue.getValue().stream().map(entry -> entry.cell.getPos()).collect(Collectors.toSet()), Color.RED, List.of(keyValue.getKey()))
                                    ).toList());
                    return Optional.of(TechniqueAction.builder()
                            .name("3D Medusa")
                            .description("In cell " + cell.getPos() + " both " + colorPerCell.get(0).getNum() + " and " + colorPerCell.get(1).getNum() + " get the same color")
                            .removeCandidatesMap(
                                    colorPerCell.stream()
                                            .collect(Collectors.groupingBy(CellNumPair::getCell))
                                            .entrySet().stream()
                                            .collect(Collectors.toMap(entry -> entry.getKey().getPos(), entry -> entry.getValue().stream().map(CellNumPair::getNum).collect(Collectors.toSet())))
                            ).cellColorings(techniqueCellColoring).build());
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> hasDuplicateInUnit(Collection<ICell> colorGroup0, Collection<ICell> colorGroup1, int num, List<TechniqueAction.CellColoring> techniqueCellColoring, String name) {
        // Rule 2
        // Check for color appearing twice in unit [row, column, square]
        for (Collection<ICell> colorGroup : List.of(colorGroup0, colorGroup1)) {
            Map<Integer, ICell> rowsSeen = new HashMap<>();
            Map<Integer, ICell> colsSeen = new HashMap<>();
            Map<Pair<Integer, Integer>, ICell> squaresSeen = new HashMap<>();
            for (ICell cell : colorGroup) {
                Pair<Integer, Integer> pair = new Pair<>(cell.getX() / 3, cell.getY() / 3);

                ICell rowConflict = rowsSeen.get(cell.getY());
                ICell colConflict = colsSeen.get(cell.getX());
                ICell squareConflict = squaresSeen.get(pair);

                if (rowConflict != null) {
                    techniqueCellColoring.add(
                            new TechniqueAction.CandidatesColoring(Set.of(rowConflict.getPos(), cell.getPos()), Color.RED, Set.of(num))
                    );
                    return Optional.of(TechniqueAction.builder()
                            .name(name)
                            .description("In row " + cell.getY() + " both " + rowConflict.getPos() + " and " + cell.getPos() + " get the same color")
                            .removeCandidatesMap(Map.of(rowConflict.getPos(), Set.of(num), cell.getPos(), Set.of(num)))
                            .cellColorings(techniqueCellColoring).build());
                }
                else if (colConflict != null){
                    techniqueCellColoring.add(
                            new TechniqueAction.CandidatesColoring(Set.of(colConflict.getPos(), cell.getPos()), Color.RED, Set.of(num))
                    );
                    return Optional.of(TechniqueAction.builder()
                            .name(name)
                            .description("In column " + cell.getX() + " both " + colConflict.getPos() + " and " + cell.getPos() + " get the same color")
                            .removeCandidatesMap(Map.of(colConflict.getPos(), Set.of(num), cell.getPos(), Set.of(num)))
                            .cellColorings(techniqueCellColoring).build());
                }
                else if (squareConflict != null) {
                    techniqueCellColoring.add(
                            new TechniqueAction.CandidatesColoring(Set.of(squareConflict.getPos(), cell.getPos()), Color.RED, Set.of(num))
                    );
                    return Optional.of(TechniqueAction.builder()
                            .name(name)
                            .description("In square " + pair + " both " + squareConflict.getPos() + " and " + cell.getPos() + " get the same color")
                            .removeCandidatesMap(Map.of(squareConflict.getPos(), Set.of(num), cell.getPos(), Set.of(num)))
                            .cellColorings(techniqueCellColoring).build());
                }
                rowsSeen.put(cell.getY(), cell);
                colsSeen.put(cell.getX(), cell);
                squaresSeen.put(pair, cell);
            }
        }
        return Optional.empty();
    }


    private static Optional<TechniqueAction> eliminateExtraCandidatesFromBicolorCells(Map<CellNumPair, Integer> coloring, Set<ICell> cells, List<TechniqueAction.CellColoring> techniqueCellColoring) {
        // Rule 3
        // Check two colors in a cell with two candidates
        for(ICell cell : cells){
            CellNumPair color0 = coloring.entrySet().stream()
                    .filter(e -> e.getValue() == 0)
                    .map(Map.Entry::getKey)
                    .filter(pair -> pair.getCell() == cell)
                    .findFirst().orElse(null);
            CellNumPair color1 = coloring.entrySet().stream()
                    .filter(e -> e.getValue() == 1)
                    .map(Map.Entry::getKey)
                    .filter(pair -> pair.getCell() == cell)
                    .findFirst().orElse(null);
            if(color0 != null && color1 != null){
                if(cell.getCandidates().size() > 2){
                    Set<Integer> otherCandidates = cell.getCandidates().stream().filter(n -> n != color0.getNum() && n != color1.getNum()).collect(Collectors.toSet());
                    techniqueCellColoring.add(
                            new TechniqueAction.CandidatesColoring(Set.of(cell.getPos()), Color.RED, otherCandidates)
                    );
                    return Optional.of(TechniqueAction.builder()
                            .name("3D Medusa")
                            .description("Eliminate " + otherCandidates + " from " + cell.getPos() + " due to two colors in a cell with two candidates")
                            .removeCandidatesMap(Map.of(cell.getPos(), otherCandidates))
                            .cellColorings(techniqueCellColoring).build());
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> eliminateFromOutsideNeighboringCells(Set<ICell> emptyCells, int num, Collection<ICell> colorGroup0, Collection<ICell> colorGroup1, Set<ICell> group, List<TechniqueAction.CellColoring> techniqueCellColoring, String name) {
        // Rule 4
        // Check for two colors neighbouring a non-chain cells
        for (ICell cell : emptyCells) {
            if (!group.contains(cell) && cell.getCandidates().contains(num) &&
                    hasConnection(cell, colorGroup0) && hasConnection(cell, colorGroup1)) {
                techniqueCellColoring.add(
                        new TechniqueAction.CandidatesColoring(Set.of(cell.getPos()), Color.RED, Set.of(num))
                );
                return Optional.of(TechniqueAction.builder()
                        .name(name)
                        .description("Eliminate " + num + " from " + cell.getPos() + " due to two colors neighboring a non-chain cell")
                        .removeCandidatesMap(Map.of(cell.getPos(), Set.of(num)))
                        .cellColorings(techniqueCellColoring).build());
            }
        }
        return Optional.empty();
    }

    // Rule 5 is not tested cause of complexity of test propagation
    // Rule 5 is causing several cells to be to the 3d chain
    // and causes rule 4 to be triggered almost after every rule 5
    private static Optional<TechniqueAction> removeUncoloredDueToUnitAndCellConflict(Map<CellNumPair, Integer> coloring, Set<ICell> cells, List<TechniqueAction.CellColoring> techniqueCellColoring) {
        // Rule 5
        // Check for uncolored candidate in a colored cell with a peer with the same candidate colored

        for (ICell cell : cells) {
            // Extract the colored candidates for the current cell
            Map<CellNumPair, Integer> coloredCandidates = coloring.entrySet().stream()
                    .filter(entry -> entry.getKey().getCell() == cell)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // Extract numbers not present in the colored candidates for the current cell
            Set<Integer> otherCandidates = cell.getCandidates().stream()
                    .filter(n -> !coloredCandidates.keySet().stream().map(CellNumPair::getNum).collect(Collectors.toSet()).contains(n))
                    .collect(Collectors.toSet());

            int color = coloredCandidates.values().stream().findFirst().orElseThrow(() -> new RuntimeException("No color found for cell for a colored cell"));

            // Check if there's a connection between the current cell and other color group for each other candidate
            for (Integer otherCandidate : otherCandidates) {
                Set<ICell> otherColorGroup = coloring.entrySet().stream()
                        .filter(entry -> entry.getValue() != color && entry.getKey().getNum() == otherCandidate)
                        .map(entry -> entry.getKey().getCell())
                        .collect(Collectors.toSet());

                if (hasConnection(cell, otherColorGroup)) {
                    techniqueCellColoring.add(
                            new TechniqueAction.CandidatesColoring(Set.of(cell.getPos()), Color.RED, Set.of(otherCandidate))
                    );
                    return Optional.of(TechniqueAction.builder()
                            .name("3D Medusa")
                            .description("Eliminate " + otherCandidate + " from " + cell.getPos() + " due to uncolored candidate in a colored cell with a peer with the same candidate colored")
                            .removeCandidatesMap(Map.of(cell.getPos(), Set.of(otherCandidate)))
                            .cellColorings(techniqueCellColoring).build());
                }
            }
        }
        return Optional.empty();
    }

    // Rule 6 is not tested cause of complexity of test propagation
    // Rule 6 is not triggering cause previous rules trigger first
    private static Optional<TechniqueAction> removeUncoloredDueToAllCandidatesSeeSameColor(ISudoku sudoku, Map<CellNumPair, Integer> coloring, Set<ICell> cells, List<TechniqueAction.CellColoring> techniqueColoring) {
        // Rule 6
        // Check if uncolored cell has all candidates seeing the same color
        Set<ICell> uncoloredCells = sudoku.getEmptyCells().stream().filter(cell -> !cells.contains(cell)).collect(Collectors.toSet());
        for(ICell uncoloredCell : uncoloredCells) {
            for (int color = 0; color <= 1; color++) {
                int finalColor = color;
                Set<CellNumPair> neighboringColorGroup = coloring.entrySet().stream()
                        .filter(entry -> entry.getValue() == finalColor && CellUtils.isPeer(uncoloredCell, entry.getKey().getCell()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
                if (uncoloredCell.getCandidates().stream().allMatch(candidate ->
                        neighboringColorGroup.stream().anyMatch(pair -> pair.getNum() == candidate)
                )) {

                    Map<Pos, Set<Integer>> candidatesToRemove = neighboringColorGroup.stream()
                            .filter(toBeDeleted -> uncoloredCell.getCandidates().contains(toBeDeleted.getNum()))
                            .collect(Collectors.groupingBy(pair -> pair.getCell().getPos(), Collectors.mapping(CellNumPair::getNum, Collectors.toSet())));
                    techniqueColoring.addAll(
                            candidatesToRemove.entrySet().stream()
                                    .map(entry -> new TechniqueAction.CandidatesColoring(Set.of(entry.getKey()), Color.RED, entry.getValue())
                                    ).toList());
                    return Optional.of(TechniqueAction.builder()
                            .name("3D Medusa")
                            .description("Eliminate " + candidatesToRemove.keySet() + " from " + uncoloredCell.getPos() + " due to all candidates seeing the same color")
                            .removeCandidatesMap(candidatesToRemove)
                            .cellColorings(techniqueColoring).build());
                }
            }
        }
        return Optional.empty();
    }


    // https://www.sudokuwiki.org/X_Cycles
    // https://www.sudokuwiki.org/X_Cycles_Part_2
    // X-Cycles
    public static Optional<TechniqueAction> xCycle(ISudoku sudoku) {
        for(int num =1; num <= ISudoku.SUDOKU_SIZE; num ++){
            Set<Pair<ICell, ICell>> strongLinks = generateStrongLinks(sudoku, num);
            for(Pair<ICell, ICell> strongLink: strongLinks){
                List<Link> cycle = new ArrayList<>();
                Set<ICell> visited = new HashSet<>();
                cycle.add(new Link(strongLink.getFirst(), strongLink.getSecond(), LinkType.STRONG));
                visited.add(strongLink.getFirst());
                if(findCycle(sudoku, strongLink.getSecond(), cycle, num, visited, strongLinks)){
                    Optional<TechniqueAction> techniqueAction = handleCycle(sudoku, cycle, num);
                    if(techniqueAction.isPresent())
                        return techniqueAction;
                }
                cycle = new ArrayList<>();
                visited = new HashSet<>();
                cycle.add(new Link(strongLink.getSecond(), strongLink.getFirst(), LinkType.STRONG));
                visited.add(strongLink.getFirst());
                if(findCycle(sudoku, strongLink.getFirst(), cycle, num, visited, strongLinks)){
                    Optional<TechniqueAction> techniqueAction = handleCycle(sudoku, cycle, num);
                    if(techniqueAction.isPresent())
                        return techniqueAction;
                }
            }
        }
        return Optional.empty();
    }

    private static boolean findCycle(ISudoku sudoku, ICell current, List<Link> cycle, int num, Set<ICell> visited, Set<Pair<ICell, ICell>> strongLinks) {
        if(cycle.getFirst().start == current)
            return true;
        if(CellUtils.isPeer(current, cycle.getFirst().start) && cycle.size() > 2){
            // Cycle found
            // Check for contradictions and make deductions
            cycle.add(new Link(current, cycle.getFirst().start, LinkType.WEAK));
            return true;
        }

        // Find links from current cell
        List<Link> possibleLinks = new ArrayList<>();
        for(Pair<ICell, ICell> strongLink : strongLinks){
            ICell first = strongLink.getFirst();
            ICell second = strongLink.getSecond();
            if(visited.contains(first) || visited.contains(second))
                continue;
            if(CellUtils.isPeer(current, first)){
                possibleLinks.add(new Link(first, second, LinkType.STRONG));
            }
            if(CellUtils.isPeer(current, second)){
                possibleLinks.add(new Link(second, first, LinkType.STRONG));
            }
        }

        for (Link possibleLink: possibleLinks){
            if(strongLinks.stream().map(l -> List.of(l.getFirst(), l.getSecond())).anyMatch(l -> l.contains(current) && l.contains(possibleLink.start)))
                continue;
            cycle.add(new Link(current, possibleLink.start, LinkType.WEAK));
            cycle.add(possibleLink);
            visited.add(possibleLink.start);
            visited.add(possibleLink.end);
            if(findCycle(sudoku, possibleLink.end, cycle, num, visited, strongLinks)){
                return true;
            }
            cycle.removeLast();
            cycle.removeLast();
            visited.remove(possibleLink.start);
            visited.remove(possibleLink.end);
        }
        Set<ICell> commonPeers = getCommonPeers(sudoku, List.of(cycle.getFirst().start, current));
        commonPeers = commonPeers.stream().filter(c -> c.getCandidates().contains(num)).collect(Collectors.toSet());
        if(!commonPeers.isEmpty()){
            ICell commonPeer = commonPeers.iterator().next();
            if(strongLinks.stream().map(l-> List.of(l.getFirst(), l.getSecond())).anyMatch(l ->
                    (l.contains(current) && l.contains(commonPeer))
                            || (l.contains(cycle.getFirst().start) && l.contains(commonPeer))
            ))
                return false;
            cycle.add(new Link(current, commonPeer, LinkType.WEAK));
            cycle.addFirst(new Link(commonPeer, cycle.getFirst().start, LinkType.WEAK));
            return true;
        }
        return false;
    }

    private static Set<Pair<ICell, ICell>> generateStrongLinks(ISudoku sudoku, int num) {
        Set<Pair<ICell, ICell>> strongLinks = new HashSet<>();
        for(int i=0; i<ISudoku.SUDOKU_SIZE;i++){
            List<ICell> row = Arrays.stream(sudoku.getRow(i)).filter(c -> c.getCandidates().contains(num)).toList();
            if(row.size() == 2){
                strongLinks.add(Pair.create(row.getFirst(), row.getLast()));
            }
            List<ICell> col = Arrays.stream(sudoku.getColumn(i)).filter(c -> c.getCandidates().contains(num)).toList();
            if(col.size() == 2){
                strongLinks.add(Pair.create(col.getFirst(), col.getLast()));
            }
            List<ICell> square = Arrays.stream(sudoku.getSquare(i)).filter(c -> c.getCandidates().contains(num)).toList();
            if(square.size() == 2){
                strongLinks.add(Pair.create(square.getFirst(), square.getLast()));
            }
        }
        return strongLinks;
    }

    private static Optional<TechniqueAction> handleCycle(ISudoku sudoku, List<Link> cycle, int num) {
        Set<Pos> col1 = cycle.stream().filter(l -> l.type == LinkType.STRONG).map(l -> l.start.getPos()).collect(Collectors.toSet());
        Set<Pos> col2 = cycle.stream().filter(l -> l.type == LinkType.STRONG).map(l -> l.end.getPos()).collect(Collectors.toSet());
        List<Pair<Pos, Pos>> weakLinks = cycle.stream().filter(l -> l.type == LinkType.WEAK).map(l -> Pair.create(l.start.getPos(), l.end.getPos())).toList();
        List<Pair<Pos, Pos>> strongLinks = cycle.stream().filter(l -> l.type == LinkType.STRONG).map(l -> Pair.create(l.start.getPos(), l.end.getPos())).toList();
        Link startLink = cycle.getFirst();
        Link endLink = cycle.getLast();
        if(startLink.type == endLink.type){
            // Discontinuous cycle
            if(startLink.type == LinkType.STRONG){
                return Optional.of(TechniqueAction.builder()
                        .name("X-Cycle")
                        .description("Cell " + startLink.start.getPos() + " has to be " + num)
                        .setValueMap(Map.of(startLink.start.getPos(), num))
                        .cellColorings(List.of(
                                new TechniqueAction.CandidatesColoring(col1, Color.YELLOW, Set.of(num)),
                                new TechniqueAction.CandidatesColoring(col2, Color.BLUE, Set.of(num)),
                                new TechniqueAction.LineColoring(weakLinks, Color.BLUE, num, true),
                                new TechniqueAction.LineColoring(strongLinks, Color.BLUE, num, false),
                                new TechniqueAction.CandidatesColoring(List.of(startLink.start.getPos()), Color.GREEN, Set.of(num))
                        )).build());
            }
            else {
                return Optional.of(TechniqueAction.builder()
                        .name("X-Cycle")
                        .description("Eliminate " + num + " from " + startLink.start.getPos())
                        .removeCandidatesMap(Map.of(startLink.start.getPos(), Set.of(num)))
                        .cellColorings(List.of(
                                new TechniqueAction.CandidatesColoring(col1, Color.YELLOW, Set.of(num)),
                                new TechniqueAction.CandidatesColoring(col2, Color.GREEN, Set.of(num)),
                                new TechniqueAction.LineColoring(weakLinks, Color.BLUE, num, true),
                                new TechniqueAction.LineColoring(strongLinks, Color.BLUE, num, false),
                                new TechniqueAction.CandidatesColoring(List.of(startLink.start.getPos()), Color.RED, Set.of(num))
                        )).build());
            }
        }
        else {
            // Continuous cycle
            List<ICell> affectedCells = new ArrayList<>();
            List<Pair<Pos, Pos>> groupColoring = new ArrayList<>();
            for (Link link : cycle.stream().filter(l -> l.type == LinkType.WEAK).toList()) {
                Pair<List<ICell>, ISudoku.GroupType> result = findCommon(sudoku, link.start, link.end);
                List<ICell> common = result.getFirst().stream().filter(c -> c.getCandidates().contains(num)).toList();
                if(!common.isEmpty()){
                    affectedCells.addAll(common);
                    groupColoring.add(switch (result.getSecond()) {
                        case ROW -> Pair.create(new Pos(0, link.start.getPos().y()), new Pos(8, link.start.getPos().y()));
                        case COLUMN -> Pair.create(new Pos(link.start.getPos().x(), 0), new Pos(link.start.getPos().x(), 8));
                        case SQUARE -> Pair.create(new Pos((link.start.getPos().x() / 3) * 3, (link.start.getPos().y() / 3) * 3), new Pos((link.start.getPos().x() / 3) * 3 + 2, (link.start.getPos().y() / 3) * 3 + 2));
                    });
                }
            }
            if(!affectedCells.isEmpty())
                return Optional.of(TechniqueAction.builder()
                        .name("X-Cycle")
                        .description("Eliminate " + num + " from common peers")
                        .removeCandidatesMap(affectedCells.stream().collect(Collectors.toMap(ICell::getPos, _ -> Set.of(num))))
                        .cellColorings(List.of(
                                new TechniqueAction.CandidatesColoring(col1, Color.YELLOW, Set.of(num)),
                                new TechniqueAction.CandidatesColoring(col2, Color.GREEN, Set.of(num)),
                                new TechniqueAction.LineColoring(weakLinks, Color.BLUE, num, true),
                                new TechniqueAction.LineColoring(strongLinks, Color.BLUE, num, false),
                                new TechniqueAction.GroupColoring(groupColoring, Color.YELLOW),
                                new TechniqueAction.CandidatesColoring(affectedCells.stream().map(ICell::getPos).collect(Collectors.toSet()), Color.RED, Set.of(num))
                        )).build());
        }
        return Optional.empty();
    }

    // https://www.sudokuwiki.org/XY_Chains
    // XY-Chains
    // This technique is not tested cause of complexity of test
    // XY chains can be a bit tricky and one may cause another to be created or destroyed
    public static Optional<TechniqueAction> xyChain(ISudoku sudoku) {
        for(ICell cell : sudoku.getEmptyCells().stream().filter(c -> c.getCandidates().size() == 2).toList()){
            for (int candidate: cell.getCandidates()){
                int otherCandidate = cell.getCandidates().stream()
                        .filter(c -> c != candidate)
                        .findFirst().orElseThrow( () -> new IllegalStateException("Expected other candidate not found"));
                List<List<ICell>> chains = findXYChains(sudoku, cell, candidate, otherCandidate);
                for (List<ICell> chain: chains){
                    // Check for contradictions and make deductions
                    Optional<TechniqueAction> techniqueAction = handleXYChain(sudoku, chain, otherCandidate);
                    if(techniqueAction.isPresent()){
                        return techniqueAction;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> handleXYChain(ISudoku sudoku, List<ICell> chain, int otherCandidate) {
        List<Pair<Integer, Pair<Pos, Pos>>> weakLinks = new ArrayList<>();
        int currentCandidate = chain.getFirst().getCandidates().stream()
                .filter(c -> c != otherCandidate)
                .findFirst().orElseThrow( () -> new IllegalStateException("Expected other candidate not found"));
        for(int i=0; i<chain.size()-1; i++){
            weakLinks.add(Pair.create(currentCandidate, Pair.create(chain.get(i).getPos(), chain.get(i+1).getPos())));
            int finalCurrentCandidate = currentCandidate;
            currentCandidate = chain.get(i+1).getCandidates().stream()
                    .filter(c -> c != finalCurrentCandidate)
                    .findFirst().orElseThrow( () -> new IllegalStateException("Expected other candidate not found"));
        }
        ICell start = chain.getFirst();
        ICell end = chain.getLast();
        Set<ICell> commonPeers = CellUtils.getPeers(sudoku, start);
        commonPeers.retainAll(CellUtils.getPeers(sudoku, end));
        commonPeers = commonPeers.stream().filter(c -> c.getCandidates().contains(otherCandidate)).collect(Collectors.toSet());
        if(!commonPeers.isEmpty()){
            List<TechniqueAction.CellColoring> coloringList = weakLinks.stream().map(link -> new TechniqueAction.LineColoring(List.of(link.getSecond()), Color.BLUE, link.getFirst(), true)).collect(Collectors.toList());
            coloringList.addAll(weakLinks.stream().map(link -> new TechniqueAction.CandidatesColoring(Set.of(link.getSecond().getFirst()), Color.BLUE, Set.of(link.getFirst()))).toList());
            coloringList.addAll(weakLinks.stream().map(link -> new TechniqueAction.CandidatesColoring(Set.of(link.getSecond().getSecond()), Color.YELLOW, Set.of(link.getFirst()))).toList());
            coloringList.addAll(List.of(
                    new TechniqueAction.CandidatesColoring(Set.of(start.getPos()), Color.YELLOW, Set.of(otherCandidate)),
                    new TechniqueAction.CandidatesColoring(Set.of(end.getPos()), Color.BLUE, Set.of(otherCandidate)),
                    new TechniqueAction.CandidatesColoring(commonPeers.stream().map(ICell::getPos).collect(Collectors.toSet()), Color.RED, Set.of(otherCandidate))));
            return Optional.of(TechniqueAction.builder()
                    .name("XY-Chain")
                    .description("Eliminate " + otherCandidate + " from common peers of " + start.getPos() + " and " + end.getPos())
                    .removeCandidatesMap(commonPeers.stream().collect(Collectors.toMap(ICell::getPos, _ -> Set.of(otherCandidate))))
                    .cellColorings(coloringList).build());
        }
        return Optional.empty();
    }

    private static List<List<ICell>> findXYChains(ISudoku sudoku, ICell startCell, int currentCandidate, int otherCandidate) {
        List<List<ICell>> allChains = new ArrayList<>();
        List<ICell> chain = new ArrayList<>();
        chain.add(startCell);
        findXyChainsRecursive(sudoku, startCell, currentCandidate, otherCandidate, chain, allChains);
        return allChains;
    }
    private static void findXyChainsRecursive(ISudoku sudoku, ICell cell, int currentCandidate, int otherCandidate, List<ICell> chain, List<List<ICell>> allChains) {
        if(currentCandidate == otherCandidate)
            allChains.add(new ArrayList<>(chain));

        List<ICell> peers = CellUtils.getPeers(sudoku, cell).stream()
                .filter(c -> c.getCandidates().contains(currentCandidate) && c.getCandidates().size() == 2)
                .toList();

        for(ICell peer : peers){
            if(chain.contains(peer))
                continue;
            int nextCandidate = peer.getCandidates().stream()
                    .filter(c -> c != currentCandidate)
                    .findFirst().orElseThrow( () -> new IllegalStateException("Expected other candidate not found"));

            chain.add(peer);
            findXyChainsRecursive(sudoku, peer, nextCandidate, otherCandidate, chain, allChains);
            chain.removeLast();
        }
    }
    // https://www.sudokuwiki.org/SK_Loops
    // SK Loops
    public static Optional<TechniqueAction> skLoop(ISudoku sudoku) {
        Set<Pos> nonEmptyPosSet = sudoku.getNonEmptyCells().stream().map(ICell::getPos).collect(Collectors.toSet());
        Iterator<Pos> posIterator = nonEmptyPosSet.iterator();
        while (posIterator.hasNext()) {
            Pos pos1 = posIterator.next();
            posIterator.remove();

            for (Pos pos4 : nonEmptyPosSet) {
                if (pos1.x() / 3 == pos4.x() / 3 || pos1.y() / 3 == pos4.y() / 3)
                    continue;
                Pos pos2 = new Pos(pos4.x(), pos1.y());
                if (!nonEmptyPosSet.contains(pos2))
                    continue;
                Pos pos3 = new Pos(pos1.x(), pos4.y());
                if (!nonEmptyPosSet.contains(pos3))
                    continue;

                Map<Pos, Map<ISudoku.GroupType, Set<ICell>>> cellsMap = Map.of(
                        pos1, Map.of(ISudoku.GroupType.ROW, findAdjacentCells(sudoku, pos1, true), ISudoku.GroupType.COLUMN, findAdjacentCells(sudoku, pos1, false)),
                        pos2, Map.of(ISudoku.GroupType.ROW, findAdjacentCells(sudoku, pos2, true), ISudoku.GroupType.COLUMN, findAdjacentCells(sudoku, pos2, false)),
                        pos3, Map.of(ISudoku.GroupType.ROW, findAdjacentCells(sudoku, pos3, true), ISudoku.GroupType.COLUMN, findAdjacentCells(sudoku, pos3, false)),
                        pos4, Map.of(ISudoku.GroupType.ROW, findAdjacentCells(sudoku, pos4, true), ISudoku.GroupType.COLUMN, findAdjacentCells(sudoku, pos4, false))
                );
                Map<Integer, List<Set<Integer>>> rowCommonCandidatesCombinations = new HashMap<>();
                Map<Integer, List<Set<Integer>>> colCommonCandidatesCombinations = new HashMap<>();

                rowCommonCandidatesCombinations.put(pos1.y(), getCommonCandidatesCombinations(cellsMap.get(pos1).get(ISudoku.GroupType.ROW), cellsMap.get(pos2).get(ISudoku.GroupType.ROW)));
                rowCommonCandidatesCombinations.put(pos4.y(), getCommonCandidatesCombinations(cellsMap.get(pos3).get(ISudoku.GroupType.ROW), cellsMap.get(pos4).get(ISudoku.GroupType.ROW)));

                colCommonCandidatesCombinations.put(pos1.x(), getCommonCandidatesCombinations(cellsMap.get(pos1).get(ISudoku.GroupType.COLUMN), cellsMap.get(pos3).get(ISudoku.GroupType.COLUMN)));
                colCommonCandidatesCombinations.put(pos4.x(), getCommonCandidatesCombinations(cellsMap.get(pos2).get(ISudoku.GroupType.COLUMN), cellsMap.get(pos4).get(ISudoku.GroupType.COLUMN)));

                if (rowCommonCandidatesCombinations.values().stream().anyMatch(List::isEmpty) || colCommonCandidatesCombinations.values().stream().anyMatch(List::isEmpty))
                    continue;
                for (int k = 0; k < rowCommonCandidatesCombinations.get(pos1.y()).size(); k++) {
                    for (int l = 0; l < rowCommonCandidatesCombinations.get(pos4.y()).size(); l++) {
                        for (int m = 0; m < colCommonCandidatesCombinations.get(pos1.x()).size(); m++) {
                            for (int n = 0; n < colCommonCandidatesCombinations.get(pos4.x()).size(); n++) {
                                Set<Integer> rowCommonCandidates1 = rowCommonCandidatesCombinations.get(pos1.y()).get(k);
                                Set<Integer> rowCommonCandidates2 = rowCommonCandidatesCombinations.get(pos4.y()).get(l);
                                Set<Integer> colCommonCandidates1 = colCommonCandidatesCombinations.get(pos1.x()).get(m);
                                Set<Integer> colCommonCandidates2 = colCommonCandidatesCombinations.get(pos4.x()).get(n);
                                if (rowCommonCandidates1.isEmpty() || rowCommonCandidates2.isEmpty() || colCommonCandidates1.isEmpty() || colCommonCandidates2.isEmpty())
                                    continue;

                                Map<Integer, Set<Integer>> rowCommonCandidates = Map.of(pos1.y(), rowCommonCandidates1, pos4.y(), rowCommonCandidates2);
                                Map<Integer, Set<Integer>> colCommonCandidates = Map.of(pos1.x(), colCommonCandidates1, pos4.x(), colCommonCandidates2);
                                Optional<TechniqueAction> techniqueAction = handlePossibleSKLoop(sudoku, pos1, pos2, pos3, pos4, cellsMap, rowCommonCandidates, colCommonCandidates);
                                if (techniqueAction.isPresent())
                                    return techniqueAction;
                            }
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> handlePossibleSKLoop(ISudoku sudoku, Pos pos1, Pos pos2, Pos pos3, Pos pos4, Map<Pos, Map<ISudoku.GroupType, Set<ICell>>> cellsMap, Map<Integer, Set<Integer>> rowCommonCandidates, Map<Integer, Set<Integer>> colCommonCandidates) {
        Map<Pos, Set<Integer>> uniqueCandidates = new HashMap<>();
        boolean hadMismatch = false;
        for (Pos pos : List.of(pos1, pos2, pos3, pos4)) {
            Set<Integer> candidateRows = cellsMap.get(pos).get(ISudoku.GroupType.ROW).stream()
                    .flatMap(c -> c.getCandidates().stream())
                    .filter(candidate -> !rowCommonCandidates.get(pos.y()).contains(candidate))
                    .collect(Collectors.toSet());
            Set<Integer> candidateCols = cellsMap.get(pos).get(ISudoku.GroupType.COLUMN).stream()
                    .flatMap(c -> c.getCandidates().stream())
                    .filter(candidate -> !colCommonCandidates.get(pos.x()).contains(candidate))
                    .collect(Collectors.toSet());
            if (candidateCols.isEmpty() || candidateRows.isEmpty() || !candidateCols.equals(candidateRows)) {
                hadMismatch = true;
                break;
            }
            uniqueCandidates.put(pos, candidateRows);
        }
        if (hadMismatch)
            return Optional.empty();

        int counter1 = 0;
        int counter2 = 0;
        Map<Pos, Set<Integer>> coloring1 = new HashMap<>();
        Map<Pos, Set<Integer>> coloring2 = new HashMap<>();
        boolean oscillator = false;
        for (Pos pos : List.of(pos1, pos2, pos4, pos3)) {
            if (oscillator) {
                Set<Integer> uniqueCandidates1 = uniqueCandidates.get(pos);
                counter1 += uniqueCandidates1.size();
                counter1 += colCommonCandidates.get(pos.x()).size();

                Set<Integer> uniqueCandidates2 = uniqueCandidates.get(pos);
                counter2 += uniqueCandidates2.size();
                counter2 += rowCommonCandidates.get(pos.y()).size();

                cellsMap.get(pos).get(ISudoku.GroupType.ROW).forEach(c -> coloring1.put(c.getPos(), uniqueCandidates1));
                cellsMap.get(pos).get(ISudoku.GroupType.COLUMN).forEach(c -> coloring1.put(c.getPos(), colCommonCandidates.get(pos.x())));

                cellsMap.get(pos).get(ISudoku.GroupType.COLUMN).forEach(c -> coloring2.put(c.getPos(), uniqueCandidates2));
                cellsMap.get(pos).get(ISudoku.GroupType.ROW).forEach(c -> coloring2.put(c.getPos(), rowCommonCandidates.get(pos.y())));
            } else {
                Set<Integer> uniqueCandidates2 = uniqueCandidates.get(pos);
                counter2 += uniqueCandidates2.size();
                counter2 += colCommonCandidates.get(pos.x()).size();

                Set<Integer> uniqueCandidates1 = uniqueCandidates.get(pos);
                counter1 += uniqueCandidates1.size();
                counter1 += rowCommonCandidates.get(pos.y()).size();

                cellsMap.get(pos).get(ISudoku.GroupType.ROW).forEach(c -> coloring2.put(c.getPos(), uniqueCandidates2));
                cellsMap.get(pos).get(ISudoku.GroupType.COLUMN).forEach(c -> coloring2.put(c.getPos(), colCommonCandidates.get(pos.x())));

                cellsMap.get(pos).get(ISudoku.GroupType.COLUMN).forEach(c -> coloring1.put(c.getPos(), uniqueCandidates1));
                cellsMap.get(pos).get(ISudoku.GroupType.ROW).forEach(c -> coloring1.put(c.getPos(), rowCommonCandidates.get(pos.y())));
            }
            oscillator = !oscillator;
        }
        if (counter1 > 16 || counter2 > 16)
            return Optional.empty();

        // Found SK-Loop
        Map<Pos, Set<Integer>> candidatesToRemove = new HashMap<>();
        rowCommonCandidates.forEach((row, candidates) -> Arrays.stream(sudoku.getRow(row))
                .filter(c -> c.getX() / 3 != pos1.x() / 3 && c.getX() / 3 != pos4.x() / 3)
                .filter(c -> c.getCandidates().stream().anyMatch(candidates::contains))
                .forEach(c -> candidatesToRemove.put(c.getPos(), candidates)));
        colCommonCandidates.forEach((col, candidates) -> Arrays.stream(sudoku.getColumn(col))
                .filter(c -> c.getY() / 3 != pos1.y() / 3 && c.getY() / 3 != pos4.y() / 3)
                .filter(c -> c.getCandidates().stream().anyMatch(candidates::contains))
                .forEach(c -> candidatesToRemove.put(c.getPos(), candidates)));
        for (Pos pos : List.of(pos1, pos2, pos3, pos4)) {
            Set<Integer> candidates = uniqueCandidates.get(pos);
            Arrays.stream(sudoku.getSquare(pos.x(), pos.y()))
                    .filter(c -> c.getX() != pos.x() && c.getY() != pos.y())
                    .filter(c -> c.getCandidates().stream().anyMatch(candidates::contains))
                    .forEach(c -> candidatesToRemove.put(c.getPos(), candidates));
        }

        if (candidatesToRemove.isEmpty())
            return Optional.empty();

        List<TechniqueAction.CellColoring> colorings = new ArrayList<>();
        coloring1.forEach((pos, candidates) -> colorings.add(new TechniqueAction.CandidatesColoring(Set.of(pos), Color.YELLOW, candidates)));
        coloring2.forEach((pos, candidates) -> colorings.add(new TechniqueAction.CandidatesColoring(Set.of(pos), Color.BLUE, candidates)));
        candidatesToRemove.forEach((pos, candidates) -> colorings.add(new TechniqueAction.CandidatesColoring(Set.of(pos), Color.RED, candidates)));
        Stream.of(pos1, pos2, pos3, pos4).forEach(pos -> colorings.add(new TechniqueAction.GroupColoring(List.of(Pair.create(pos, pos)), Color.CYAN)));

        return Optional.of(TechniqueAction.builder()
                .name("SK-Loop")
                .description("Eliminate candidates from SK-Loop formed by " + pos1 + ", " + pos2 + ", " + pos3 + ", " + pos4)
                .removeCandidatesMap(candidatesToRemove)
                .cellColorings(colorings)
                .build());
    }

    private static List<Set<Integer>> getCommonCandidatesCombinations(Set<ICell> cellsA, Set<ICell> cellsB){
        Set<Integer> candidates = getCommonCandidates(cellsA, cellsB);
        List<Set<Integer>> combinations = new ArrayList<>();
        for(int i=1; i<Math.pow(2, candidates.size()); i++){
            Set<Integer> combination = new HashSet<>();
            for(int j=0; j<candidates.size(); j++){
                if((i & (1 << j)) > 0)
                    combination.add((Integer) candidates.toArray()[j]);
            }
            combinations.add(combination);
        }
        combinations.sort(Comparator.comparingInt(Set<Integer>::size).reversed());
        return combinations;
    }

    private static Set<Integer> getCommonCandidates(Set<ICell> cellsA, Set<ICell> cellsB) {
        Set<Integer> candidatesA = cellsA.stream().flatMap(c -> c.getCandidates().stream()).collect(Collectors.toSet());
        Set<Integer> candidatesB = cellsB.stream().flatMap(c -> c.getCandidates().stream()).collect(Collectors.toSet());
        candidatesA.retainAll(candidatesB);
        return candidatesA;
    }

    private static Set<ICell> findAdjacentCells(ISudoku sudoku, Pos pos, boolean isRow){
        if(isRow)
            return IntStream.range(0,3)
                    .map(i -> pos.x()/3*3 + i)
                    .filter(i -> i != pos.x())
                    .mapToObj(i -> sudoku.getCell(i, pos.y()))
                    .collect(Collectors.toSet());
        else
            return IntStream.range(0,3)
                    .map(i -> pos.y()/3*3 + i)
                    .filter(i -> i != pos.y())
                    .mapToObj(i -> sudoku.getCell(pos.x(), i))
                    .collect(Collectors.toSet());
    }

    // https://www.sudokuwiki.org/Grouped_X-Cycles
    // Grouped X-Cycles
    public static Optional<TechniqueAction> groupedXCycle(ISudoku sudoku) {
        for(int num = 1; num <= ISudoku.SUDOKU_SIZE; num++) {
            Set<Pair<GroupCell, GroupCell>> strongLinks = generateGroupedStrongLinks(sudoku, num);
            for(Pair<GroupCell, GroupCell> strongLink: strongLinks){
                List<GroupLink> cycle = new ArrayList<>();
                cycle.add(new GroupLink(strongLink.getFirst(), strongLink.getSecond(), LinkType.STRONG));
                if(findGroupCycle(sudoku, strongLink.getSecond(), cycle, num, strongLinks)){
                    Optional<TechniqueAction> techniqueAction = handleGroupCycle(sudoku, cycle, num);
                    if(techniqueAction.isPresent())
                        return techniqueAction;
                }
                cycle = new ArrayList<>();
                cycle.add(new GroupLink(strongLink.getSecond(), strongLink.getFirst(), LinkType.STRONG));
                if(findGroupCycle(sudoku, strongLink.getFirst(), cycle, num, strongLinks)){
                    Optional<TechniqueAction> techniqueAction = handleGroupCycle(sudoku, cycle, num);
                    if(techniqueAction.isPresent())
                        return techniqueAction;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> handleGroupCycle(ISudoku sudoku, List<GroupLink> cycle, int num) {
        Set<Pos> col1 = cycle.stream().filter(l -> l.type == LinkType.STRONG).flatMap(l -> l.start.cells().stream().map(ICell::getPos)).collect(Collectors.toSet());
        Set<Pos> col2 = cycle.stream().filter(l -> l.type == LinkType.STRONG).flatMap(l -> l.end.cells().stream().map(ICell::getPos)).collect(Collectors.toSet());
        List<Pair<Pos, Pos>> weakLinks = cycle.stream().filter(l -> l.type == LinkType.WEAK)
                .map(l -> {
                    List<Pos> startPositions = l.start.cells().stream().map(ICell::getPos).toList();
                    List<Pos> endPositions = l.end.cells().stream().map(ICell::getPos).toList();

                    return startPositions.stream()
                            .flatMap(startPos -> endPositions.stream()
                                    .map(endPos -> Pair.create(startPos, endPos)))
                            // switched from pythagorean distance to chebyshev distance so diagonal and orthogonal links are treated equally
                            .min(Comparator.comparingDouble(pair -> Math.max((pair.getFirst().x() - pair.getSecond().x()), (pair.getFirst().y() - pair.getSecond().y()))))
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .toList();
        List<Pair<Pos, Pos>> strongLinks = cycle.stream().filter(l -> l.type == LinkType.STRONG)
                .map(l -> {
                    List<Pos> startPositions = l.start.cells().stream().map(ICell::getPos).toList();
                    List<Pos> endPositions = l.end.cells().stream().map(ICell::getPos).toList();

                    return startPositions.stream()
                            .flatMap(startPos -> endPositions.stream()
                                    .map(endPos -> Pair.create(startPos, endPos)))
                            // switched from pythagorean distance to chebyshev distance so diagonal and orthogonal links are treated equally
                            .min(Comparator.comparingDouble(pair -> Math.max((pair.getFirst().x() - pair.getSecond().x()), (pair.getFirst().y() - pair.getSecond().y()))))
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .toList();
        GroupLink startLink = cycle.getFirst();
        GroupLink endLink = cycle.getLast();
        if(startLink.type == endLink.type){
            // Disconnect cycle
            if(startLink.type == LinkType.STRONG) {
                List<ICell> peers = getCommonPeers(sudoku, startLink.start.cells()).stream().filter(c -> c.getCandidates().contains(num)).toList();
                return Optional.of(TechniqueAction.builder()
                        .name("Grouped X-Cycle")
                        .description("One of the cells " + startLink.start.cells().getFirst().getPos() + " has to be " + num)
                        .removeCandidatesMap(peers.stream().collect(Collectors.toMap(ICell::getPos, _ -> Set.of(num))))
                        .cellColorings(List.of(
                                new TechniqueAction.CandidatesColoring(col1, Color.YELLOW, Set.of(num)),
                                new TechniqueAction.CandidatesColoring(col2, Color.BLUE, Set.of(num)),
                                new TechniqueAction.LineColoring(weakLinks, Color.BLUE, num, true),
                                new TechniqueAction.LineColoring(strongLinks, Color.BLUE, num, false),
                                new TechniqueAction.CandidatesColoring(peers.stream().map(ICell::getPos).toList(), Color.RED, Set.of(num))
                        )).build());
            }
            else {
                return Optional.of(TechniqueAction.builder()
                        .name("Grouped X-Cycle")
                        .description("Eliminate " + num + " from " + startLink.start.cells().stream().map(ICell::getPos))
                        .removeCandidatesMap(startLink.start.cells().stream().collect(Collectors.toMap(ICell::getPos, _ -> Set.of(num))))
                        .cellColorings(List.of(
                                new TechniqueAction.CandidatesColoring(col1, Color.YELLOW, Set.of(num)),
                                new TechniqueAction.CandidatesColoring(col2, Color.GREEN, Set.of(num)),
                                new TechniqueAction.LineColoring(weakLinks, Color.BLUE, num, true),
                                new TechniqueAction.LineColoring(strongLinks, Color.BLUE, num, false),
                                new TechniqueAction.CandidatesColoring(startLink.start.cells().stream().map(ICell::getPos).toList(), Color.RED, Set.of(num))
                        )).build());
            }
        }
        else {
            // Continues cycle
            Set<ICell> affectedCells = new HashSet<>();
            List<Pair<Pos, Pos>> groupColoring = new ArrayList<>();
            for (GroupLink link : cycle.stream().filter(l -> l.type == LinkType.WEAK).toList()) {
                Pair<List<ICell>, ISudoku.GroupType> result = findCommon(sudoku, link.start, link.end);
                List<ICell> common = result.getFirst().stream().filter(c -> c.getCandidates().contains(num)).toList();
                if(!common.isEmpty()){
                    affectedCells.addAll(common);
                    groupColoring.add(switch (result.getSecond()) {
                        case ROW -> Pair.create(new Pos(0, link.start.cells().getFirst().getY()), new Pos(8, link.start.cells().getFirst().getY()));
                        case COLUMN -> Pair.create(new Pos(link.start.cells().getFirst().getX(), 0), new Pos(link.start.cells().getFirst().getX(), 8));
                        case SQUARE -> Pair.create(new Pos((link.start.cells().getFirst().getX() / 3) * 3, (link.start.cells().getFirst().getY() / 3) * 3), new Pos((link.start.cells().getFirst().getX() / 3) * 3 + 2, (link.start.cells().getFirst().getY() / 3) * 3 + 2));
                    });
                }
            }
            if(!affectedCells.isEmpty())
                return Optional.of(TechniqueAction.builder()
                        .name("Grouped X-Cycle")
                        .description("Eliminate " + num + " from common peers")
                        .removeCandidatesMap(affectedCells.stream().collect(Collectors.toMap(ICell::getPos, _ -> Set.of(num))))
                        .cellColorings(List.of(
                                new TechniqueAction.CandidatesColoring(col1, Color.YELLOW, Set.of(num)),
                                new TechniqueAction.CandidatesColoring(col2, Color.GREEN, Set.of(num)),
                                new TechniqueAction.LineColoring(weakLinks, Color.BLUE, num, true),
                                new TechniqueAction.LineColoring(strongLinks, Color.BLUE, num, false),
                                new TechniqueAction.GroupColoring(groupColoring, Color.YELLOW),
                                new TechniqueAction.CandidatesColoring(affectedCells.stream().map(ICell::getPos).collect(Collectors.toSet()), Color.RED, Set.of(num))
                        )).build());
        }
        return Optional.empty();
    }

    private static boolean findGroupCycle(ISudoku sudoku, GroupCell current, List<GroupLink> cycle, int num, Set<Pair<GroupCell, GroupCell>> strongLinks) {
        if (cycle.getFirst().start == current)
            return true;
        if(current.allowedConnections().stream().anyMatch(c -> cycle.getFirst().start.allowedConnections.contains(c)) && cycle.size() > 2){
            // Cycle found
            // Check for contradictions and make deductions
            cycle.add(new GroupLink(current, cycle.getFirst().start, LinkType.WEAK));
            return true;
        }

        // Find links from current cell
        List<GroupLink> possibleLinks = new ArrayList<>();
        List<ICell> visited = cycle.stream().flatMap(l -> Stream.of(l.start.cells(), l.end.cells())).flatMap(Collection::stream).toList();
        for(Pair<GroupCell, GroupCell> strongLink : strongLinks){
            GroupCell first = strongLink.getFirst();
            GroupCell second = strongLink.getSecond();
            if(first.cells().stream().anyMatch(visited::contains) || second.cells().stream().anyMatch(visited::contains))
                continue;
            if (first.allowedConnections.stream()
                    .anyMatch(pair1 -> current.allowedConnections.stream()
                            .anyMatch(pair2 -> pair1.getFirst() == pair2.getFirst() && pair1.getSecond().equals(pair2.getSecond())))) {
                possibleLinks.add(new GroupLink(first, second, LinkType.STRONG));
            }
            if (second.allowedConnections.stream()
                    .anyMatch(pair1 -> current.allowedConnections.stream()
                            .anyMatch(pair2 -> pair1.getFirst() == pair2.getFirst() && pair1.getSecond().equals(pair2.getSecond())))) {
                possibleLinks.add(new GroupLink(second, first, LinkType.STRONG));
            }
        }

        for(GroupLink possibleLink: possibleLinks){
            if(strongLinks.stream().map(l -> List.of(l.getFirst().cells(), l.getSecond().cells())).anyMatch(l -> l.contains(current.cells()) && l.contains(possibleLink.start.cells())))
//            if(strongLinks.stream().anyMatch(l ->
//                    (l.getFirst().cells().stream().anyMatch(current.cells()::contains) && l.getSecond().cells.stream().anyMatch(possibleLink.start.cells::contains))
//                    || (l.getFirst().cells().stream().anyMatch(possibleLink.start.cells::contains) && l.getSecond().cells.stream().anyMatch(current.cells()::contains))
//            ))
                continue;
            if(current.cells().stream().anyMatch(possibleLink.start.cells()::contains))
                continue;
            cycle.add(new GroupLink(current, possibleLink.start, LinkType.WEAK));
            cycle.add(possibleLink);
            if(findGroupCycle(sudoku, possibleLink.end, cycle, num, strongLinks)){
                return true;
            }
            cycle.removeLast();
            cycle.removeLast();
        }
        Set<ICell> commonPeers = getCommonPeers(sudoku, cycle.getFirst().start, current);
        commonPeers = commonPeers.stream().filter(c -> c.getCandidates().contains(num)).collect(Collectors.toSet());
        if(!commonPeers.isEmpty()){
            ICell commonPeer = commonPeers.iterator().next();
            if(strongLinks.stream().map(l-> List.of(l.getFirst().cells(), l.getSecond().cells())).anyMatch(l ->
                    (l.contains(current.cells()) && l.contains(List.of(commonPeer)))
                            || (l.contains(cycle.getFirst().start.cells()) && l.contains(List.of(commonPeer)))
            ))
//            if(strongLinks.stream().flatMap(l-> Stream.of(l.getFirst(), l.getSecond())).map(GroupCell::cells)
//                    .anyMatch(l ->
//                            (l.stream().anyMatch(current.cells()::contains) && l.contains(commonPeer))
//                                    || (l.stream().anyMatch(cycle.getFirst().start.cells()::contains) && l.contains(commonPeer))
//            ))
                return false;
            GroupCell commonGroupCell = new GroupCell(List.of(commonPeer), List.of());
            cycle.add(new GroupLink(current, commonGroupCell, LinkType.WEAK));
            cycle.addFirst(new GroupLink(commonGroupCell, cycle.getFirst().start, LinkType.WEAK));
            return true;
        }
        return false;
    }

    private static Set<Pair<GroupCell, GroupCell>> generateGroupedStrongLinks(ISudoku sudoku, int num) {
        Set<Pair<GroupCell, GroupCell>> strongLinks = new HashSet<>();
        for(int i=0; i<ISudoku.SUDOKU_SIZE;i++){
            List<ICell> row = Arrays.stream(sudoku.getRow(i)).filter(c -> c.getCandidates().contains(num)).toList();
            if(row.size() == 2){
                strongLinks.add(Pair.create(
                        new GroupCell(
                                List.of(row.getFirst()),
                                List.of(Pair.create(ISudoku.GroupType.COLUMN, row.getFirst().getX()), Pair.create(ISudoku.GroupType.SQUARE, row.getFirst().getSquare()))),
                        new GroupCell(
                                List.of(row.getLast()),
                                List.of(Pair.create(ISudoku.GroupType.COLUMN, row.getLast().getX()), Pair.create(ISudoku.GroupType.SQUARE, row.getLast().getSquare())
                        ))
                ));
            }
            else if(row.size() > 2) {
                List<List<ICell>> rowGroupedBySquare = row.stream().collect(Collectors.groupingBy(ICell::getSquare)).values().stream().toList();
                if(rowGroupedBySquare.size() == 2){
                    strongLinks.add(Pair.create(
                            new GroupCell(
                                    rowGroupedBySquare.getFirst(),
                                    rowGroupedBySquare.getFirst().size() == 1?
                                            List.of(
                                                    Pair.create(ISudoku.GroupType.COLUMN, rowGroupedBySquare.getFirst().getFirst().getX()),
                                                    Pair.create(ISudoku.GroupType.SQUARE, rowGroupedBySquare.getFirst().getFirst().getSquare())
                                            ):
                                            List.of(Pair.create(ISudoku.GroupType.SQUARE, rowGroupedBySquare.getFirst().getFirst().getSquare()))
                            ),
                            new GroupCell(
                                    rowGroupedBySquare.getLast(),
                                    rowGroupedBySquare.getLast().size() == 1?
                                            List.of(
                                                    Pair.create(ISudoku.GroupType.COLUMN, rowGroupedBySquare.getLast().getFirst().getX()),
                                                    Pair.create(ISudoku.GroupType.SQUARE, rowGroupedBySquare.getLast().getFirst().getSquare())
                                            ):
                                            List.of(Pair.create(ISudoku.GroupType.SQUARE, rowGroupedBySquare.getLast().getFirst().getSquare()))
                            )
                    ));
                }
            }
            List<ICell> col = Arrays.stream(sudoku.getColumn(i)).filter(c -> c.getCandidates().contains(num)).toList();
            if(col.size() == 2){
                strongLinks.add(Pair.create(
                        new GroupCell(
                                List.of(col.getFirst()),
                                List.of(Pair.create(ISudoku.GroupType.ROW, col.getFirst().getY()), Pair.create(ISudoku.GroupType.SQUARE, col.getFirst().getSquare()))),
                        new GroupCell(
                                List.of(col.getLast()),
                                List.of(Pair.create(ISudoku.GroupType.ROW, col.getLast().getY()), Pair.create(ISudoku.GroupType.SQUARE, col.getLast().getSquare())
                        ))
                ));
            }
            else if(col.size() > 2){
                List<List<ICell>> colGroupedBySquare = col.stream().collect(Collectors.groupingBy(ICell::getSquare)).values().stream().toList();
                if(colGroupedBySquare.size() == 2){
                    strongLinks.add(Pair.create(
                            new GroupCell(
                                    colGroupedBySquare.getFirst(),
                                    colGroupedBySquare.getFirst().size() == 1?
                                            List.of(
                                                    Pair.create(ISudoku.GroupType.ROW, colGroupedBySquare.getFirst().getFirst().getY()),
                                                    Pair.create(ISudoku.GroupType.SQUARE, colGroupedBySquare.getFirst().getFirst().getSquare())
                                            ):
                                            List.of(Pair.create(ISudoku.GroupType.SQUARE, colGroupedBySquare.getFirst().getFirst().getSquare()))
                            ),
                            new GroupCell(
                                    colGroupedBySquare.getLast(),
                                    colGroupedBySquare.getLast().size() == 1?
                                            List.of(
                                                    Pair.create(ISudoku.GroupType.ROW, colGroupedBySquare.getLast().getFirst().getY()),
                                                    Pair.create(ISudoku.GroupType.SQUARE, colGroupedBySquare.getLast().getFirst().getSquare())
                                            ):
                                            List.of(Pair.create(ISudoku.GroupType.SQUARE, colGroupedBySquare.getLast().getFirst().getSquare()))
                            )
                    ));
                }
            }
            List<ICell> square = Arrays.stream(sudoku.getSquare(i)).filter(c -> c.getCandidates().contains(num)).toList();
            if(square.size() == 2){
                strongLinks.add(Pair.create(
                        new GroupCell(
                                List.of(square.getFirst()),
                                List.of(Pair.create(ISudoku.GroupType.ROW, square.getFirst().getY()), Pair.create(ISudoku.GroupType.COLUMN, square.getFirst().getX()))),
                        new GroupCell(
                                List.of(square.getLast()),
                                List.of(Pair.create(ISudoku.GroupType.ROW, square.getLast().getY()), Pair.create(ISudoku.GroupType.COLUMN, square.getLast().getX())
                        ))
                ));
            } else if (square.size() > 2) {
                List<List<ICell>> squareGroupedByRow = square.stream().collect(Collectors.groupingBy(ICell::getY)).values().stream().toList();
                if(squareGroupedByRow.size() == 2){
                    strongLinks.add(Pair.create(
                            new GroupCell(
                                    squareGroupedByRow.getFirst(),
                                    squareGroupedByRow.getFirst().size() == 1?
                                            List.of(
                                                    Pair.create(ISudoku.GroupType.ROW, squareGroupedByRow.getFirst().getFirst().getY()),
                                                    Pair.create(ISudoku.GroupType.COLUMN, squareGroupedByRow.getFirst().getFirst().getX())
                                            ):
                                            List.of(Pair.create(ISudoku.GroupType.ROW, squareGroupedByRow.getFirst().getFirst().getY()))
                            ),
                            new GroupCell(
                                    squareGroupedByRow.getLast(),
                                    squareGroupedByRow.getLast().size() == 1?
                                            List.of(
                                                    Pair.create(ISudoku.GroupType.ROW, squareGroupedByRow.getLast().getFirst().getY()),
                                                    Pair.create(ISudoku.GroupType.COLUMN, squareGroupedByRow.getLast().getFirst().getX())
                                            ):
                                            List.of(Pair.create(ISudoku.GroupType.ROW, squareGroupedByRow.getLast().getFirst().getY()))
                            )
                    ));
                } else if (squareGroupedByRow.size() == 3) {
                    List<List<ICell>> squareGroupedByRowPairList = squareGroupedByRow.stream().filter(l -> l.size() == 2).toList();
                    for (List<ICell> squareGroupedByRowPair: squareGroupedByRowPairList){
                        List<ICell> otherCells = squareGroupedByRow.stream().flatMap(List::stream).filter(c -> !squareGroupedByRowPair.contains(c)).toList();
                        if(otherCells.stream().map(ICell::getX).distinct().count() == 1){
                            strongLinks.add(Pair.create(
                                    new GroupCell(
                                            squareGroupedByRowPair,
                                            List.of(Pair.create(ISudoku.GroupType.ROW, squareGroupedByRowPair.getFirst().getY()))
                                    ),
                                    new GroupCell(
                                            otherCells,
                                            List.of(Pair.create(ISudoku.GroupType.COLUMN, otherCells.getFirst().getX()))
                                    )
                            ));
                        }
                    }
                }
                List<List<ICell>> squareGroupedByCol = square.stream().collect(Collectors.groupingBy(ICell::getX)).values().stream().toList();
                if(squareGroupedByCol.size() == 2){
                    strongLinks.add(Pair.create(
                            new GroupCell(
                                    squareGroupedByCol.getFirst(),
                                    squareGroupedByCol.getFirst().size() == 1?
                                            List.of(
                                                    Pair.create(ISudoku.GroupType.ROW, squareGroupedByCol.getFirst().getFirst().getY()),
                                                    Pair.create(ISudoku.GroupType.COLUMN, squareGroupedByCol.getFirst().getFirst().getX())
                                            ):
                                            List.of(Pair.create(ISudoku.GroupType.COLUMN, squareGroupedByCol.getFirst().getFirst().getX()))
                            ),
                            new GroupCell(
                                    squareGroupedByCol.getLast(),
                                    squareGroupedByCol.getLast().size() == 1?
                                            List.of(
                                                    Pair.create(ISudoku.GroupType.ROW, squareGroupedByCol.getLast().getFirst().getY()),
                                                    Pair.create(ISudoku.GroupType.COLUMN, squareGroupedByCol.getLast().getFirst().getX())
                                            ):
                                            List.of(Pair.create(ISudoku.GroupType.COLUMN, squareGroupedByCol.getLast().getFirst().getX()))
                            )
                    ));
                } else if (squareGroupedByCol.size() == 3) {
                    List<List<ICell>> squareGroupedByColPairList = squareGroupedByCol.stream().filter(l -> l.size() == 2).toList();
                    for (List<ICell> squareGroupedByColPair: squareGroupedByColPairList){
                        List<ICell> otherCells = squareGroupedByCol.stream().flatMap(List::stream).filter(c -> !squareGroupedByColPair.contains(c)).toList();
                        if(otherCells.stream().map(ICell::getY).distinct().count() == 1){
                            strongLinks.add(Pair.create(
                                    new GroupCell(
                                            otherCells,
                                            List.of(Pair.create(ISudoku.GroupType.ROW, otherCells.getFirst().getY()))
                                    ),
                                    new GroupCell(
                                            squareGroupedByColPair,
                                            List.of(Pair.create(ISudoku.GroupType.COLUMN, squareGroupedByColPair.getFirst().getX()))
                                    )
                            ));
                        }
                    }
                }
            }
        }
        return strongLinks;
    }

    // Helper function to determine if a cell is connected to any cell in a chain

    private static Pair<List<ICell>, ISudoku.GroupType> findCommon(ISudoku sudoku, ICell start, ICell end) {
        List<ICell> common = new ArrayList<>();
        ISudoku.GroupType groupType;
        if(start.getY() == end.getY()) {
            common.addAll(List.of(sudoku.getRow(start.getY())));
            groupType = ISudoku.GroupType.ROW;
        }
        else if(start.getX() == end.getX()) {
            common.addAll(List.of(sudoku.getColumn(start.getX())));
            groupType = ISudoku.GroupType.COLUMN;
        }
        else if (start.getSquare() == end.getSquare()) {
            common.addAll(List.of(sudoku.getSquare(start.getSquare())));
            groupType = ISudoku.GroupType.SQUARE;
        }
        else
            throw new IllegalArgumentException("Cells are not connected");
        common.remove(start);
        common.remove(end);
        return Pair.create(common, groupType);
    }


    private static Pair<List<ICell>, ISudoku.GroupType> findCommon(ISudoku sudoku, GroupCell start, GroupCell end) {
        List<ICell> common = new ArrayList<>();
        ISudoku.GroupType groupType = null;
        for(ISudoku.GroupType type: ISudoku.GroupType.values()) {
            List<Integer> distinct = getDistinct(Stream.of(start.cells(), end.cells()).flatMap(List::stream).toList(), type);
            if (distinct.size() == 1) {
                common.addAll(Arrays.stream(sudoku.getCells(type, distinct.getFirst())).toList());
                groupType = type;
                break;
            }
        }
        if (groupType == null)
            throw new IllegalArgumentException("Cells are not connected");

        common.removeAll(start.cells());
        common.removeAll(end.cells());
        return Pair.create(common, groupType);
    }

    private static List<Integer> getDistinct(List<ICell> list, ISudoku.GroupType type) {
        return switch (type) {
            case ROW -> list.stream().map(ICell::getY).distinct().toList();
            case COLUMN -> list.stream().map(ICell::getX).distinct().toList();
            case SQUARE -> list.stream().map(ICell::getSquare).distinct().toList();
        };
    }

    private static Set<ICell> findLinks(ISudoku sudoku, ICell cell, int num) {
        Set<ICell> links = new HashSet<>();
        Set<ICell> column = (Arrays.stream(sudoku.getColumn(cell.getX())).filter(c -> c.getCandidates().contains(num) && !cell.equals(c)).collect(Collectors.toSet()));
        if(column.size() == 1){
            links.addAll(column);
        }
        Set<ICell> row = Arrays.stream(sudoku.getRow(cell.getY())).filter(c -> c.getCandidates().contains(num) && !cell.equals(c)).collect(Collectors.toSet());
        if(row.size() == 1){
            links.addAll(row);
        }
        Set<ICell> square = Arrays.stream(sudoku.getSquare(cell.getX() / 3 + cell.getY() / 3 * 3)).filter(c -> c.getCandidates().contains(num) && !cell.equals(c)).collect(Collectors.toSet());
        if(square.size() == 1){
            links.addAll(square);
        }
        return links;
    }

    private static boolean hasConnection(ICell cell, Collection<ICell> chain) {
        return chain.stream().anyMatch(c -> CellUtils.isPeer(cell, c));
    }

    private static Set<ICell> getCommonPeers(ISudoku sudoku, List<ICell> cells) {
        return cells.stream()
                .flatMap(c -> CellUtils.getPeers(sudoku, c).stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() == cells.size())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }


    private static Set<ICell> getCommonPeers(ISudoku sudoku, GroupCell group1, GroupCell group2) {
        Set<ICell> commonPeers = new HashSet<>();
        for(Pair<ISudoku.GroupType, Integer> connection1: group1.allowedConnections()){
            for(Pair<ISudoku.GroupType, Integer> connection2: group2.allowedConnections()){
                Set<ICell> connectionIntersection = Arrays.stream(sudoku.getCells(connection1.getFirst(), connection1.getSecond())).collect(Collectors.toSet());
                connectionIntersection.retainAll(Arrays.stream(sudoku.getCells(connection2.getFirst(), connection2.getSecond())).collect(Collectors.toSet()));
                if(!connectionIntersection.isEmpty()){
                    commonPeers.addAll(connectionIntersection);
                }
            }
        }
        group1.cells().forEach(commonPeers::remove);
        group2.cells().forEach(commonPeers::remove);
        return commonPeers;
    }
}
