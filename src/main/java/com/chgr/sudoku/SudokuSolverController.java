package com.chgr.sudoku;

import com.chgr.sudoku.models.Sudoku;
import com.chgr.sudoku.solver.BacktrackingSolver;
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
        solver.setOnSucceeded(event -> {
            boolean result = solver.getValue();
            if(result){
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Solver finished");
                alert.setContentText("Sudoku solved!");
                alert.show();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Solve finished");
                alert.setContentText("Sudoku not solvable");
                alert.show();
            }
        });
        executorService.submit(solver);
    }

    @FXML
    private void logicalSolve(){
        //todo
    }

    @FXML
    private void clear(){
        sudoku.clear();
    }
}