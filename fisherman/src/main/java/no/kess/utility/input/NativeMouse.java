package no.kess.utility.input;

import no.kess.utility.util.Humanizer;

import java.awt.*;
import java.awt.event.InputEvent;

public class NativeMouse {
    private static Robot robot;

    static {
        try {
            robot = new Robot();
            robot.setAutoDelay(0);
        } catch (AWTException e) {
            System.err.println("[ERROR] Could not initialize Robot for NativeMouse: " + e.getMessage());
        }
    }

    public static void mouseMove(int targetX, int targetY, int screenIdx) {
        PointerInfo info = MouseInfo.getPointerInfo();
        Point current = (info != null) ? info.getLocation() : new Point(targetX, targetY);

        double distance = current.distance(targetX, targetY);
        if (distance < 5) {
            mouseMoveDirect(targetX, targetY);
            return;
        }

        // Calculate steps based on distance (human-like speed)
        // Aim for ~200-500ms total duration
        int steps = (int) (distance / 20) + Humanizer.randomInt(15, 25);
        steps = Math.max(20, Math.min(steps, 60));

        // Create a control point for a slight curve (Bézier)
        // Offset is perpendicular-ish to the path
        int offsetX = (targetY - current.y) / 4 + Humanizer.randomInt(-20, 20);
        int offsetY = (current.x - targetX) / 4 + Humanizer.randomInt(-20, 20);
        Point controlPoint = new Point(
                (current.x + targetX) / 2 + offsetX / 2,
                (current.y + targetY) / 2 + offsetY / 2
        );

        for (int i = 1; i <= steps; i++) {
            if (Thread.currentThread().isInterrupted()) break;
            double t = (double) i / steps;

            // Cubic ease-out to simulate slowing down as we approach the target
            double ease = 1 - Math.pow(1 - t, 3);

            // Quadratic Bézier curve for more natural spatial path
            double invT = 1 - ease;
            int currX = (int) (invT * invT * current.x + 2 * invT * ease * controlPoint.x + ease * ease * targetX);
            int currY = (int) (invT * invT * current.y + 2 * invT * ease * controlPoint.y + ease * ease * targetY);

            // Add subtle jitter (simulates minor muscle tremors)
            if (i < steps) {
                currX += Humanizer.randomInt(-1, 1);
                currY += Humanizer.randomInt(-1, 1);
            }

            mouseMoveDirect(currX, currY);

            // Small variable delay between steps (8-12ms)
            Humanizer.sleepSmall(10, 2);
        }

        // Final snap to target for precision
        mouseMoveDirect(targetX, targetY);
    }

    /**
     * Internal direct movement (teleport)
     */
    private static void mouseMoveDirect(int x, int y) {
        if (robot != null) {
            robot.mouseMove(x, y);
        }
    }

    public static void leftClick() {
        if (robot != null) {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            Humanizer.sleep(100, 20);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        }
    }

    public static void rightClick() {
        if (robot != null) {
            robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
            Humanizer.sleep(100, 20);
            robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        }
    }

}
