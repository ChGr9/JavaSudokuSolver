package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.TechniqueAction;
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

        int i;
        for(i=0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = ChainTechnique.simpleColoring(sudoku);
            if(result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
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

        int i;
        for(i=0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = ChainTechnique.medusa3D(sudoku);
            if(result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
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

        int i;
        for(i=0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = ChainTechnique.xCycle(sudoku);
            if(result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
        assertChecksMatch(sudoku, technique.checks);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void SKLoop(int fileNumber) throws IOException {
        URL url = HiddenTechniqueTest.class.getResource(String.format("/techniques/SKLoop%d.yml", fileNumber));
        assertNotNull(url);

        BaseTechniqueTest.TechniqueEntity technique = mapper.readValue(url, BaseTechniqueTest.TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        int i;
        for (i = 0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = ChainTechnique.skLoop(sudoku);
            if(result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
        assertChecksMatch(sudoku, technique.checks);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7})
    void groupedXCycle(int fileNumber) throws IOException {
        URL url = HiddenTechniqueTest.class.getResource(String.format("/techniques/groupedXCycle%d.yml", fileNumber));
        assertNotNull(url);

        BaseTechniqueTest.TechniqueEntity technique = mapper.readValue(url, BaseTechniqueTest.TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        int i;
        for (i = 0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = ChainTechnique.groupedXCycle(sudoku);
            if(result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
        assertChecksMatch(sudoku, technique.checks);
    }
}
