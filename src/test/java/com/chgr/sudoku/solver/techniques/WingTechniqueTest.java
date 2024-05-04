package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.TechniqueAction;
import org.junit.jupiter.api.RepeatedTest;
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

        for(int i=0; i < technique.repetitions; i++) {
            Optional<TechniqueAction> result = WingTechnique.xWing(sudoku);
            assertTrue(result.isPresent());
            result.get().apply(sudoku);
        }
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

        for(int i=0; i < technique.repetitions; i++) {
            boolean result = WingTechnique.swordfish(sudoku);
            assertTrue(result);
        }
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

        for(int i=0; i < technique.repetitions; i++) {
            boolean result = WingTechnique.jellyfish(sudoku);
            assertTrue(result);
        }
        assertChecksMatch(sudoku, technique.checks);
    }

    @RepeatedTest(100)
    void jellyfishtest() throws IOException {
        int fileNumber = 4;
        URL url = WingTechniqueTest.class.getResource(String.format("/techniques/jellyfish%d.yml", fileNumber));
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        for(int i=0; i < technique.repetitions; i++) {
            boolean result = WingTechnique.jellyfish(sudoku);
            assertTrue(result);
        }
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

        for(int i=0; i < technique.repetitions; i++) {
            Optional<TechniqueAction> result = WingTechnique.yWing(sudoku);
            assertTrue(result.isPresent());
            result.get().apply(sudoku);
        }
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

        for(int i=0; i < technique.repetitions; i++) {
            boolean result = WingTechnique.xyzWing(sudoku);
            assertTrue(result);
        }
        assertChecksMatch(sudoku, technique.checks);
    }
}