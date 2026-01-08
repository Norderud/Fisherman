package no.kess.fisherman;

import java.awt.*;
import java.util.function.Consumer;

public class BotEngine {
    private final FishermanConfig config;
    private final Consumer<String> statusListener;
    private final Runnable onStop;
    private final AudioMonitor audioMonitor;
    private volatile boolean running = false;

    public BotEngine(FishermanConfig config, AudioMonitor audioMonitor, Consumer<String> statusListener, Runnable onStop) {
        this.config = config;
        this.audioMonitor = audioMonitor;
        this.statusListener = statusListener;
        this.onStop = onStop;
    }

    public void start() {
        if (running) return;
        running = true;
        new Thread(this::botLoop).start();
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    private void botLoop() {
        try {
            Robot eye = new Robot();
            updateStatus("Starting...");

            // Give time to switch window
            for (int i = 3; i > 0; i--) {
                if (!running) return;
                updateStatus("Starting in " + i + "...");
                Humanizer.sleep(1000, 0);
            }

            while (running) {
                // 1. Safety Check (Optical Bridge)
                Color statusColor = eye.getPixelColor(0, 0);
                if (statusColor.getRed() > 200 && statusColor.getGreen() < 50) {
                    updateStatus("Bags Full");
                    running = false;
                    break;
                }

                // 2. Cast Line
                updateStatus("Casting...");
                NativeKeyboard.sendKey(NativeKeyboard.SCANCODE_F6);
                long castTime = System.currentTimeMillis();

                // 3. Wait for Splash (Max 22 seconds)
                boolean fishHooked = false;
                updateStatus("Listening...");

                double threshold = config.getSplashThreshold();

                while (System.currentTimeMillis() - castTime < 22000 && running) {
                    if (audioMonitor.getCurrentVolume() > threshold) {
                        updateStatus("Splash Detected!");
                        fishHooked = true;
                        break;
                    }
                    Thread.sleep(50);
                }

                if (!running) break;

                // 4. React
                if (fishHooked) {
                    Humanizer.sleep(config.getReactionTime(), 75);
                    NativeKeyboard.sendKey(NativeKeyboard.SCANCODE_F10);

                    config.incrementFishCaught();
                    config.save();

                    updateStatus("Looting...");
                    Humanizer.sleep(2000, 500);
                } else {
                    updateStatus("Timeout, recasting...");
                    Humanizer.sleep(1000, 200);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Error: " + e.getMessage());
            running = false;
        } finally {
            updateStatus("Stopped");
            if (onStop != null) {
                onStop.run();
            }
        }
    }

    private void updateStatus(String status) {
        if (statusListener != null) {
            statusListener.accept(status);
        }
    }
}
