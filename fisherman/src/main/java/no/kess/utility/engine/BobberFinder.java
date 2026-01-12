package no.kess.utility.engine;

import no.kess.utility.config.AppConfig;

import java.awt.*;
import java.awt.image.BufferedImage;

public class BobberFinder {
    private final Robot robot;
    private final AppConfig config;

    public BobberFinder(AppConfig config) {
        this.config = config;
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException("Could not initialize Robot for BobberFinder", e);
        }
    }

    public Point findBobber() {
        int screenIdx = config.getScreenIndex();
        GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        Rectangle screenBounds;
        if (screenIdx >= 0 && screenIdx < screens.length) {
            screenBounds = screens[screenIdx].getDefaultConfiguration().getBounds();
        } else {
            screenBounds = screens[0].getDefaultConfiguration().getBounds();
        }

        // Define ROI: use configured ROI if available, otherwise use default
        Rectangle roi = config.getRoi();
        if (roi == null) {
            int width = (int) (screenBounds.width * 0.5);
            int height = (int) (screenBounds.height * 0.4);
            int x = screenBounds.x + (screenBounds.width - width) / 2;
            int y = screenBounds.y + (screenBounds.height - height) / 2 - (int) (screenBounds.height * 0.05);
            roi = new Rectangle(x, y, width, height);
        }

        System.out.println("[DEBUG] Capturing ROI: " + roi);
        BufferedImage screenshot = robot.createScreenCapture(roi);

        // Scan for the most "bobber-like" pixel
        // Bobbers often have a bright red top.
        Point bestPoint = null;
        double maxRedness = 0;

        for (int i = 0; i < screenshot.getWidth(); i += 2) { // Step 2 for performance
            for (int j = 0; j < screenshot.getHeight(); j += 2) {
                int rgb = screenshot.getRGB(i, j);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Redness score: Red component relative to others
                if (r > 0 && r > g * 1.5 && r > b * 1.5) {
                    double redness = r - (g + b) / 2.0;
                    if (redness > maxRedness) {
                        maxRedness = redness;
                        bestPoint = new Point(roi.x + i, roi.y + j);
                    }
                }
            }
        }

        if (bestPoint != null) {
            System.out.println("[DEBUG] Found bobber candidate at (" + bestPoint.x + ", " + bestPoint.y + ") with redness " + maxRedness);
        } else {
            System.out.println("[DEBUG] No bobber candidate found.");
        }

        return bestPoint;
    }
}
