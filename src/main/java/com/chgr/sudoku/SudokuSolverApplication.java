package com.chgr.sudoku;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SudokuSolverApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SudokuSolverApplication.class.getResource("SudokuSolver.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Sudoku Solver");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> Platform.exit());
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}