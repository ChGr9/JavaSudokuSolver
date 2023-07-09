module com.chgr.sudoku.javasudokusolver {
    requires javafx.controls;
    requires javafx.fxml;
    requires lombok;
    requires commons.math3;
    requires org.apache.commons.collections4;

    opens com.chgr.sudoku to javafx.fxml;
    exports com.chgr.sudoku;
    exports com.chgr.sudoku.models;
}