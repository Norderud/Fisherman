package no.kess.utility.input;

import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import no.kess.utility.util.Humanizer;

import java.awt.*;

public class NativeMouse {
    private static final int MOUSEEVENTF_MOVE = 0x0001;
    private static final int MOUSEEVENTF_LEFTDOWN = 0x0002;
    private static final int MOUSEEVENTF_LEFTUP = 0x0004;
    private static final int MOUSEEVENTF_RIGHTDOWN = 0x0008;
    private static final int MOUSEEVENTF_RIGHTUP = 0x0010;
    private static final int MOUSEEVENTF_ABSOLUTE = 0x8000;
    private static final int MOUSEEVENTF_VIRTUALDESK = 0x4000;

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
        Rectangle virtualBounds = getVirtualDesktopBounds();
        int absX = (x - virtualBounds.x) * 65536 / virtualBounds.width;
        int absY = (y - virtualBounds.y) * 65536 / virtualBounds.height;

        sendInput(absX, absY, MOUSEEVENTF_MOVE | MOUSEEVENTF_ABSOLUTE | MOUSEEVENTF_VIRTUALDESK);
    }

    public static void leftClick() {
        sendInput(0, 0, MOUSEEVENTF_LEFTDOWN);
        Humanizer.sleep(100, 20);
        sendInput(0, 0, MOUSEEVENTF_LEFTUP);
    }

    public static void rightClick() {
        sendInput(0, 0, MOUSEEVENTF_RIGHTDOWN);
        Humanizer.sleep(100, 20);
        sendInput(0, 0, MOUSEEVENTF_RIGHTUP);
    }

    private static void sendInput(int x, int y, int flags) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_MOUSE);
        input.input.setType("mi");
        input.input.mi.dx = new WinDef.LONG(x);
        input.input.mi.dy = new WinDef.LONG(y);
        input.input.mi.mouseData = new WinDef.DWORD(0);
        input.input.mi.dwFlags = new WinDef.DWORD(flags);
        input.input.mi.time = new WinDef.DWORD(0);
        input.input.mi.dwExtraInfo = new BaseTSD.ULONG_PTR(0);

        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());
    }

    private static Rectangle getVirtualDesktopBounds() {
        Rectangle virtualBounds = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (GraphicsDevice gd : gs) {
            virtualBounds = virtualBounds.union(gd.getDefaultConfiguration().getBounds());
        }
        return virtualBounds;
    }
}
