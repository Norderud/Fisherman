package no.kess.fisherman;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class FishermanUI extends JFrame {
    public static final int SCREEN_WIDTH = 650;
    public static final int SCREEN_HEIGHT = 500;
    private final FishermanConfig config;
    private final AudioMonitor audioMonitor;
    private final BotEngine botEngine;

    private JLabel statusLabel;
    private StatusIndicator statusIndicator;
    private JLabel fishCaughtLabel;
    private JProgressBar volumeBar;
    private JTextField thresholdField;
    private JTextField reactionField;
    private JTextField actionKeyField;
    private JCheckBox lureCheckBox;
    private JTextField lureIntervalField;
    private JTextField lureKeyField;
    private JComboBox<String> deviceBox;
    private JComboBox<String> screenBox;
    private JButton startStopButton;
    private Robot robot;

    public FishermanUI() {
        super("Fisherman");
        this.config = new FishermanConfig();
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }

        this.audioMonitor = new AudioMonitor(this::updateVolumeUI);
        this.botEngine = new BotEngine(config, audioMonitor, this::updateBotStatusUI, this::onBotStoppedUI);

        setupUI();
        audioMonitor.start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                botEngine.stop();
                audioMonitor.stop();
                config.save();
                System.exit(0);
            }
        });

        setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setVisible(true);
    }

    private void updateVolumeUI(double vol) {
        SwingUtilities.invokeLater(() -> {
            volumeBar.setValue((int) (vol * 100));
            volumeBar.setString(String.format("%.4f", vol));

            // Connection & Fishing Status Check
            if (robot != null) {
                int screenIdx = config.getScreenIndex();
                GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
                int x = 0, y = 0;
                if (screenIdx >= 0 && screenIdx < screens.length) {
                    Rectangle bounds = screens[screenIdx].getDefaultConfiguration().getBounds();
                    x = bounds.x;
                    y = bounds.y;
                }
                Color c = robot.getPixelColor(x, y);
                int blue = c.getBlue();
                boolean addonDetected = blue > 80; // Heartbeat (102), Too Far (191) or Catch (255)
                boolean addonFishing = c.getGreen() > 200;

                if (!addonDetected) {
                    statusIndicator.setColor(Color.RED);
                } else if (addonFishing) {
                    statusIndicator.setColor(Color.GREEN);
                } else {
                    statusIndicator.setColor(Color.YELLOW);
                }

                if (botEngine.isRunning()) {
                    fishCaughtLabel.setText("Fish Caught: " + config.getFishCaught());
                    if (addonFishing) {
                        statusLabel.setForeground(new Color(0, 128, 0)); // Dark green when fishing
                    } else {
                        statusLabel.setForeground(Color.BLACK);
                    }
                } else {
                    statusLabel.setForeground(Color.BLACK);
                    if (!addonDetected) {
                        statusLabel.setText("Status: Addon Not Detected");
                    } else {
                        statusLabel.setText("Status: Ready");
                    }
                }
            }
        });
    }

    private void updateBotStatusUI(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Status: " + status));
    }

    private void onBotStoppedUI() {
        SwingUtilities.invokeLater(() -> {
            deviceBox.setEnabled(true);
            screenBox.setEnabled(true);
            thresholdField.setEnabled(true);
            reactionField.setEnabled(true);
            actionKeyField.setEnabled(true);
            lureCheckBox.setEnabled(true);
            lureIntervalField.setEnabled(true);
            lureKeyField.setEnabled(true);
            startStopButton.setText("Start Fishing");
        });
    }

    private void updateSelectedMixer(String name) {
        Mixer.Info selected = null;
        if (!"Default Device".equals(name) && name != null) {
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            for (Mixer.Info mixerInfo : mixers) {
                if (mixerInfo.getName().equals(name)) {
                    selected = mixerInfo;
                    break;
                }
            }
        }
        audioMonitor.stop();
        audioMonitor.setMixerInfo(selected);
        audioMonitor.start();
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10));

        // Top Panel: Stats
        JPanel statsPanel = new JPanel(new GridLayout(2, 1));

        JPanel statusLine = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        statusIndicator = new StatusIndicator();
        statusLabel = new JLabel("Status: Stopped");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLine.add(statusIndicator);
        statusLine.add(statusLabel);
        
        fishCaughtLabel = new JLabel("Fish Caught: " + config.getFishCaught(), SwingConstants.CENTER);
        statsPanel.add(statusLine);
        statsPanel.add(fishCaughtLabel);
        add(statsPanel, BorderLayout.NORTH);

        // Center Panel: Volume & Config
        JPanel centerPanel = new JPanel(new GridLayout(11, 1, 5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. Audio Device
        JPanel devicePanel = new JPanel(new BorderLayout());
        devicePanel.add(new JLabel("Audio Device: "), BorderLayout.WEST);

        DefaultComboBoxModel<String> deviceModel = new DefaultComboBoxModel<>();
        deviceModel.addElement("Default Device");

        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        for (Mixer.Info mixerInfo : mixers) {
            if (AudioSystem.getMixer(mixerInfo).isLineSupported(info)) {
                deviceModel.addElement(mixerInfo.getName());
            }
        }

        deviceBox = new JComboBox<>(deviceModel);
        String savedDevice = config.getAudioDevice();
        if (!savedDevice.isEmpty()) {
            deviceBox.setSelectedItem(savedDevice);
            // Initialize audio monitor with saved device
            updateSelectedMixer(savedDevice);
        }

        deviceBox.addActionListener(e -> {
            String selected = (String) deviceBox.getSelectedItem();
            config.setAudioDevice(selected);
            config.save();
            updateSelectedMixer(selected);
        });
        devicePanel.add(deviceBox, BorderLayout.CENTER);
        centerPanel.add(devicePanel);

        // 2. Screen Selection
        JPanel screenPanel = new JPanel(new BorderLayout());
        screenPanel.add(new JLabel("Screen: "), BorderLayout.WEST);

        DefaultComboBoxModel<String> screenModel = new DefaultComboBoxModel<>();
        GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        for (int i = 0; i < screens.length; i++) {
            Rectangle bounds = screens[i].getDefaultConfiguration().getBounds();
            screenModel.addElement("Monitor " + (i + 1) + " (" + bounds.width + "x" + bounds.height + ")");
        }

        screenBox = new JComboBox<>(screenModel);
        int savedScreen = config.getScreenIndex();
        if (savedScreen >= 0 && savedScreen < screens.length) {
            screenBox.setSelectedIndex(savedScreen);
        }

        screenBox.addActionListener(e -> {
            config.setScreenIndex(screenBox.getSelectedIndex());
            config.save();
        });
        screenPanel.add(screenBox, BorderLayout.CENTER);
        centerPanel.add(screenPanel);

        // 3. Search Area Selection
        JPanel areaPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton selectAreaBtn = new JButton("Select Search Area");
        JLabel areaStatusLabel = new JLabel(config.getRoi() == null ? "Default (Full)" : "Custom Area Set : " + config.getRoi(), SwingConstants.CENTER);

        selectAreaBtn.addActionListener(e -> {
            Rectangle roi = AreaSelector.selectArea(this, config.getScreenIndex());
            config.setRoi(roi);
            config.save();
            areaStatusLabel.setText(roi == null ? "Default (Full)" : "Custom Area Set");
        });

        areaPanel.add(selectAreaBtn, BorderLayout.CENTER);
        areaPanel.add(areaStatusLabel, BorderLayout.EAST);
        centerPanel.add(areaPanel);

        // 4. Volume
        JPanel volPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        volPanel.add(new JLabel("Volume: "), BorderLayout.WEST);
        volumeBar = new JProgressBar(0, 100);
        volumeBar.setStringPainted(true);
        volPanel.add(volumeBar, BorderLayout.CENTER);
        centerPanel.add(volPanel);

        // 5. Threshold
        JPanel threshPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        threshPanel.add(new JLabel("Splash Threshold (0.0 - 1.0): "));
        thresholdField = new JTextField(String.valueOf(config.getSplashThreshold()), 10);
        threshPanel.add(thresholdField);
        centerPanel.add(threshPanel);

        // 6. Reaction Time
        JPanel reactPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        reactPanel.add(new JLabel("Reaction Delay (ms): "));
        reactionField = new JTextField(String.valueOf(config.getReactionTime()), 10);
        reactPanel.add(reactionField);
        centerPanel.add(reactPanel);

        // 7. Action Keybind
        JPanel keybindPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        keybindPanel.add(new JLabel("Action Key: "));
        actionKeyField = new JTextField(KeyEvent.getKeyText(NativeKeyboard.User32Ext.INSTANCE.MapVirtualKey(config.getActionKey(), 1)), 10); // MAPVK_VSC_TO_VK = 1
        actionKeyField.setEditable(false);
        actionKeyField.setHorizontalAlignment(JTextField.CENTER);
        actionKeyField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int vk = e.getKeyCode();
                int sc = NativeKeyboard.getScanCode(vk);
                System.out.println("[DEBUG] Setting Action Key: " + KeyEvent.getKeyText(vk) + " (Scan Code: 0x" + Integer.toHexString(sc) + ")");
                config.setActionKey(sc);
                config.save();
                actionKeyField.setText(KeyEvent.getKeyText(vk));
            }
        });
        keybindPanel.add(actionKeyField);
        centerPanel.add(keybindPanel);

        // 8. Lure Toggle & Interval
        JPanel lureRow1 = new JPanel(new FlowLayout(FlowLayout.CENTER));
        lureCheckBox = new JCheckBox("Use Lure every ", config.isLureEnabled());
        lureRow1.add(lureCheckBox);
        lureIntervalField = new JTextField(String.valueOf(config.getLureInterval()), 4);
        lureRow1.add(lureIntervalField);
        lureRow1.add(new JLabel(" minutes"));
        centerPanel.add(lureRow1);

        // 9. Lure Keybind
        JPanel lureKeyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        lureKeyPanel.add(new JLabel("Lure Key: "));
        String lureKeyName = "None";
        if (config.getLureKey() != 0) {
            lureKeyName = KeyEvent.getKeyText(NativeKeyboard.User32Ext.INSTANCE.MapVirtualKey(config.getLureKey(), 1));
        }
        lureKeyField = new JTextField(lureKeyName, 10);
        lureKeyField.setEditable(false);
        lureKeyField.setHorizontalAlignment(JTextField.CENTER);
        lureKeyField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int vk = e.getKeyCode();
                int sc = NativeKeyboard.getScanCode(vk);
                System.out.println("[DEBUG] Setting Lure Key: " + KeyEvent.getKeyText(vk) + " (Scan Code: 0x" + Integer.toHexString(sc) + ")");
                config.setLureKey(sc);
                config.save();
                lureKeyField.setText(KeyEvent.getKeyText(vk));
            }
        });
        lureKeyPanel.add(lureKeyField);
        centerPanel.add(lureKeyPanel);

        // 10. Test Detection
        JPanel testPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton testBtn = new JButton("Test Bobber Detection");
        testBtn.addActionListener(e -> {
            BobberFinder finder = new BobberFinder(config);
            Point p = finder.findBobber();
            if (p != null) {
                int targetX = p.x + Humanizer.getGaussianInt(25, 25);
                int targetY = p.y - Humanizer.getGaussianInt(25, 25);
                NativeMouse.mouseMove(targetX, targetY, config.getScreenIndex());
                JOptionPane.showMessageDialog(this, "Bobber found at " + p.x + ", " + p.y + "\nGaussian target: " + targetX + ", " + targetY + "\nMouse moved to target.");
            } else {
                JOptionPane.showMessageDialog(this, "Bobber NOT found.");
            }
        });
        testPanel.add(testBtn);
        centerPanel.add(testPanel);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom Panel: Controls
        startStopButton = new JButton("Start Fishing");
        startStopButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        startStopButton.addActionListener(this::toggleBot);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(startStopButton, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void toggleBot(ActionEvent e) {
        if (botEngine.isRunning()) {
            botEngine.stop();
        } else {
            startBot();
        }
    }

    private void startBot() {
        try {
            double thresh = Double.parseDouble(thresholdField.getText());
            int react = Integer.parseInt(reactionField.getText());
            int lureInterval = Integer.parseInt(lureIntervalField.getText());
            System.out.println("[DEBUG] Applying configuration - Threshold: " + thresh + ", Reaction: " + react + ", Lure: " + lureCheckBox.isSelected() + " every " + lureInterval + " mins");
            config.setSplashThreshold(thresh);
            config.setReactionTime(react);
            config.setLureEnabled(lureCheckBox.isSelected());
            config.setLureInterval(lureInterval);
            config.save();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid Threshold, Reaction Time, or Lure Interval Value");
            return;
        }

        thresholdField.setEnabled(false);
        reactionField.setEnabled(false);
        actionKeyField.setEnabled(false);
        deviceBox.setEnabled(false);
        screenBox.setEnabled(false);
        lureCheckBox.setEnabled(false);
        lureIntervalField.setEnabled(false);
        lureKeyField.setEnabled(false);
        startStopButton.setText("Stop Fishing");
        botEngine.start();
    }

    private static class StatusIndicator extends JPanel {
        private Color color = Color.RED;

        public StatusIndicator() {
            setPreferredSize(new Dimension(15, 15));
            setOpaque(false);
        }

        public void setColor(Color color) {
            if (this.color != color) {
                this.color = color;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(color);
            g2d.fillOval(2, 2, getWidth() - 4, getHeight() - 4);
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawOval(2, 2, getWidth() - 4, getHeight() - 4);
        }
    }
}

