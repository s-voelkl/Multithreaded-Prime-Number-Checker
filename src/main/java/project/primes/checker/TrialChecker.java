/**
 * A prime number checker implementation that uses the trial division algorithm.
 * This class extends PrimeChecker and processes numbers in blocks, checking each
 * candidate by dividing it by all numbers up to its square root. Results are
 * collected in a local buffer before being synchronized with the shared list
 * to minimize thread contention.
 * 
 * @see PrimeChecker
 */
package project.primes.checker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class TrialChecker extends PrimeChecker {

    /**
     * Constructs a new TrialChecker with the specified parameters.
     *
     * @param sharedList the shared list where all threads store their results
     * @param maxNumber  the maximum number up to which primes should be found
     * @param nextStart  atomic counter for the next block start position
     * @param blockSize  the size of each block to be processed by a thread
     */
    public TrialChecker(List<Long> sharedList, long maxNumber, AtomicLong nextStart, long blockSize) {
        super(sharedList, maxNumber, nextStart, blockSize);
    }

    /**
     * Calculates prime numbers in blocks using trial division.
     * This method processes numbers in blocks, using a local buffer to collect
     * primes before synchronizing with the shared list. It continuously fetches
     * new blocks until all numbers up to maxNumber have been processed.
     */
    @Override
    public void run() {
        // local buffer for less thread syncing. Else, after each found prime number a
        // synchronization process would have to be started, significantly slowing down
        // the prime number search.
        // A large ArrayList size (as initialized with) may reserve a lot of storage,
        // but doesn't need to be reinitialized that often. See ReadMe for the
        // discussion.
        List<Long> localList = new ArrayList<>((int) blockSize);

        while (true) {
            long start = nextStart.getAndAdd(blockSize);
            if (start > maxNumber)
                break;

            // maximum index must not be greater than the maxNumber
            long end = Math.min(maxNumber, start + blockSize - 1);
            localList.addAll(calculatePrimes(start, end));
        }

        synchronized (sharedList) {
            sharedList.addAll(localList);
        }
    }

    /**
     * Checks whether a given number is prime using trial division.
     * Tests divisibility by all numbers from 2 up to the square root of the number.
     * Numbers less than 2 are not considered prime.
     * 
     * @param number the number to check for primality
     * @return true if the number is prime, false otherwise
     */
    static boolean isPrime(long number) {
        // could be made more efficient by not using Math.sqrt but instead using
        // the multiplication of divisors.
        // also checks all even numbers yet. Benchmark test source: 13.
        if (number < 2) {
            return false;
        }
        for (long i = 2; i <= Math.sqrt(number); i++) {
            if (number % i == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates all prime numbers within the specified range using trial division.
     * The method checks each number in the range [start, end] for primality.
     * If the range is invalid (end less than 2 or start greater than end), an empty
     * list is returned.
     * 
     * @param start the starting number of the range (inclusive)
     * @param end   the ending number of the range (inclusive)
     * @return a list containing all prime numbers found in the specified range
     */
    public static List<Long> calculatePrimes(long start, long end) {
        List<Long> list = new ArrayList<>();

        // checking validity of inputs
        if (end < 2 || start > end) {
            return list;
        }

        // starting from 2, checking all primes.
        for (long i = Math.max(2, start); i <= end; i++) {
            if (isPrime(i)) {
                list.add(i);
            }
        }

        return list;
    }

}
