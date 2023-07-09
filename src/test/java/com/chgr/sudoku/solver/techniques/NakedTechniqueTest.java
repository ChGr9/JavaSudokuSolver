package com.chgr.sudoku.solver.techniques;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NakedTechniqueTest extends BaseTechniqueTest {

    @Test
    void nakedSingle() throws IOException {
        URL url = NakedTechniqueTest.class.getResource("/techniques/nakedSingle.yml");
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

        boolean result = NakedTechnique.nakedSingle(sudoku);

        assertTrue(result);
        assertChecksMatch(sudoku, technique.checks);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void nakedPair(int fileNumber) throws IOException {
        URL url = NakedTechniqueTest.class.getResource(String.format("/techniques/nakedPair%d.yml", fileNumber));
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
            boolean result = NakedTechnique.nakedPair(sudoku);
            assertTrue(result);
        }
        assertChecksMatch(sudoku, technique.checks);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void nakedTriple(int fileNumber) throws IOException {
        URL url = NakedTechniqueTest.class.getResource(String.format("/techniques/nakedTriple%d.yml", fileNumber));
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
            boolean result = NakedTechnique.nakedTriple(sudoku);
            assertTrue(result);
        }
        assertChecksMatch(sudoku, technique.checks);
    }

    @Test
    void nakedQuad() throws IOException {
        URL url = NakedTechniqueTest.class.getResource("/techniques/nakedQuad.yml");
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
            boolean result = NakedTechnique.nakedQuad(sudoku);
            assertTrue(result);
        }
        assertChecksMatch(sudoku, technique.checks);
    }
}