/**
 * Concurrent runner for prime number computation using multiple threads.
 * 
 * This class manages the parallel execution of prime number finding algorithms,
 * either using the Sieve of Eratosthenes or Trial Division method. It creates
 * and coordinates multiple worker threads that process number ranges concurrently
 * using dynamic block allocation for load balancing.
 * 
 * The class handles thread synchronization, result collection, and performance
 * measurement. Results are stored in a thread-safe synchronized list.
 */
package project.primes.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import project.primes.checker.SieveOfEratosthenes;
import project.primes.checker.TrialChecker;
import project.primes.io.CmdInput;
import project.primes.io.CmdOutput;

public class ConcurrentRunner {
    /**
     * Data structure: Collections.synchronizedList, with generic typing, but using
     * long here.
     * Problem with CopyOnWriteArrayList: higher memory usage and write overhead.
     * Problem with object lock: could forget this accidently.
     * Using synchronizedList instead. Sources 2, 3, 4.
     */
    protected List<Long> primes;
    private int nInterruptedThreads;
    private int nThreads;

    private List<Long> basePrimes;
    private boolean useSieveOfEratosthenes;
    private long upperBound;
    private long durationInitialization;

    private Thread[] threads;
    private long timeStartNs;
    private long timeEndNs;

    /**
     * Using dynamic blocks for load balancing for TrialChecker. Sources: 14, 15,
     * 19. Large chunk size for each thread so they are lock-free, with concurrent
     * scheduling. The value of the AtomicLong is taken and increased by the
     * blockSize for each Thread until the primes were found. It is also dynamically
     * fitted for the Sieve of Erathosthenes. The blockSize is the maximum of 20.000
     * and sqrt(n) but not greater than the Integer Maximum.
     */
    private long dynamicBlockSize;

    /**
     * Thread-safe range allocation. Steadily increased by blockSize.
     * Operations on the AtomicLong are thread-safe without the need of explicit
     * synchronization (user-friendly and safe!).
     */
    private final AtomicLong nextStart = new AtomicLong(2L);

    /**
     * Constructs a ConcurrentRunner with specified parameters for prime number
     * computation.
     * 
     * This constructor initializes the runner with the given number of threads and
     * upper bound,
     * and selects the prime-checking algorithm. It creates worker threads and
     * optionally
     * precomputes base primes for the Sieve of Eratosthenes algorithm.
     * 
     * The dynamic block size is automatically calculated as the maximum of 10,000
     * and the
     * square root of the upper bound, but capped at Integer.MAX_VALUE for load
     * balancing.
     * 
     * @param nThreads               the number of worker threads to use for
     *                               parallel computation (must be at least 1)
     * @param upperBound             the highest number to check for primality (must
     *                               be at least 1)
     * @param useSieveOfEratosthenes true to use Sieve of Eratosthenes algorithm,
     *                               false for Trial Division
     * @throws IllegalArgumentException if nThreads is less than 1
     * @throws IllegalArgumentException if upperBound is less than or equal to 0
     */
    public ConcurrentRunner(int nThreads, long upperBound, Boolean useSieveOfEratosthenes) {
        // Notice: It is explicitly allowed to insert much higher thread numbers than
        // the actual upper bound number for the prime search, as users might want to
        // test multiple scenarios. E.g. nThreads=100 and upperBound=10 is allowed!
        if (nThreads < 1) {
            throw new IllegalArgumentException("Count of threads must be at least 1.");
        }
        if (upperBound <= 0) {
            throw new IllegalArgumentException("The highest number must be at least 1.");
        }

        this.nThreads = nThreads;
        this.threads = new Thread[nThreads];
        this.nInterruptedThreads = 0;
        this.upperBound = upperBound;
        this.dynamicBlockSize = Math.min(Math.max(10000L, (long) Math.sqrt(upperBound)), Integer.MAX_VALUE);

        this.primes = Collections.synchronizedList(new ArrayList<>());
        this.basePrimes = new ArrayList<>();

        this.useSieveOfEratosthenes = useSieveOfEratosthenes;
        this.durationInitialization = 0;

        if (this.useSieveOfEratosthenes) {
            long initializationStart = System.nanoTime();
            basePrimes = SieveOfEratosthenes.computeBasePrimesUpToSqrt(upperBound);
            this.durationInitialization = System.nanoTime() - initializationStart;
        }

        // creating threads
        try {
            for (int i = 0; i < nThreads; i++) {
                if (useSieveOfEratosthenes) {
                    threads[i] = new Thread(
                            new SieveOfEratosthenes(primes, upperBound, nextStart, dynamicBlockSize, basePrimes));
                } else {
                    threads[i] = new Thread(new TrialChecker(primes, upperBound, nextStart, dynamicBlockSize));
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Constructs a ConcurrentRunner with default parameters obtained through
     * interactive user input.
     * 
     * This constructor prompts the user for the number of threads, upper bound for
     * prime search,
     * and the prime-checking algorithm to use (Sieve of Eratosthenes or Trial
     * Division).
     * It then delegates to the main constructor with the collected parameters.
     * 
     * The user is interactively prompted for:
     * - Thread count (number of worker threads)
     * - Upper bound (highest number to check for primality)
     * - Prime checker algorithm selection
     * 
     * @see #ConcurrentRunner(int, long, Boolean)
     */
    public ConcurrentRunner() {
        // interactive constructor with user input, calling specific constructor.
        this(CmdInput.getThreadCount(), CmdInput.getUpperBound(), CmdInput.getPrimeCheckerAlgorithm());
    }

    /**
     * Executes the concurrent prime number computation using the configured worker
     * threads.
     * 
     * This method starts all worker threads to find prime numbers in parallel,
     * waits for their
     * completion, and sorts the results in ascending order. Each thread processes
     * number ranges
     * using dynamic block allocation until all numbers up to the upper bound are
     * checked.
     * 
     * If a thread is interrupted during execution, the interruption is handled
     * gracefully,
     * the interrupted thread count is incremented, and an error message is printed.
     * The current thread's interrupt status is preserved.
     * 
     * @throws IllegalStateException if time measurements are invalid after
     *                               execution
     */
    public void run() {
        // starting the Runnable.run() method -> starting execution.
        this.timeStartNs = System.nanoTime();

        for (int i = 0; i < this.nThreads; i++) {
            threads[i].start();
        }

        // ... - other possible operations. system.outs are deprecated as of time
        // measurements.

        // ending the threads with time measurement.
        for (int i = 0; i < this.nThreads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                this.nInterruptedThreads++;
                System.err.println("Thread with number " + i + " was interruped.");
                ;
            }
        }
        this.timeEndNs = System.nanoTime();
        // this.removeDuplicates(); // not needed, as partitioning works perfectly fine!
        this.sortPrimesAsc();
    }

    /**
     * Prints the results of the prime number computation to the console.
     * 
     * This method outputs information about the execution including thread count,
     * interrupted threads,
     * upper bound, a sample of found primes (first 20), total prime count, and
     * total execution duration.
     * The output is formatted for readability and includes a blank line separator
     * at the end.
     * 
     * @throws IllegalStateException if time measurements are not available or
     *                               invalid
     */
    public void printResults() {
        long duration = 0;
        try {
            duration = this.getTotalDurationMS();
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e.getMessage());
        }

        CmdOutput.printRunnerInformation(nThreads, nInterruptedThreads, upperBound, dynamicBlockSize);
        CmdOutput.printReducedPrimes(this.primes, 20);
        CmdOutput.printTotalPrimeCount(primes);
        CmdOutput.printTotalDuration(duration);
        System.out.println();
    }

    /**
     * Calculates the total duration of the prime computation in milliseconds.
     * 
     * This method computes the elapsed time from the start to the end of execution,
     * including any initialization time (e.g., for precomputing base primes in
     * Sieve of Eratosthenes).
     * The result is converted from nanoseconds to milliseconds.
     * 
     * @return the total duration of the computation in milliseconds
     * @throws IllegalStateException if time measurements are not available (both
     *                               start and end are zero)
     * @throws IllegalStateException if the calculated duration is negative
     *                               (indicating invalid measurements)
     */
    public long getTotalDurationMS() {
        if (this.timeStartNs == 0 && this.timeEndNs == 0) {
            throw new IllegalStateException("Time measurements are not available yet");
        }

        long duration = this.timeEndNs - this.timeStartNs + this.durationInitialization;
        if (duration < 0) {
            throw new IllegalStateException("Invalid duration.");
        }
        return duration / 1000 / 1000;
    }

    /**
     * Sorts the list of found prime numbers in ascending order.
     * 
     * This method performs an in-place sort using TimSort algorithm (via
     * List.sort).
     * Even though the primes list is a synchronized list, this compound action
     * requires
     * external synchronization to prevent race conditions during the sort
     * operation.
     * This method should typically be called after all threads have completed their
     * work.
     * 
     * The method uses method reference Long::compare for natural ordering
     * comparison.
     */
    void sortPrimesAsc() {
        // Synchronization, it is expected to be used only after the prime search.
        // Stable, in-place sort using TimSort under the hood for List<Long>. sources 7,
        // 8, 9, 10.
        // Even though this.primes is a Collections.synchronizedList, compound actions
        // like sorting must be guarded by external synchronization on the list
        // object to avoid race conditions. See source 2.
        synchronized (this.primes) {
            this.primes.sort(Long::compare);
        }
    }

    /**
     * Removes duplicate entries from the list of prime numbers.
     * 
     * This is a time-intensive operation that uses a stream-based approach with
     * distinct()
     * to eliminate duplicates while preserving the synchronized wrapper of the
     * list.
     * The method rebuilds the list contents in-place rather than replacing the list
     * reference,
     * thus maintaining the thread-safe properties of the
     * Collections.synchronizedList wrapper.
     * 
     * Note: This method is currently not in use as the dynamic block allocation
     * strategy
     * prevents duplicates from occurring. It is preserved for potential future use
     * cases.
     * 
     * Time complexity: O(n) space for the deduplicated snapshot
     * Uses HashSet internally for duplicate detection
     */
    void removeDuplicates() {
        // Very time intensive operation. Could be needed later on again. Currently not
        // in use. Should only be used on the non-changing list.
        // Stable, in-place dedupe with keeping the synchronized wrapper.
        // Uses a hashset internally. sources: 11, 12.
        // this.primes is not replaced with a new list (which would discard the
        // synchronized wrapper). Instead, rebuild contents inside a synchronized block.

        synchronized (this.primes) {
            // Duplicated snapshot. Obviously needs O(n) storage space, but keeps the
            // synchronized wrapper.
            List<Long> deduped = this.primes.stream()
                    .distinct()
                    .collect(Collectors.toList());

            // Preserve the synchronized wrapper.
            this.primes.clear();
            this.primes.addAll(deduped);
        }
    }

    /**
     * Returns the number of worker threads configured for this runner.
     * 
     * @return the number of threads used for parallel prime computation
     */
    int getNThreads() {
        return this.nThreads;
    }

    /**
     * Returns the upper bound for the prime number search.
     * 
     * @return the highest number checked for primality
     */
    long getUpperBound() {
        return this.upperBound;
    }

    /**
     * Returns the list of found prime numbers.
     * 
     * The returned list is the same synchronized list used internally by the worker
     * threads.
     * Callers should be aware that this list may be modified by worker threads if
     * they are
     * still running, and should use appropriate synchronization for compound
     * operations.
     * 
     * @return a synchronized list containing all prime numbers found up to the
     *         upper bound
     */
    List<Long> getPrimes() {
        return this.primes;
    }

}
