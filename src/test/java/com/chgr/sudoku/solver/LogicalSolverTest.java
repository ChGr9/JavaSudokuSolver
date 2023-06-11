package com.chgr.sudoku.solver;

import com.chgr.sudoku.solver.utils.SudokuWithoutUI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;

class LogicalSolverTest {

    private record Check(int x, int y, Integer value, Set<Integer> candidates) {
    }
    @Setter
    private static class TechniqueEntity{
        private List<List<Integer>> grid;
        private List<Check> checks;
        private Integer repetitions = 1;
    }

    private SudokuWithoutUI sudoku;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        sudoku = new SudokuWithoutUI();
        mapper = new ObjectMapper(new YAMLFactory());
    }

    @Test
    void nakedSingle() throws IOException {
        URL url = LogicalSolverTest.class.getResource("/techniques/nakedSingle.yml");
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        for(int i = 0; i<9; i++){
            List<Integer> row = technique.grid.get(i);
            assertNotNull(row);
            for(int j = 0; j<9; j++){
                Integer value = row.get(j);
                assertNotNull(value);
                sudoku.getCell(j, i).setValue(value);
            }
        }

        sudoku.loadCandidates();

        boolean result = LogicalSolver.nakedSingle(sudoku);

        assertTrue(result);
        assertChecksMatch(sudoku, technique.checks);
    }

    private void assertChecksMatch(SudokuWithoutUI sudoku, List<Check> checks) {
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

    @ParameterizedTest
    @ValueSource(strings = {"Square", "Row", "Column"})
    void hiddenSingle(String fileSuffix) throws IOException {
        URL url = LogicalSolverTest.class.getResource(String.format("/techniques/hiddenSingle%s.yml", fileSuffix));
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        for(int i = 0; i<9; i++){
            List<Integer> row = technique.grid.get(i);
            assertNotNull(row);
            for(int j = 0; j<9; j++){
                Integer value = row.get(j);
                assertNotNull(value);
                sudoku.getCell(j, i).setValue(value);
            }
        }

        sudoku.loadCandidates();

        boolean result = LogicalSolver.hiddenSingle(sudoku);

        assertTrue(result);
        assertChecksMatch(sudoku, technique.checks);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void nakedPair(int fileNumber) throws IOException {
        URL url = LogicalSolverTest.class.getResource(String.format("/techniques/nakedPair%d.yml", fileNumber));
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        for(int i = 0; i<9; i++){
            List<Integer> row = technique.grid.get(i);
            assertNotNull(row);
            for(int j = 0; j<9; j++){
                Integer value = row.get(j);
                assertNotNull(value);
                sudoku.getCell(j, i).setValue(value);
            }
        }

        sudoku.loadCandidates();

        for(int i=0; i < technique.repetitions; i++) {
            boolean result = LogicalSolver.nakedPair(sudoku);
            assertTrue(result);
        }
        assertChecksMatch(sudoku, technique.checks);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void nakedTriple(int fileNumber) throws IOException {
        URL url = LogicalSolverTest.class.getResource(String.format("/techniques/nakedTriple%d.yml", fileNumber));
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        for(int i = 0; i<9; i++){
            List<Integer> row = technique.grid.get(i);
            assertNotNull(row);
            for(int j = 0; j<9; j++){
                Integer value = row.get(j);
                assertNotNull(value);
                sudoku.getCell(j, i).setValue(value);
            }
        }

        sudoku.loadCandidates();

        for(int i=0; i < technique.repetitions; i++) {
            boolean result = LogicalSolver.nakedTriple(sudoku);
            assertTrue(result);
        }
        assertChecksMatch(sudoku, technique.checks);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void hiddenPair(int fileNumber) throws IOException {
        URL url = LogicalSolverTest.class.getResource(String.format("/techniques/hiddenPair%d.yml", fileNumber));
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        for(int i = 0; i<9; i++){
            List<Integer> row = technique.grid.get(i);
            assertNotNull(row);
            for(int j = 0; j<9; j++){
                Integer value = row.get(j);
                assertNotNull(value);
                sudoku.getCell(j, i).setValue(value);
            }
        }

        sudoku.loadCandidates();

        for(int i=0; i < technique.repetitions; i++) {
            boolean result = LogicalSolver.hiddenPair(sudoku);
            assertTrue(result);
        }
        assertChecksMatch(sudoku, technique.checks);
    }

    @Test
    void hiddenTriple() throws IOException {
        URL url = LogicalSolverTest.class.getResource("/techniques/hiddenTriple.yml");
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        for(int i = 0; i<9; i++){
            List<Integer> row = technique.grid.get(i);
            assertNotNull(row);
            for(int j = 0; j<9; j++){
                Integer value = row.get(j);
                assertNotNull(value);
                sudoku.getCell(j, i).setValue(value);
            }
        }

        sudoku.loadCandidates();

        for(int i=0; i < technique.repetitions; i++) {
            boolean result = LogicalSolver.hiddenTriple(sudoku);
            assertTrue(result);
        }
        assertChecksMatch(sudoku, technique.checks);
    }
}