package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.ICell;
import com.chgr.sudoku.models.SudokuWithoutUI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class BaseTechniqueTest {

    protected record Check(int x, int y, Integer value, Set<Integer> candidates) {
    }
    protected record CandidatePos(int x, int y, Set<Integer> candidates) {
    }
    @Setter
    protected static class TechniqueEntity{
        public List<List<Integer>> grid;
        public List<Check> checks;
        public List<CandidatePos> override;
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

    protected void loadSudoku(TechniqueEntity technique) {
        for (int i = 0; i < 9; i++) {
            List<Integer> row = technique.grid.get(i);
            assertNotNull(row);
            for (int j = 0; j < 9; j++) {
                Integer value = row.get(j);
                assertNotNull(value);
                sudoku.getCell(j, i).setValue(value);
            }
        }

        sudoku.loadCandidates();

        if (technique.override != null)
            for (CandidatePos candidatePos : technique.override) {
                ICell cell = sudoku.getCell(candidatePos.x(), candidatePos.y());
                cell.clearCandidates();
                cell.addCandidates(candidatePos.candidates());
            }
    }
}
