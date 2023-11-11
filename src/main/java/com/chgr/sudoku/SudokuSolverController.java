package com.chgr.sudoku;

import com.chgr.sudoku.models.BaseAction;
import com.chgr.sudoku.models.Cell;
import com.chgr.sudoku.models.ISudoku;
import com.chgr.sudoku.models.Sudoku;
import com.chgr.sudoku.solver.BacktrackingSolver;
import com.chgr.sudoku.solver.LogicalSolver;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SudokuSolverController implements Initializable {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @FXML
    private Sudoku sudoku;
    @FXML
    private Button btnBruteSolve;
    @FXML
    private Button btnLogicalSolve;
    @FXML
    private VBox solveStepView;
    @FXML
    private ListView<String> solveStepList;

    private List<BaseAction> solveSteps;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        for(int i=0;i< ISudoku.SUDOKU_SIZE; i++) {
            for (int j = 0; j < ISudoku.SUDOKU_SIZE; j++) {
                Cell cell = sudoku.getCell(i, j);
                cell.setOnMouseClicked(event -> cell.requestFocus());
                cell.setOnKeyPressed(cell::onKeyPress);
            }
        }
        solveStepList.addEventFilter(MouseEvent.MOUSE_PRESSED, MouseEvent::consume);
    }

    @FXML
    private void bruteSolve(){
        BacktrackingSolver solver = new BacktrackingSolver(sudoku);
        btnBruteSolve.setDisable(true);
        btnLogicalSolve.setDisable(true);
        setSuccessHandler(solver);
        executorService.submit(solver);
    }

    private void setSuccessHandler(Task<Boolean> solver) {
        solver.setOnSucceeded(event -> {
            if(solver instanceof LogicalSolver logicalSolver){
                solveSteps = logicalSolver.getSteps();
                solveStepView.setVisible(true);
                solveStepList.getItems().clear();
                solveStepList.getItems().addAll(solveSteps.stream().map(BaseAction::getName).toList());
                setSolveStepView(0);
            }
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
            btnBruteSolve.setDisable(false);
            btnLogicalSolve.setDisable(false);
            alert.show();
        });
    }

    @FXML
    private void logicalSolve(){
        LogicalSolver solver = new LogicalSolver(sudoku);
        btnBruteSolve.setDisable(true);
        btnLogicalSolve.setDisable(true);
        setSuccessHandler(solver);
        executorService.submit(solver);
    }

    @FXML
    private void clear(){
        sudoku.clear();
        solveStepView.setVisible(false);
        solveStepList.getItems().clear();
    }

    public void goToStart() {
        setSolveStepView(0);
    }

    public void previousStep() {
        setSolveStepView(solveStepList.getSelectionModel().getSelectedIndex()-1);
    }

    public void nextStep() {
        setSolveStepView(solveStepList.getSelectionModel().getSelectedIndex()+1);
    }

    public void goToEnd() {
        setSolveStepView(solveSteps.size()-1);
    }

    private void setSolveStepView(int index){
        if(index < 0 || index >= solveSteps.size())
            return;
        int current = solveStepList.getSelectionModel().getSelectedIndex();
        if(current < index){
            sudoku.clear();
            current = 0;
        }
        for(int i=current; i<index; i++){
            solveSteps.get(i).apply(sudoku);
        }
        solveSteps.get(index).display(sudoku);
        solveStepList.getSelectionModel().select(index);
    }
}