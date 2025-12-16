package project.primes.checker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class TrialCheckerTest {
    @ParameterizedTest
    @CsvSource({ "2", "3", "5", "7", "43", "999671" })
    public void isPrimeTrueTest(long value) {
        // act
        boolean isPrime = TrialChecker.isPrime(value);

        // assert
        assertTrue(isPrime);
    }

    @ParameterizedTest
    @CsvSource({ "0", "1", "4", "100", "1200", "1520", "999999678" })
    public void isPrimeFalseTest(long value) {
        // act
        boolean isPrime = TrialChecker.isPrime(value);

        // assert
        assertFalse(isPrime);
    }

    @Test
    public void calculatePrimesTest() {
        // arrange
        List<Long> expected = List.of(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L);
        List<Long> actual = new ArrayList<>();

        // act
        actual = TrialChecker.calculatePrimes(0, 20);

        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }

    @Test
    public void singleRunUpToMaxTest() {
        // arrange
        List<Long> shared = new ArrayList<>();
        long maxNumber = 20;
        AtomicLong nextStart = new AtomicLong(0);
        long blockSize = 7; // arbitrary
        List<Long> expected = Arrays.asList(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L);

        // act
        TrialChecker checker = new TrialChecker(shared, maxNumber, nextStart, blockSize);
        checker.run();

        // assert: should keep chronological order
        assertEquals(expected.size(), shared.size());
        assertEquals(expected, shared);
    }

    @Test
    public void multiRunUpToMaxTest() throws InterruptedException {
        // arrange
        List<Long> shared = new ArrayList<>();
        long maxNumber = 200;
        AtomicLong nextStart = new AtomicLong(0);
        long blockSize = 11;

        // act
        TrialChecker c1 = new TrialChecker(shared, maxNumber, nextStart, blockSize);
        TrialChecker c2 = new TrialChecker(shared, maxNumber, nextStart, blockSize);
        TrialChecker c3 = new TrialChecker(shared, maxNumber, nextStart, blockSize);

        Thread t1 = new Thread(c1);
        Thread t2 = new Thread(c2);
        Thread t3 = new Thread(c3);

        t1.start();
        t2.start();
        t3.start();
        t1.join();
        t2.join();
        t3.join();

        // build expected list
        List<Long> expected = new ArrayList<>();
        expected = TrialChecker.calculatePrimes(0, maxNumber);

        // order in shared may vary because multiple threads append at different times
        List<Long> actual = new ArrayList<>(shared);
        actual.sort(Long::compareTo); // same sort like in ConcurrentRunner

        // assert
        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }

    @Test
    @Disabled // disabled due to instability.
    public void run1MioTimeTest() {
        final long maxNumber = 1_000_000L;
        final long blockSize = 10_000L;
        final int iterations = 3; // against randomness and outliers.

        long bestMs = Long.MAX_VALUE;

        for (int i = 0; i < iterations; i++) {
            List<Long> shared = new ArrayList<>();
            AtomicLong nextStart = new AtomicLong(0);
            TrialChecker checker = new TrialChecker(shared, maxNumber, nextStart, blockSize);

            long timeStartNs = System.nanoTime();
            checker.run();
            long timeEndNs = System.nanoTime();

            long durationMs = (timeEndNs - timeStartNs) / 1_000_000L;
            bestMs = Math.min(bestMs, durationMs);
        }

        // Goal: ~20ms for an upper bound of 1 million. Currently circa 110ms.
        // Using a soft buffer of 10*expected.
        assertTrue(bestMs < 20 * 10);
    }

    @Test
    @Disabled // disabled due to instability.
    public void runSingleVsMultiTimeTest() throws InterruptedException {
        final long maxNumber = 100_000L;
        final long blockSize = 1000;
        final int iterations = 5;
        final int threadCount = 8;

        long bestSingleMs = Long.MAX_VALUE;
        long bestMultiMs = Long.MAX_VALUE;

        // Single-thread timing (best of 5)
        for (int it = 0; it < iterations; it++) {
            List<Long> shared = new ArrayList<>();
            AtomicLong nextStart = new AtomicLong(0);
            TrialChecker single = new TrialChecker(shared, maxNumber, nextStart, blockSize);

            long timeStartNs = System.nanoTime();
            single.run();
            long timeEndNs = System.nanoTime();

            long ms = (timeEndNs - timeStartNs) / 1_000_000L;
            if (ms < bestSingleMs)
                bestSingleMs = ms;
        }

        // Multi-thread timing
        for (int it = 0; it < iterations; it++) {
            List<Long> shared = new ArrayList<>();
            AtomicLong nextStart = new AtomicLong(0);

            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(new TrialChecker(shared, maxNumber, nextStart, blockSize));
            }

            long timeStartNs = System.nanoTime();
            for (Thread t : threads)
                t.start();
            for (Thread t : threads)
                t.join();
            long timeEndNs = System.nanoTime();

            long ms = (timeEndNs - timeStartNs) / 1_000_000L;
            if (ms < bestMultiMs)
                bestMultiMs = ms;
        }

        assertTrue(bestMultiMs < bestSingleMs);
    }

    @Test
    public void run1MioTotalPrimesTest() {
        // arrange
        final long maxNumber = 1_000_000L;

        List<Long> list = new ArrayList<>();

        // act
        list = TrialChecker.calculatePrimes(1, maxNumber);

        // assert
        assertEquals(78_498, list.size());
    }

}
