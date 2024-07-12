package com.chgr.sudoku.models;

import javafx.scene.paint.Color;

import java.util.Collection;
import java.util.Set;

public interface ICell {
    int EMPTY = 0;
    Set<Integer> DIGITS = Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9);

    Set<Integer> getCandidates();

    int getX();

    int getY();

    int getValue();

    void clearCandidates();

    void setValue(int num);

    void clear();

    void addCandidate(int num);

    void addCandidates(Collection<Integer> candidates);

    boolean removeCandidate(int candidate);

    boolean removeCandidates(Collection<Integer> candidates);

    Pos getPos();

    default void colorCandidates(Collection<Integer> candidates, Color color) {}

    default void clearColorCandidates(Collection<Integer> candidates) {}

    default void reRender(boolean fromUser) {}
}
