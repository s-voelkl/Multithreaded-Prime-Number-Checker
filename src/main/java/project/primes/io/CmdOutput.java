/**
 * Utility class for formatting and printing command-line output related to prime number calculations.
 * This class provides static methods for displaying prime numbers, statistics, and computation information.
 */

package project.primes.io;

import java.util.List;
import java.util.StringJoiner;

public abstract class CmdOutput {
    /**
     * Joins a list of elements into a formatted string representation.
     * Numbers are formatted with thousand separators and enclosed in square
     * brackets.
     *
     * @param <T>         the type of elements in the list
     * @param list        the list of elements to join
     * @param emptySymbol the symbol to use for null elements
     * @return a string representation of the list with elements separated by commas
     *         and enclosed in brackets
     */
    static <T> String joinList(List<T> list, String emptySymbol) {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");

        for (T t : list) {
            String strRep = t == null ? emptySymbol : String.format("%,d", t);
            joiner.add(strRep);
        }

        return joiner.toString();
    }

    /**
     * Prints a reduced view of prime numbers, showing only the smallest and largest
     * sections.
     * The output is limited to the first and last elements up to the specified
     * section width.
     * If the list is smaller than twice the section width, the sections may
     * overlap.
     *
     * @param <T>          the type of elements in the prime numbers list
     * @param primeNumbers the sorted list of prime numbers to display
     * @param sectionWidth the maximum number of elements to show in each section
     * @throws IllegalArgumentException if sectionWidth is less than or equal to 0
     */
    public static <T> void printReducedPrimes(List<T> primeNumbers, int sectionWidth) {
        if (sectionWidth <= 0) {
            throw new IllegalArgumentException("A section width of at leat 1 is expected");
        }

        if (primeNumbers == null || primeNumbers.isEmpty()) {
            System.out.println("No prime numbers were found.");
            return;
        }

        final int total = primeNumbers.size();
        final int firstCount = Math.min(sectionWidth, total);
        final int lastCount = Math.min(sectionWidth, total);

        // First (smallest) section: [0, firstCount)
        final int firstEndExclusive = firstCount;

        // Last (largest) section: [start, total)
        final int lastStartInclusive = Math.max(0, total - lastCount);

        if (lastStartInclusive < firstEndExclusive) {
            // if total <= 2*sectionWidth, there might be printed overlapping ranges.
            System.out.println("(Note: smallest and largest sections may overlap)");
        }

        System.out.print("Smallest " + firstCount + " prime numbers: ");
        System.out.println(joinList(primeNumbers.subList(0, firstEndExclusive), "?"));

        System.out.print("Largest " + lastCount + " prime numbers: ");
        System.out.println(joinList(primeNumbers.subList(lastStartInclusive, total), "?"));
    }

    /**
     * Prints the total count of prime numbers found.
     * The count is formatted with thousand separators.
     *
     * @param <T>          the type of elements in the prime numbers list
     * @param primeNumbers the list of prime numbers whose count will be printed
     */
    public static <T> void printTotalPrimeCount(List<T> primeNumbers) {
        // final keyword source: 5.
        final int count = (primeNumbers == null) ? 0 : primeNumbers.size();
        String strRep = String.format("%,d", count);
        System.out.println("Total prime numbers found: " + strRep);
    }

    /**
     * Prints the total duration of the computation in milliseconds.
     * The duration is formatted with thousand separators.
     *
     * @param durationMS the computation duration in milliseconds
     */
    public static void printTotalDuration(long durationMS) {
        String strRep = String.format("%,d", durationMS);

        System.out.println("Total computation time: " + strRep + " ms");
    }

    /**
     * Prints information about the prime number search execution.
     * Displays the upper bound for the search, total number of threads used,
     * and the number of interrupted versus successful threads.
     *
     * @param nThreads            the total number of threads used
     * @param nInterruptedThreads the number of threads that were interrupted or
     *                            failed
     * @param upperBound          the upper limit for the prime number search
     */
    public static void printRunnerInformation(int nThreads, int nInterruptedThreads, long upperBound, long blockSize) {
        String maxStr = String.format("%,d", upperBound);
        String blockSizeStr = String.format("%,d", blockSize);
        System.out.println("Upper bound for prime search: " + maxStr);
        System.out.println("Total number of threads: " + nThreads + " (" + nInterruptedThreads + "/"
                + (nThreads - nInterruptedThreads) + " failed)");
        System.out.println("Dynamic block size chosen: " + blockSizeStr + "\n");
    }

}