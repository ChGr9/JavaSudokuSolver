package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.TechniqueAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

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

        loadSudoku(technique);

        Optional<TechniqueAction> result = NakedTechnique.nakedSingle(sudoku);

        assertTrue(result.isPresent());
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

        loadSudoku(technique);

        for(int i=0; i < technique.repetitions; i++) {
            Optional<TechniqueAction> result = NakedTechnique.nakedPair(sudoku);
            assertTrue(result.isPresent());
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

        loadSudoku(technique);

        for(int i=0; i < technique.repetitions; i++) {
            Optional<TechniqueAction> result = NakedTechnique.nakedTriple(sudoku);
            assertTrue(result.isPresent());
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

        loadSudoku(technique);

        for(int i=0; i < technique.repetitions; i++) {
            Optional<TechniqueAction> result = NakedTechnique.nakedQuad(sudoku);
            assertTrue(result.isPresent());
        }
        assertChecksMatch(sudoku, technique.checks);
    }
}