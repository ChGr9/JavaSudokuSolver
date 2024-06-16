package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.TechniqueAction;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChainTechniqueTest extends BaseTechniqueTest{

    //Disabled test 1, prone to errors due to order of operations
    @ParameterizedTest
    @ValueSource(ints = {2})
    void simpleColoring(int fileNumber) throws IOException {
        URL url = HiddenTechniqueTest.class.getResource(String.format("/techniques/simpleColoring%d.yml", fileNumber));
        assertNotNull(url);

        BaseTechniqueTest.TechniqueEntity technique = mapper.readValue(url, BaseTechniqueTest.TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        for(int i=0; i < technique.repetitions; i++) {
            Optional<TechniqueAction> result = ChainTechnique.simpleColoring(sudoku);
            assertTrue(result.isPresent());
            result.get().apply(sudoku);
        }
        assertChecksMatch(sudoku, technique.checks);
    }

    //Disabled test 4, prone to errors due to order of operations
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void medusa3D(int fileNumber) throws IOException {
        URL url = HiddenTechniqueTest.class.getResource(String.format("/techniques/3DMedusa%d.yml", fileNumber));
        assertNotNull(url);

        BaseTechniqueTest.TechniqueEntity technique = mapper.readValue(url, BaseTechniqueTest.TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        for(int i=0; i < technique.repetitions; i++) {
            Optional<TechniqueAction> result = ChainTechnique.medusa3D(sudoku);
            assertTrue(result.isPresent());
            result.get().apply(sudoku);
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
            Optional<TechniqueAction> result = ChainTechnique.xCycle(sudoku);
            assertTrue(result.isPresent());
            result.get().apply(sudoku);
        }
        assertChecksMatch(sudoku, technique.checks);
    }
}
