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

        int i;
        for (i = 0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = WingTechnique.xWing(sudoku);
            if (result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
        assertChecksMatch(sudoku, technique.checks);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void swordfish(int fileNumber) throws IOException {
        URL url = WingTechniqueTest.class.getResource(String.format("/techniques/swordfish%d.yml", fileNumber));
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        int i;
        for (i = 0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = WingTechnique.swordfish(sudoku);
            if (result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
        assertChecksMatch(sudoku, technique.checks);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void jellyfish(int fileNumber) throws IOException {
        URL url = WingTechniqueTest.class.getResource(String.format("/techniques/jellyfish%d.yml", fileNumber));
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        int i;
        for (i = 0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = WingTechnique.jellyfish(sudoku);
            if (result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
        assertChecksMatch(sudoku, technique.checks);
    }


    @Test
    void yWing() throws Exception {
        URL url = WingTechniqueTest.class.getResource("/techniques/yWing.yml");
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        int i;
        for (i = 0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = WingTechnique.yWing(sudoku);
            if (result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
        assertChecksMatch(sudoku, technique.checks);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void xyzWing(int fileNumber) throws Exception {
        URL url = WingTechniqueTest.class.getResource(String.format("/techniques/xyzWing%d.yml", fileNumber));
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        int i;
        for (i = 0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = WingTechnique.xyzWing(sudoku);
            if (result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
        assertChecksMatch(sudoku, technique.checks);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "2", "3", "4a", "4b"})
    void wxyzWing(String filenames) throws Exception {
        URL url = WingTechniqueTest.class.getResource(String.format("/techniques/wxyzWing%s.yml", filenames));
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        int i;
        for (i = 0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = WingTechnique.wxyzWing(sudoku);
            if (result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
        assertChecksMatch(sudoku, technique.checks);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void finnedXWing(int fileNumber) throws Exception {
        URL url = WingTechniqueTest.class.getResource(String.format("/techniques/finnedXWing%d.yml", fileNumber));
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        int i;
        for (i = 0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = WingTechnique.finnedXWing(sudoku);
            if (result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
        assertChecksMatch(sudoku, technique.checks);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void finnedSwordfish(int fileNumber) throws Exception {
        URL url = WingTechniqueTest.class.getResource(String.format("/techniques/finnedSwordfish%d.yml", fileNumber));
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        int i;
        for (i = 0; i < technique.maxRepetitions; i++) {
            Optional<TechniqueAction> result = WingTechnique.finnedSwordfish(sudoku);
            if (result.isEmpty())
                break;
            result.get().apply(sudoku);
        }
        assertTrue(technique.repetitions.contains(i));
        assertChecksMatch(sudoku, technique.checks);
    }
}