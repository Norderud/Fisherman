package no.kess.utility;

import no.kess.utility.ui.AppUI;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Set nice Look and Feel
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(AppUI::new);
    }
}
