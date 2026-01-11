package no.kess.fisherman.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * A transparent overlay that shows a red circle at the point of bobber detection.
 */
public class DetectionOverlay extends JWindow {
    private static DetectionOverlay instance;
    private final Timer hideTimer;

    private DetectionOverlay() {
        setAlwaysOnTop(true);
        setFocusableWindowState(false);
        // Set background to fully transparent
        setBackground(new Color(0, 0, 0, 0));
        setSize(20, 20); // Slightly larger than 10px to allow for antialiasing and stroke

        hideTimer = new Timer(2000, e -> setVisible(false));
        hideTimer.setRepeats(false);
    }

    /**
     * Shows the detection circle at the specified screen coordinates.
     *
     * @param screenPoint The absolute screen coordinates (x, y) where the bobber was detected.
     */
    public static void showAt(Point screenPoint) {
        if (screenPoint == null) return;
        SwingUtilities.invokeLater(() -> {
            if (instance == null) {
                instance = new DetectionOverlay();
            }
            instance.display(screenPoint);
        });
    }

    private void display(Point p) {
        // Center the window on the detection point
        setLocation(p.x - getWidth() / 2, p.y - getHeight() / 2);
        setVisible(true);
        repaint();
        hideTimer.restart();
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Clear background (some platforms require this for transparency)
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setComposite(AlphaComposite.SrcOver);

        int diameter = 10;
        double x = (getWidth() - diameter) / 2.0;
        double y = (getHeight() - diameter) / 2.0;

        // Draw the red transparent circle
        g2d.setColor(new Color(255, 0, 0, 150)); // Red with ~60% alpha
        g2d.fill(new Ellipse2D.Double(x, y, diameter, diameter));

        // Draw a solid red border
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(new Ellipse2D.Double(x, y, diameter, diameter));
    }
}
