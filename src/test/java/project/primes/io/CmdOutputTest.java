package project.primes.io;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class CmdOutputTest {

    @Test
    public void printReducedPrimesValidTest() {
        // Arrange
        List<Long> list = Arrays.asList(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L);
        List<Long> listIn = Arrays.asList(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L);

        // act
        CmdOutput.printReducedPrimes(new ArrayList<>(), 3);
        CmdOutput.printReducedPrimes(listIn, 3);

        // Assert
        assertEquals(list, listIn); // unchanged
    }

    @Test
    public void printReducedPrimesInvalidTest() {
        // Arrange
        List<Long> list = Arrays.asList(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L);

        // Assert
        assertThrows(IllegalArgumentException.class, () -> CmdOutput.printReducedPrimes(list, 0));
    }

    public void printTotalPrimeCountTest() {
        // Arrange
        List<Long> list = Arrays.asList(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L);
        List<Long> listIn = Arrays.asList(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L);

        // Act
        CmdOutput.printTotalPrimeCount(list);

        // Assert
        assertEquals(list, listIn); // unchanged
    }

    public void printTotalDurationTest() {
        long duration = 50L;

        // Act
        CmdOutput.printTotalDuration(50L);
        assertEquals(50L, duration); // unchanged
    }
}
