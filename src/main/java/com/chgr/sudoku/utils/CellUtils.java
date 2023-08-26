package com.chgr.sudoku.utils;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;

import java.util.LinkedHashSet;
import java.util.Set;

public class CellUtils {

    public static Set<ICell> getPeers(ISudoku sudoku, ICell cell) {
        Set<ICell> peers = new LinkedHashSet<>();
        peers.addAll(Set.of(sudoku.getColumn(cell.getX())));
        peers.addAll(Set.of(sudoku.getRow(cell.getY())));
        peers.addAll(Set.of(sudoku.getSquare(cell.getX(), cell.getY())));
        peers.removeIf(c -> cell.getX() == c.getX() && cell.getY() == c.getY());
        return peers;
    }

}
