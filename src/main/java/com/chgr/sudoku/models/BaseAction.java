package com.chgr.sudoku.models;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public abstract class BaseAction {
    private String name;
    private String description;
    public abstract void apply(ISudoku sudoku);
    public abstract void display(ISudoku sudoku);
}
