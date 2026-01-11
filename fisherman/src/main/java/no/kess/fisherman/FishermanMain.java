package no.kess.fisherman;

import no.kess.fisherman.ui.FishermanUI;

import javax.swing.*;

public class FishermanMain {
    public static void main(String[] args) {
        // Set nice Look and Feel
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(FishermanUI::new);
    }
}
