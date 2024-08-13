package com.chgr.sudoku.solver;

import com.chgr.sudoku.models.SudokuWithoutUI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LogicalSolverTest{

    private record SudokuMap (String unsolved, String solved) {
    }
    private SudokuWithoutUI sudoku;
    private ObjectMapper mapper;
    private static final Pattern PATTERN = Pattern.compile("[0-9]{81}");

    @BeforeEach
    void setUp() {
        sudoku = new SudokuWithoutUI();
        mapper = new ObjectMapper(new YAMLFactory());
    }

    static Stream<Path> sudokuFiles() throws IOException, URISyntaxException {
        URL url = LogicalSolverTest.class.getResource("/sudoku");
        assertNotNull(url);

        Path dir = Path.of(url.toURI());
        return Files.list(dir);
    }

    @ParameterizedTest
    @MethodSource("sudokuFiles")
    public void whenGivenSudokuGridFromFile_thenSolvesCorrectly(Path sudokuPath) throws IOException {
        assertNotNull(sudokuPath);
        assertTrue(Files.exists(sudokuPath));
        SudokuMap sudokuMap = mapper.readValue(sudokuPath.toFile(), SudokuMap.class);
        assertNotNull(sudokuMap);
        assertNotNull(sudokuMap.unsolved);
        assertNotNull(sudokuMap.solved);
        assertTrue(PATTERN.matcher(sudokuMap.unsolved).matches());
        assertTrue(PATTERN.matcher(sudokuMap.solved).matches());

        for(int i = 0; i<9; i++){
            for(int j = 0; j<9; j++){
                int value = Character.getNumericValue(sudokuMap.unsolved.charAt(i*9 + j));
                if(value != 0){
                    sudoku.getCell(j, i).setValue(value);
                }
            }
        }

        LogicalSolver solver = new LogicalSolver(sudoku);

        assertTrue(solver.solve());
        solver.getSteps().forEach(step -> step.apply(sudoku));
        assertEquals(sudokuMap.solved, sudoku.toString());
    }
}