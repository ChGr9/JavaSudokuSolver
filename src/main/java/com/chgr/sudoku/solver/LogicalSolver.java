package com.chgr.sudoku.solver;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.ISudoku;
import com.chgr.sudoku.solver.techniques.HiddenTechnique;
import com.chgr.sudoku.solver.techniques.IntersectionTechnique;
import com.chgr.sudoku.solver.techniques.NakedTechnique;
import com.chgr.sudoku.solver.techniques.WingTechnique;

import java.util.List;
import java.util.function.Function;

public class LogicalSolver {

    private static boolean isSolved(ISudoku sudoku) {
        for (int i = 0; i < ISudoku.SUDOKU_SIZE; i++) {
            if (!sudoku.getRowValue(i).containsAll(ICell.DIGITS) ||
                    !sudoku.getColumnValue(i).containsAll(ICell.DIGITS) ||
                    !sudoku.getSquareValue(i).containsAll(ICell.DIGITS))
                return false;
        }
        return true;
    }

    private static final List<Function<ISudoku, Boolean>> techniques = List.of(
            NakedTechnique::nakedSingle,
            HiddenTechnique::hiddenSingle,
            NakedTechnique::nakedPair,
            NakedTechnique::nakedTriple,
            HiddenTechnique::hiddenPair,
            HiddenTechnique::hiddenTriple,
            NakedTechnique::nakedQuad,
            HiddenTechnique::hiddenQuad,
            IntersectionTechnique::pointingTuple,
            IntersectionTechnique::boxLineReduction,
            WingTechnique::xWing
    );

    public static boolean solve(ISudoku sudoku){
        sudoku.loadCandidates();
        boolean changed = true;
        while(changed){
            changed = false;
            for(Function<ISudoku, Boolean> technique : techniques){
                if(technique.apply(sudoku)){
                    changed = true;
                }
            }
        }
        return isSolved(sudoku);
    }
}
