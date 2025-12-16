/**
 * Utility class for reading and validating command-line input from the user.
 * Provides methods to prompt for various configuration parameters needed for prime number calculations.
 * All methods validate user input and re-prompt until valid values are provided.
 */
package project.primes.io;

import java.util.Scanner;

public abstract class CmdInput {

    private static final Scanner scanner = new Scanner(System.in);

    /**
     * Prompts the user for the number of threads to use.
     * Validates that the value is >= 1.
     *
     * A suggestion shows the number of available processors.
     *
     * @return number of threads (>= 1)
     */

    public static int getThreadCount() {
        int suggested = Runtime.getRuntime().availableProcessors();

        while (true) {
            try {
                System.out.print("Please enter the number of threads (suggested: " + suggested + "): ");
                String input = scanner.nextLine().trim();
                int nThreads = Integer.parseInt(input);

                if (nThreads < 1) {
                    System.err.println("Invalid number! Please enter a number greater than 0.");
                    continue;
                }

                return nThreads;

            } catch (NumberFormatException e) {
                System.err.println("Invalid input! Please enter a numeric value.");
            }
        }
    }

    /**
     * Prompts the user for the inclusive upper bound of the prime search.
     * Validates that the value is > 0.
     *
     * @return upper bound (> 0) as a long
     */

    public static long getUpperBound() {
        while (true) {
            try {
                System.out.print("Please enter the upper bound for prime search (greater than 0): ");
                String input = scanner.nextLine().trim();
                long upper = Long.parseLong(input);

                if (upper <= 0) {
                    System.err.println("Invalid number! Please enter a number greater than 0.");
                    continue;
                }

                return upper;

            } catch (NumberFormatException e) {
                System.err.println("Invalid input! Please enter a numeric value.");
            }
        }
    }

    /**
     * Prompts the user to choose which prime checking algorithm to use.
     * Asks whether to use the Sieve of Eratosthenes algorithm.
     * Validates that the input is either 'y' or 'n' (case-insensitive).
     *
     * @return true if the user chooses the Sieve of Eratosthenes, false otherwise
     */
    public static boolean getPrimeCheckerAlgorithm() {
        while (true) {
            System.out.print("Use the Sieve of Eratosthenes as the prime number checker? (y/n): ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equalsIgnoreCase("y")) {
                return true;
            }
            if (input.equalsIgnoreCase("n")) {
                return false;
            }
            System.err.println("Invalid input! Please enter 'y' or 'n'.");
        }
    }
}
