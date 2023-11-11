package com.chgr.sudoku.utils;

import com.chgr.sudoku.models.*;

import java.util.HashMap;
import java.util.Map;

public class SudokuConverter {
    public static Map<Pos, Integer> getInitialValues(ISudoku sudoku){
        Map<Pos, Integer> initialValues = new HashMap<>();
        for(int i=0; i<ISudoku.SUDOKU_SIZE; i++){
            for(int j=0; j<ISudoku.SUDOKU_SIZE; j++) {
                initialValues.put(new Pos(i, j), sudoku.getCell(i, j).getValue());
            }
        }
        return initialValues;
    }
}
