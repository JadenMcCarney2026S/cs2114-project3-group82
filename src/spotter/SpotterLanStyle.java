package spotter;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.JTextComponent;

/** Shared colors and controls for the Spotter LAN screens. */
final class SpotterLanStyle
{
    static final Color BACKGROUND = new Color(245, 244, 237);
    static final Color SURFACE = Color.WHITE;
    static final Color INK = new Color(22, 35, 28);
    static final Color MUTED = new Color(73, 94, 82);
    static final Color ACCENT = new Color(212, 247, 112);
    private static final Color OUTLINE = new Color(118, 135, 122);

    private SpotterLanStyle()
    {
    }

    static JLabel heading(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(INK);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 27f));
        return label;
    }

    static void button(JButton button, boolean primary)
    {
        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setBackground(primary ? ACCENT : SURFACE);
        button.setForeground(INK);
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.addChangeListener(event -> {
            boolean enabled = button.isEnabled();
            button.setBackground(!enabled || !primary ? SURFACE : ACCENT);
            button.setForeground(enabled ? INK : MUTED);
        });
        button.setBorder(buttonBorder(false, primary));
        button.addFocusListener(new java.awt.event.FocusAdapter()
        {
            @Override
            public void focusGained(java.awt.event.FocusEvent event)
            {
                button.setBorder(buttonBorder(true, primary));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent event)
            {
                button.setBorder(buttonBorder(false, primary));
            }
        });
    }

    private static javax.swing.border.Border buttonBorder(boolean focused, boolean primary)
    {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(focused ? INK : primary ? ACCENT : OUTLINE, 2),
            BorderFactory.createEmptyBorder(8, 14, 8, 14));
    }

    static void input(JTextComponent input)
    {
        input.setBackground(SURFACE);
        input.setForeground(INK);
        input.setCaretColor(INK);
        input.setDisabledTextColor(MUTED);
        input.setSelectionColor(ACCENT);
        input.setSelectedTextColor(INK);
        input.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(OUTLINE),
            BorderFactory.createEmptyBorder(7, 9, 7, 9)));
    }

    static void alert(Component owner, String title, String detail, String guidance)
    {
        JOptionPane.showMessageDialog(owner, alertContent(detail, guidance), title,
            JOptionPane.WARNING_MESSAGE);
    }

    static JPanel alertContent(String detail, String guidance)
    {
        JPanel panel = new JPanel(new java.awt.BorderLayout(0, 8));
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        panel.add(alertText(detail, INK), java.awt.BorderLayout.CENTER);
        panel.add(alertText(guidance, MUTED), java.awt.BorderLayout.SOUTH);
        return panel;
    }

    private static JTextArea alertText(String text, Color color)
    {
        JTextArea area = new JTextArea(text, 0, 34);
        area.setBackground(BACKGROUND);
        area.setForeground(color);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setSize(area.getPreferredSize().width, Short.MAX_VALUE);
        return area;
    }
}
