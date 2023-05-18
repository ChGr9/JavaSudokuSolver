package com.chgr.sudoku.models;

import java.util.Collection;
import java.util.Set;

public interface ICell {
    int EMPTY = 0;
    Set<Integer> DIGITS = Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9);

    Collection<Integer> getCandidates();

    int getX();

    int getY();

    void clearCandidates();

    void setValue(int num);

    void clear();

    void addCandidate(int num);

    void addCandidates(Collection<Integer> candidates);
}
