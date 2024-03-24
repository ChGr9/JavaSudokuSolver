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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Cell extends StackPane implements ICell {

    public static final int SIZE = 80;
    @Setter
    @Getter
    private int x;
    @Setter
    @Getter
    private int y;
    private final Set<Integer> candidates;
    private final StackPane[][] candidatesPane = new StackPane[3][3];
    private final Text[][] candidatesText = new Text[3][3];
    @Getter
    private int value;
    private final StackPane valuePane = new StackPane();
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
        this.valuePane.getChildren().add(this.valueText);
        this.valuePane.setMaxSize(SIZE/1.5f, SIZE/1.5f);
        this.setPrefSize(SIZE, SIZE);
        this.setLayoutX(x * SIZE);
        this.setLayoutY(y * SIZE);

        //Set background color
//        this.setStyle(((x / 3) + (y / 3)) % 2 == 0
//                ? "-fx-background-color: white;"
//                : "-fx-background-color: gray;");
        String style = "-fx-background-color: white; ";
        style += "-fx-border-color: black; ";
        // Set flat look
        style += "-fx-background-radius: 0; -fx-border-radius: 0; ";

        int rightBorderWidth = x % 3 == 2 ? 5 : 1;
        int bottomBorderWidth = y % 3 == 2 ? 5 : 1;

        int topBorderWidth = y % 3 == 0 ? 5 : 1;
        int leftBorderWidth = x % 3 == 0 ? 5 : 1;

        style += "-fx-border-width: " + topBorderWidth + " " + rightBorderWidth + " " + bottomBorderWidth + " " + leftBorderWidth + "; ";

        this.setStyle(style);


        GridPane candidateGrid = new GridPane();
        candidateGrid.setAlignment(javafx.geometry.Pos.CENTER);
        candidateGrid.setHgap(12.5);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.candidatesPane[i][j] = new StackPane();
                this.candidatesText[i][j] = new Text(" ");
                // Make the candidate numbers small
                this.candidatesText[i][j].setFont(new Font(this.candidatesText[i][j].getFont().getFamily(), SIZE/4f));
                this.candidatesPane[i][j].getChildren().add(this.candidatesText[i][j]);
                candidateGrid.add(this.candidatesPane[i][j], j, i);
            }
        }
        this.getChildren().addAll(candidateGrid, this.valuePane);

        this.x = x;
        this.y = y;
        this.candidates = new HashSet<>();
        this.value = EMPTY;

        this.setOnMouseClicked(event -> this.requestFocus());
        this.setOnKeyPressed(this::onKeyPress);
    }

    public void onKeyPress(KeyEvent keyEvent) {
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
        clearColorCandidates(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
        clearColorValue();
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

    public boolean removeCandidates(Collection<Integer> candidates) {
        return candidates.stream()
                .map(this::removeCandidate)
                .reduce(false, (a, b) -> a || b);
    }

    @Override
    public com.chgr.sudoku.models.Pos getPos() {
        return new Pos(x, y);
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

    public void colorCandidates(Collection<Integer> candidates, Color color) {
        for(int candidate : candidates){
            if(candidate > 0 && candidate < 10){
                int candidateX = (candidate - 1) / 3;
                int candidateY = (candidate - 1) % 3;
                if(this.candidatesText[candidateX][candidateY].getText().equals(" "))
                    continue;
                String colorAsHex = String.format("#%02X%02X%02X%02X",
                        (int) (color.getRed() * 255),
                        (int) (color.getGreen() * 255),
                        (int) (color.getBlue() * 255),
                        (int) (0.5 * 255));
                this.candidatesPane[candidateX][candidateY]
                        .setStyle(String.format("-fx-background-color: %s;", colorAsHex));
            }
        }
    }

    public void clearColorCandidates(Collection<Integer> candidates) {
        for(int candidate : candidates){
            if(candidate > 0 && candidate < 10){
                this.candidatesPane[(candidate - 1) / 3][(candidate - 1) % 3]
                        .setStyle("");
            }
        }
    }

    public void colorValue(Color color) {
        String colorAsHex = String.format("#%02X%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255),
                (int) (0.5 * 255));
        this.valuePane.setStyle(String.format("-fx-background-color: %s;", colorAsHex));
    }

    public void clearColorValue() {
        this.valuePane.setStyle("");
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
                    candidatesText[i][j].setText(" ");
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
                        candidatesText[i][j].setText(" ");
                    }
                }
            }
        }
    }
}
