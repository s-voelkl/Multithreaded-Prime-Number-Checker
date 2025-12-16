package project.primes.checker;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class SieveOfEratosthenesTest {

    @Test
    void constructorInvalidTest() {
        List<Long> shared = new ArrayList<>();
        long max = 100;
        AtomicLong next = new AtomicLong(2L);
        long block = 20_000L;

        assertThrows(IllegalArgumentException.class, () -> new SieveOfEratosthenes(shared, max, next, block, null));
    }

    @Test
    void computeBasePrimesUpToSqrt_smallUpperBoundTest() {
        // sqrt(30) = 5.xxx -> base primes: [2, 3, 5]
        List<Long> expected = List.of(2L, 3L, 5L);
        List<Long> actual = SieveOfEratosthenes.computeBasePrimesUpToSqrt(30);
        assertEquals(expected, actual);
    }

    @Test
    void computeBasePrimesUpToSqrt_returnsEmptyTest() {
        assertTrue(SieveOfEratosthenes.computeBasePrimesUpToSqrt(0).isEmpty());
        assertTrue(SieveOfEratosthenes.computeBasePrimesUpToSqrt(1).isEmpty());
        assertTrue(SieveOfEratosthenes.computeBasePrimesUpToSqrt(3).isEmpty());
    }

    @Test
    void initSegmentTest() {
        boolean[] seg = SieveOfEratosthenes.initSegment(5);
        assertEquals(5, seg.length);
        for (boolean b : seg) {
            assertTrue(b);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "10,3,4",
            "9,3,3",
            "0,5,0",
            "1,1,1",
            "7,2,4"
    })
    void ceilDiv_validCases(long a, long b, long expected) {
        long actual = SieveOfEratosthenes.ceilDiv(a, b);
        assertEquals(expected, actual);
    }

    @Test
    void markMultiplesTest() {
        long start = 10;
        long end = 30;
        boolean[] seg = SieveOfEratosthenes.initSegment((int) (end - start + 1));

        // act
        // Mark with p=5 -> ceil(10/5)*5=10, firstMultiple = max(25, 10) = 25
        // This will mark 25 and 30, but not 10,15,20.
        SieveOfEratosthenes.markMultiples(seg, start, end, 5L);

        // assert
        assertTrue(seg[(int) (10 - start)]);
        assertTrue(seg[(int) (15 - start)]);
        assertTrue(seg[(int) (20 - start)]);
        assertFalse(seg[(int) (25 - start)]); // 25 marked
        assertFalse(seg[(int) (30 - start)]); // 30 marked
    }

    @Test
    void markSegmentWithBasePrimesTo30Test() {
        long start = 2;
        long end = 30;
        List<Long> expected = List.of(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L, 23L, 29L);
        boolean[] seg = SieveOfEratosthenes.initSegment((int) (end - start + 1));
        List<Long> base = SieveOfEratosthenes.computeBasePrimesUpToSqrt(end);

        // act
        SieveOfEratosthenes.markSegmentWithBasePrimes(seg, start, end, base);
        List<Long> primes = SieveOfEratosthenes.collectPrimesFromSegment(seg, start);

        assertEquals(expected, primes);
    }

    @Test
    void collectPrimesFromSegmentTest() {
        long start = 20;
        long end = 29;
        List<Long> expected = List.of(23L, 29L);
        boolean[] seg = SieveOfEratosthenes.initSegment((int) (end - start + 1));
        List<Long> base = SieveOfEratosthenes.computeBasePrimesUpToSqrt(end);
        SieveOfEratosthenes.markSegmentWithBasePrimes(seg, start, end, base);

        // act
        List<Long> primes = SieveOfEratosthenes.collectPrimesFromSegment(seg, start);

        assertEquals(expected, primes);
    }

    @Test
    void runSingleThreadTest() {
        List<Long> expected = List.of(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L, 23L, 29L);
        List<Long> shared = new ArrayList<>();
        long max = 30;
        AtomicLong next = new AtomicLong(2L);
        long block = 10;
        List<Long> base = SieveOfEratosthenes.computeBasePrimesUpToSqrt(max);

        SieveOfEratosthenes worker = new SieveOfEratosthenes(shared, max, next, block, base);

        // act
        worker.run();

        assertEquals(expected, shared);
    }

    @Test
    @Disabled // disabled due to instability.
    public void run1MioTimeTest() {
        final long maxNumber = 1_000_000L;
        final long blockSize = 10_000L;
        final int iterations = 3; // against randomness and outliers.

        long bestMs = Long.MAX_VALUE;

        for (int i = 0; i < iterations; i++) {
            long timeStartNs = System.nanoTime();
            List<Long> basePrimes = SieveOfEratosthenes.computeBasePrimesUpToSqrt(maxNumber);
            long timeEndNs = System.nanoTime();
            long initDuration = timeEndNs - timeStartNs;

            List<Long> shared = new ArrayList<>();
            AtomicLong nextStart = new AtomicLong(0);
            SieveOfEratosthenes checker = new SieveOfEratosthenes(shared, maxNumber, nextStart, blockSize, basePrimes);

            timeStartNs = System.nanoTime();
            checker.run();
            timeEndNs = System.nanoTime();

            long durationMs = (timeEndNs - timeStartNs + initDuration) / 1_000_000L;
            bestMs = Math.min(bestMs, durationMs);
        }

        // Goal: ~20ms for an upper bound of 1 million. Currently circa 110ms.
        // Using a soft buffer of 10*expected.
        assertTrue(bestMs < 20 * 10);
    }

    @Test
    @Disabled // disabled due to instability.
    public void runSingleVsMultiTimeTest() throws InterruptedException {
        final long maxNumber = 500_000_000L;
        final long blockSize = 25_000L;
        final int iterations = 3;
        final int threadCount = 24;

        long bestSingleMs = Long.MAX_VALUE;
        long bestMultiMs = Long.MAX_VALUE;

        // Single-thread timing (best of 5)
        for (int it = 0; it < iterations; it++) {
            long timeStartNs = System.nanoTime();
            List<Long> basePrimes = SieveOfEratosthenes.computeBasePrimesUpToSqrt(maxNumber);
            long timeEndNs = System.nanoTime();
            long initDuration = timeEndNs - timeStartNs;

            List<Long> shared = new ArrayList<>();
            AtomicLong nextStart = new AtomicLong(0);
            SieveOfEratosthenes single = new SieveOfEratosthenes(shared, maxNumber, nextStart, blockSize, basePrimes);

            timeStartNs = System.nanoTime();
            single.run();
            timeEndNs = System.nanoTime();

            long ms = (timeEndNs - timeStartNs + initDuration) / 1_000_000L;
            if (ms < bestSingleMs)
                bestSingleMs = ms;
        }

        // Multi-thread timing
        for (int it = 0; it < iterations; it++) {
            long timeStartNs = System.nanoTime();
            List<Long> basePrimes = SieveOfEratosthenes.computeBasePrimesUpToSqrt(maxNumber);
            long timeEndNs = System.nanoTime();
            long initDuration = timeEndNs - timeStartNs;

            List<Long> shared = new ArrayList<>();
            AtomicLong nextStart = new AtomicLong(0);

            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(new SieveOfEratosthenes(shared, maxNumber, nextStart, blockSize, basePrimes));
            }

            timeStartNs = System.nanoTime();
            for (Thread t : threads)
                t.start();
            for (Thread t : threads)
                t.join();
            timeEndNs = System.nanoTime();

            long ms = (timeEndNs - timeStartNs + initDuration) / 1_000_000L;
            if (ms < bestMultiMs)
                bestMultiMs = ms;
        }

        assertTrue(bestMultiMs < bestSingleMs);
    }
}
