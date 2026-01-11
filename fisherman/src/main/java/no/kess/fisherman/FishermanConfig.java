package no.kess.fisherman;

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
        String val = properties.getProperty("splashThreshold", "0.15");
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0.15;
        }
    }

    public void setSplashThreshold(double val) {
        properties.setProperty("splashThreshold", String.valueOf(val));
    }

    public int getReactionTime() {
        String val = properties.getProperty("reactionTime", "350");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 350;
        }
    }

    public void setReactionTime(int val) {
        properties.setProperty("reactionTime", String.valueOf(val));
    }

    public int getFishCaught() {
        String val = properties.getProperty("fishCaught", "0");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
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

    public int getActionKey() {
        String val = properties.getProperty("actionKey", properties.getProperty("interactKey", String.valueOf(NativeKeyboard.SCANCODE_F10)));
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return NativeKeyboard.SCANCODE_F10;
        }
    }

    public void setActionKey(int scanCode) {
        properties.setProperty("actionKey", String.valueOf(scanCode));
    }

    public int getScreenIndex() {
        String val = properties.getProperty("screenIndex", "0");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
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
        String val = properties.getProperty("lureKey", "0");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void setLureKey(int scanCode) {
        properties.setProperty("lureKey", String.valueOf(scanCode));
    }

    public int getLureInterval() {
        String val = properties.getProperty("lureInterval", "10");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    public void setLureInterval(int minutes) {
        properties.setProperty("lureInterval", String.valueOf(minutes));
    }
}
