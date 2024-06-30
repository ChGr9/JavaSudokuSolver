package com.chgr.sudoku.models;

import javafx.scene.paint.Color;
import lombok.experimental.SuperBuilder;
import org.apache.commons.math3.util.Pair;

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
            GROUP,
            LINE,
            DOUBLE_LINE
        }

        Collection<Pos> pos;
        List<Pair<Pos, Pos>> linePos;
        Color color;
        Collection<Integer> candidates;
        ColoringType type;

        public static CellColoring valueColoring(List<Pos> pos, Color color) {
            return new CellColoring(pos, color, ColoringType.VALUE);
        }

        public static CellColoring candidatesColoring(Collection<Pos> pos, Color color, Collection<Integer> candidates) {
            return new CellColoring(pos, color, candidates);
        }

        public static CellColoring groupColoring(List<Pos> pos, Color color) {
            return new CellColoring(pos, color, ColoringType.GROUP);
        }

        public static CellColoring lineColoring(List<Pair<Pos, Pos>> linePos, Color color, int candidate) {
            return new CellColoring(linePos, candidate, color, false);
        }

        public static CellColoring doubleLineColoring(List<Pair<Pos, Pos>> linePos, Color color, int candidate) {
            return new CellColoring(linePos, candidate, color, true);
        }

        private CellColoring(Collection<Pos> pos, Color color, Collection<Integer> candidates) {
            this.pos = pos;
            this.color = color;
            this.candidates = candidates;
            this.type = ColoringType.CANDIDATES;
        }

        private CellColoring(List<Pos> pos, Color color, ColoringType coloringType) {
            this.pos = pos;
            this.color = color;
            this.type = coloringType;
        }

        private CellColoring(List<Pair<Pos, Pos>> linePos, int candidate, Color color, boolean doubleLine) {
            this.linePos = linePos;
            this.color = color;
            this.type = doubleLine? ColoringType.DOUBLE_LINE : ColoringType.LINE;
            this.candidates = Set.of(candidate);
        }
    }
    private List<CellColoring> colorings;
    private Map<Pos, Set<Integer>> removeCandidatesMap;
    private Map<Pos, Integer> setValueMap;

    @Override
    public void apply(ISudoku sudoku) {
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

    public void clearColoring(ISudoku sudoku) {
        for (CellColoring coloring : colorings) {
            switch (coloring.type) {
                case VALUE -> coloring.pos.forEach(pos -> sudoku.getCell(pos).clearColorValue());
                case CANDIDATES -> coloring.pos.forEach(pos -> sudoku.getCell(pos).clearColorCandidates(coloring.candidates));
                case GROUP -> sudoku.clearColorGroup();
                case LINE, DOUBLE_LINE -> sudoku.clearColorLine();
            }
        }
    }

    @Override
    public void display(ISudoku sudoku) {
        for (CellColoring coloring : colorings) {
            switch (coloring.type) {
                case VALUE -> coloring.pos.forEach(pos -> sudoku.getCell(pos).colorValue(coloring.color));
                case CANDIDATES -> coloring.pos.forEach(pos -> sudoku.getCell(pos).colorCandidates(coloring.candidates, coloring.color));
                case GROUP -> sudoku.colorGroup(coloring.pos, coloring.color);
                case LINE -> coloring.linePos.forEach(poses -> sudoku.colorLine(poses.getFirst(), poses.getSecond(), coloring.candidates.stream().findAny().orElseThrow(), coloring.color, false));
                case DOUBLE_LINE -> coloring.linePos.forEach(poses -> sudoku.colorLine(poses.getFirst(), poses.getSecond(), coloring.candidates.stream().findAny().orElseThrow(), coloring.color, true));
            }
        }
    }
}
