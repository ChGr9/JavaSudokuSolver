package com.chgr.sudoku.models;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public abstract class BaseAction {
    private String name;
    private String description;
    public abstract void apply(Sudoku sudoku);
    public abstract void display(Sudoku sudoku);
}
