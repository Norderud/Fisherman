package no.kess.utility.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AreaSelector extends JDialog {
    private final Rectangle screenBounds;
    private Point startPoint;
    private Rectangle selection;

    public AreaSelector(Frame owner, Rectangle screenBounds) {
        super(owner, true);
        this.screenBounds = screenBounds;
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 50));
        setBounds(screenBounds);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                selection = new Rectangle(startPoint);
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int x = Math.min(startPoint.x, e.getX());
                int y = Math.min(startPoint.y, e.getY());
                int width = Math.abs(startPoint.x - e.getX());
                int height = Math.abs(startPoint.y - e.getY());
                selection = new Rectangle(x, y, width, height);
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (selection != null && selection.width > 5 && selection.height > 5) {
                    // Selection is relative to the dialog, which is relative to the screen.
                    // We need it in absolute screen coordinates.
                    selection.x += screenBounds.x;
                    selection.y += screenBounds.y;
                    dispose();
                } else {
                    selection = null;
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selection = null;
                    dispose();
                }
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);

        // Escape to cancel
        getRootPane().registerKeyboardAction(e -> {
            selection = null;
            dispose();
        }, KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public static Rectangle selectArea(Frame owner, int screenIdx) {
        GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        Rectangle bounds;
        if (screenIdx >= 0 && screenIdx < screens.length) {
            bounds = screens[screenIdx].getDefaultConfiguration().getBounds();
        } else {
            bounds = screens[0].getDefaultConfiguration().getBounds();
        }

        AreaSelector selector = new AreaSelector(owner, bounds);
        selector.setVisible(true);
        return selector.getSelection();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (selection != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(2));
            // Selection in paint is relative to the dialog
            Rectangle drawRect = new Rectangle(selection);
            if (selection.x >= screenBounds.x) {
                drawRect.x -= screenBounds.x;
                drawRect.y -= screenBounds.y;
            }
            g2d.draw(drawRect);
            g2d.setColor(new Color(255, 0, 0, 50));
            g2d.fill(drawRect);
        }

        g.setColor(Color.WHITE);
        g.drawString("Click and drag to select search area. Double-click to reset. ESC to cancel.", 10, 20);
    }

    public Rectangle getSelection() {
        return selection;
    }
}
