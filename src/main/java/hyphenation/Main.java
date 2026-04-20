package hyphenation;

import hyphenation.ui.LineBreakerFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Точка входа в приложение.
 * Запускает графический интерфейс Swing.
 */
public class Main {

    /**
     * Запускает окно приложения.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            new LineBreakerFrame().setVisible(true);
        });
    }
}