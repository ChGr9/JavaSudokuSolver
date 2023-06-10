package com.chgr.sudoku.models;

import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public class Cell extends StackPane implements ICell {

    public static final int SIZE = 80;
    @Setter
    @Getter
    private int x;
    @Setter
    @Getter
    private int y;
    private final Set<Integer> candidates;
    private final Text[][] candidatesText = new Text[3][3];
    @Getter
    private int value;
    private final Text valueText = new Text();
    private boolean hasChanged = false;

    public void setValue(int value) {
        this.value = value;
        clearCandidates();
        hasChanged = true;
    }

    public Cell(int x, int y) {
        super();

        this.valueText.setFont(Font.font(this.valueText.getFont().getFamily(), FontWeight.BOLD, SIZE/2.5f));
        this.setPrefSize(SIZE, SIZE);
        this.setLayoutX(x * SIZE);
        this.setLayoutY(y * SIZE);

        //Set background color
        this.setStyle(((x / 3) + (y / 3)) % 2 == 0
                ? "-fx-background-color: white;"
                : "-fx-background-color: gray;");

        GridPane candidateGrid = new GridPane();
        candidateGrid.setHgap(7.5);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.candidatesText[i][j] = new Text();
                // Make the candidate numbers small
                this.candidatesText[i][j].setFont(new Font(this.candidatesText[i][j].getFont().getFamily(), SIZE/4f));
                candidateGrid.add(this.candidatesText[i][j], j, i);
            }
        }
        this.getChildren().addAll(candidateGrid, this.valueText);
        //Set border
        this.setStyle(this.getStyle() + "-fx-border-color: black;");

        //Set flat look
        this.setStyle(this.getStyle() + "-fx-background-radius: 0; -fx-border-radius: 0;");

        this.x = x;
        this.y = y;
        this.candidates = new HashSet<>();
        this.value = EMPTY;

        this.setOnMouseClicked(event -> this.requestFocus());
        this.setOnKeyPressed(this::onKeyPress);
    }

    private void onKeyPress(KeyEvent keyEvent) {
        try {
            int value = Integer.parseInt(keyEvent.getText());
            if (value == 0)
                clear();
            else {
                valueText.setFill(Color.BLACK);
                setValue(value);
            }
            hasChanged = true;
            reRender(true);
        } catch (NumberFormatException ignored) {
        }
    }

    public void clear() {
        setValue(EMPTY);
    }

    public void addCandidate(int candidate) {
        if(this.value != EMPTY)
            return;
        if (candidate > 0 && candidate < 10) {
            candidates.add(candidate);
            hasChanged = true;
        }
    }

    public void addCandidates(Collection<Integer> candidates) {
        candidates.forEach(this::addCandidate);
    }

    public boolean removeCandidate(int candidate) {
        if (candidate > 0 && candidate < 10 && value == EMPTY) {
            if(candidates.remove(candidate)) {
                hasChanged = true;
                return true;
            }
        }
        return false;
    }

    public void removeCandidates(Collection<Integer> candidates) {
        candidates.forEach(this::removeCandidate);
    }

    public boolean hasCandidate(int candidate) {
        if(this.value == EMPTY)
            return false;
        return candidates.contains(candidate);
    }

    public void clearCandidates() {
        candidates.clear();
        hasChanged = true;
    }

    public Set<Integer> getCandidates() {
        if(this.value != EMPTY)
            return Set.of();
        return Set.copyOf(candidates);
    }

    public void reRender() {
        reRender(false);
    }

    public void reRender(boolean fromUser) {
        if(!hasChanged)
            return;
        hasChanged = false;
        if(value != EMPTY){
            if(fromUser)
                valueText.setFill(Color.BLACK);
            else
                valueText.setFill(Color.RED);
            valueText.setText(String.valueOf(value));
            for(int i = 0; i < 3; i++){
                for(int j = 0; j < 3; j++){
                    candidatesText[i][j].setText("");
                }
            }
        }
        else{
            valueText.setText("");
            for(int i = 0; i < 3; i++){
                for(int j = 0; j < 3; j++){
                    int candidate = i * 3 + j + 1;
                    if(candidates.contains(candidate)){
                        candidatesText[i][j].setText(String.valueOf(candidate));
                    }
                    else{
                        candidatesText[i][j].setText("");
                    }
                }
            }
        }
    }
}
