package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.TechniqueAction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntersectionTechniqueTest extends BaseTechniqueTest {
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void pointingTuple(int fileNumber) throws IOException {
        URL url = IntersectionTechniqueTest.class.getResource(String.format("/techniques/pointingTuple%d.yml", fileNumber));
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        int i;
        for (i = 0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = IntersectionTechnique.pointingTuple(sudoku);
            if (result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
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

        loadSudoku(technique);

        int i;
        for (i = 0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = IntersectionTechnique.boxLineReduction(sudoku);
            if (result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
        assertChecksMatch(sudoku, technique.checks);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void firework(int fileNumber) throws IOException {
        URL url = IntersectionTechniqueTest.class.getResource(String.format("/techniques/firework%d.yml", fileNumber));
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        int i;
        for (i = 0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = IntersectionTechnique.firework(sudoku);
            if (result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
        assertChecksMatch(sudoku, technique.checks);
    }
}