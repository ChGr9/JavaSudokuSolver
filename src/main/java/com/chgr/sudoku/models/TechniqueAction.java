package com.chgr.sudoku.models;

import javafx.scene.paint.Color;
import lombok.experimental.SuperBuilder;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;

@SuperBuilder
public class TechniqueAction extends BaseAction {
    public static class CellColoring{
        List<Pos> pos;
        Color color;
        List<Integer> candidates;
        boolean colorValue;

        public CellColoring(List<Pos> pos, Color color, List<Integer> candidates) {
            this.pos = pos;
            this.color = color;
            this.candidates = candidates;
            this.colorValue = false;
        }

        public CellColoring(List<Pos> pos, Color color) {
            this.pos = pos;
            this.color = color;
            this.colorValue = true;
        }
    }
    List<CellColoring> colorings;
    private Map<Pos, List<Integer>> removeCandidatesMap;
    private Map<Pos, Integer> setValueMap;

    @Override
    public void apply(Sudoku sudoku) {
        for(var setValue : setValueMap.entrySet()){
            sudoku.getCell(setValue.getKey()).setValue(setValue.getValue());
        }
        for(var removeCandidates : removeCandidatesMap.entrySet()){
            sudoku.getCell(removeCandidates.getKey()).removeCandidates(removeCandidates.getValue());
        }
    }

    @Override
    public void display(Sudoku sudoku) {
        for (CellColoring coloring : colorings) {
            for (Pos pos : coloring.pos){
                Cell cell = sudoku.getCell(pos);
                if (coloring.colorValue)
                    cell.colorValue(coloring.color);
                else
                    cell.colorCandidates(coloring.candidates, coloring.color);
            }
        }
    }
}
