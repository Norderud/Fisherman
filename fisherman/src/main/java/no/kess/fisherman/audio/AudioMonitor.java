package no.kess.fisherman.audio;

import javax.sound.sampled.*;
import java.util.function.Consumer;

public class AudioMonitor {
    private final AudioSensor audioSensor = new AudioSensor();
    private final Consumer<Double> volumeListener;
    private volatile double currentVolume = 0.0;
    private volatile boolean monitoring = false;
    private Mixer.Info selectedMixerInfo;
    private Thread monitorThread;
    private volatile TargetDataLine activeLine;

    public AudioMonitor(Consumer<Double> volumeListener) {
        this.volumeListener = volumeListener;
    }

    public void setMixerInfo(Mixer.Info mixerInfo) {
        this.selectedMixerInfo = mixerInfo;
    }

    public synchronized void start() {
        if (monitoring) return;
        monitoring = true;
        monitorThread = new Thread(this::monitorLoop, "AudioMonitorThread");
        monitorThread.start();
    }

    public synchronized void stop() {
        monitoring = false;
        if (activeLine != null) {
            activeLine.close();
        }
    }

    public double getCurrentVolume() {
        return currentVolume;
    }

    private void monitorLoop() {
        try {
            // Use 16-bit Mono, Little-Endian
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            TargetDataLine line;
            if (selectedMixerInfo != null) {
                Mixer mixer = AudioSystem.getMixer(selectedMixerInfo);
                line = (TargetDataLine) mixer.getLine(info);
            } else {
                line = AudioSystem.getTargetDataLine(format);
            }

            activeLine = line;
            line.open(format);
            line.start();

            while (monitoring) {
                double vol = audioSensor.getCurrentVolume(line);
                currentVolume = vol;
                if (volumeListener != null) {
                    volumeListener.accept(vol);
                }
            }
        } catch (Exception e) {
            if (monitoring) {
                e.printStackTrace();
            }
        } finally {
            TargetDataLine line = activeLine;
            if (line != null) {
                line.stop();
                line.close();
            }
            activeLine = null;
            monitoring = false;
        }
    }
}
