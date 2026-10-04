package no.kess.utility.engine;

import no.kess.utility.audio.AudioMonitor;
import no.kess.utility.config.AppConfig;
import no.kess.utility.input.NativeKeyboard;
import no.kess.utility.input.NativeMouse;
import no.kess.utility.ui.DetectionOverlay;
import no.kess.utility.util.Humanizer;

import java.awt.*;
import java.util.function.Consumer;

public class BotEngine {
    private final AppConfig config;
    private final Consumer<String> statusListener;
    private final Runnable onStop;
    private final AudioMonitor audioMonitor;
    private final PixelBridge pixelBridge;
    private volatile boolean running = false;
    private Thread botThread;

    private int sessionThrows = 0;
    private int sessionFishCaught = 0;
    private long startTime = 0;
    private long stopTime = 0;
    private StopReason stopReason = StopReason.MANUAL;

    private long nextShortBreakTime;
    private long nextLongBreakTime;

    private void scheduleNextShortBreak() {
        long intervalMs = Humanizer.randomInt(config.getShortBreakIntervalMin(), config.getShortBreakIntervalMax()) * 60 * 1000L;
        nextShortBreakTime = System.currentTimeMillis() + intervalMs;
        System.out.println("[DEBUG] Next short break in " + (intervalMs / 1000 / 60) + " minutes");
    }

    private void scheduleNextLongBreak() {
        long intervalMs = Humanizer.randomInt(config.getLongBreakIntervalMin(), config.getLongBreakIntervalMax()) * 60 * 1000L;
        nextLongBreakTime = System.currentTimeMillis() + intervalMs;
        System.out.println("[DEBUG] Next long break in " + (intervalMs / 1000 / 60) + " minutes");
    }

    public void start() {
        if (running) return;
        System.out.println("[DEBUG] Starting BotEngine...");
        sessionThrows = 0;
        sessionFishCaught = 0;
        startTime = System.currentTimeMillis();
        stopTime = 0;
        stopReason = StopReason.MANUAL;
        running = true;
        startStopKeyListener();
        botThread = new Thread(this::botLoop, "BotLoop");
        botThread.start();
    }

    public BotEngine(AppConfig config, AudioMonitor audioMonitor, Consumer<String> statusListener, Runnable onStop) {
        this.config = config;
        this.audioMonitor = audioMonitor;
        this.statusListener = statusListener;
        this.onStop = onStop;
        this.pixelBridge = new PixelBridge();
    }

    public int getSessionThrows() {
        return sessionThrows;
    }

    public int getSessionFishCaught() {
        return sessionFishCaught;
    }

    public StopReason getStopReason() {
        return stopReason;
    }

    public long getSessionDurationMs() {
        if (startTime == 0) return 0;
        long end = (running || stopTime == 0) ? System.currentTimeMillis() : stopTime;
        return end - startTime;
    }

    public String getSessionDurationFormatted() {
        long durationMs = getSessionDurationMs();
        long seconds = (durationMs / 1000) % 60;
        long minutes = (durationMs / (1000 * 60)) % 60;
        long hours = (durationMs / (1000 * 60 * 60));

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString();
    }

    private void handleBreaks() {
        long now = System.currentTimeMillis();
        if (now > nextLongBreakTime) {
            takeLongBreak();
            scheduleNextLongBreak();
            scheduleNextShortBreak(); // Reset short break after long one
        } else if (now > nextShortBreakTime) {
            takeShortBreak();
            scheduleNextShortBreak();
        }
    }

    private void takeShortBreak() {
        int durationSeconds = Humanizer.randomInt(config.getShortBreakDurationMin(), config.getShortBreakDurationMax());
        System.out.println("[DEBUG] Taking short break for " + durationSeconds + " seconds");
        for (int i = 0; i < durationSeconds; i++) {
            if (!running) break;
            updateStatus("Short break (" + (durationSeconds - i) + "s)");
            Humanizer.sleep(1000, 0);
        }
    }

    private void takeLongBreak() {
        int durationMinutes = Humanizer.randomInt(config.getLongBreakDurationMin(), config.getLongBreakDurationMax());
        System.out.println("[DEBUG] Taking long break for " + durationMinutes + " minutes");
        for (int i = 0; i < durationMinutes * 60; i++) {
            if (!running) break;
            if (i % 60 == 0) {
                updateStatus("Long break (" + (durationMinutes - (i / 60)) + "m)");
            }
            Humanizer.sleep(1000, 0);
        }
    }

    private void botLoop() {
        try {
            BobberFinder bobberFinder = new BobberFinder(config);
            int screenIdx = config.getScreenIndex();
            GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            int offsetX = 0;
            int offsetY = 0;
            if (screenIdx >= 0 && screenIdx < screens.length) {
                Rectangle bounds = screens[screenIdx].getDefaultConfiguration().getBounds();
                offsetX = bounds.x;
                offsetY = bounds.y;
                System.out.println("[DEBUG] Using screen " + screenIdx + " at offset (" + offsetX + ", " + offsetY + ")");
            }
            pixelBridge.setOffset(offsetX, offsetY);

            updateStatus("Starting...");

            // Give time to switch window
            for (int i = 3; i > 0; i--) {
                if (!running) return;
                updateStatus("Starting in " + i + "...");
                Humanizer.sleep(1000, 0);
            }

            long lastLureTime = 0;
            scheduleNextShortBreak();
            scheduleNextLongBreak();

            while (running) {
                handleBreaks();
                if (!running) break;

                // Check time limit
                int limitMinutes = config.getRunTimeLimit();
                if (limitMinutes > 0) {
                    long elapsedMs = System.currentTimeMillis() - startTime;
                    if (elapsedMs > limitMinutes * 60 * 1000L) {
                        updateStatus("Time Limit Reached");
                        stopReason = StopReason.TIME_LIMIT;
                        logout();
                        running = false;
                        break;
                    }
                }

                lastLureTime = applyLureIfNeeded(lastLureTime);
                if (!running) break;

                // 2. Safety Check (Optical Bridge)
                PixelBridge.Status status = pixelBridge.getStatus();

                if (status.isBagsFull()) {
                    updateStatus("Bags Full");
                    stopReason = StopReason.BAGS_FULL;
                    if (config.isLogoutAfterFullBag()) {
                        logout();
                    }
                    running = false;
                    break;
                }

                if (status.isCaught()) {
                    System.out.println("[DEBUG] Addon reported successful catch via pixel bridge.");
                }

                // 2. Cast Line
                if (!running) break;
                handlePreCastCleanup(status);
                if (!running) break;

                updateStatus("Casting...");
                long castTime = performCast();

                if (!verifyCastConfirmation() || !running) {
                    if (running) {
                        System.out.println("[DEBUG] Failed to confirm fishing state after cast. Recasting in next loop.");
                        updateStatus("Cast Failed");
                        Humanizer.sleep(1000, 500);
                    }
                    continue;
                }

                // 2.5. Verify Bobber Location & Move Mouse
                if (!running) break;
                Point bobberPos = waitForBobberAndPrePositionMouse(bobberFinder, screenIdx);
                if (running && bobberPos == null) {
                    continue;
                }

                if (!running) break;

                // 3. Wait for Splash (Max 22 seconds)
                boolean fishHooked = waitForSplash(castTime);
                if (!running) break;

                // 4. React
                if (fishHooked && running) {
                    performCatchAndConfirm();
                    if (!running) break;
                    Humanizer.sleep(2000, 500);
                } else if (running) {
                    updateStatus("Timeout/Cancelled");
                    // Ensure we are really not fishing anymore before next loop
                    NativeKeyboard.sendKey(NativeKeyboard.SCANCODE_ESC);
                    Humanizer.sleep(1000, 200);
                }
            }
        } catch (Exception e) {
            if (running) { // Only log errors if we were supposed to be running
                e.printStackTrace();
                updateStatus("Error: " + e.getMessage());
                stopReason = StopReason.ERROR;
            }
            running = false;
        } finally {
            stopTime = System.currentTimeMillis();
            updateStatus("Stopped");
            if (onStop != null) {
                onStop.run();
            }
        }
    }

    private void startStopKeyListener() {
        new Thread(() -> {
            int stopKeySC = config.getStopKey();
            int stopKeyVK = NativeKeyboard.User32Ext.INSTANCE.MapVirtualKey(stopKeySC, 1); // MAPVK_VSC_TO_VK = 1
            System.out.println("[DEBUG] Stop key listener started. Monitoring VK: 0x" + Integer.toHexString(stopKeyVK));
            while (running && !Thread.currentThread().isInterrupted()) {
                if (NativeKeyboard.isKeyPressed(stopKeyVK)) {
                    System.out.println("[DEBUG] Stop key pressed! Stopping bot...");
                    stop();
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
            System.out.println("[DEBUG] Stop key listener finished.");
        }, "StopKeyListener").start();
    }

    public void stop() {
        System.out.println("[DEBUG] Stopping BotEngine...");
        running = false;
        if (botThread != null) {
            botThread.interrupt();
        }
    }

    public boolean isRunning() {
        return running;
    }

    private long performCast() {
        int castKey = config.getCastKey();
        System.out.println("[DEBUG] Casting line using scan code: 0x" + Integer.toHexString(castKey));
        sessionThrows++;
        config.incrementThrows();
        NativeKeyboard.sendKey(castKey);
        return System.currentTimeMillis();
    }

    private void updateStatus(String status) {
        if (statusListener != null) {
            statusListener.accept(status);
        }
    }

    private long applyLureIfNeeded(long lastLureTime) {
        if (!config.isLureEnabled()) return lastLureTime;

        long currentTime = System.currentTimeMillis();
        long intervalMs = config.getLureInterval() * 60 * 1000L + 800;

        if (lastLureTime == 0 || (currentTime - lastLureTime) >= intervalMs) {
            if (config.getLureKey() != 0) {
                System.out.println("[DEBUG] Applying lure before fishing...");
                updateStatus("Applying Lure...");
                NativeKeyboard.sendKey(config.getLureKey());
                // Wait 7 seconds for lure application
                Humanizer.sleep(7000, 500);
                return System.currentTimeMillis();
            } else {
                System.out.println("[DEBUG] Lure enabled but no key bound.");
            }
        }
        return lastLureTime;
    }

    private void handlePreCastCleanup(PixelBridge.Status status) {
        if (status.isFishing()) {
            System.out.println("[DEBUG] Addon reported 'isFishing' BEFORE cast. Cancelling to ensure fresh state.");
            NativeKeyboard.sendKey(NativeKeyboard.SCANCODE_ESC);
            Humanizer.sleep(800, 200);
        }
    }

    private void performCatchAndConfirm() {
        int clickDelay = Humanizer.randomInt(200, 500);
        System.out.println("[DEBUG] Splash reaction click in " + clickDelay + "ms...");
        Humanizer.sleep(clickDelay, 50);
        if (!running) return;

        int interactKey = config.getInteractKey();
        System.out.println("[DEBUG] Pressing interact key: 0x" + Integer.toHexString(interactKey));
        NativeKeyboard.sendKey(interactKey);
        if (!running) return;

        // Wait for catch confirmation from addon (max 3s)
        long lootWait = System.currentTimeMillis();
        boolean confirmedCatch = false;
        boolean confirmedTooFar = false;
        while (System.currentTimeMillis() - lootWait < 3000 && running) {
            PixelBridge.Status s = pixelBridge.getStatus();
            if (s.isCaught()) {
                confirmedCatch = true;
                System.out.println("[DEBUG] Addon confirmed catch!");
                break;
            }
            if (s.isTooFar()) {
                confirmedTooFar = true;
                System.out.println("[DEBUG] Addon confirmed 'Too Far' error!");
                break;
            }
            Humanizer.sleep(100, 0);
        }

        if (confirmedCatch) {
            sessionFishCaught++;
            config.incrementFishCaught();
            updateStatus("Caught Fish!");
        } else if (running) {
            PixelBridge.Status s = pixelBridge.getStatus();
            if (confirmedTooFar || s.isFishing()) {
                System.out.println("[DEBUG] Failed to loot. Bobber was too far or interact failed. Cancelling...");
                updateStatus("Too Far - Cancelling");
                NativeKeyboard.sendKey(NativeKeyboard.SCANCODE_ESC);
                Humanizer.sleep(500, 100);
            } else {
                updateStatus("Looting...");
            }
        }
    }

    private boolean verifyCastConfirmation() {
        long castWait = System.currentTimeMillis();
        while (System.currentTimeMillis() - castWait < 2000 && running) {
            if (pixelBridge.getStatus().isFishing()) {
                System.out.println("[DEBUG] Addon confirmed 'isFishing' state.");
                return true;
            }
            Humanizer.sleep(100, 0);
        }
        return false;
    }

    private Point waitForBobberAndPrePositionMouse(BobberFinder bobberFinder, int screenIdx) {
        updateStatus("Searching for bobber...");
        // Wait for the splash to subside and bobber to appear/settle
        Humanizer.sleep(1500, 250);
        if (!running) return null;

        Point bobberPos = bobberFinder.findBobber();
        if (bobberPos == null) {
            System.out.println("[DEBUG] Bobber not found in search area. Recasting...");
            updateStatus("Bobber not found - Recasting");
            NativeKeyboard.sendKey(NativeKeyboard.SCANCODE_ESC);
            Humanizer.sleep(1000, 500);
            return null;
        }

        // Visual feedback
        if (config.isShowDetectionPoint()) {
            DetectionOverlay.showAt(bobberPos);
        }

        // Move mouse towards bobber with reaction time and gaussian blur
        int reactionDelay = config.getReactionTime();
        System.out.println("[DEBUG] Bobber found. Moving mouse in " + reactionDelay + "ms...");
        Humanizer.sleep(reactionDelay, 75);
        if (!running) return bobberPos;

        int targetX = bobberPos.x + Humanizer.getGaussianInt(10, 5);
        int targetY = bobberPos.y - Humanizer.getGaussianInt(0, 5);
        System.out.println("[DEBUG] Moving mouse to randomized bobber position: (" + targetX + ", " + targetY + ")");
        NativeMouse.mouseMove(targetX, targetY, screenIdx);
        return bobberPos;
    }

    private boolean waitForSplash(long castTime) {
        updateStatus("Listening...");
        double threshold = config.getSplashThreshold();
        System.out.println("[DEBUG] Listening for splash (threshold: " + threshold + ")...");

        while (System.currentTimeMillis() - castTime < 22000 && running) {
            double volume = audioMonitor.getCurrentVolume();
            if (volume > threshold) {
                System.out.println("[DEBUG] Splash detected! Volume: " + volume + " > Threshold: " + threshold);
                updateStatus("Splash Detected!");
                return true;
            }

            // Also check if addon stopped fishing unexpectedly (e.g. cancelled)
            if (System.currentTimeMillis() - castTime > 1000) {
                if (!pixelBridge.getStatus().isFishing()) {
                    System.out.println("[DEBUG] Addon reported fishing stopped.");
                    return false;
                }
            }
            Humanizer.sleep(60, 15);
        }
        return false;
    }

    private void logout() {
        updateStatus("Logging out...");
        Humanizer.sleep(1000, 500);

        int logoutKey = config.getLogoutKey();
        if (logoutKey != 0) {
            System.out.println("[DEBUG] Sending logout key: " + logoutKey);
            NativeKeyboard.sendKey(logoutKey);
        } else {
            System.out.println("[DEBUG] No logout key configured. Skipping logout.");
        }

        Humanizer.sleep(2000, 500);
    }

    public enum StopReason {
        MANUAL,
        BAGS_FULL,
        TIME_LIMIT,
        ERROR
    }
}
