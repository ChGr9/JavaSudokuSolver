package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import lombok.AllArgsConstructor;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

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

    // https://www.sudokuwiki.org/Singles_Chains
    // Simple Coloring or Single Chains
    public static boolean simpleColoring(ISudoku sudoku) {
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

            // Color chains alternately
            List<ICell> keys = new ArrayList<>(linkMap.keySet());
            while(!keys.isEmpty()){
                ICell current = keys.get(0);
                keys.remove(current);
                List<ICell> openCells = new ArrayList<>();
                openCells.add(current);
                Map<ICell, Integer> coloring = new HashMap<>();
                coloring.put(current, 0);
                while (!openCells.isEmpty()) {
                    ICell cell = openCells.get(0);
                    openCells.remove(cell);
                    for (ICell link : linkMap.get(cell)) {
                        if (!coloring.containsKey(link)) {
                            coloring.put(link, 1 - coloring.get(cell));
                            if(!openCells.contains(link))
                                openCells.add(link);
                            keys.remove(link);
                        } else if (Objects.equals(coloring.get(link), coloring.get(cell))) {
                            // Contradiction found
                            // Remove candidates from cells in the chain
                            for (ICell chainCell : coloring.keySet()) {
                                if (Objects.equals(coloring.get(chainCell), coloring.get(cell))) {
                                    chainCell.removeCandidate(num);
                                }
                            }
                            return true;
                        }
                    }
                }

                // Check for contradictions and make deductions
                List<ICell> colorGroup0 = coloring.entrySet().stream().filter(e -> e.getValue() == 0).map(Map.Entry::getKey).toList();
                List<ICell> colorGroup1 = coloring.entrySet().stream().filter(e -> e.getValue() == 1).map(Map.Entry::getKey).toList();
                if (checkGroupContradiction(colorGroup0, num)) return true;
                if (checkGroupContradiction(colorGroup1, num)) return true;

                // Check for contradictions and make deductions outside the chain
                for(ICell cell: emptyCells){
                    if(!coloring.containsKey(cell) && cell.getCandidates().contains(num) &&
                            hasConnection(cell, colorGroup0) && hasConnection(cell, colorGroup1)){
                        cell.removeCandidate(num);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean checkGroupContradiction(List<ICell> colorGroup, int num) {
        Set<Integer> rowsSeen = new HashSet<>();
        Set<Integer> colsSeen = new HashSet<>();
        Set<Pair<Integer, Integer>> squaresSeen = new HashSet<>();

        for (ICell cell : colorGroup) {
            Pair<Integer, Integer> pair = new Pair<>(cell.getX() / 3, cell.getY() / 3);
            if (rowsSeen.contains(cell.getY()) || colsSeen.contains(cell.getX()) ||
                    squaresSeen.contains(pair)) {
                for (ICell cellToBeDeleted : colorGroup) {
                    cellToBeDeleted.removeCandidate(num);
                }
                return true;
            }
            rowsSeen.add(cell.getY());
            colsSeen.add(cell.getX());
            squaresSeen.add(pair);
        }
        return false;
    }

    // https://www.sudokuwiki.org/X_Cycles
    // https://www.sudokuwiki.org/X_Cycles_Part_2
    // X-Cycles
    public static boolean xCycle(ISudoku sudoku) {
        for(int num =1; num <= ISudoku.SUDOKU_SIZE; num ++){
            Set<List<ICell>> strongLinks = generateStrongLinks(sudoku, num);
            for(List<ICell> strongLink: strongLinks){
                List<Link> cycle = new ArrayList<>();
                Set<ICell> visited = new HashSet<>();
                cycle.add(new Link(strongLink.get(0), strongLink.get(1), LinkType.STRONG));
                visited.add(strongLink.get(1));
                if(findCycle(sudoku, strongLink.get(1), cycle, num, visited, strongLinks)){
                    if(handleCycle(sudoku, cycle, num))
                        return true;
                }
                cycle = new ArrayList<>();
                visited = new HashSet<>();
                cycle.add(new Link(strongLink.get(1), strongLink.get(0), LinkType.STRONG));
                visited.add(strongLink.get(0));
                if(findCycle(sudoku, strongLink.get(0), cycle, num, visited, strongLinks)){
                    if(handleCycle(sudoku, cycle, num))
                        return true;
                }
            }
        }
        return false;
    }

    private static boolean findCycle(ISudoku sudoku, ICell current, List<Link> cycle, int num, Set<ICell> visited, Set<List<ICell>> strongLinks) {
        if(cycle.get(0).start == current)
            return true;
        if(areConnected(current, cycle.get(0).start) && cycle.size() > 2){
            // Cycle found
            // Check for contradictions and make deductions
            cycle.add(new Link(current, cycle.get(0).start, LinkType.WEAK));
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
            cycle.remove(cycle.size()-1);
            cycle.remove(cycle.size()-1);
            visited.remove(possibleLink.start);
            visited.remove(possibleLink.end);
        }
        Set<ICell> commonPeers = getPeers(sudoku, cycle.get(0).start);
        commonPeers.retainAll(getPeers(sudoku, current));
        commonPeers = commonPeers.stream().filter(c -> c.getCandidates().contains(num)).collect(Collectors.toSet());
        if(commonPeers.size() > 0){
            ICell commonPeer = commonPeers.iterator().next();
            if(strongLinks.stream().anyMatch(l ->
                    (l.contains(current) && l.contains(commonPeer))
                            || (l.contains(cycle.get(0).start) && l.contains(commonPeer))
            ))
                return false;
            cycle.add(new Link(current, commonPeer, LinkType.WEAK));
            cycle.add(0, new Link(commonPeer, cycle.get(0).start, LinkType.WEAK));
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

    private static boolean handleCycle(ISudoku sudoku, List<Link> cycle, int num) {
        Link startLink = cycle.get(0);
        Link endLink = cycle.get(cycle.size() - 1);
        if(startLink.type == endLink.type){
            // Discontinuous cycle
            if(startLink.type == LinkType.STRONG){
                startLink.start.setValue(num);
                return true;
            }
            else{
                return startLink.start.removeCandidate(num);
            }
        }
        else {
            // Continuous cycle
            boolean changed = false;
            for (Link link : cycle.stream().filter(l -> l.type == LinkType.WEAK).toList()) {
                for (ICell cell : findCommon(sudoku, link.start, link.end)) {
                    changed |= cell.removeCandidate(num);
                }
            }
            return changed;
        }
    }

    // https://www.sudokuwiki.org/XY_Chains
    // XY-Chains
    // This technique is not tested cause of complexity of test
    // XY chains can be a bit tricky and one may cause another to be created or destroyed
    public static boolean xyChain(ISudoku sudoku) {
        for(ICell cell : sudoku.getEmptyCells().stream().filter(c -> c.getCandidates().size() == 2).toList()){
            for (int candidate: cell.getCandidates()){
                int otherCandidate = cell.getCandidates().stream()
                        .filter(c -> c != candidate)
                        .findFirst().orElseThrow( () -> new IllegalStateException("Expected other candidate not found"));
                List<ICell> chain = new ArrayList<>();
                chain.add(cell);
                if(findXYChain(sudoku, cell, candidate, otherCandidate, chain)){
                    // Chain found
                    // Check for contradictions and make deductions
                    if(handleChain(sudoku, chain, otherCandidate)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean handleChain(ISudoku sudoku, List<ICell> chain, int otherCandidate) {
        ICell start = chain.get(0);
        ICell end = chain.get(chain.size()-1);
        Set<ICell> commonPeers = getPeers(sudoku, start);
        commonPeers.retainAll(getPeers(sudoku, end));
        commonPeers = commonPeers.stream().filter(c -> c.getCandidates().contains(otherCandidate)).collect(Collectors.toSet());
        if(commonPeers.size() > 0){
            for(ICell commonPeer : commonPeers){
                commonPeer.removeCandidate(otherCandidate);
            }
            return true;
        }
        return false;
    }

    private static boolean findXYChain(ISudoku sudoku, ICell cell, int currentCandidate, int otherCandidate, List<ICell> chain) {
        if(cell.getCandidates().contains(otherCandidate) && chain.size() > 1)
            return true;
        int finalCurrentCandidate = currentCandidate;
        List<ICell> peers = getPeers(sudoku, cell).stream()
                .filter(c -> c.getCandidates().contains(finalCurrentCandidate) && c.getCandidates().size() == 2)
                .toList();
        for(ICell peer : peers){
            if(chain.contains(peer))
                continue;
            currentCandidate = peer.getCandidates().stream()
                    .filter(c -> c != finalCurrentCandidate)
                    .findFirst().orElseThrow( () -> new IllegalStateException("Expected other candidate not found"));
            chain.add(peer);
            if(findXYChain(sudoku, peer, currentCandidate, otherCandidate, chain)){
                return true;
            }
            chain.remove(chain.size()-1);
        }
        return false;
    }

    // Helper function to determine if a cell is connected to any cell in a chain

    private static List<ICell> findCommon(ISudoku sudoku, ICell start, ICell end) {
        List<ICell> common = new ArrayList<>();
        if(start.getY() == end.getY())
            common.addAll(List.of(sudoku.getRow(start.getY())));
        else if(start.getX() == end.getX())
            common.addAll(List.of(sudoku.getColumn(start.getX())));
        else if (start.getX() / 3 == end.getX() / 3 && start.getY() / 3 == end.getY() / 3)
            common.addAll(List.of(sudoku.getSquare(start.getX(), start.getY())));
        else
            throw new IllegalArgumentException("Cells are not connected");
        common.remove(start);
        common.remove(end);
        return common;
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

    private static Set<ICell> getPeers(ISudoku sudoku, ICell cell) {
        Set<ICell> peers = new LinkedHashSet<>();
        peers.addAll(Set.of(sudoku.getColumn(cell.getX())));
        peers.addAll(Set.of(sudoku.getRow(cell.getY())));
        peers.addAll(Set.of(sudoku.getSquare(cell.getX(), cell.getY())));
        peers.removeIf(c -> cell.getX() == c.getX() && cell.getY() == c.getY());
        return peers;
    }

    private static boolean hasConnection(ICell cell, List<ICell> chain) {
        for (ICell chainCell : chain)
            if (areConnected(cell, chainCell))
                return true;
        return false;
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
