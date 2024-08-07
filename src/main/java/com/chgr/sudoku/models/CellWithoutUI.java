package com.chgr.sudoku.models;

import lombok.Getter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CellWithoutUI implements ICell {

    @Getter
    private final int x;
    @Getter
    private final int y;
    private final Set<Integer> candidates;
    @Getter
    private int value;

    public CellWithoutUI(int x, int y){
        this.x = x;
        this.y = y;
        this.candidates = new HashSet<>();
        this.value = EMPTY;
    }

    @Override
    public Set<Integer> getCandidates() {
        if(this.value != EMPTY)
            return Set.of();
        return Set.copyOf(candidates);
    }

    @Override
    public void clearCandidates() {
        candidates.clear();
    }

    @Override
    public void setValue(int value) {
        this.value = value;
        clearCandidates();
    }

    @Override
    public void clear() {
        setValue(EMPTY);
    }

    @Override
    public void addCandidate(int candidate) {
        if(this.value != EMPTY)
            return;
        if (candidate > 0 && candidate < 10) {
            candidates.add(candidate);
        }
    }

    @Override
    public void addCandidates(Collection<Integer> candidates) {
        candidates.forEach(this::addCandidate);
    }

    @Override
    public boolean removeCandidate(int candidate) {
        if (candidate > 0 && candidate < 10 && value == EMPTY) {
            return candidates.remove(candidate);
        }
        return false;
    }

    @Override
    public boolean removeCandidates(Collection<Integer> candidates) {
        return candidates.stream()
                .map(this::removeCandidate)
                .reduce(false, (a, b) -> a || b);
    }

    @Override
    public Pos getPos() {
        return new Pos(x, y);
    }

    @Override
    public int getSquare() {
        return x / 3 + y / 3 * 3;
    }
}