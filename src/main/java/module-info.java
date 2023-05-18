module com.chgr.sudoku.javasudokusolver {
    requires javafx.controls;
    requires javafx.fxml;
    requires lombok;


    opens com.chgr.sudoku to javafx.fxml;
    exports com.chgr.sudoku;
    exports com.chgr.sudoku.models;
}