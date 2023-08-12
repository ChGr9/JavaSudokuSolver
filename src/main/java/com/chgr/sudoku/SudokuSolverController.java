package com.chgr.sudoku;

import com.chgr.sudoku.models.Sudoku;
import com.chgr.sudoku.solver.BacktrackingSolver;
import com.chgr.sudoku.solver.LogicalSolver;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SudokuSolverController {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @FXML
    private Sudoku sudoku;

    @FXML
    private void bruteSolve(){
        BacktrackingSolver solver = new BacktrackingSolver(sudoku);
        setSuccessHandler(solver);
        executorService.submit(solver);
    }

    private void setSuccessHandler(Task<Boolean> solver) {
        solver.setOnSucceeded(event -> {
            boolean result = solver.getValue();
            Alert alert;
            if(result){
                alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Solver finished");
                alert.setContentText("Sudoku solved!");
            } else {
                alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Solve finished");
                alert.setContentText("Sudoku not solvable");
            }
            alert.show();
        });
    }

    @FXML
    private void logicalSolve(){
        LogicalSolver solver = new LogicalSolver(sudoku);
        setSuccessHandler(solver);
        executorService.submit(solver);
    }

    @FXML
    private void clear(){
        sudoku.clear();
    }
}