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

    /**
     * Small sleep for smooth movements (ms).
     */
    public static void sleepSmall(int mean, int deviation) {
        try {
            double val = random.nextGaussian() * deviation + mean;
            Thread.sleep((long) Math.max(1, val));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static int getGaussianInt(int mean, int deviation) {
        return (int) (random.nextGaussian() * deviation + mean);
    }

    public static int randomInt(int min, int max) {
        if (min >= max) return min;
        return random.nextInt(max - min + 1) + min;
    }

    public static double randomDouble(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    public static boolean coinFlip(double probability) {
        return random.nextDouble() < probability;
    }
}
