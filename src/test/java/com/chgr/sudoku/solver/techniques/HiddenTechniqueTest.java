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

        int i;
        for (i = 0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = HiddenTechnique.hiddenSingle(sudoku);
            if (result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
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

        int i;
        for (i = 0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = HiddenTechnique.hiddenPair(sudoku);
            if (result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
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

        int i;
        for (i = 0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = HiddenTechnique.hiddenTriple(sudoku);
            if (result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
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

        int i;
        for (i = 0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = HiddenTechnique.hiddenQuad(sudoku);
            if (result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
        assertChecksMatch(sudoku, technique.checks);
    }
}
