package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import com.chgr.sudoku.models.Pos;
import com.chgr.sudoku.models.TechniqueAction;
import javafx.scene.paint.Color;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.chgr.sudoku.utils.CellUtils.getPeers;

public class ChainTechnique {

    private enum LinkType {
        STRONG,
        WEAK
    }

    @AllArgsConstructor
    private static class Link {
        ICell start;
        ICell end;
        LinkType type;
    }

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
                List<TechniqueAction.CellColoring> techniqueColoring = new ArrayList<>();
                techniqueColoring.add(
                        TechniqueAction.CellColoring.candidatesColoring(
                                colorGroup0.stream().map(ICell::getPos).collect(Collectors.toSet()),
                                Color.YELLOW, Set.of(num))
                );
                techniqueColoring.add(
                        TechniqueAction.CellColoring.candidatesColoring(
                                colorGroup1.stream().map(ICell::getPos).collect(Collectors.toSet()),
                                Color.GREEN, Set.of(num))
                );
                techniqueColoring.add(
                        TechniqueAction.CellColoring.lineColoring(linkList, Color.BLUE, num)
                );
                Optional<TechniqueAction> techniqueAction = hasDuplicateInUnit(colorGroup0, colorGroup1, num, techniqueColoring, "Simple Coloring");
                if (techniqueAction.isPresent())
                    return techniqueAction;
                techniqueAction = eliminateFromOutsideNeighboringCells(emptyCells, num, colorGroup0, colorGroup1, coloring.keySet(), techniqueColoring, "Simple Coloring");
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
            List<TechniqueAction.CellColoring> techniqueColoring = new ArrayList<>();
            techniqueColoring.addAll(
                    coloring.entrySet().stream()
                            .filter(e -> e.getValue() == 0)
                            .map(entry -> TechniqueAction.CellColoring.candidatesColoring(Set.of(entry.getKey().getCell().getPos()), Color.YELLOW, Set.of(entry.getKey().getNum()))
                            ).toList());
            techniqueColoring.addAll(
                    coloring.entrySet().stream()
                            .filter(e -> e.getValue() == 1)
                            .map(entry -> TechniqueAction.CellColoring.candidatesColoring(Set.of(entry.getKey().getCell().getPos()), Color.GREEN, Set.of(entry.getKey().getNum()))
                            ).toList());
            techniqueColoring.addAll(
                    linkList.stream()
                            .filter(pair -> coloring.keySet().stream().anyMatch(cellNumPair -> cellNumPair.getCell().getPos().equals(pair.getFirst().getFirst()) && cellNumPair.getNum() == pair.getSecond()))
                            .map(pair -> TechniqueAction.CellColoring.lineColoring(List.of(pair.getFirst()), Color.BLUE, pair.getSecond())
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
                Optional<TechniqueAction> techniqueAction = hasDuplicateInUnit(colorGroup0, colorGroup1, num, techniqueColoring, "3D Medusa");
                if(techniqueAction.isPresent())
                    return techniqueAction;
                techniqueAction = eliminateFromOutsideNeighboringCells(emptyCells, num, colorGroup0, colorGroup1, group, techniqueColoring, "3D Medusa");
                if(techniqueAction.isPresent())
                    return techniqueAction;
            }
            Set<ICell> cells = coloring.keySet().stream().map(CellNumPair::getCell).collect(Collectors.toSet());
            Optional<TechniqueAction> techniqueAction = hasColorConflictInCell(coloring, cells, techniqueColoring);
            if (techniqueAction.isPresent()) return techniqueAction;
            techniqueAction = eliminateExtraCandidatesFromBicolorCells(coloring, cells, techniqueColoring);
            if (techniqueAction.isPresent()) return techniqueAction;
            techniqueAction = removeUncoloredDueToUnitAndCellConflict(coloring, cells, techniqueColoring);
            if (techniqueAction.isPresent()) return techniqueAction;
            techniqueAction = removeUncoloredDueToAllCandidatesSeeSameColor(sudoku, coloring, cells, techniqueColoring);
            if (techniqueAction.isPresent()) return techniqueAction;
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> hasColorConflictInCell(Map<CellNumPair, Integer> coloring, Set<ICell> cells, List<TechniqueAction.CellColoring> techniqueColoring) {
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
                    techniqueColoring.addAll(
                            colorPerCell.stream()
                                    .collect(Collectors.groupingBy(CellNumPair::getNum))
                                    .entrySet().stream()
                                    .map(keyValue -> TechniqueAction.CellColoring.candidatesColoring(keyValue.getValue().stream().map(entry -> entry.cell.getPos()).collect(Collectors.toSet()), Color.RED, List.of(keyValue.getKey()))
                                    ).toList());
                    return Optional.of(TechniqueAction.builder()
                            .name("3D Medusa")
                            .description("In cell " + cell.getPos() + " both " + colorPerCell.get(0).getNum() + " and " + colorPerCell.get(1).getNum() + " get the same color")
                            .removeCandidatesMap(
                                    colorPerCell.stream()
                                            .collect(Collectors.groupingBy(CellNumPair::getCell))
                                            .entrySet().stream()
                                            .collect(Collectors.toMap(entry -> entry.getKey().getPos(), entry -> entry.getValue().stream().map(CellNumPair::getNum).collect(Collectors.toSet())))
                            ).colorings(techniqueColoring).build());
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> hasDuplicateInUnit(Collection<ICell> colorGroup0, Collection<ICell> colorGroup1, int num, List<TechniqueAction.CellColoring> techniqueColoring, String name) {
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
                    techniqueColoring.add(
                            TechniqueAction.CellColoring.candidatesColoring(Set.of(rowConflict.getPos(), cell.getPos()), Color.RED, Set.of(num))
                    );
                    return Optional.of(TechniqueAction.builder()
                            .name(name)
                            .description("In row " + cell.getY() + " both " + rowConflict.getPos() + " and " + cell.getPos() + " get the same color")
                            .removeCandidatesMap(Map.of(rowConflict.getPos(), Set.of(num), cell.getPos(), Set.of(num)))
                            .colorings(techniqueColoring).build());
                }
                else if (colConflict != null){
                    techniqueColoring.add(
                            TechniqueAction.CellColoring.candidatesColoring(Set.of(colConflict.getPos(), cell.getPos()), Color.RED, Set.of(num))
                    );
                    return Optional.of(TechniqueAction.builder()
                            .name(name)
                            .description("In column " + cell.getX() + " both " + colConflict.getPos() + " and " + cell.getPos() + " get the same color")
                            .removeCandidatesMap(Map.of(colConflict.getPos(), Set.of(num), cell.getPos(), Set.of(num)))
                            .colorings(techniqueColoring).build());
                }
                else if (squareConflict != null) {
                    techniqueColoring.add(
                            TechniqueAction.CellColoring.candidatesColoring(Set.of(squareConflict.getPos(), cell.getPos()), Color.RED, Set.of(num))
                    );
                    return Optional.of(TechniqueAction.builder()
                            .name(name)
                            .description("In square " + pair + " both " + squareConflict.getPos() + " and " + cell.getPos() + " get the same color")
                            .removeCandidatesMap(Map.of(squareConflict.getPos(), Set.of(num), cell.getPos(), Set.of(num)))
                            .colorings(techniqueColoring).build());
                }
                rowsSeen.put(cell.getY(), cell);
                colsSeen.put(cell.getX(), cell);
                squaresSeen.put(pair, cell);
            }
        }
        return Optional.empty();
    }


    private static Optional<TechniqueAction> eliminateExtraCandidatesFromBicolorCells(Map<CellNumPair, Integer> coloring, Set<ICell> cells, List<TechniqueAction.CellColoring> techniqueColoring) {
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
                    techniqueColoring.add(
                            TechniqueAction.CellColoring.candidatesColoring(Set.of(cell.getPos()), Color.RED, otherCandidates)
                    );
                    return Optional.of(TechniqueAction.builder()
                            .name("3D Medusa")
                            .description("Eliminate " + otherCandidates + " from " + cell.getPos() + " due to two colors in a cell with two candidates")
                            .removeCandidatesMap(Map.of(cell.getPos(), otherCandidates))
                            .colorings(techniqueColoring).build());
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<TechniqueAction> eliminateFromOutsideNeighboringCells(Set<ICell> emptyCells, int num, Collection<ICell> colorGroup0, Collection<ICell> colorGroup1, Set<ICell> group, List<TechniqueAction.CellColoring> techniqueColoring, String name) {
        // Rule 4
        // Check for two colors neighbouring a non-chain cells
        for (ICell cell : emptyCells) {
            if (!group.contains(cell) && cell.getCandidates().contains(num) &&
                    hasConnection(cell, colorGroup0) && hasConnection(cell, colorGroup1)) {
                techniqueColoring.add(
                        TechniqueAction.CellColoring.candidatesColoring(Set.of(cell.getPos()), Color.RED, Set.of(num))
                );
                return Optional.of(TechniqueAction.builder()
                        .name(name)
                        .description("Eliminate " + num + " from " + cell.getPos() + " due to two colors neighboring a non-chain cell")
                        .removeCandidatesMap(Map.of(cell.getPos(), Set.of(num)))
                        .colorings(techniqueColoring).build());
            }
        }
        return Optional.empty();
    }

    // Rule 5 is not tested cause of complexity of test propagation
    // Rule 5 is causing several cells to be to the 3d chain
    // and causes rule 4 to be triggered almost after every rule 5
    private static Optional<TechniqueAction> removeUncoloredDueToUnitAndCellConflict(Map<CellNumPair, Integer> coloring, Set<ICell> cells, List<TechniqueAction.CellColoring> techniqueColoring) {
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
                    techniqueColoring.add(
                            TechniqueAction.CellColoring.candidatesColoring(Set.of(cell.getPos()), Color.RED, Set.of(otherCandidate))
                    );
                    return Optional.of(TechniqueAction.builder()
                            .name("3D Medusa")
                            .description("Eliminate " + otherCandidate + " from " + cell.getPos() + " due to uncolored candidate in a colored cell with a peer with the same candidate colored")
                            .removeCandidatesMap(Map.of(cell.getPos(), Set.of(otherCandidate)))
                            .colorings(techniqueColoring).build());
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
                        .filter(entry -> entry.getValue() == finalColor && areConnected(uncoloredCell, entry.getKey().getCell()))
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
                                    .map(entry -> TechniqueAction.CellColoring.candidatesColoring(Set.of(entry.getKey()), Color.RED, entry.getValue())
                                    ).toList());
                    return Optional.of(TechniqueAction.builder()
                            .name("3D Medusa")
                            .description("Eliminate " + candidatesToRemove.keySet() + " from " + uncoloredCell.getPos() + " due to all candidates seeing the same color")
                            .removeCandidatesMap(candidatesToRemove)
                            .colorings(techniqueColoring).build());
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
            Set<List<ICell>> strongLinks = generateStrongLinks(sudoku, num);
            for(List<ICell> strongLink: strongLinks){
                List<Link> cycle = new ArrayList<>();
                Set<ICell> visited = new HashSet<>();
                cycle.add(new Link(strongLink.get(0), strongLink.get(1), LinkType.STRONG));
                visited.add(strongLink.get(1));
                if(findCycle(sudoku, strongLink.get(1), cycle, num, visited, strongLinks)){
                    Optional<TechniqueAction> techniqueAction = handleCycle(sudoku, cycle, num);
                    if(techniqueAction.isPresent())
                        return techniqueAction;
                }
                cycle = new ArrayList<>();
                visited = new HashSet<>();
                cycle.add(new Link(strongLink.get(1), strongLink.get(0), LinkType.STRONG));
                visited.add(strongLink.get(0));
                if(findCycle(sudoku, strongLink.get(0), cycle, num, visited, strongLinks)){
                    Optional<TechniqueAction> techniqueAction = handleCycle(sudoku, cycle, num);
                    if(techniqueAction.isPresent())
                        return techniqueAction;
                }
            }
        }
        return Optional.empty();
    }

    private static boolean findCycle(ISudoku sudoku, ICell current, List<Link> cycle, int num, Set<ICell> visited, Set<List<ICell>> strongLinks) {
        if(cycle.getFirst().start == current)
            return true;
        if(areConnected(current, cycle.getFirst().start) && cycle.size() > 2){
            // Cycle found
            // Check for contradictions and make deductions
            cycle.add(new Link(current, cycle.getFirst().start, LinkType.WEAK));
            return true;
        }

        // Find links from current cell
        List<Link> possibleLinks = new ArrayList<>();
        for(List<ICell> strongLink : strongLinks){
            ICell first = strongLink.get(0);
            ICell second = strongLink.get(1);
            if(visited.contains(first) || visited.contains(second))
                continue;
            if(areConnected(current, first)){
                possibleLinks.add(new Link(first, second, LinkType.STRONG));
            }
            if(areConnected(current, second)){
                possibleLinks.add(new Link(second, first, LinkType.STRONG));
            }
        }

        for (Link possibleLink: possibleLinks){
            if(strongLinks.stream().anyMatch(l -> l.contains(current) && l.contains(possibleLink.start)))
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
        Set<ICell> commonPeers = getPeers(sudoku, cycle.getFirst().start);
        commonPeers.retainAll(getPeers(sudoku, current));
        commonPeers = commonPeers.stream().filter(c -> c.getCandidates().contains(num)).collect(Collectors.toSet());
        if(!commonPeers.isEmpty()){
            ICell commonPeer = commonPeers.iterator().next();
            if(strongLinks.stream().anyMatch(l ->
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

    private static Set<List<ICell>> generateStrongLinks(ISudoku sudoku, int num) {
        Set<List<ICell>> strongLinks = new HashSet<>();
        for(int i=0; i<ISudoku.SUDOKU_SIZE;i++){
            List<ICell> row = Arrays.stream(sudoku.getRow(i)).filter(c -> c.getCandidates().contains(num)).toList();
            if(row.size() == 2){
                strongLinks.add(row);
            }
            List<ICell> col = Arrays.stream(sudoku.getColumn(i)).filter(c -> c.getCandidates().contains(num)).toList();
            if(col.size() == 2){
                strongLinks.add(col);
            }
            List<ICell> square = Arrays.stream(sudoku.getSquare(i)).filter(c -> c.getCandidates().contains(num)).toList();
            if(square.size() == 2){
                strongLinks.add(square);
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
                        .colorings(List.of(
                                TechniqueAction.CellColoring.candidatesColoring(col1, Color.YELLOW, Set.of(num)),
                                TechniqueAction.CellColoring.candidatesColoring(col2, Color.GREEN, Set.of(num)),
                                TechniqueAction.CellColoring.doubleLineColoring(weakLinks, Color.BLUE, num),
                                TechniqueAction.CellColoring.lineColoring(strongLinks, Color.BLUE, num),
                                TechniqueAction.CellColoring.candidatesColoring(List.of(startLink.start.getPos()), Color.BLUE, Set.of(num))
                        )).build());
            }
            else{
                if(startLink.start.getCandidates().contains(num))
                    return Optional.of(TechniqueAction.builder()
                            .name("X-Cycle")
                            .description("Eliminate " + num + " from " + startLink.start.getPos())
                            .removeCandidatesMap(Map.of(startLink.start.getPos(), Set.of(num)))
                            .colorings(List.of(
                                    TechniqueAction.CellColoring.candidatesColoring(col1, Color.YELLOW, Set.of(num)),
                                    TechniqueAction.CellColoring.candidatesColoring(col2, Color.GREEN, Set.of(num)),
                                    TechniqueAction.CellColoring.doubleLineColoring(weakLinks, Color.BLUE, num),
                                    TechniqueAction.CellColoring.lineColoring(strongLinks, Color.BLUE, num),
                                    TechniqueAction.CellColoring.candidatesColoring(List.of(startLink.start.getPos()), Color.RED, Set.of(num))
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
                        .colorings(List.of(
                                TechniqueAction.CellColoring.candidatesColoring(col1, Color.YELLOW, Set.of(num)),
                                TechniqueAction.CellColoring.candidatesColoring(col2, Color.GREEN, Set.of(num)),
                                TechniqueAction.CellColoring.doubleLineColoring(weakLinks, Color.BLUE, num),
                                TechniqueAction.CellColoring.lineColoring(strongLinks, Color.BLUE, num),
                                TechniqueAction.CellColoring.groupColoring(groupColoring, Color.YELLOW),
                                TechniqueAction.CellColoring.candidatesColoring(affectedCells.stream().map(ICell::getPos).collect(Collectors.toSet()), Color.RED, Set.of(num))
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
        Set<ICell> commonPeers = getPeers(sudoku, start);
        commonPeers.retainAll(getPeers(sudoku, end));
        commonPeers = commonPeers.stream().filter(c -> c.getCandidates().contains(otherCandidate)).collect(Collectors.toSet());
        if(!commonPeers.isEmpty()){
            List<TechniqueAction.CellColoring> coloringList = weakLinks.stream().map(link -> TechniqueAction.CellColoring.lineColoring(List.of(link.getSecond()), Color.BLUE, link.getFirst())).collect(Collectors.toList());
            coloringList.addAll(weakLinks.stream().map(link -> TechniqueAction.CellColoring.candidatesColoring(Set.of(link.getSecond().getFirst()), Color.BLUE, Set.of(link.getFirst()))).toList());
            coloringList.addAll(weakLinks.stream().map(link -> TechniqueAction.CellColoring.candidatesColoring(Set.of(link.getSecond().getSecond()), Color.YELLOW, Set.of(link.getFirst()))).toList());
            coloringList.addAll(List.of(
                    TechniqueAction.CellColoring.candidatesColoring(Set.of(start.getPos()), Color.YELLOW, Set.of(otherCandidate)),
                    TechniqueAction.CellColoring.candidatesColoring(Set.of(end.getPos()), Color.BLUE, Set.of(otherCandidate)),
                    TechniqueAction.CellColoring.candidatesColoring(commonPeers.stream().map(ICell::getPos).collect(Collectors.toSet()), Color.RED, Set.of(otherCandidate))));
            return Optional.of(TechniqueAction.builder()
                    .name("XY-Chain")
                    .description("Eliminate " + otherCandidate + " from common peers of " + start.getPos() + " and " + end.getPos())
                    .removeCandidatesMap(commonPeers.stream().collect(Collectors.toMap(ICell::getPos, _ -> Set.of(otherCandidate))))
                    .colorings(coloringList).build());
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

        List<ICell> peers = getPeers(sudoku, cell).stream()
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

    public static Optional<TechniqueAction> SKLoop(ISudoku sudoku) {
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
        coloring1.forEach((pos, candidates) -> colorings.add(TechniqueAction.CellColoring.candidatesColoring(Set.of(pos), Color.YELLOW, candidates)));
        coloring2.forEach((pos, candidates) -> colorings.add(TechniqueAction.CellColoring.candidatesColoring(Set.of(pos), Color.BLUE, candidates)));
        candidatesToRemove.forEach((pos, candidates) -> colorings.add(TechniqueAction.CellColoring.candidatesColoring(Set.of(pos), Color.RED, candidates)));
        Stream.of(pos1, pos2, pos3, pos4).forEach(pos -> colorings.add(TechniqueAction.CellColoring.groupColoring(List.of(Pair.create(pos, pos)), Color.CYAN)));

        return Optional.of(TechniqueAction.builder()
                .name("SK-Loop")
                .description("Eliminate candidates from SK-Loop formed by " + pos1 + ", " + pos2 + ", " + pos3 + ", " + pos4)
                .removeCandidatesMap(candidatesToRemove)
                .colorings(colorings)
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
        else if (start.getX() / 3 == end.getX() / 3 && start.getY() / 3 == end.getY() / 3) {
            common.addAll(List.of(sudoku.getSquare(start.getX(), start.getY())));
            groupType = ISudoku.GroupType.SQUARE;
        }
        else
            throw new IllegalArgumentException("Cells are not connected");
        common.remove(start);
        common.remove(end);
        return Pair.create(common, groupType);
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
        return chain.stream().anyMatch(c -> areConnected(cell, c));
    }

    private static boolean areConnected(ICell cell1, ICell cell2) {
        return areInSameRow(cell1, cell2) || areInSameColumn(cell1, cell2) || areInSameSquare(cell1, cell2);
    }

    private static boolean areInSameRow(ICell cell1, ICell cell2) {
        return cell1.getY() == cell2.getY();
    }

    private static boolean areInSameColumn(ICell cell1, ICell cell2) {
        return cell1.getX() == cell2.getX();
    }

    private static boolean areInSameSquare(ICell cell1, ICell cell2) {
        return cell1.getX() / 3 == cell2.getX() / 3 && cell1.getY() / 3 == cell2.getY() / 3;
    }
}
