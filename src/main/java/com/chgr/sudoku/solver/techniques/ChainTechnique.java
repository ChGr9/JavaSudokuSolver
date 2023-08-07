package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class ChainTechnique {

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

    // Helper function to determine if a cell is connected to any cell in a chain

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
