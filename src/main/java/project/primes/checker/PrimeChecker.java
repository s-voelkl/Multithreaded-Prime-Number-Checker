/**
 * Abstract base class for prime number checking implementations.
 * This class provides the common infrastructure for different prime checking strategies
 * that can be executed in parallel threads.
 */
package project.primes.checker;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public abstract class PrimeChecker implements Runnable {
    protected List<Long> sharedList;
    protected final long maxNumber;
    protected final AtomicLong nextStart;
    protected final long blockSize;

    /**
     * Constructs a new PrimeChecker with the specified parameters.
     * 
     * @param sharedList the thread-safe list where discovered prime numbers are
     *                   stored
     * @param maxNumber  the upper limit for prime number checking (inclusive)
     * @param nextStart  atomic counter for coordinating the next block start
     *                   position
     * @param blockSize  the number of consecutive numbers to check in each block
     *                   (minimum 1)
     */
    public PrimeChecker(List<Long> sharedList, long maxNumber, AtomicLong nextStart, long blockSize) {
        this.sharedList = sharedList;
        this.maxNumber = maxNumber;
        this.nextStart = nextStart;
        this.blockSize = Math.max(1, blockSize); // at least block size of 1.
    }

    /**
     * Executes the prime checking algorithm.
     * This method must be implemented by subclasses to define the specific
     * prime checking strategy and thread execution behavior.
     */
    @Override
    public abstract void run();
}