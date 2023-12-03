package com.chgr.sudoku.models;

import javafx.scene.paint.Color;
import lombok.experimental.SuperBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuperBuilder
public class TechniqueAction extends BaseAction {
    public static class CellColoring{
        enum ColoringType{
            VALUE,
            CANDIDATES,
            LINE
        }

        List<Pos> pos;
        Color color;
        Collection<Integer> candidates;
        ColoringType type;

        public static CellColoring valueColoring(List<Pos> pos, Color color) {
            return new CellColoring(pos, color, null, ColoringType.VALUE);
        }

        public static CellColoring candidatesColoring(List<Pos> pos, Color color, Collection<Integer> candidates) {
            return new CellColoring(pos, color, candidates, ColoringType.CANDIDATES);
        }

        public static CellColoring lineColoring(List<Pos> pos, Color color) {
            return new CellColoring(pos, color, null, ColoringType.LINE);
        }

        private CellColoring(List<Pos> pos, Color color, Collection<Integer> candidates, ColoringType type) {
            this.pos = pos;
            this.color = color;
            this.candidates = candidates;
            this.type = type;
        }
    }
    private List<CellColoring> colorings;
    private Map<Pos, Set<Integer>> removeCandidatesMap;
    private Map<Pos, Integer> setValueMap;

    @Override
    public void apply(Sudoku sudoku) {
        clearColoring(sudoku);
        if (setValueMap != null)
            for (var setValue : setValueMap.entrySet()) {
                sudoku.getCell(setValue.getKey()).setValue(setValue.getValue());
                sudoku.removeAffectedCandidates(setValue.getKey().x(), setValue.getKey().y(), setValue.getValue());
            }
        if (removeCandidatesMap != null)
            for (var removeCandidates : removeCandidatesMap.entrySet()) {
                sudoku.getCell(removeCandidates.getKey()).removeCandidates(removeCandidates.getValue());
            }
        sudoku.reRender();
    }

    public void clearColoring(Sudoku sudoku) {
        for (CellColoring coloring : colorings) {
            for (Pos pos : coloring.pos){
                switch (coloring.type) {
                    case VALUE -> sudoku.getCell(pos).clearColorValue();
                    case CANDIDATES -> sudoku.getCell(pos).clearColorCandidates(coloring.candidates);
                    case LINE -> sudoku.clearColorLine();
                }
            }
        }
    }

    @Override
    public void display(Sudoku sudoku) {
        for (CellColoring coloring : colorings) {
            for (Pos pos : coloring.pos){
                switch (coloring.type) {
                    case VALUE -> sudoku.getCell(pos).colorValue(coloring.color);
                    case CANDIDATES -> sudoku.getCell(pos).colorCandidates(coloring.candidates, coloring.color);
                    case LINE -> sudoku.colorLine(coloring.pos, coloring.color);
                }
            }
        }
    }
}
