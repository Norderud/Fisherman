package no.kess.fisherman;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import java.util.function.Consumer;

public class AudioMonitor {
    private final AudioSensor audioSensor = new AudioSensor();
    private final Consumer<Double> volumeListener;
    private volatile double currentVolume = 0.0;
    private volatile boolean monitoring = true;

    public AudioMonitor(Consumer<Double> volumeListener) {
        this.volumeListener = volumeListener;
    }

    public void start() {
        new Thread(this::monitorLoop).start();
    }

    public void stop() {
        monitoring = false;
    }

    public double getCurrentVolume() {
        return currentVolume;
    }

    private void monitorLoop() {
        try {
            // Use 16-bit Mono, Little-Endian
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            TargetDataLine line = AudioSystem.getTargetDataLine(format);
            line.open();
            line.start();

            while (monitoring) {
                double vol = audioSensor.getCurrentVolume(line);
                currentVolume = vol;
                if (volumeListener != null) {
                    volumeListener.accept(vol);
                }
            }

            line.stop();
            line.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
