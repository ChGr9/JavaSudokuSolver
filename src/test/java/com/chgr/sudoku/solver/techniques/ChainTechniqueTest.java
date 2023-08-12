package com.chgr.sudoku.solver.techniques;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChainTechniqueTest extends BaseTechniqueTest{

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void simpleColoring(int fileNumber) throws IOException {
        URL url = HiddenTechniqueTest.class.getResource(String.format("/techniques/simpleColoring%d.yml", fileNumber));
        assertNotNull(url);

        BaseTechniqueTest.TechniqueEntity technique = mapper.readValue(url, BaseTechniqueTest.TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        for(int i=0; i < technique.repetitions; i++) {
            boolean result = ChainTechnique.simpleColoring(sudoku);
            assertTrue(result);
        }
        assertChecksMatch(sudoku, technique.checks);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void xCycle(int fileNumber) throws IOException {
        URL url = HiddenTechniqueTest.class.getResource(String.format("/techniques/xCycle%d.yml", fileNumber));
        assertNotNull(url);

        BaseTechniqueTest.TechniqueEntity technique = mapper.readValue(url, BaseTechniqueTest.TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        for(int i=0; i < technique.repetitions; i++) {
            boolean result = ChainTechnique.xCycle(sudoku);
            assertTrue(result);
        }
        assertChecksMatch(sudoku, technique.checks);
    }
}
