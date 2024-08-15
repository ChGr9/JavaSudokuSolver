package com.chgr.sudoku.solver;

import com.chgr.sudoku.models.*;
import com.chgr.sudoku.solver.techniques.*;
import com.chgr.sudoku.utils.SudokuConverter;
import javafx.concurrent.Task;

import java.util.*;
import java.util.function.Function;

public class LogicalSolver extends Task<Boolean> {

    private final List<BaseAction> solveSteps;
    private final Map<Pos, Integer> initialValues;
    private final SudokuWithoutUI sudoku;

    public LogicalSolver(ISudoku sudoku) {
        this.solveSteps = new ArrayList<>();
        this.initialValues = SudokuConverter.getInitialValues(sudoku);
        this.sudoku = new SudokuWithoutUI();
        for(Map.Entry<Pos, Integer> entry : initialValues.entrySet()){
            this.sudoku.getCell(entry.getKey()).setValue(entry.getValue());
        }
    }

    private boolean isSolved() {
        for (int i = 0; i < ISudoku.SUDOKU_SIZE; i++) {
            if (!sudoku.getRowValue(i).containsAll(ICell.DIGITS) ||
                    !sudoku.getColumnValue(i).containsAll(ICell.DIGITS) ||
                    !sudoku.getSquareValue(i).containsAll(ICell.DIGITS))
                return false;
        }
        return true;
    }

    private static final List<Function<ISudoku, Optional<TechniqueAction>>> techniques = List.of(
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
            WingTechnique::xWing,
            ChainTechnique::simpleColoring,
            WingTechnique::yWing,
            RectangleTechnique::rectangleElimination,
            WingTechnique::swordfish,
            WingTechnique::xyzWing,
            ChainTechnique::xCycle,
            ChainTechnique::xyChain,
            ChainTechnique::medusa3D,
            WingTechnique::jellyfish,
            RectangleTechnique::uniqueRectangle,
            IntersectionTechnique::firework,
            ChainTechnique::skLoop,
            RectangleTechnique::extendedUniqueRectangle,
            RectangleTechnique::hiddenUniqueRectangle,
            WingTechnique::wxyzWing,
            IntersectionTechnique::alignedPairExclusion,
            ChainTechnique::groupedXCycle,
            WingTechnique::finnedXWing,
            WingTechnique::finnedSwordfish
    );

    public boolean solve(){
        if(!sudoku.initialValidation())
            return false;
        solveSteps.add(SimpleAction.builder()
                        .name("Initial values")
                        .description("Set initial values")
                        .function( (sud) -> {
                            for(Map.Entry<Pos, Integer> entry : initialValues.entrySet()){
                                ICell cell = sud.getCell(entry.getKey());
                                cell.setValue(entry.getValue());
                                cell.reRender(true);
                            }
                            return null;
                        })
                        .build());
        solveSteps.add(SimpleAction.builder()
                .name("Load candidates")
                .description("Load candidates")
                .function( (sud) -> {
                    sud.loadCandidates();
                    sud.reRender();
                    return null;
                })
                .build());
        sudoku.loadCandidates();
        boolean changed;
        do {
            sudoku.reRender();
            changed = false;
            for(Function<ISudoku, Optional<TechniqueAction>> technique : techniques){
                Optional<TechniqueAction> action = technique.apply(sudoku);
                if(action.isPresent()){
                    action.get().apply(sudoku);
                    solveSteps.add(action.get());
                    changed = true;
                    break;
                }
            }
        }
        while (changed);
        solveSteps.add(SimpleAction.builder()
                .name("End")
                .description("Finished solving")
                .function(_ -> null)
                .build());
        return isSolved();
    }

    @Override
    protected Boolean call() {
        return solve();
    }

    public List<BaseAction> getSteps() {
        return Collections.unmodifiableList(solveSteps);
    }
}
