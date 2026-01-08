package no.kess.fisherman;

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
}
