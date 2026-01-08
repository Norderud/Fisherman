package no.kess.fisherman;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class FishermanUI extends JFrame {
    private final FishermanConfig config;
    private final AudioMonitor audioMonitor;
    private final BotEngine botEngine;

    private JLabel statusLabel;
    private JLabel fishCaughtLabel;
    private JProgressBar volumeBar;
    private JTextField thresholdField;
    private JTextField reactionField;
    private JButton startStopButton;

    public FishermanUI() {
        super("Fisherman");
        this.config = new FishermanConfig();

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

        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setVisible(true);
    }

    private void updateVolumeUI(double vol) {
        SwingUtilities.invokeLater(() -> {
            volumeBar.setValue((int) (vol * 100));
            volumeBar.setString(String.format("%.4f", vol));

            if (botEngine.isRunning()) {
                fishCaughtLabel.setText("Fish Caught: " + config.getFishCaught());
            }
        });
    }

    private void updateBotStatusUI(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Status: " + status));
    }

    private void onBotStoppedUI() {
        SwingUtilities.invokeLater(() -> {
            thresholdField.setEnabled(true);
            reactionField.setEnabled(true);
            startStopButton.setText("Start Fishing");
        });
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10));

        // Top Panel: Stats
        JPanel statsPanel = new JPanel(new GridLayout(2, 1));
        statusLabel = new JLabel("Status: Stopped", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        fishCaughtLabel = new JLabel("Fish Caught: " + config.getFishCaught(), SwingConstants.CENTER);
        statsPanel.add(statusLabel);
        statsPanel.add(fishCaughtLabel);
        add(statsPanel, BorderLayout.NORTH);

        // Center Panel: Volume & Config
        JPanel centerPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Volume
        JPanel volPanel = new JPanel(new BorderLayout());
        volPanel.add(new JLabel("Volume: "), BorderLayout.WEST);
        volumeBar = new JProgressBar(0, 100);
        volumeBar.setStringPainted(true);
        volPanel.add(volumeBar, BorderLayout.CENTER);
        centerPanel.add(volPanel);

        // Threshold
        JPanel threshPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        threshPanel.add(new JLabel("Splash Threshold (0.0 - 1.0): "));
        thresholdField = new JTextField(String.valueOf(config.getSplashThreshold()), 10);
        threshPanel.add(thresholdField);
        centerPanel.add(threshPanel);

        // Reaction Time
        JPanel reactPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        reactPanel.add(new JLabel("Reaction Delay (ms): "));
        reactionField = new JTextField(String.valueOf(config.getReactionTime()), 10);
        reactPanel.add(reactionField);
        centerPanel.add(reactPanel);

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
            config.setSplashThreshold(thresh);
            config.setReactionTime(react);
            config.save();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid Threshold or Reaction Time Value");
            return;
        }

        thresholdField.setEnabled(false);
        reactionField.setEnabled(false);
        startStopButton.setText("Stop Fishing");
        botEngine.start();
    }
}

