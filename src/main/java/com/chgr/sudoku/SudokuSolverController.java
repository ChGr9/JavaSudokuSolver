package com.chgr.sudoku;

import com.chgr.sudoku.models.*;
import com.chgr.sudoku.solver.BacktrackingSolver;
import com.chgr.sudoku.solver.LogicalSolver;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
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
    @FXML
    public TextField importTextField;

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
                solveStepList.getSelectionModel().select(0);
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
        if(current > index){
            BaseAction lastAction = solveSteps.get(current);
            if(lastAction instanceof TechniqueAction techniqueAction){
                techniqueAction.clearColoring(sudoku);
            }
            sudoku.clear();
            current = 0;
        }
        for(int i=current; i<index; i++){
            solveSteps.get(i).apply(sudoku);
        }
        solveSteps.get(index).display(sudoku);
        solveStepList.getSelectionModel().select(index);
        double itemHeight = solveStepList.getFixedCellSize();
        double listViewHeight = solveStepList.getHeight();
        int visibleItemCount = (int) Math.floor(listViewHeight / itemHeight);

        int targetIndex = Math.max(0, index - visibleItemCount/2);
        solveStepList.scrollTo(targetIndex);
    }

    public void importSudoku() {
        String text = importTextField.getText().replace(" ", "");
        if(text.length() != ISudoku.SUDOKU_SIZE*ISudoku.SUDOKU_SIZE){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Import error");
            alert.setContentText("Sudoku must be 81 characters long");
            alert.show();
            return;
        }
        if(!text.matches("\\d+")){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Import error");
            alert.setContentText("Sudoku must contain only numbers");
            alert.show();
            return;
        }
        sudoku.clear();
        for(int i=0; i<ISudoku.SUDOKU_SIZE; i++){
            for(int j=0; j<ISudoku.SUDOKU_SIZE; j++){
                int value = Character.getNumericValue(text.charAt(i*ISudoku.SUDOKU_SIZE+j));
                if(value != 0){
                    Cell cell = sudoku.getCell(j, i);
                    cell.setValue(value);
                    cell.reRender(true);
                }
            }
        }
    }
}