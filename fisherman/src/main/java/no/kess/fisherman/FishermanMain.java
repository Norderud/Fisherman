package no.kess.fisherman;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import java.awt.Color;
import java.awt.Robot;

public class FishermanMain {
    // Config
    private static final double SPLASH_THRESHOLD = 0.15; // Requires calibration

    public static void main(String[] args) throws Exception {
        Robot eye = new Robot(); // Used only for reading pixels, not input
        AudioSensor ear = new AudioSensor();
        TargetDataLine line = AudioSystem.getTargetDataLine(new AudioFormat(44100, 16, 1, true, true));
        line.open();
        line.start();

        System.out.println("Bot Started. Switch to WoW window.");
        Humanizer.sleep(3000, 0);

        while (true) {
            // 1. Safety Check (Optical Bridge)
            // Check pixel at 0,0. If Red component > 200 (and green low), bags are full.
            Color statusColor = eye.getPixelColor(0, 0);
            if (statusColor.getRed() > 200 && statusColor.getGreen() < 50) {
                System.out.println("Bags Full. Stopping.");
                break;
            }

            // 2. Cast Line
            System.out.println("Casting...");
            NativeKeyboard.sendKey(NativeKeyboard.SCANCODE_F6);
            long castTime = System.currentTimeMillis();

            // 3. Wait for Splash (Max 22 seconds)
            boolean fishHooked = false;
            while (System.currentTimeMillis() - castTime < 22000) {
                double volume = ear.getCurrentVolume(line);
                if (volume > SPLASH_THRESHOLD) {
                    System.out.println("Splash Detected! Vol: " + String.format("%.4f", volume));
                    fishHooked = true;
                    break;
                }
                // Check audio 20 times a second
                Thread.sleep(50);
            }

            // 4. React
            if (fishHooked) {
                // IMPORTANT: Human Reaction Time Delay
                // Humans take 250ms+ to realize a sound happened and press a key
                // ReactionTime = Mean(350ms) + (Random.nextGaussian() * StdDev(75ms))
                Humanizer.sleep(350, 75);

                // Press Soft Interact Key
                NativeKeyboard.sendKey(NativeKeyboard.SCANCODE_F10);

                // Wait for loot animation (3-5s)
                Humanizer.sleep(2000, 500);
            } else {
                System.out.println("No fish detected, recasting...");
                Humanizer.sleep(1000, 200); // Wait before retry
            }
        }

        line.stop();
        line.close();
    }
}

