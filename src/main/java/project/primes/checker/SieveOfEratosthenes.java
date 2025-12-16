package project.primes.checker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Segmented Sieve of Eratosthenes worker.
 *
 * This worker repeatedly claims disjoint segments of the global range
 * [2..maxNumber] using an atomic counter and marks composites in its segment
 * using a shared list of base primes less than or equal to
 * floor(sqrt(maxNumber)). Primes discovered in each segment are collected and
 * appended to a shared list.
 * Instances are intended to be executed by threads to parallelize the segmented
 * sieve.
 *
 * @see #computeBasePrimesUpToSqrt(long)
 */
public class SieveOfEratosthenes extends PrimeChecker {
    private final List<Long> basePrimes;

    /**
     * Constructs a segmented-sieve worker.
     *
     * The worker pulls its next segment start from nextStart, advances it by
     * blockSize, and sieves the interval [start, min(start + blockSize - 1,
     * maxNumber)] by marking multiples of the provided base primes. Results for
     * each segment are appended to the shared list.
     *
     * @param sharedList thread-safe list that receives primes discovered in this
     *                   worker's segments
     * @param maxNumber  inclusive global upper bound n of the sieve range [2..n]
     * @param nextStart  atomic counter that yields the next segment start;
     *                   incremented by blockSize per claim
     * @param blockSize  segment size Δ; must be less than or equal to
     *                   Integer.MAX_VALUE for safe boolean[] indexing
     * @param basePrimes primes less than or equal to floor(sqrt(maxNumber)) to mark
     *                   composites in all segments
     * @throws IllegalArgumentException if basePrimes is null
     */

    public SieveOfEratosthenes(List<Long> sharedList, long maxNumber, AtomicLong nextStart, long blockSize,
            List<Long> basePrimes) {
        super(sharedList, maxNumber, nextStart, blockSize);

        if (basePrimes == null) {
            throw new IllegalArgumentException("The base primes must not be null for segmented sieve.");
        }
        this.basePrimes = basePrimes;
    }

    /**
     * Executes the segmented sieve for one or more segments.
     *
     * Repeatedly claims disjoint segments from the global range using the atomic
     * nextStart counter. For each segment, initializes a boolean array, marks
     * composites using the base primes, collects primes, and appends them to the
     * shared list. Terminates when the claimed segment start exceeds maxNumber.
     *
     * @see #initSegment(int)
     * @see #markSegmentWithBasePrimes(boolean[], long, long, List)
     * @see #collectPrimesFromSegment(boolean[], long)
     */
    @Override
    public void run() {
        // Inspiration from source 16, 17, 18.
        // Phase 2 of segmented sieve: Each worker repeatedly takes the next [start,
        // end] block and marks multiples of base primes inside that block.

        while (true) {
            // Run the next block if in bounds.
            long start = nextStart.getAndAdd(blockSize);
            if (start > maxNumber) {
                return;
            }

            long end = Math.min(start + blockSize - 1L, maxNumber);
            long span = end - start + 1L;
            if (span <= 0) {
                return;
            }

            if (span > Integer.MAX_VALUE) {
                span = Integer.MAX_VALUE;
            }

            boolean[] isPrime = initSegment((int) span);

            // Mark multiples for each base prime p in the range [start, end].
            // We only need multiples; primes > sqrt(end) won't have multiples here.
            markSegmentWithBasePrimes(isPrime, start, end, basePrimes);

            // Collect primes from this segment.
            List<Long> local = collectPrimesFromSegment(isPrime, start);

            // Append to shared list in one atomic step to reduce contention.
            if (!local.isEmpty()) {
                // this synchronized block is not really needed for synchronizedList, still keep
                // in case of other Collection types.
                synchronized (sharedList) {
                    sharedList.addAll(local);
                }
            }
        }
    }

    /**
     * Initializes a new boolean segment for primality candidates.
     *
     * All entries are set to true initially. Subsequent marking operations will set
     * composite positions to false.
     *
     * @param len the length of the segment; must be at least 1 and at most
     *            Integer.MAX_VALUE
     * @return a new boolean array of length len with all values initialized to true
     */
    static boolean[] initSegment(int len) {
        boolean[] isPrime = new boolean[len];
        Arrays.fill(isPrime, true);
        return isPrime;
    }

    /**
     * Marks composite numbers within the segment [start, end] using the provided
     * base primes.
     *
     * For each base prime p, this method delegates to
     * {@link #markMultiples(boolean[], long, long, long)}
     * to mark all multiples of p inside the current segment. If the segment
     * includes 0 or 1, they are explicitly marked non-prime.
     *
     * @param isPrime    boolean array representing the current segment; true
     *                   indicates a prime candidate
     * @param start      the inclusive start value of the segment
     * @param end        the inclusive end value of the segment
     * @param basePrimes primes less than or equal to floor(sqrt(maxNumber)) used to
     *                   mark composites
     */
    static void markSegmentWithBasePrimes(boolean[] isPrime,
            long start,
            long end,
            List<Long> basePrimes) {
        for (long p : basePrimes) {
            // If p > end, no multiples of p exist in this segment.
            if (p > end) {
                break;
            }

            markMultiples(isPrime, start, end, p);
        }

        // If start <= 1, explicitly clear 0 and/or 1.
        // This is harmless even if your driver always starts at 2.
        if (start <= 1) {
            int zeroIdx = (int) (0 - start); // may be negative
            int oneIdx = (int) (1 - start);
            if (zeroIdx >= 0 && zeroIdx < isPrime.length)
                isPrime[zeroIdx] = false;
            if (oneIdx >= 0 && oneIdx < isPrime.length)
                isPrime[oneIdx] = false;
        }
    }

    /**
     * Computes the ceiling of a divided by b for positive a and b.
     *
     * Examples:
     * ceilDiv(10, 3) == 4
     * ceilDiv(9, 3) == 3
     *
     * @param a dividend; expected to be non-negative in this usage
     * @param b divisor; must be positive
     * @return the smallest integer q such that q * b >= a
     * @throws ArithmeticException if b is zero
     */
    static long ceilDiv(long a, long b) {
        long q = a / b;
        long r = a % b;
        return (r == 0) ? q : (q + 1);
    }

    /**
     * Marks multiples of a single base prime p within the segment [start, end].
     *
     * The first multiple to mark is computed as: max(p*p, ceil(start / p) * p).
     * Each multiple m from firstMultiple to end in steps of p is set to false
     * in the segment array.
     *
     * @param isPrime boolean array for the current segment; composites are set to
     *                false
     * @param start   inclusive start of the current segment
     * @param end     inclusive end of the current segment
     * @param p       base prime whose multiples will be marked as composite
     * @see #ceilDiv(long, long)
     */
    static void markMultiples(boolean[] isPrime, long start, long end, long p) {
        long p2 = p * p;

        // First multiple of p in the current segment: max(p*p, ceil(start/p)*p)
        long firstMultiple = Math.max(p2, ceilDiv(start, p) * p);
        if (firstMultiple > end) {
            return;
        }

        // Mark multiples m = firstMultiple, firstMultiple + p, ...
        for (long m = firstMultiple; m <= end; m += p) {
            int idx = (int) (m - start);
            isPrime[idx] = false;
        }
    }

    /**
     * Collects all primes indicated by the segment array.
     *
     * For each index i with isPrime[i] equal to true, the number start + i is added
     * to the returned list.
     *
     * @param isPrime boolean array for the current segment; true indicates a prime
     *                candidate
     * @param start   inclusive start of the current segment
     * @return list of primes found in the current segment; may be empty
     */

    static List<Long> collectPrimesFromSegment(boolean[] isPrime, long start) {
        List<Long> local = new ArrayList<>();
        for (int i = 0; i < isPrime.length; i++) {
            if (isPrime[i]) {
                local.add(start + i);
            }
        }
        return local;
    }

    /**
     * Computes the base primes up to floor(sqrt(upperBound)) using a regular sieve.
     *
     * Wikipedia segmented-sieve guidance:
     * 1) Divide 2..n into segments of size delta greater than or equal to sqrt(n).
     * 2) Find the primes in the first segment using the regular sieve.
     * 3) Use these base primes to mark multiples in subsequent segments.
     * 4) The unmarked positions in each segment are the primes of that segment.
     *
     * This method implements step 2 for the lowest segment and returns the base
     * primes that workers will use to mark composites in their segments.
     * Source 16, 17, and especially 18.
     *
     * @param upperBound inclusive global upper bound n; base primes are returned up
     *                   to floor(sqrt(n))
     * @return unmodifiable list of base primes less than or equal to
     *         floor(sqrt(upperBound));
     *         returns an empty list if floor(sqrt(upperBound)) is less than 2
     */
    public static List<Long> computeBasePrimesUpToSqrt(long upperBound) {

        long limitLong = (long) Math.floor(Math.sqrt(Math.max(upperBound, 0L)));
        if (limitLong < 2L) {
            return List.of();
        }
        int limit = (int) limitLong;

        // Regular sieve
        boolean[] isPrime = new boolean[limit + 1];
        Arrays.fill(isPrime, true);
        isPrime[0] = false;
        isPrime[1] = false;

        // Sieve core: mark multiples starting at p*p.
        for (int p = 2; (long) p * p <= limit; p++) {
            if (isPrime[p]) {
                for (int m = p * p; m <= limit; m += p) {
                    isPrime[m] = false;
                }
            }
        }

        // Collect base primes <= sqrt(upperBound) (the "lowest segment" primes).
        List<Long> basePrimes = new ArrayList<>();
        for (int v = 2; v <= limit; v++) {
            if (isPrime[v]) {
                basePrimes.add((long) v);
            }
        }

        // Unmodifiable to avoid accidental mutation by workers or outside calls.
        return java.util.Collections.unmodifiableList(basePrimes);
    }

}
