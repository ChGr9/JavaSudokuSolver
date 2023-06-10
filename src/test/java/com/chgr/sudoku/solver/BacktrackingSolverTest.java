package com.chgr.sudoku.solver;

import com.chgr.sudoku.solver.utils.SudokuWithoutUI;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class BacktrackingSolverTest {

    private record SudokuMap (List<List<Integer>> unsolved, List<List<Integer>> solved) {
    }

    private BacktrackingSolver solver;
    private SudokuWithoutUI sudoku;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        sudoku = new SudokuWithoutUI();
        solver = new BacktrackingSolver(sudoku);
        mapper = new ObjectMapper(new YAMLFactory());
    }

    static Stream<Path> sudokuFiles() throws IOException, URISyntaxException {
        URL url = BacktrackingSolverTest.class.getResource("/sudoku");
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
        assertEquals(9, sudokuMap.unsolved.size());
        assertEquals(9, sudokuMap.solved.size());

        for(int i = 0; i<9; i++){
            List<Integer> unsolvedRow = sudokuMap.unsolved.get(i);
            assertEquals(9, unsolvedRow.size());
            for(int j = 0; j<9; j++){
                Integer value = unsolvedRow.get(j);
                if(value != 0){
                    sudoku.getCell(j, i).setValue(value);
                }
            }
        }

        solver.solve();
        assertEquals(sudokuMap.solved, sudoku.toIntList());
    }
}