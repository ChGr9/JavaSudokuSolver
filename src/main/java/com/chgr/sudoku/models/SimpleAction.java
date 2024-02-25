package com.chgr.sudoku.models;

import lombok.experimental.SuperBuilder;

import java.util.function.Function;

@SuperBuilder
public class SimpleAction extends BaseAction {
    private Function<ISudoku, Void> function;

    @Override
    public void apply(ISudoku sudoku) {
        function.apply(sudoku);
    }

    @Override
    public void display(ISudoku sudoku) {
        function.apply(sudoku);
    }
}
