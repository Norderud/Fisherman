package no.kess.fisherman;

import javax.sound.sampled.*;

public class AudioSensor {
    // Threshold must be calibrated by the user!
    // 0.0 = Silent, 1.0 = Max Volume
    public double getCurrentVolume(TargetDataLine line) {
        byte[] buffer = new byte[4096];
        int bytesRead = line.read(buffer, 0, buffer.length);
        if (bytesRead == -1) return 0.0;

        // Calculate RMS (Root Mean Square) amplitude
        long sum = 0;
        for (int i = 0; i < bytesRead; i += 2) {
            // Convert byte pair to short (16-bit audio)
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xff));
            sum += sample * sample;
        }

        double rms = Math.sqrt(sum / (bytesRead / 2.0));
        return rms / 32768.0; // Normalize to 0.0 - 1.0
    }
}

