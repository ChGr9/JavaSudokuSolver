package com.chgr.sudoku.solver.techniques;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntersectionTechniqueTest extends BaseTechniqueTest {
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void pointingTuple(int fileNumber) throws IOException {
        URL url = IntersectionTechniqueTest.class.getResource(String.format("/techniques/pointingTuple%d.yml", fileNumber));
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
            boolean result = IntersectionTechnique.pointingTuple(sudoku);
            assertTrue(result);
        }
        assertChecksMatch(sudoku, technique.checks);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void boxLineReduction(int fileNumber) throws IOException {
        URL url = IntersectionTechniqueTest.class.getResource(String.format("/techniques/boxLineReduction%d.yml", fileNumber));
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
            boolean result = IntersectionTechnique.boxLineReduction(sudoku);
            assertTrue(result);
        }
        assertChecksMatch(sudoku, technique.checks);
    }
}