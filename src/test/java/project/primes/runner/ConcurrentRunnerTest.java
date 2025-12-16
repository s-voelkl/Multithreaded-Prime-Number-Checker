package project.primes.runner;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ConcurrentRunnerTest {

    @ParameterizedTest
    @CsvSource({
            "-5, 1",
            "0, 1",
            "1, -1",
            "0, 0",
    })
    public void constructorInvalidTest(int nThreads, long upperBound) {
        // Assert
        assertThrows(IllegalArgumentException.class, () -> new ConcurrentRunner(nThreads, upperBound, false));
        assertThrows(IllegalArgumentException.class, () -> new ConcurrentRunner(nThreads, upperBound, true));
    }

    @ParameterizedTest
    @CsvSource({
            "1, 1",
            "1, 2",
            "10, 1",
            "10, 10",
            "3, 10",
    })
    public void constructorValidTest(int nThreads, long upperBound) {
        // Act
        ConcurrentRunner runner1 = new ConcurrentRunner(nThreads, upperBound, false);
        ConcurrentRunner runner2 = new ConcurrentRunner(nThreads, upperBound, true);
        assertEquals(nThreads, runner1.getNThreads());
        assertEquals(nThreads, runner2.getNThreads());
        assertEquals(upperBound, runner1.getUpperBound());
        assertEquals(upperBound, runner2.getUpperBound());
    }

    public void runSingleThreadTrialCheckerTest() {
        // Arrange
        int nThreads = 1;
        long upperBound = 20;
        List<Long> expectedList = Collections.synchronizedList(new ArrayList<>(
                Arrays.asList(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L)));

        ConcurrentRunner runner = new ConcurrentRunner(nThreads, upperBound, false);

        // Act
        runner.run();

        // Assert
        assertEquals(nThreads, runner.getNThreads());
        assertEquals(upperBound, runner.getUpperBound());
        assertEquals(expectedList.size(), runner.getPrimes().size());
        assertEquals(expectedList, runner.getPrimes());
    }

    public void runSingleThreadSieveTest() {
        // Arrange
        int nThreads = 1;
        long upperBound = 20;
        List<Long> expectedList = Collections.synchronizedList(new ArrayList<>(
                Arrays.asList(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L)));

        ConcurrentRunner runner = new ConcurrentRunner(nThreads, upperBound, true);

        // Act
        runner.run();

        // Assert
        assertEquals(nThreads, runner.getNThreads());
        assertEquals(upperBound, runner.getUpperBound());
        assertEquals(expectedList.size(), runner.getPrimes().size());
        assertEquals(expectedList, runner.getPrimes());
    }

    @Test
    public void runTwoThreadsTrialCheckerTest() {
        // Arrange
        int nThreads = 2;
        long upperBound = 20;
        List<Long> expectedList = Collections.synchronizedList(new ArrayList<>(
                Arrays.asList(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L)));

        ConcurrentRunner runner = new ConcurrentRunner(nThreads, upperBound, false);

        // Act
        runner.run();

        // Assert
        assertEquals(nThreads, runner.getNThreads());
        assertEquals(upperBound, runner.getUpperBound());
        assertEquals(expectedList.size(), runner.getPrimes().size());
        assertEquals(expectedList, runner.getPrimes());
    }

    @Test
    public void runTwoThreadsSieveTest() {
        // Arrange
        int nThreads = 2;
        long upperBound = 20;
        List<Long> expectedList = Collections.synchronizedList(new ArrayList<>(
                Arrays.asList(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L)));

        ConcurrentRunner runner = new ConcurrentRunner(nThreads, upperBound, true);

        // Act
        runner.run();

        // Assert
        assertEquals(nThreads, runner.getNThreads());
        assertEquals(upperBound, runner.getUpperBound());
        assertEquals(expectedList.size(), runner.getPrimes().size());
        assertEquals(expectedList, runner.getPrimes());
    }

    @Test
    public void runMultiThreadTrialCheckerTest() {
        // Arrange
        int nThreads = 12;
        long upperBound = 1_000_000;
        ConcurrentRunner runner = new ConcurrentRunner(nThreads, upperBound, false);

        // Act
        runner.run();

        // Assert
        // up to 1.000.000 must be exactly 78.498 prime numbers.
        assertEquals(nThreads, runner.getNThreads());
        assertEquals(upperBound, runner.getUpperBound());
        assertEquals(78_498, runner.getPrimes().size());
    }

    @Test
    public void runMultiThreadSieveTest() {
        // Arrange
        int nThreads = 12;
        long upperBound = 1_000_000;
        ConcurrentRunner runner = new ConcurrentRunner(nThreads, upperBound, true);

        // Act
        runner.run();

        // Assert
        // up to 1.000.000 must be exactly 78.498 prime numbers.
        assertEquals(nThreads, runner.getNThreads());
        assertEquals(upperBound, runner.getUpperBound());
        assertEquals(78_498, runner.getPrimes().size());
    }

    @Test
    public void getTotalDurationMSInvalidTest() {
        // Arrange
        int nThreads = 1;
        long upperBound = 3;
        ConcurrentRunner runner = new ConcurrentRunner(nThreads, upperBound, true);

        // Act & Assert. Call on runner.run(); is missing!
        assertThrows(IllegalStateException.class, () -> runner.getTotalDurationMS());
    }

    @Test
    public void getTotalDurationMSValidTest() {
        // Arrange
        int nThreads = 1;
        long upperBound = 3;
        ConcurrentRunner runner = new ConcurrentRunner(nThreads, upperBound, true);
        runner.run();
        runner.printResults();

        // Act
        long duration = runner.getTotalDurationMS();

        // Assert. Could possibly only need 0ms which is correct.
        assertTrue(duration >= 0);
    }

    @Test
    public void printResultsInvalidTest() {
        // Arrange
        int nThreads = 1;
        long upperBound = 3;
        ConcurrentRunner runner = new ConcurrentRunner(nThreads, upperBound, false);

        // Act & Assert. Call on runner.run(); is missing!
        assertThrows(IllegalStateException.class, () -> runner.printResults());
    }

}
