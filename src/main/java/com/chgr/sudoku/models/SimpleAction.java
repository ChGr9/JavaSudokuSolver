package com.chgr.sudoku.models;

import lombok.experimental.SuperBuilder;

import java.util.function.Function;

@SuperBuilder
public class SimpleAction extends BaseAction {
    private Function<Sudoku, Void> function;

    @Override
    public void apply(Sudoku sudoku) {
        function.apply(sudoku);
    }

    @Override
    public void display(Sudoku sudoku) {
        function.apply(sudoku);
    }
}
