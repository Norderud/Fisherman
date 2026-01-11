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
        System.out.println("[DEBUG] Starting BotEngine...");
        running = true;
        new Thread(this::botLoop).start();
    }

    public void stop() {
        System.out.println("[DEBUG] Stopping BotEngine...");
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    private void botLoop() {
        try {
            Robot eye = new Robot();
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

            updateStatus("Starting...");

            // Give time to switch window
            for (int i = 3; i > 0; i--) {
                if (!running) return;
                updateStatus("Starting in " + i + "...");
                Humanizer.sleep(1000, 0);
            }

            long lastLureTime = 0;

            while (running) {
                // 1. Lure Check
                if (config.isLureEnabled()) {
                    long currentTime = System.currentTimeMillis();
                    long intervalMs = config.getLureInterval() * 60 * 1000L;
                    if (lastLureTime == 0 || (currentTime - lastLureTime) >= intervalMs) {
                        if (config.getLureKey() != 0) {
                            System.out.println("[DEBUG] Applying lure before fishing...");
                            updateStatus("Applying Lure...");
                            NativeKeyboard.sendKey(config.getLureKey());
                            // Wait 7 seconds for lure application
                            Humanizer.sleep(7000, 500);
                            lastLureTime = System.currentTimeMillis();
                        } else {
                            System.out.println("[DEBUG] Lure enabled but no key bound.");
                        }
                    }
                }

                // 2. Safety Check (Optical Bridge)
                Color statusColor = eye.getPixelColor(offsetX, offsetY);
                int blue = statusColor.getBlue();
                boolean bagsFull = statusColor.getRed() > 200;
                boolean addonFishing = statusColor.getGreen() > 200;
                boolean addonCaught = blue > 230;
                boolean addonTooFar = blue > 170 && blue < 210;

                if (bagsFull) {
                    updateStatus("Bags Full");
                    running = false;
                    break;
                }

                if (addonCaught) {
                    System.out.println("[DEBUG] Addon reported successful catch via pixel bridge.");
                }

                // 2. Cast Line
                if (addonFishing) {
                    System.out.println("[DEBUG] Addon reported 'isFishing' BEFORE cast. Cancelling to ensure fresh state.");
                    NativeKeyboard.sendKey(NativeKeyboard.SCANCODE_ESC);
                    Humanizer.sleep(800, 200);
                }

                updateStatus("Casting...");
                int actionKey = config.getActionKey();
                System.out.println("[DEBUG] Casting line using scan code: 0x" + Integer.toHexString(actionKey));
                NativeKeyboard.sendKey(actionKey);
                long castTime = System.currentTimeMillis();

                // Wait for addon to confirm fishing status (max 2s)
                long castWait = System.currentTimeMillis();
                boolean castConfirmed = false;
                while (System.currentTimeMillis() - castWait < 2000 && running) {
                    if (eye.getPixelColor(offsetX, offsetY).getGreen() > 200) {
                        System.out.println("[DEBUG] Addon confirmed 'isFishing' state.");
                        castConfirmed = true;
                        break;
                    }
                    Thread.sleep(100);
                }

                if (!castConfirmed) {
                    System.out.println("[DEBUG] Failed to confirm fishing state after cast. Recasting in next loop.");
                    updateStatus("Cast Failed");
                    Humanizer.sleep(1000, 500);
                    continue;
                }

                // 2.5. Verify Bobber Location
                if (!running) break;
                updateStatus("Searching for bobber...");
                // Wait for the splash to subside and bobber to appear/settle
                Humanizer.sleep(1500, 250);
                if (running && bobberFinder.findBobber() == null) {
                    if (!running) break;
                    System.out.println("[DEBUG] Bobber not found in search area. Recasting...");
                    updateStatus("Bobber not found - Recasting");
                    NativeKeyboard.sendKey(NativeKeyboard.SCANCODE_ESC);
                    Humanizer.sleep(1000, 500);
                    continue;
                }

                if (!running) break;

                // 3. Wait for Splash (Max 22 seconds)
                boolean fishHooked = false;
                updateStatus("Listening...");

                double threshold = config.getSplashThreshold();
                System.out.println("[DEBUG] Listening for splash (threshold: " + threshold + ")...");

                while (System.currentTimeMillis() - castTime < 22000 && running) {
                    double volume = audioMonitor.getCurrentVolume();
                    if (volume > threshold) {
                        System.out.println("[DEBUG] Splash detected! Volume: " + volume + " > Threshold: " + threshold);
                        updateStatus("Splash Detected!");
                        fishHooked = true;
                        break;
                    }

                    // Also check if addon stopped fishing unexpectedly (e.g. cancelled)
                    if (System.currentTimeMillis() - castTime > 1000) { // Give it some time to start
                        if (eye.getPixelColor(offsetX, offsetY).getGreen() < 50) {
                            System.out.println("[DEBUG] Addon reported fishing stopped.");
                            break;
                        }
                    }

                    Thread.sleep(50);
                }

                if (!running) break;

                // 4. React
                if (fishHooked) {
                    int reactionDelay = config.getReactionTime();
                    System.out.println("[DEBUG] Reacting in " + reactionDelay + "ms...");
                    Humanizer.sleep(reactionDelay, 75);

                    // Try to find bobber and move mouse to it for "Interact with Mouseover"
                    Point bobberPos = bobberFinder.findBobber();
                    if (bobberPos != null) {
                        // Use Gaussian distribution for interaction point (std dev 25, mean 25)
                        // to avoid static interaction position and simulate human clicking.
                        int targetX = bobberPos.x + Humanizer.getGaussianInt(25, 25);
                        int targetY = bobberPos.y - Humanizer.getGaussianInt(25, 25);
                        System.out.println("[DEBUG] Moving mouse to randomized bobber position: (" + targetX + ", " + targetY + ")");
                        NativeMouse.mouseMove(targetX, targetY, screenIdx);
                        Humanizer.sleep(150, 50);
                    }

                    int interactKey = config.getActionKey();
                    System.out.println("[DEBUG] Pressing interact key: 0x" + Integer.toHexString(interactKey));
                    NativeKeyboard.sendKey(interactKey);

                    // Wait for catch confirmation from addon (max 3s)
                    long lootWait = System.currentTimeMillis();
                    boolean confirmedCatch = false;
                    boolean confirmedTooFar = false;
                    while (System.currentTimeMillis() - lootWait < 3000 && running) {
                        Color c = eye.getPixelColor(offsetX, offsetY);
                        int b = c.getBlue();
                        if (b > 230) {
                            confirmedCatch = true;
                            System.out.println("[DEBUG] Addon confirmed catch!");
                            break;
                        }
                        if (b > 170 && b < 210) {
                            confirmedTooFar = true;
                            System.out.println("[DEBUG] Addon confirmed 'Too Far' error!");
                            break;
                        }
                        Thread.sleep(100);
                    }

                    if (confirmedCatch) {
                        config.incrementFishCaught();
                        updateStatus("Caught Fish!");
                    } else {
                        // Check if we are still fishing (means interact failed, e.g. too far)
                        if (confirmedTooFar || eye.getPixelColor(offsetX, offsetY).getGreen() > 200) {
                            System.out.println("[DEBUG] Failed to loot. Bobber was too far or interact failed. Cancelling...");
                            updateStatus("Too Far - Cancelling");
                            NativeKeyboard.sendKey(NativeKeyboard.SCANCODE_ESC);
                            Humanizer.sleep(500, 100);
                        } else {
                            updateStatus("Looting...");
                        }
                    }

                    Humanizer.sleep(2000, 500);
                } else {
                    updateStatus("Timeout/Cancelled");
                    // Ensure we are really not fishing anymore before next loop
                    NativeKeyboard.sendKey(NativeKeyboard.SCANCODE_ESC);
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
