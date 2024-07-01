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
        peers.removeIf(cell::equals);
        return peers;
    }

    public static boolean isPeer(ICell cell1, ICell cell2) {
        return cell1.getX() == cell2.getX() || cell1.getY() == cell2.getY()
                || (cell1.getX() / 3 == cell2.getX() / 3 && cell1.getY() / 3 == cell2.getY() / 3);
    }
}
