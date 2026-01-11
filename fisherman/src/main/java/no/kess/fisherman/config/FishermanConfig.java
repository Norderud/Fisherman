package no.kess.fisherman.config;

import no.kess.fisherman.input.NativeKeyboard;

import java.awt.*;
import java.io.*;
import java.util.Properties;

public class FishermanConfig {
    private static final String CONFIG_FILE = "fisherman.properties";
    private final Properties properties = new Properties();

    public FishermanConfig() {
        load();
    }

    public void load() {
        File f = new File(CONFIG_FILE);
        if (f.exists()) {
            try (InputStream input = new FileInputStream(f)) {
                properties.load(input);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void save() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "Fisherman Bot Configuration");
            System.out.println("[DEBUG] Configuration saved to " + CONFIG_FILE);
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public double getSplashThreshold() {
        return getDoubleProperty("splashThreshold", 0.15);
    }

    public void setSplashThreshold(double val) {
        properties.setProperty("splashThreshold", String.valueOf(val));
    }

    public int getReactionTime() {
        return getIntProperty("reactionTime", 350);
    }

    public void setReactionTime(int val) {
        properties.setProperty("reactionTime", String.valueOf(val));
    }

    public int getFishCaught() {
        return getIntProperty("fishCaught", 0);
    }

    public void setFishCaught(int val) {
        properties.setProperty("fishCaught", String.valueOf(val));
    }

    public void incrementFishCaught() {
        setFishCaught(getFishCaught() + 1);
    }

    public String getAudioDevice() {
        return properties.getProperty("audioDevice", "");
    }

    public void setAudioDevice(String name) {
        properties.setProperty("audioDevice", name);
    }

    public int getInteractKey() {
        // Fallback to SCANCODE_F10 if not set
        return getIntProperty("interactKey", getIntProperty("actionKey", NativeKeyboard.SCANCODE_F10));
    }

    public void setInteractKey(int scanCode) {
        properties.setProperty("interactKey", String.valueOf(scanCode));
        // Remove old property name
        properties.remove("actionKey");
    }

    public int getCastKey() {
        // Fallback to interactKey if not set
        return getIntProperty("castKey", getInteractKey());
    }

    public void setCastKey(int scanCode) {
        properties.setProperty("castKey", String.valueOf(scanCode));
    }

    public int getStopKey() {
        return getIntProperty("stopKey", NativeKeyboard.SCANCODE_SPACE);
    }

    public void setStopKey(int scanCode) {
        properties.setProperty("stopKey", String.valueOf(scanCode));
    }

    public int getScreenIndex() {
        return getIntProperty("screenIndex", 0);
    }

    public void setScreenIndex(int index) {
        properties.setProperty("screenIndex", String.valueOf(index));
    }

    public Rectangle getRoi() {
        String x = properties.getProperty("roiX");
        String y = properties.getProperty("roiY");
        String w = properties.getProperty("roiW");
        String h = properties.getProperty("roiH");
        if (x != null && y != null && w != null && h != null) {
            try {
                return new Rectangle(
                        Integer.parseInt(x),
                        Integer.parseInt(y),
                        Integer.parseInt(w),
                        Integer.parseInt(h)
                );
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public void setRoi(Rectangle roi) {
        if (roi == null) {
            properties.remove("roiX");
            properties.remove("roiY");
            properties.remove("roiW");
            properties.remove("roiH");
        } else {
            properties.setProperty("roiX", String.valueOf(roi.x));
            properties.setProperty("roiY", String.valueOf(roi.y));
            properties.setProperty("roiW", String.valueOf(roi.width));
            properties.setProperty("roiH", String.valueOf(roi.height));
        }
    }

    public boolean isLureEnabled() {
        return Boolean.parseBoolean(properties.getProperty("lureEnabled", "false"));
    }

    public void setLureEnabled(boolean enabled) {
        properties.setProperty("lureEnabled", String.valueOf(enabled));
    }

    public int getLureKey() {
        return getIntProperty("lureKey", 0);
    }

    public void setLureKey(int scanCode) {
        properties.setProperty("lureKey", String.valueOf(scanCode));
    }

    public int getLureInterval() {
        return getIntProperty("lureInterval", 10);
    }

    public void setLureInterval(int minutes) {
        properties.setProperty("lureInterval", String.valueOf(minutes));
    }

    public boolean isShowDetectionPoint() {
        return Boolean.parseBoolean(properties.getProperty("showDetectionPoint", "false"));
    }

    public void setShowDetectionPoint(boolean show) {
        properties.setProperty("showDetectionPoint", String.valueOf(show));
    }

    private int getIntProperty(String key, int defaultValue) {
        String val = properties.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double getDoubleProperty(String key, double defaultValue) {
        String val = properties.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
