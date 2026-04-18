package hyphenation.ui;

import hyphenation.ui.LineBreakerApplicationService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class LineBreakerFrame extends JFrame {

    private final LineBreakerApplicationService service;

    private final JTextArea inputArea;
    private final JTextArea outputArea;

    private final JComboBox<String> fontComboBox;
    private final JSpinner fontSizeSpinner;
    private final JSpinner maxWidthSpinner;
    private final JComboBox<String> localeComboBox;

    public LineBreakerFrame() {
        super("Adaptive Line Breaker");

        this.service = new LineBreakerApplicationService();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder("Параметры"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        String[] fontNames = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();

        fontComboBox = new JComboBox<>(fontNames);
        fontComboBox.setSelectedItem("Arial");

        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(18, 6, 200, 1));
        maxWidthSpinner = new JSpinner(new SpinnerNumberModel(140.0, 20.0, 5000.0, 5.0));

        localeComboBox = new JComboBox<>(new String[]{"ru", "en"});
        localeComboBox.setSelectedItem("ru");

        JButton breakButton = new JButton("Разбить");
        breakButton.addActionListener(e -> runBreak());

        gbc.gridx = 0;
        gbc.gridy = 0;
        controlPanel.add(new JLabel("Шрифт:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        controlPanel.add(fontComboBox, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        controlPanel.add(new JLabel("Размер:"), gbc);

        gbc.gridx = 3;
        controlPanel.add(fontSizeSpinner, gbc);

        gbc.gridx = 4;
        controlPanel.add(new JLabel("Макс. ширина:"), gbc);

        gbc.gridx = 5;
        controlPanel.add(maxWidthSpinner, gbc);

        gbc.gridx = 6;
        controlPanel.add(new JLabel("Язык:"), gbc);

        gbc.gridx = 7;
        controlPanel.add(localeComboBox, gbc);

        gbc.gridx = 8;
        controlPanel.add(breakButton, gbc);

        inputArea = new JTextArea();
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setMargin(new Insets(8, 8, 8, 8));
        inputArea.setFont(new Font("Arial", Font.PLAIN, 18));
        inputArea.setText("За окном моросил мелкий дождь. Анна сидела у окна и молча смотрела вдаль.");

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setMargin(new Insets(8, 8, 8, 8));
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 15));

        JScrollPane inputScrollPane = new JScrollPane(inputArea);
        inputScrollPane.setBorder(BorderFactory.createTitledBorder("Исходный текст"));

        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        outputScrollPane.setBorder(BorderFactory.createTitledBorder("Результат"));

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                inputScrollPane,
                outputScrollPane
        );
        splitPane.setResizeWeight(0.5);
        splitPane.setPreferredSize(new Dimension(1000, 550));

        add(controlPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        runBreak();
    }

    private void runBreak() {
        try {
            String text = inputArea.getText();
            String fontName = (String) fontComboBox.getSelectedItem();
            int fontSize = (Integer) fontSizeSpinner.getValue();
            double maxWidthValue = (Double) maxWidthSpinner.getValue();
            String language = (String) localeComboBox.getSelectedItem();

            if (text == null) {
                text = "";
            }

            Font selectedFont = new Font(fontName, Font.PLAIN, fontSize);
            inputArea.setFont(selectedFont);

            String resultText = service.process(
                    text,
                    fontName,
                    fontSize,
                    (float) maxWidthValue,
                    language
            );

            outputArea.setText(resultText);
            outputArea.setCaretPosition(0);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}