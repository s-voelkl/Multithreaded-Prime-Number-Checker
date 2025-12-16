package project.primes.checker;

public class PrimeCheckerTest {
    // Rough execution times for single-thread:
    // 1 million -> < 20 ms
    // 300 million -> < 9 s
    // 1.5 billion -> 30 s

    // Quality assurance:
    // - Proof that multithreading is really faster than single-thread
    // - Synchronization after thread completion using join()
    // - Number of prime numbers in [1, 1,000,000] is 78,498

    // All of these metrics are tested in the subclass-Test classes of PrimeChecker.
}
