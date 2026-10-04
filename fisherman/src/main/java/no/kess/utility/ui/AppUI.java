package no.kess.utility.ui;

import no.kess.utility.audio.AudioMonitor;
import no.kess.utility.config.AppConfig;
import no.kess.utility.engine.BobberFinder;
import no.kess.utility.engine.BotEngine;
import no.kess.utility.engine.PixelBridge;
import no.kess.utility.input.NativeKeyboard;
import no.kess.utility.input.NativeMouse;
import no.kess.utility.util.Humanizer;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AppUI extends JFrame {
    public static final int SCREEN_WIDTH = 600;
    public static final int SCREEN_HEIGHT = 800;
    private final AppConfig config;
    private final AudioMonitor audioMonitor;
    private final BotEngine botEngine;
    private final PixelBridge pixelBridge;

    private JLabel statusLabel;
    private StatusIndicator statusIndicator;
    private JLabel fishCaughtLabel;
    private JLabel throwsLabel;
    private JProgressBar volumeBar;
    private JTextField thresholdField;
    private JTextField reactionField;
    private JTextField castKeyField;
    private JTextField interactKeyField;
    private JTextField stopKeyField;
    private JTextField runTimeField;
    private JTextField shortBreakIntervalMinField;
    private JTextField shortBreakIntervalMaxField;
    private JTextField shortBreakDurationMinField;
    private JTextField shortBreakDurationMaxField;
    private JTextField longBreakIntervalMinField;
    private JTextField longBreakIntervalMaxField;
    private JTextField longBreakDurationMinField;
    private JTextField longBreakDurationMaxField;
    private JCheckBox lureCheckBox;
    private JCheckBox showDetectionCheckBox;
    private JCheckBox logoutAfterFullBagCheckBox;
    private JTextField logoutKeyField;
    private JTextField lureIntervalField;
    private JTextField lureKeyField;
    private JComboBox<String> deviceBox;
    private JComboBox<String> screenBox;
    private JButton startStopButton;

    public AppUI() {
        super("Utility Tool");
        this.config = new AppConfig();
        this.pixelBridge = new PixelBridge();

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
            volumeBar.setValue((int) (vol));
            volumeBar.setString(String.format("%.4f", vol));

            // Connection & Fishing Status Check
            int screenIdx = config.getScreenIndex();
            GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            int x = 0, y = 0;
            if (screenIdx >= 0 && screenIdx < screens.length) {
                Rectangle bounds = screens[screenIdx].getDefaultConfiguration().getBounds();
                x = bounds.x;
                y = bounds.y;
            }
            pixelBridge.setOffset(x, y);
            PixelBridge.Status status = pixelBridge.getStatus();

            if (!status.isAddonDetected()) {
                statusIndicator.setColor(Color.RED);
            } else if (status.isFishing()) {
                statusIndicator.setColor(Color.GREEN);
            } else {
                statusIndicator.setColor(Color.YELLOW);
            }

            if (botEngine.isRunning()) {
                fishCaughtLabel.setText("Fish Caught: " + config.getFishCaught());
                throwsLabel.setText("Throws: " + config.getThrows());
                if (status.isFishing()) {
                    statusLabel.setForeground(new Color(0, 128, 0)); // Dark green when fishing
                } else {
                    statusLabel.setForeground(Color.BLACK);
                }
            } else {
                statusLabel.setForeground(Color.BLACK);
                if (!status.isAddonDetected()) {
                    statusLabel.setText("Status: Addon Not Detected");
                } else {
                    statusLabel.setText("Status: Ready");
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
            castKeyField.setEnabled(true);
            interactKeyField.setEnabled(true);
            stopKeyField.setEnabled(true);
            runTimeField.setEnabled(true);
            shortBreakIntervalMinField.setEnabled(true);
            shortBreakIntervalMaxField.setEnabled(true);
            shortBreakDurationMinField.setEnabled(true);
            shortBreakDurationMaxField.setEnabled(true);
            longBreakIntervalMinField.setEnabled(true);
            longBreakIntervalMaxField.setEnabled(true);
            longBreakDurationMinField.setEnabled(true);
            longBreakDurationMaxField.setEnabled(true);
            lureCheckBox.setEnabled(true);
            showDetectionCheckBox.setEnabled(true);
            logoutAfterFullBagCheckBox.setEnabled(true);
            lureIntervalField.setEnabled(true);
            lureKeyField.setEnabled(true);
            logoutKeyField.setEnabled(true);
            startStopButton.setText("Start Fishing");

            int sessionFish = botEngine.getSessionFishCaught();
            int sessionThrows = botEngine.getSessionThrows();
            String duration = botEngine.getSessionDurationFormatted();

            String stopReasonMsg = "Manual Stop";
            BotEngine.StopReason reason = botEngine.getStopReason();
            if (reason == BotEngine.StopReason.BAGS_FULL) {
                stopReasonMsg = "Bags Full";
            } else if (reason == BotEngine.StopReason.TIME_LIMIT) {
                stopReasonMsg = "Time Limit Reached";
            } else if (reason == BotEngine.StopReason.ERROR) {
                stopReasonMsg = "Error occurred";
            }

            JOptionPane.showMessageDialog(this,
                    "Session Report:\n" +
                            "Stop Reason: " + stopReasonMsg + "\n" +
                            "Run Time: " + duration + "\n" +
                            "Throws: " + sessionThrows + "\n" +
                            "Fish Caught: " + sessionFish,
                    "Bot Stopped", JOptionPane.INFORMATION_MESSAGE);
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

        add(createStatsPanel(), BorderLayout.NORTH);
        add(new JScrollPane(createConfigPanel()), BorderLayout.CENTER);
        add(createControlsPanel(), BorderLayout.SOUTH);
    }

    private JPanel createStatsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(3, 1));

        JPanel statusLine = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        statusIndicator = new StatusIndicator();
        statusLabel = new JLabel("Status: Stopped");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLine.add(statusIndicator);
        statusLine.add(statusLabel);

        fishCaughtLabel = new JLabel("Fish Caught: " + config.getFishCaught(), SwingConstants.CENTER);
        throwsLabel = new JLabel("Throws: " + config.getThrows(), SwingConstants.CENTER);
        statsPanel.add(statusLine);
        statsPanel.add(fishCaughtLabel);
        statsPanel.add(throwsLabel);
        return statsPanel;
    }

    private JPanel createConfigPanel() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Group 1: Screen and Detection Area
        JPanel screenAreaPanel = new JPanel(new GridLayout(1, 2, 10, 5));
        screenAreaPanel.setBorder(BorderFactory.createTitledBorder("Screen & Detection Area"));
        screenAreaPanel.add(createScreenSelectionPanel());
        screenAreaPanel.add(createSearchAreaPanel());
        centerPanel.add(screenAreaPanel);

        // Group 2: Audio Settings
        JPanel audioGroupPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        audioGroupPanel.setBorder(BorderFactory.createTitledBorder("Audio Settings"));
        audioGroupPanel.add(createAudioDevicePanel());
        audioGroupPanel.add(createVolumePanel());
        audioGroupPanel.add(createThresholdPanel());
        centerPanel.add(audioGroupPanel);

        // Group 3: Timing & Breaks
        JPanel timingPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        timingPanel.setBorder(BorderFactory.createTitledBorder("Timing & Breaks"));
        timingPanel.add(createReactionTimePanel());
        timingPanel.add(createRunTimeLimitPanel());
        timingPanel.add(createShortBreakPanel());
        timingPanel.add(createLongBreakPanel());
        centerPanel.add(timingPanel);

        // Group 4: Keybinds
        JPanel keybindsPanel = new JPanel(new GridLayout(3, 2, 10, 5));
        keybindsPanel.setBorder(BorderFactory.createTitledBorder("Keybinds"));
        keybindsPanel.add(createCastKeyPanel());
        keybindsPanel.add(createInteractKeyPanel());
        keybindsPanel.add(createLureKeyPanel());
        keybindsPanel.add(createStopKeyPanel());
        keybindsPanel.add(createLogoutKeyPanel());
        keybindsPanel.add(new JPanel()); // Filler
        centerPanel.add(keybindsPanel);

        // Group 5: Options & Testing
        JPanel optionsPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Options & Testing"));
        optionsPanel.add(createLureTogglePanel());
        optionsPanel.add(createShowDetectionPanel());
        optionsPanel.add(createLogoutAfterFullBagPanel());
        optionsPanel.add(createTestDetectionPanel());
        centerPanel.add(optionsPanel);

        return centerPanel;
    }

    private JPanel createRunTimeLimitPanel() {
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timePanel.add(new JLabel("Run Limit (min, 0=off): "));
        runTimeField = new JTextField(String.valueOf(config.getRunTimeLimit()), 5);
        timePanel.add(runTimeField);
        return timePanel;
    }

    private JPanel createShortBreakPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Short Break every "));
        shortBreakIntervalMinField = new JTextField(String.valueOf(config.getShortBreakIntervalMin()), 3);
        panel.add(shortBreakIntervalMinField);
        panel.add(new JLabel("-"));
        shortBreakIntervalMaxField = new JTextField(String.valueOf(config.getShortBreakIntervalMax()), 3);
        panel.add(shortBreakIntervalMaxField);
        panel.add(new JLabel(" min, for "));
        shortBreakDurationMinField = new JTextField(String.valueOf(config.getShortBreakDurationMin()), 3);
        panel.add(shortBreakDurationMinField);
        panel.add(new JLabel("-"));
        shortBreakDurationMaxField = new JTextField(String.valueOf(config.getShortBreakDurationMax()), 3);
        panel.add(shortBreakDurationMaxField);
        panel.add(new JLabel(" sec"));
        return panel;
    }

    private JPanel createLongBreakPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Long Break every "));
        longBreakIntervalMinField = new JTextField(String.valueOf(config.getLongBreakIntervalMin()), 3);
        panel.add(longBreakIntervalMinField);
        panel.add(new JLabel("-"));
        longBreakIntervalMaxField = new JTextField(String.valueOf(config.getLongBreakIntervalMax()), 3);
        panel.add(longBreakIntervalMaxField);
        panel.add(new JLabel(" min, for "));
        longBreakDurationMinField = new JTextField(String.valueOf(config.getLongBreakDurationMin()), 3);
        panel.add(longBreakDurationMinField);
        panel.add(new JLabel("-"));
        longBreakDurationMaxField = new JTextField(String.valueOf(config.getLongBreakDurationMax()), 3);
        panel.add(longBreakDurationMaxField);
        panel.add(new JLabel(" min"));
        return panel;
    }

    private JPanel createAudioDevicePanel() {
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
            updateSelectedMixer(savedDevice);
        }

        deviceBox.addActionListener(e -> {
            String selected = (String) deviceBox.getSelectedItem();
            config.setAudioDevice(selected);
            config.save();
            updateSelectedMixer(selected);
        });
        devicePanel.add(deviceBox, BorderLayout.CENTER);
        return devicePanel;
    }

    private JPanel createScreenSelectionPanel() {
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
        return screenPanel;
    }

    private JPanel createSearchAreaPanel() {
        JPanel areaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton selectAreaBtn = new JButton("Select Area");
        JLabel areaStatusLabel = new JLabel(config.getRoi() == null ? "Default" : "Custom", SwingConstants.CENTER);

        selectAreaBtn.addActionListener(e -> {
            Rectangle roi = AreaSelector.selectArea(this, config.getScreenIndex());
            config.setRoi(roi);
            config.save();
            areaStatusLabel.setText(roi == null ? "Default" : "Custom");
        });

        areaPanel.add(selectAreaBtn);
        areaPanel.add(areaStatusLabel);
        return areaPanel;
    }

    private JPanel createVolumePanel() {
        JPanel volPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        volPanel.add(new JLabel("Volume: "));
        volumeBar = new JProgressBar(0, 100);
        volumeBar.setStringPainted(true);
        volPanel.add(volumeBar);
        return volPanel;
    }

    private JPanel createThresholdPanel() {
        JPanel threshPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        threshPanel.add(new JLabel("Splash Threshold (0.0 - 1.0): "));
        thresholdField = new JTextField(String.valueOf(config.getSplashThreshold()), 10);
        threshPanel.add(thresholdField);
        return threshPanel;
    }

    private JPanel createReactionTimePanel() {
        JPanel reactPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        reactPanel.add(new JLabel("Reaction Delay (ms): "));
        reactionField = new JTextField(String.valueOf(config.getReactionTime()), 10);
        reactPanel.add(reactionField);
        return reactPanel;
    }

    private JPanel createCastKeyPanel() {
        JPanel castKeyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        castKeyPanel.add(new JLabel("Cast Key: "));
        castKeyField = new JTextField(KeyEvent.getKeyText(NativeKeyboard.User32Ext.INSTANCE.MapVirtualKey(config.getCastKey(), 1)), 10);
        castKeyField.setEditable(false);
        castKeyField.setHorizontalAlignment(JTextField.CENTER);
        castKeyField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int vk = e.getKeyCode();
                int sc = NativeKeyboard.getScanCode(vk);
                System.out.println("[DEBUG] Setting Cast Key: " + KeyEvent.getKeyText(vk) + " (Scan Code: 0x" + Integer.toHexString(sc) + ")");
                config.setCastKey(sc);
                config.save();
                castKeyField.setText(KeyEvent.getKeyText(vk));
            }
        });
        castKeyPanel.add(castKeyField);
        return castKeyPanel;
    }

    private JPanel createInteractKeyPanel() {
        JPanel keybindPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        keybindPanel.add(new JLabel("Interact Key: "));
        interactKeyField = new JTextField(KeyEvent.getKeyText(NativeKeyboard.User32Ext.INSTANCE.MapVirtualKey(config.getInteractKey(), 1)), 10);
        interactKeyField.setEditable(false);
        interactKeyField.setHorizontalAlignment(JTextField.CENTER);
        interactKeyField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int vk = e.getKeyCode();
                int sc = NativeKeyboard.getScanCode(vk);
                System.out.println("[DEBUG] Setting Interact Key: " + KeyEvent.getKeyText(vk) + " (Scan Code: 0x" + Integer.toHexString(sc) + ")");
                config.setInteractKey(sc);
                config.save();
                interactKeyField.setText(KeyEvent.getKeyText(vk));
            }
        });
        keybindPanel.add(interactKeyField);
        return keybindPanel;
    }

    private JPanel createStopKeyPanel() {
        JPanel stopKeyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        stopKeyPanel.add(new JLabel("Stop Key: "));
        stopKeyField = new JTextField(KeyEvent.getKeyText(NativeKeyboard.User32Ext.INSTANCE.MapVirtualKey(config.getStopKey(), 1)), 10);
        stopKeyField.setEditable(false);
        stopKeyField.setHorizontalAlignment(JTextField.CENTER);
        stopKeyField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int vk = e.getKeyCode();
                int sc = NativeKeyboard.getScanCode(vk);
                System.out.println("[DEBUG] Setting Stop Key: " + KeyEvent.getKeyText(vk) + " (Scan Code: 0x" + Integer.toHexString(sc) + ")");
                config.setStopKey(sc);
                config.save();
                stopKeyField.setText(KeyEvent.getKeyText(vk));
            }
        });
        stopKeyPanel.add(stopKeyField);
        return stopKeyPanel;
    }

    private JPanel createLureTogglePanel() {
        JPanel lureRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lureCheckBox = new JCheckBox("Use Lure every ", config.isLureEnabled());
        lureRow1.add(lureCheckBox);
        lureIntervalField = new JTextField(String.valueOf(config.getLureInterval()), 4);
        lureRow1.add(lureIntervalField);
        lureRow1.add(new JLabel(" minutes"));
        return lureRow1;
    }

    private JPanel createLureKeyPanel() {
        JPanel lureKeyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
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
        return lureKeyPanel;
    }

    private JPanel createLogoutKeyPanel() {
        JPanel logoutKeyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        logoutKeyPanel.add(new JLabel("Logout Macro Key: "));
        String logoutKeyName = "None";
        if (config.getLogoutKey() != 0) {
            logoutKeyName = KeyEvent.getKeyText(NativeKeyboard.User32Ext.INSTANCE.MapVirtualKey(config.getLogoutKey(), 1));
        }
        logoutKeyField = new JTextField(logoutKeyName, 10);
        logoutKeyField.setEditable(false);
        logoutKeyField.setHorizontalAlignment(JTextField.CENTER);
        logoutKeyField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int vk = e.getKeyCode();
                int sc = NativeKeyboard.getScanCode(vk);
                System.out.println("[DEBUG] Setting Logout Key: " + KeyEvent.getKeyText(vk) + " (Scan Code: 0x" + Integer.toHexString(sc) + ")");
                config.setLogoutKey(sc);
                config.save();
                logoutKeyField.setText(KeyEvent.getKeyText(vk));
            }
        });
        logoutKeyPanel.add(logoutKeyField);
        return logoutKeyPanel;
    }

    private JPanel createShowDetectionPanel() {
        JPanel showDetectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        showDetectionCheckBox = new JCheckBox("Show detection point (red circle)", config.isShowDetectionPoint());
        showDetectionCheckBox.addActionListener(e -> {
            config.setShowDetectionPoint(showDetectionCheckBox.isSelected());
            config.save();
        });
        showDetectionPanel.add(showDetectionCheckBox);
        return showDetectionPanel;
    }

    private JPanel createLogoutAfterFullBagPanel() {
        JPanel logoutPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        logoutAfterFullBagCheckBox = new JCheckBox("Logout after full bag", config.isLogoutAfterFullBag());
        logoutAfterFullBagCheckBox.addActionListener(e -> {
            config.setLogoutAfterFullBag(logoutAfterFullBagCheckBox.isSelected());
            config.save();
        });
        logoutPanel.add(logoutAfterFullBagCheckBox);
        return logoutPanel;
    }

    private JPanel createTestDetectionPanel() {
        JPanel testPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton testBtn = new JButton("Test Bobber Detection");
        testBtn.addActionListener(e -> {
            BobberFinder finder = new BobberFinder(config);
            Point p = finder.findBobber();
            if (p != null) {
                if (config.isShowDetectionPoint()) {
                    DetectionOverlay.showAt(p);
                }
                int targetX = p.x + Humanizer.getGaussianInt(10, 5);
                int targetY = p.y - Humanizer.getGaussianInt(0, 5);
                NativeMouse.mouseMove(targetX, targetY, config.getScreenIndex());
                JOptionPane.showMessageDialog(this, "Bobber found at " + p.x + ", " + p.y + "\nGaussian target: " + targetX + ", " + targetY + "\nMouse moved to target.");
            } else {
                JOptionPane.showMessageDialog(this, "Bobber NOT found.");
            }
        });
        testPanel.add(testBtn);
        return testPanel;
    }

    private JPanel createControlsPanel() {
        startStopButton = new JButton("Start Fishing");
        startStopButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        startStopButton.addActionListener(this::toggleBot);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(startStopButton, BorderLayout.CENTER);
        return bottomPanel;
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
            int runLimit = Integer.parseInt(runTimeField.getText());

            config.setShortBreakIntervalMin(Integer.parseInt(shortBreakIntervalMinField.getText()));
            config.setShortBreakIntervalMax(Integer.parseInt(shortBreakIntervalMaxField.getText()));
            config.setShortBreakDurationMin(Integer.parseInt(shortBreakDurationMinField.getText()));
            config.setShortBreakDurationMax(Integer.parseInt(shortBreakDurationMaxField.getText()));
            config.setLongBreakIntervalMin(Integer.parseInt(longBreakIntervalMinField.getText()));
            config.setLongBreakIntervalMax(Integer.parseInt(longBreakIntervalMaxField.getText()));
            config.setLongBreakDurationMin(Integer.parseInt(longBreakDurationMinField.getText()));
            config.setLongBreakDurationMax(Integer.parseInt(longBreakDurationMaxField.getText()));

            System.out.println("[DEBUG] Applying configuration - Threshold: " + thresh + ", Reaction: " + react + ", Lure: " + lureCheckBox.isSelected() + " every " + lureInterval + " mins, Limit: " + runLimit + " mins");
            config.setSplashThreshold(thresh);
            config.setReactionTime(react);
            config.setLureEnabled(lureCheckBox.isSelected());
            config.setLureInterval(lureInterval);
            config.setRunTimeLimit(runLimit);
            config.save();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid configuration values. Please check all numeric fields.");
            return;
        }

        thresholdField.setEnabled(false);
        reactionField.setEnabled(false);
        castKeyField.setEnabled(false);
        interactKeyField.setEnabled(false);
        stopKeyField.setEnabled(false);
        runTimeField.setEnabled(false);
        shortBreakIntervalMinField.setEnabled(false);
        shortBreakIntervalMaxField.setEnabled(false);
        shortBreakDurationMinField.setEnabled(false);
        shortBreakDurationMaxField.setEnabled(false);
        longBreakIntervalMinField.setEnabled(false);
        longBreakIntervalMaxField.setEnabled(false);
        longBreakDurationMinField.setEnabled(false);
        longBreakDurationMaxField.setEnabled(false);
        deviceBox.setEnabled(false);
        screenBox.setEnabled(false);
        lureCheckBox.setEnabled(false);
        showDetectionCheckBox.setEnabled(false);
        logoutAfterFullBagCheckBox.setEnabled(false);
        lureIntervalField.setEnabled(false);
        lureKeyField.setEnabled(false);
        logoutKeyField.setEnabled(false);
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

