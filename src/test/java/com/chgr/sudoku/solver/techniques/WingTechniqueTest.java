package com.chgr.sudoku.solver.techniques;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WingTechniqueTest extends BaseTechniqueTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void xWing(int fileNumber) throws Exception {
        URL url = WingTechniqueTest.class.getResource(String.format("/techniques/xWing%d.yml", fileNumber));
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        for(int i=0; i < technique.repetitions; i++) {
            boolean result = WingTechnique.xWing(sudoku);
            assertTrue(result);
        }
        assertChecksMatch(sudoku, technique.checks);
    }
}