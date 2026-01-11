package no.kess.fisherman.engine;

import java.awt.*;

public class PixelBridge {
    private final Robot robot;
    private int offsetX;
    private int offsetY;

    public PixelBridge() {
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException("Could not initialize Robot for PixelBridge", e);
        }
    }

    public void setOffset(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
    }

    public Status getStatus() {
        Color c = robot.getPixelColor(offsetX, offsetY);
        return new Status(c);
    }

    public static class Status {
        private final int r, g, b;

        public Status(Color c) {
            this.r = c.getRed();
            this.g = c.getGreen();
            this.b = c.getBlue();
        }

        public boolean isBagsFull() {
            return r > 200;
        }

        public boolean isFishing() {
            return g > 200;
        }

        public boolean isCaught() {
            return b > 230;
        }

        public boolean isTooFar() {
            return b > 170 && b < 210;
        }

        public boolean isAddonDetected() {
            return b > 80;
        }

        public int getRed() {
            return r;
        }

        public int getGreen() {
            return g;
        }

        public int getBlue() {
            return b;
        }
    }
}
