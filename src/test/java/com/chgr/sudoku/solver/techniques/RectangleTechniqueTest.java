package com.chgr.sudoku.solver.techniques;

import com.chgr.sudoku.models.TechniqueAction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RectangleTechniqueTest extends BaseTechniqueTest {

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
            Optional<TechniqueAction> result = RectangleTechnique.uniqueRectangle(sudoku);
            assertTrue(result.isPresent());
            result.get().apply(sudoku);
        }
        assertChecksMatch(sudoku, technique.checks);
    }

    @ValueSource(ints = {1, 2})
    @ParameterizedTest
    void rectangleElimination(int fileNumber) throws IOException {
        URL url = IntersectionTechniqueTest.class.getResource(String.format("/techniques/rectangleElimination%d.yml", fileNumber));
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        for (int i = 0; i < technique.repetitions; i++) {
            Optional<TechniqueAction> result = RectangleTechnique.rectangleElimination(sudoku);
            assertTrue(result.isPresent());
            result.get().apply(sudoku);
        }
        assertChecksMatch(sudoku, technique.checks);
    }

    @ValueSource(ints = {1, 2, 3, 4})
    @ParameterizedTest
    void extendedUniqueRectangle(int fileNumber) throws IOException {
        URL url = IntersectionTechniqueTest.class.getResource(String.format("/techniques/extendedUniqueRectangle%d.yml", fileNumber));
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        for (int i = 0; i < technique.repetitions; i++) {
            Optional<TechniqueAction> result = RectangleTechnique.extendedUniqueRectangle(sudoku);
            assertTrue(result.isPresent());
            result.get().apply(sudoku);
        }
        assertChecksMatch(sudoku, technique.checks);
    }

    @ValueSource(ints = {1, 2, 3, 4})
    @ParameterizedTest
    void hiddenUniqueRectangle(int fileNumber) throws IOException {
        URL url = IntersectionTechniqueTest.class.getResource(String.format("/techniques/hiddenUniqueRectangle%d.yml", fileNumber));
        assertNotNull(url);

        TechniqueEntity technique = mapper.readValue(url, TechniqueEntity.class);
        assertNotNull(technique);
        assertNotNull(technique.grid);

        loadSudoku(technique);

        for (int i = 0; i < technique.repetitions; i++) {
            Optional<TechniqueAction> result = RectangleTechnique.hiddenUniqueRectangle(sudoku);
            assertTrue(result.isPresent());
            result.get().apply(sudoku);
        }
        assertChecksMatch(sudoku, technique.checks);
    }
}