package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.solver.utils.SudokuWithoutUI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class BaseTechniqueTest {

    protected record Check(int x, int y, Integer value, Set<Integer> candidates) {
    }
    @Setter
    protected static class TechniqueEntity{
        public List<List<Integer>> grid;
        public List<Check> checks;
        public Integer repetitions = 1;
    }

    protected SudokuWithoutUI sudoku;

    protected ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        sudoku = new SudokuWithoutUI();
        mapper = new ObjectMapper(new YAMLFactory());
    }

    protected void assertChecksMatch(SudokuWithoutUI sudoku, List<Check> checks) {
        for(Check check : checks){
            if(check.candidates != null) {
                System.out.println("check.candidates: " + check.candidates()); // Print check.candidates
                Set<Integer> cellCandidates = sudoku.getCell(check.x(), check.y()).getCandidates();
                System.out.println("cellCandidates: " + cellCandidates);
                assertThat(check.candidates, containsInAnyOrder(cellCandidates.toArray()));
            }
            if(check.value != null)
                assertEquals(check.value, sudoku.getCell(check.x, check.y).getValue());
        }
    }
}
