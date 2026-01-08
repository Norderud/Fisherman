package no.kess.fisherman;

import java.util.Random;

public class Humanizer {
    private static final Random random = new Random();

    /**
     * Generates a delay based on a Gaussian distribution.
     * @param mean The target average delay (ms).
     * @param deviation The standard deviation (spread).
     * @return A random delay in ms.
     */
    public static long getDelay(int mean, int deviation) {
        double val = random.nextGaussian() * deviation + mean;
        return (long) Math.max(50, val); // Hard clamp minimum 50ms
    }

    public static void sleep(int mean, int deviation) {
        try {
            Thread.sleep(getDelay(mean, deviation));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static boolean coinFlip(double probability) {
        return random.nextDouble() < probability;
    }
}
