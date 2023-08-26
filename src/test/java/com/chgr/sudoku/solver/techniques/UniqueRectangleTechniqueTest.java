package com.chgr.sudoku.solver.techniques;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniqueRectangleTechniqueTest extends BaseTechniqueTest {

    @ValueSource(strings = {"1and4A", "2A1", "2A2", "2Band4A", "2C", "3A", "3B", "3BwithTriple", "4B", "5"})
    @ParameterizedTest
    void uniqueRectangle(String fileSuffix) throws IOException {
        URL url = NakedTechniqueTest.class.getResource(String.format("/techniques/uniqueRectangle%s.yml", fileSuffix));
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        for(int i=0; i < technique.repetitions; i++) {
            boolean result = UniqueRectangleTechnique.uniqueRectangle(sudoku);
            assertTrue(result);
        }
        assertChecksMatch(sudoku, technique.checks);
    }
}