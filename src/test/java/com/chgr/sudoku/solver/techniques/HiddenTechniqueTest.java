package com.chgr.sudoku.solver.techniques;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HiddenTechniqueTest extends BaseTechniqueTest {

    @ParameterizedTest
    @ValueSource(strings = {"Square", "Row", "Column"})
    void hiddenSingle(String fileSuffix) throws IOException {
        URL url = HiddenTechniqueTest.class.getResource(String.format("/techniques/hiddenSingle%s.yml", fileSuffix));
        assertNotNull(url);

        BaseTechniqueTest.TechniqueEntity technique = mapper.readValue(url, BaseTechniqueTest.TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        boolean result = HiddenTechnique.hiddenSingle(sudoku);

        assertTrue(result);
        assertChecksMatch(sudoku, technique.checks);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void hiddenPair(int fileNumber) throws IOException {
        URL url = HiddenTechniqueTest.class.getResource(String.format("/techniques/hiddenPair%d.yml", fileNumber));
        assertNotNull(url);

        BaseTechniqueTest.TechniqueEntity technique = mapper.readValue(url, BaseTechniqueTest.TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        for(int i=0; i < technique.repetitions; i++) {
            boolean result = HiddenTechnique.hiddenPair(sudoku);
            assertTrue(result);
        }
        assertChecksMatch(sudoku, technique.checks);
    }

    @Test
    void hiddenTriple() throws IOException {
        URL url = HiddenTechniqueTest.class.getResource("/techniques/hiddenTriple.yml");
        assertNotNull(url);

        BaseTechniqueTest.TechniqueEntity technique = mapper.readValue(url, BaseTechniqueTest.TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        for(int i=0; i < technique.repetitions; i++) {
            boolean result = HiddenTechnique.hiddenTriple(sudoku);
            assertTrue(result);
        }
        assertChecksMatch(sudoku, technique.checks);
    }

    @Test
    void hiddenQuad() throws IOException {
        URL url = HiddenTechniqueTest.class.getResource("/techniques/hiddenQuad.yml");
        assertNotNull(url);

        BaseTechniqueTest.TechniqueEntity technique = mapper.readValue(url, BaseTechniqueTest.TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        for(int i=0; i < technique.repetitions; i++) {
            boolean result = HiddenTechnique.hiddenQuad(sudoku);
            assertTrue(result);
        }
        assertChecksMatch(sudoku, technique.checks);
    }
}
