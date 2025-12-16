/**
 * Main class for testing and benchmarking concurrent prime number checking algorithms.
 * This class runs performance tests for both Trial Checker and Sieve of Eratosthenes
 * algorithms with varying thread counts and input sizes.
 * 
 * The program executes two types of prime checking algorithms:
 * 1. Trial Checker - Tests each number individually for primality
 * 2. Sieve of Eratosthenes - Uses the classical sieve algorithm
 * 
 * For each algorithm, it performs:
 * - Batch runs with different thread counts (1, 4, 12, 24) and max sizes
 * - A single detailed run with complete output information
 */
package project.primes;

import project.primes.runner.ConcurrentRunner;

public class ConcurrentPrimeChecker {

    /**
     * Main entry point for the concurrent prime checker application.
     * Executes performance benchmarks for two prime checking algorithms
     * with various configurations of thread counts and maximum number ranges.
     * 
     * The method performs the following steps:
     * 1. Runs batch tests for Trial Checker with different thread counts and ranges
     * 2. Executes a single detailed Trial Checker run with 24 threads
     * 3. Runs batch tests for Sieve of Eratosthenes with different configurations
     * 4. Executes a single detailed Sieve of Eratosthenes run with 24 threads
     * 5. Lets the user make an own run with interactive inputs.
     * 
     * @param args Command line arguments (not used in current implementation)
     */
    public static void main(String[] args) {
        int[] nThreads = { 1, 4, 12, 24 };
        long[] maxSizes = { 100_000L, 1_000_000L, 5_000_000L };

        System.out.println("----- Concurrent Prime Number Checker -----");

        // 1. TRIAL CHECKER
        System.out.println("1. Trial Checker.");

        // a) Batch run
        System.out.println("a) Batch runs with the TrialChecker:");

        for (long l : maxSizes) {
            for (int t : nThreads) {
                ConcurrentRunner runner = new ConcurrentRunner(t, l, false);
                runner.run();
                String maxStr = String.format("%,d", l);
                System.out.println(
                        "TrialChecker: \tThreads: " + t + "\t max: " + maxStr + "\t --> " + runner.getTotalDurationMS()
                                + " ms");
            }
        }

        // b) Single run
        System.out.println("\nb) Single run of the TrialChecker with additional information.");
        ConcurrentRunner trialRunner = new ConcurrentRunner(24, 1_000_000L, false);
        trialRunner.run();
        trialRunner.printResults();

        // 2. SIEVE OF ERATOSTHENES
        System.out.println("2. Sieve Of Eratosthenes.");

        // a) Batch run
        System.out.println("a) Batch runs with the Sieve Of Eratosthenes:");
        for (long l : maxSizes) {
            for (int t : nThreads) {
                ConcurrentRunner runner = new ConcurrentRunner(t, l, true);
                runner.run();
                String maxStr = String.format("%,d", l);
                System.out.println(
                        "Sieve Of Eratosthenes: \tThreads: " + t + "\t max: " + maxStr + "\t --> "
                                + runner.getTotalDurationMS()
                                + " ms");
            }
        }

        // b) Single run
        System.out.println("\nb) Single run of the Sieve Of Eratosthenes with additional information.");
        ConcurrentRunner sieveOfEratosthenes = new ConcurrentRunner(24, 1_500_000_000, true);
        sieveOfEratosthenes.run();
        sieveOfEratosthenes.printResults();

        // 3. INTERACTIVE INPUT BY USER
        // System.out.println("User Input for interactive prime number checking.");
        System.out.println("3. User Input for interactive prime number checking.");
        ConcurrentRunner interactiveRunner = new ConcurrentRunner();
        System.out.println();
        interactiveRunner.run();
        interactiveRunner.printResults();
    }
}