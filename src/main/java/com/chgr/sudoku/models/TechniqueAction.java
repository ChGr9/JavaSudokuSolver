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
    public interface CellColoring {
        void apply(ISudoku sudoku);
        void clear(ISudoku sudoku);
    }
    public static class CandidatesColoring implements CellColoring {
        private final Collection<Pos> pos;
        private final Color color;
        private final Collection<Integer> candidates;

        public CandidatesColoring(Collection<Pos> pos, Color color, Collection<Integer> candidates) {
            this.pos = pos;
            this.color = color;
            this.candidates = candidates;
        }

        @Override
        public void apply(ISudoku sudoku) {
            pos.forEach(pos -> sudoku.getCell(pos).colorCandidates(candidates, color));
        }

        @Override
        public void clear(ISudoku sudoku) {
            pos.forEach(pos -> sudoku.getCell(pos).clearColorCandidates(candidates));
        }
    }
    public static class GroupColoring implements CellColoring {
        private final List<Pair<Pos, Pos>> linePos;
        private final Color color;

        public GroupColoring(List<Pair<Pos, Pos>> linePos, Color color) {
            this.linePos = linePos;
            this.color = color;
        }

        @Override
        public void apply(ISudoku sudoku) {
            linePos.forEach(poses -> sudoku.colorGroup(poses.getFirst(), poses.getSecond(), color));
        }

        @Override
        public void clear(ISudoku sudoku) {
            sudoku.clearColorGroup();
        }
    }
    public static class LineColoring implements CellColoring {
        private final List<Pair<PosCandidate, PosCandidate>> linePos;
        private final Color color;
        protected boolean isDouble;

        public LineColoring(List<Pair<PosCandidate, PosCandidate>> linePos, Color color, boolean isDouble) {
            this.linePos = linePos;
            this.color = color;
            this.isDouble = isDouble;
        }

        public LineColoring(List<Pair<Pos, Pos>> linePos, Color color, int candidate, boolean isDouble) {
            this.linePos = linePos.stream().map(pair -> new Pair<>(new PosCandidate(pair.getFirst(), candidate), new PosCandidate(pair.getSecond(), candidate))).toList();
            this.color = color;
            this.isDouble = isDouble;
        }

        @Override
        public void apply(ISudoku sudoku) {
            linePos.forEach(poses -> sudoku.colorLine(poses.getFirst(), poses.getSecond(), color, false));
        }

        @Override
        public void clear(ISudoku sudoku) {
            sudoku.clearColorLine();
        }
    }
    private List<CellColoring> cellColorings;
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
        for (CellColoring cellColoring : cellColorings) {
            cellColoring.clear(sudoku);
        }
    }

    @Override
    public void display(ISudoku sudoku) {
        for (CellColoring cellColoring : cellColorings) {
            cellColoring.apply(sudoku);
        }
    }
}
